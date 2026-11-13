#include "kotlin_app_host.h"

#include "skiko_host.h"
#include "../../core/skiko_sdl_core.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <random>
#include <stdexcept>
#include <utility>

#include <wasmtime/anyref.h>
#include <wasmtime/arrayref.h>
#include <wasmtime/externref.h>
#include <wasmtime/global.h>
#include <wasmtime/tag.h>
#include <wasmtime/types/val.h>

namespace {

std::string Name(const wasm_name_t* name) {
    return name == nullptr ? std::string() : std::string(name->data, name->size);
}

std::string ErrorMessage(wasmtime_error_t* error) {
    wasm_byte_vec_t message;
    wasmtime_error_message(error, &message);
    std::string result(message.data, message.size);
    wasm_byte_vec_delete(&message);
    wasmtime_error_delete(error);
    return result;
}

std::string TrapMessage(wasm_trap_t* trap) {
    wasm_byte_vec_t message;
    wasm_trap_message(trap, &message);
    std::string result(message.data, message.size);
    wasm_byte_vec_delete(&message);
    wasm_trap_delete(trap);
    return result;
}

void Check(wasmtime_error_t* error, const std::string& operation) {
    if (error != nullptr) {
        throw std::runtime_error(operation + ": " + ErrorMessage(error));
    }
}

enum class ObjectKind {
    Generic,
    Error,
    Promise,
    Performance,
    Response,
    Blob,
    ArrayBuffer,
    Document,
    Element,
    NodeList,
    Description,
    Canvas,
    Gl,
    GlContext,
    HtmlCanvasClass,
    WebGlClass,
    FinalizationRegistry,
    ContextAttributes,
    WeakRef,
    WeakMap,
    Array,
    Window,
    Location,
    History,
};

struct XmlAttribute {
    std::string name;
    std::string local_name;
    std::string value;
};

struct XmlNode {
    std::string tag_name;
    std::string local_name;
    std::string namespace_uri;
    std::string text_content;
    std::vector<XmlAttribute> attributes;
    std::vector<std::shared_ptr<XmlNode>> children;
};

std::string ExtractLocalName(const std::string& name) {
    auto colon = name.find(':');
    if (colon != std::string::npos && colon + 1 < name.size()) {
        return name.substr(colon + 1);
    }
    return name;
}

std::shared_ptr<XmlNode> ParseXml(const std::string& xml) {
    if (xml.empty()) return nullptr;
    std::vector<std::shared_ptr<XmlNode>> stack;
    std::shared_ptr<XmlNode> root = nullptr;

    size_t i = 0;
    while (i < xml.size()) {
        size_t tag_start = xml.find('<', i);
        if (tag_start == std::string::npos) {
            if (!stack.empty() && i < xml.size()) {
                stack.back()->text_content += xml.substr(i);
            }
            break;
        }

        if (!stack.empty() && tag_start > i) {
            stack.back()->text_content += xml.substr(i, tag_start - i);
        }

        // Skip comments <!-- ... -->
        if (tag_start + 4 <= xml.size() && xml.compare(tag_start, 4, "<!--") == 0) {
            size_t comment_end = xml.find("-->", tag_start + 4);
            if (comment_end != std::string::npos) {
                i = comment_end + 3;
                continue;
            }
            break;
        }

        // Skip processing instructions <? ... ?> or <! ... >
        if (tag_start + 2 <= xml.size() && (xml.compare(tag_start, 2, "<?") == 0 || xml.compare(tag_start, 2, "<!") == 0)) {
            size_t pi_end = xml.find('>', tag_start + 2);
            if (pi_end != std::string::npos) {
                i = pi_end + 1;
                continue;
            }
            break;
        }

        // Closing tag </tag>
        if (tag_start + 1 < xml.size() && xml[tag_start + 1] == '/') {
            size_t tag_end = xml.find('>', tag_start + 2);
            if (tag_end != std::string::npos) {
                if (!stack.empty()) {
                    stack.pop_back();
                }
                i = tag_end + 1;
                continue;
            }
            break;
        }

        // Opening or self-closing tag <tag ...>
        size_t tag_end = tag_start + 1;
        bool in_quote = false;
        char quote_char = 0;
        while (tag_end < xml.size()) {
            char c = xml[tag_end];
            if (in_quote) {
                if (c == quote_char) in_quote = false;
            } else {
                if (c == '"' || c == '\'') {
                    in_quote = true;
                    quote_char = c;
                } else if (c == '>') {
                    break;
                }
            }
            tag_end++;
        }
        if (tag_end >= xml.size()) break;

        bool self_closing = false;
        size_t content_end = tag_end;
        if (content_end > tag_start + 1 && xml[content_end - 1] == '/') {
            self_closing = true;
            content_end--;
        }

        std::string tag_content = xml.substr(tag_start + 1, content_end - (tag_start + 1));
        size_t pos = 0;
        while (pos < tag_content.size() && std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
        size_t name_start = pos;
        while (pos < tag_content.size() && !std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
        std::string tag_name = tag_content.substr(name_start, pos - name_start);

        if (!tag_name.empty()) {
            auto node = std::make_shared<XmlNode>();
            node->tag_name = tag_name;
            node->local_name = ExtractLocalName(tag_name);

            while (pos < tag_content.size()) {
                while (pos < tag_content.size() && std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
                if (pos >= tag_content.size()) break;

                size_t attr_name_start = pos;
                while (pos < tag_content.size() && tag_content[pos] != '=' && !std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
                std::string attr_name = tag_content.substr(attr_name_start, pos - attr_name_start);

                while (pos < tag_content.size() && std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
                std::string attr_val;
                if (pos < tag_content.size() && tag_content[pos] == '=') {
                    ++pos;
                    while (pos < tag_content.size() && std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
                    if (pos < tag_content.size() && (tag_content[pos] == '"' || tag_content[pos] == '\'')) {
                        char quote = tag_content[pos++];
                        size_t val_start = pos;
                        while (pos < tag_content.size() && tag_content[pos] != quote) ++pos;
                        attr_val = tag_content.substr(val_start, pos - val_start);
                        if (pos < tag_content.size() && tag_content[pos] == quote) ++pos;
                    } else {
                        size_t val_start = pos;
                        while (pos < tag_content.size() && !std::isspace(static_cast<unsigned char>(tag_content[pos]))) ++pos;
                        attr_val = tag_content.substr(val_start, pos - val_start);
                    }
                }

                if (!attr_name.empty()) {
                    XmlAttribute attr;
                    attr.name = attr_name;
                    attr.local_name = ExtractLocalName(attr_name);
                    attr.value = attr_val;
                    if (attr.name == "xmlns" || attr.name.rfind("xmlns:", 0) == 0) {
                        if (node->namespace_uri.empty()) {
                            node->namespace_uri = attr.value;
                        }
                    }
                    node->attributes.push_back(std::move(attr));
                }
            }

            if (!stack.empty()) {
                stack.back()->children.push_back(node);
            } else if (!root) {
                root = node;
            }

            if (!self_closing) {
                stack.push_back(node);
            }
        }

        i = tag_end + 1;
    }

    return root;
}

std::u16string Utf8ToUtf16(const std::string& utf8) {
    std::u16string utf16;
    size_t i = 0;
    while (i < utf8.size()) {
        uint32_t cp = 0;
        unsigned char c = static_cast<unsigned char>(utf8[i++]);
        if (c < 0x80) {
            cp = c;
        } else if ((c & 0xE0) == 0xC0) {
            if (i >= utf8.size()) break;
            cp = (c & 0x1F) << 6;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F);
        } else if ((c & 0xF0) == 0xE0) {
            if (i + 1 >= utf8.size()) break;
            cp = (c & 0x0F) << 12;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F) << 6;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F);
        } else if ((c & 0xF8) == 0xF0) {
            if (i + 2 >= utf8.size()) break;
            cp = (c & 0x07) << 18;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F) << 12;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F) << 6;
            cp |= (static_cast<unsigned char>(utf8[i++]) & 0x3F);
        }

        if (cp <= 0xFFFF) {
            utf16.push_back(static_cast<char16_t>(cp));
        } else if (cp <= 0x10FFFF) {
            cp -= 0x10000;
            utf16.push_back(static_cast<char16_t>((cp >> 10) + 0xD800));
            utf16.push_back(static_cast<char16_t>((cp & 0x3FF) + 0xDC00));
        }
    }
    return utf16;
}

std::string Utf16ToUtf8(const std::u16string& utf16) {
    std::string utf8;
    size_t i = 0;
    while (i < utf16.size()) {
        uint32_t cp = static_cast<uint16_t>(utf16[i++]);
        if (cp >= 0xD800 && cp <= 0xDBFF && i < utf16.size()) {
            uint32_t low = static_cast<uint16_t>(utf16[i]);
            if (low >= 0xDC00 && low <= 0xDFFF) {
                cp = ((cp - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
                i++;
            }
        }

        if (cp < 0x80) {
            utf8.push_back(static_cast<char>(cp));
        } else if (cp < 0x800) {
            utf8.push_back(static_cast<char>(0xC0 | (cp >> 6)));
            utf8.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            utf8.push_back(static_cast<char>(0xE0 | (cp >> 12)));
            utf8.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            utf8.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else {
            utf8.push_back(static_cast<char>(0xF0 | (cp >> 18)));
            utf8.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
            utf8.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            utf8.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        }
    }
    return utf8;
}

std::vector<uint8_t> ReadResourceFile(const std::string& path_str) {
    if (path_str.empty()) return {};
    std::string clean_path = path_str;
    if (clean_path.rfind("./", 0) == 0) {
        clean_path = clean_path.substr(2);
    } else if (clean_path.rfind("/", 0) == 0) {
        clean_path = clean_path.substr(1);
    }

    std::vector<std::string> candidate_bases = {
        "",
        "build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "benchmarks/multiplatform/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "../../../../../benchmarks/multiplatform/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "../../../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "../../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/",
        "../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/"
    };

    for (const auto& base : candidate_bases) {
        std::string full_path = base + clean_path;
        std::ifstream file(full_path, std::ios::binary | std::ios::ate);
        if (file.is_open()) {
            std::streamsize size = file.tellg();
            if (size >= 0) {
                file.seekg(0, std::ios::beg);
                std::vector<uint8_t> buffer(static_cast<size_t>(size));
                if (file.read(reinterpret_cast<char*>(buffer.data()), size)) {
                    return buffer;
                }
            }
        }
    }
    return {};
}

} // namespace

KotlinAppHost::Callback::~Callback() {
    if (!wasmtime_anyref_is_null(&function)) {
        wasmtime_anyref_unroot(&function);
    }
}

struct KotlinAppHost::HostValue {
    enum class Kind { String, Number, Boolean, Object, Callback, Poster };
    Kind kind = Kind::Object;
    std::u16string u16_string;
    mutable std::string string;
    mutable bool string_valid = false;
    double number = 0;
    bool boolean = false;
    ObjectKind object = ObjectKind::Generic;
    int id = 0;
    std::shared_ptr<Callback> callback;
    std::shared_ptr<Callback> target_callback;
    std::vector<std::string> string_array;
    wasmtime_val_t target_val{};
    bool has_target_val = false;
    wasmtime_val_t kotlin_exception{};
    bool has_kotlin_exception = false;
    std::vector<uint8_t> byte_buffer;
    HostValue* promise_result = nullptr;
    bool promise_resolved = false;
    std::shared_ptr<XmlNode> xml_node;
    std::vector<std::shared_ptr<XmlNode>> xml_node_list;

    ~HostValue() {
        if (has_target_val) {
            wasmtime_val_unroot(&target_val);
        }
        if (has_kotlin_exception) {
            wasmtime_val_unroot(&kotlin_exception);
        }
    }

    static HostValue* String(std::string value) {
        auto* result = new HostValue;
        result->kind = Kind::String;
        result->u16_string = Utf8ToUtf16(value);
        result->string = std::move(value);
        result->string_valid = true;
        return result;
    }
    static HostValue* U16String(std::u16string value) {
        auto* result = new HostValue;
        result->kind = Kind::String;
        result->string = Utf16ToUtf8(value);
        result->u16_string = std::move(value);
        result->string_valid = true;
        return result;
    }
    static HostValue* Number(double value) {
        auto* result = new HostValue;
        result->kind = Kind::Number;
        result->number = value;
        return result;
    }
    static HostValue* BooleanValue(bool value) {
        auto* result = new HostValue;
        result->kind = Kind::Boolean;
        result->boolean = value;
        return result;
    }
    static HostValue* Object(ObjectKind kind, int id = 0) {
        auto* result = new HostValue;
        result->object = kind;
        result->id = id;
        return result;
    }
    static HostValue* StringArray(std::vector<std::string> items) {
        auto* result = new HostValue;
        result->object = ObjectKind::Array;
        result->string_array = std::move(items);
        return result;
    }
    static HostValue* WeakRefValue(const wasmtime_val_t& target) {
        auto* result = new HostValue;
        result->object = ObjectKind::WeakRef;
        wasmtime_val_clone(&target, &result->target_val);
        result->has_target_val = true;
        return result;
    }
    static HostValue* CallbackValue(std::shared_ptr<Callback> value) {
        auto* result = new HostValue;
        result->kind = Kind::Callback;
        result->callback = std::move(value);
        return result;
    }
    static HostValue* Poster(std::shared_ptr<Callback> target) {
        auto* result = new HostValue;
        result->kind = Kind::Poster;
        result->target_callback = std::move(target);
        return result;
    }
};

struct KotlinAppHost::Binding {
    KotlinAppHost* host;
    std::string module;
    std::string field;
    std::string name;
};

KotlinAppHost::KotlinAppHost(
    wasmtime_context_t* context,
    SkikoHost* skiko,
    const std::string& benchmark_args,
    skiko::core::SkikoSdlWindow* window
)
    : context_(context),
      skiko_(skiko),
      benchmark_args_(benchmark_args),
      window_(window) {}

KotlinAppHost::~KotlinAppHost() = default;

void KotlinAppHost::DeleteHostValue(void* value) {
    delete static_cast<HostValue*>(value);
}

void KotlinAppHost::DeleteBinding(void* value) {
    delete static_cast<Binding*>(value);
}

void KotlinAppHost::SetExternResult(
    wasmtime_val_t* result,
    HostValue* value
) {
    result->kind = WASMTIME_EXTERNREF;
    if (!wasmtime_externref_new(context_, value, DeleteHostValue,
                                &result->of.externref)) {
        delete value;
        throw std::runtime_error("Could not allocate an externref");
    }
}

KotlinAppHost::HostValue* KotlinAppHost::GetHostValue(
    const wasmtime_val_t& value
) const {
    if (value.kind != WASMTIME_EXTERNREF ||
        wasmtime_externref_is_null(&value.of.externref)) {
        return nullptr;
    }
    return static_cast<HostValue*>(
        wasmtime_externref_data(context_, &value.of.externref));
}

std::string KotlinAppHost::GetString(const wasmtime_val_t& value) const {
    HostValue* host = GetHostValue(value);
    if (host == nullptr) return "null";
    if (host->kind == HostValue::Kind::String) {
        if (!host->string_valid) {
            host->string = Utf16ToUtf8(host->u16_string);
            host->string_valid = true;
        }
        return host->string;
    }
    if (host->kind == HostValue::Kind::Number) {
        return std::to_string(host->number);
    }
    if (host->kind == HostValue::Kind::Boolean) {
        return host->boolean ? "true" : "false";
    }
    if (host->string_valid && !host->string.empty()) return host->string;
    if (!host->u16_string.empty()) {
        host->string = Utf16ToUtf8(host->u16_string);
        host->string_valid = true;
        return host->string;
    }
    return "[object Object]";
}

wasm_trap_t* KotlinAppHost::Dispatch(
    void* environment,
    wasmtime_caller_t* caller,
    const wasmtime_val_t* args,
    size_t nargs,
    wasmtime_val_t* results,
    size_t nresults
) {
    auto* binding = static_cast<Binding*>(environment);
    if (std::getenv("SKIKO_WASMTIME_TRACE") != nullptr) {
        std::cerr << "[Kotlin host] " << binding->name << '\n';
    }
    try {
        return binding->host->CallHost(
            *binding, caller, args, nargs, results, nresults);
    } catch (const std::exception& exception) {
        return SkikoHost::Trap(binding->name + ": " + exception.what());
    }
}

void KotlinAppHost::RegisterImports(
    wasmtime_linker_t* linker,
    const wasmtime_module_t* application_module
) {
    wasm_importtype_vec_t imports;
    wasmtime_module_imports(application_module, &imports);

    size_t functions = 0;
    size_t constants = 0;
    size_t tags = 0;
    for (size_t index = 0; index < imports.size; ++index) {
        const wasm_importtype_t* import = imports.data[index];
        const std::string module = Name(wasm_importtype_module(import));
        const std::string field = Name(wasm_importtype_name(import));
        const wasm_externtype_t* external = wasm_importtype_type(import);

        if (module == "./skiko.mjs") continue;

        if (module == "'") {
            const wasm_globaltype_t* type =
                wasm_externtype_as_globaltype_const(external);
            if (type == nullptr) {
                wasm_importtype_vec_delete(&imports);
                throw std::runtime_error("String constant import is not global");
            }
            wasmtime_val_t value{};
            SetExternResult(&value, HostValue::String(field));
            wasmtime_global_t global;
            Check(
                wasmtime_global_new(context_, type, &value, &global),
                "Creating string constant"
            );
            wasmtime_val_unroot(&value);
            wasmtime_extern_t item{};
            item.kind = WASMTIME_EXTERN_GLOBAL;
            item.of.global = global;
            Check(
                wasmtime_linker_define(
                    linker, context_, module.data(), module.size(),
                    field.data(), field.size(), &item),
                "Defining string constant"
            );
            ++constants;
            continue;
        }

        if (module == "intrinsics") {
            const wasm_tagtype_t* type = wasm_externtype_as_tagtype_const(external);
            if (type == nullptr) {
                wasm_importtype_vec_delete(&imports);
                throw std::runtime_error("intrinsics.tag is not a tag");
            }
            wasmtime_tag_t tag;
            Check(wasmtime_tag_new(context_, type, &tag), "Creating JS tag");
            wasmtime_extern_t item{};
            item.kind = WASMTIME_EXTERN_TAG;
            item.of.tag = tag;
            Check(
                wasmtime_linker_define(
                    linker, context_, module.data(), module.size(),
                    field.data(), field.size(), &item),
                "Defining JS tag"
            );
            ++tags;
            continue;
        }

        const wasm_functype_t* function =
            wasm_externtype_as_functype_const(external);
        if (function == nullptr) {
            wasm_importtype_vec_delete(&imports);
            throw std::runtime_error(
                "Unsupported application import " + module + "." + field);
        }
        auto* binding = new Binding{this, module, field, module + "." + field};

        wasmtime_error_t* error = wasmtime_linker_define_func(
            linker,
            module.data(), module.size(),
            field.data(), field.size(),
            function,
            Dispatch,
            binding,
            DeleteBinding
        );
        if (error != nullptr) {
            delete binding;
            wasm_importtype_vec_delete(&imports);
            throw std::runtime_error(ErrorMessage(error));
        }
        ++functions;
    }
    wasm_importtype_vec_delete(&imports);
    std::cout << "Registered " << functions << " Kotlin host functions, "
              << constants << " string constants, and " << tags << " tag\n";
}

void KotlinAppHost::AttachInstance(const wasmtime_instance_t& instance) {
    instance_ = instance;
    attached_ = true;
}

void KotlinAppHost::Start() {
    if (!attached_) throw std::runtime_error("Application instance is not attached");
    wasmtime_extern_t item;
    if (!wasmtime_instance_export_get(
            context_, &instance_, "_start", sizeof("_start") - 1, &item) ||
        item.kind != WASMTIME_EXTERN_FUNC) {
        throw std::runtime_error("Application does not export _start");
    }
    std::cout << "Kotlin host bridge revision 3\n";
    wasm_trap_t* trap = nullptr;
    wasmtime_error_t* error = wasmtime_func_call(
        context_, &item.of.func, nullptr, 0, nullptr, 0, &trap);
    if (error != nullptr) throw std::runtime_error(ErrorMessage(error));
    if (trap != nullptr) throw std::runtime_error(TrapMessage(trap));
    DrainReadyCallbacks();
}

bool KotlinAppHost::Invoke(
    const std::shared_ptr<Callback>& callback,
    double timestamp,
    HostValue* arg
) {
    if (!callback) return false;
    wasmtime_func_t target_func{};

    if (callback->has_trampoline) {
        target_func = callback->trampoline;
    } else {
        const char* export_name = nullptr;
        if (callback->signature == Callback::Signature::UnitToUnit) {
            export_name = "__callFunction_(()->Unit)";
        } else if (callback->signature == Callback::Signature::JsToJs) {
            export_name = "__callFunction_((Js?)->Js?)";
        } else if (callback->signature == Callback::Signature::JsToUnit) {
            export_name = "__callFunction_((Js)->Unit)";
        } else {
            export_name = "__callFunction_((Double)->Unit)";
        }

        wasmtime_extern_t item;
        if (!wasmtime_instance_export_get(
                context_, &instance_, export_name, std::strlen(export_name), &item) ||
            item.kind != WASMTIME_EXTERN_FUNC) {
            std::cerr << "[KotlinAppHost] Missing callback export " << export_name << std::endl;
            return false;
        }
        target_func = item.of.func;
    }

    wasm_functype_t* func_type = wasmtime_func_type(context_, &target_func);
    const wasm_valtype_vec_t* results_types = wasm_functype_results(func_type);
    const size_t result_count = results_types != nullptr ? results_types->size : 0;
    const wasm_valtype_vec_t* params_types = wasm_functype_params(func_type);
    const size_t param_count = params_types != nullptr ? params_types->size : 0;
    const bool is_f64_arg = (callback->signature == Callback::Signature::DoubleToUnit);
    wasm_functype_delete(func_type);

    wasmtime_val_t args[2]{};
    args[0].kind = WASMTIME_ANYREF;
    wasmtime_anyref_clone(&callback->function, &args[0].of.anyref);
    if (param_count > 1) {
        if (is_f64_arg) {
            args[1].kind = WASMTIME_F64;
            args[1].of.f64 = timestamp;
        } else if (arg != nullptr) {
            SetExternResult(&args[1], arg);
        } else {
            args[1].kind = WASMTIME_EXTERNREF;
            wasmtime_externref_set_null(&args[1].of.externref);
        }
    }

    wasmtime_val_t result{};
    wasm_trap_t* trap = nullptr;
    wasmtime_error_t* error = wasmtime_func_call(
        context_, &target_func, args, std::min<size_t>(param_count, 2),
        result_count ? &result : nullptr, result_count, &trap);
    wasmtime_val_unroot(&args[0]);
    if (param_count > 1 && !is_f64_arg) {
        wasmtime_val_unroot(&args[1]);
    }
    bool ok = true;
    if (error != nullptr) {
        ok = false;
        std::cerr << "[KotlinAppHost] Callback error: " << ErrorMessage(error) << std::endl;
    }
    if (trap != nullptr) {
        ok = false;
        std::cerr << "[KotlinAppHost] Callback trap: " << TrapMessage(trap) << std::endl;
    }
    if (result_count != 0) wasmtime_val_unroot(&result);
    return ok;
}

void KotlinAppHost::DrainReadyCallbacks() {
    size_t iterations = 0;
    while (!ready_callbacks_.empty() && iterations++ < 1000) {
        auto ready = ready_callbacks_.front();
        ready_callbacks_.pop_front();
        Invoke(ready.callback, 0.0, ready.arg);
    }
}

void KotlinAppHost::RunExpiredTimers(const std::chrono::steady_clock::time_point& now) {
    if (timers_.empty()) return;
    std::vector<std::shared_ptr<Callback>> expired;
    std::vector<Timer> remaining;
    for (auto& timer : timers_) {
        if (now >= timer.due_time) {
            if (timer.callback) {
                expired.push_back(timer.callback);
            }
            if (timer.is_interval) {
                timer.due_time = now + std::chrono::milliseconds(timer.interval_ms);
                remaining.push_back(timer);
            }
        } else {
            remaining.push_back(timer);
        }
    }
    timers_ = std::move(remaining);
    for (const auto& cb : expired) {
        try {
            Invoke(cb, 0.0);
        } catch (const std::exception& e) {
            std::cerr << "[KotlinAppHost] Timer callback error: " << e.what() << std::endl;
        }
    }
}

std::chrono::steady_clock::time_point KotlinAppHost::NextDeadline(
    const std::chrono::steady_clock::time_point& default_deadline
) const {
    auto deadline = default_deadline;
    for (const auto& timer : timers_) {
        if (timer.due_time < deadline) {
            deadline = timer.due_time;
        }
    }
    return deadline;
}

size_t KotlinAppHost::AnimationCallbackCount() const {
    return animation_callbacks_.size();
}

bool KotlinAppHost::HasPendingWork() const {
    return !should_exit_ && (!ready_callbacks_.empty() || !animation_callbacks_.empty() || !timers_.empty());
}

bool KotlinAppHost::ShouldExit() const {
    return should_exit_;
}

void KotlinAppHost::RunNextAnimationCallback(double timestamp_millis) {
    if (animation_callbacks_.empty()) return;
    const std::shared_ptr<Callback> callback = animation_callbacks_.front();
    animation_callbacks_.pop_front();
    Invoke(callback, timestamp_millis);
}

void KotlinAppHost::DispatchPointerEvent(
    int32_t event_type,
    double x,
    double y,
    int32_t button,
    double scroll_delta_x,
    double scroll_delta_y
) {
    if (!pointer_callback_) return;

    wasmtime_func_t target_func{};
    if (pointer_callback_->has_trampoline) {
        target_func = pointer_callback_->trampoline;
    } else {
        const char* export_name = "__callFunction_((Int,Double,Double,Int,Double,Double)->Unit)";
        wasmtime_extern_t item;
        if (!wasmtime_instance_export_get(
                context_, &instance_, export_name, std::strlen(export_name), &item) ||
            item.kind != WASMTIME_EXTERN_FUNC) {
            std::cerr << "[KotlinAppHost] Missing callback export " << export_name << std::endl;
            return;
        }
        target_func = item.of.func;
    }

    wasm_functype_t* func_type = wasmtime_func_type(context_, &target_func);
    const wasm_valtype_vec_t* params_types = wasm_functype_params(func_type);
    const size_t param_count = params_types != nullptr ? params_types->size : 0;
    wasm_functype_delete(func_type);

    wasmtime_val_t args[7]{};
    args[0].kind = WASMTIME_ANYREF;
    wasmtime_anyref_clone(&pointer_callback_->function, &args[0].of.anyref);
    args[1].kind = WASMTIME_I32;
    args[1].of.i32 = event_type;
    args[2].kind = WASMTIME_F64;
    args[2].of.f64 = x;
    args[3].kind = WASMTIME_F64;
    args[3].of.f64 = y;
    args[4].kind = WASMTIME_I32;
    args[4].of.i32 = button;
    args[5].kind = WASMTIME_F64;
    args[5].of.f64 = scroll_delta_x;
    args[6].kind = WASMTIME_F64;
    args[6].of.f64 = scroll_delta_y;

    wasm_trap_t* trap = nullptr;
    wasmtime_error_t* error = wasmtime_func_call(
        context_, &target_func, args, std::min<size_t>(param_count, 7),
        nullptr, 0, &trap
    );
    wasmtime_val_unroot(&args[0]);
    if (error != nullptr) {
        std::cerr << "[KotlinAppHost] Pointer dispatch error: " << ErrorMessage(error) << std::endl;
    }
    if (trap != nullptr) {
        std::cerr << "[KotlinAppHost] Pointer dispatch trap: " << TrapMessage(trap) << std::endl;
    }
    DrainReadyCallbacks();
    RunExpiredTimers(std::chrono::steady_clock::now());
    DrainReadyCallbacks();
}

const std::string& KotlinAppHost::Title() const {
    return title_;
}

wasm_trap_t* KotlinAppHost::CallHost(
    const Binding& binding,
    wasmtime_caller_t* caller,
    const wasmtime_val_t* args,
    size_t nargs,
    wasmtime_val_t* results,
    size_t nresults
) {
    const std::string& qualified_name = binding.name;
    const std::string& name = binding.field.empty() ? binding.name : binding.field;

    auto string_result = [&](std::string value) {
        SetExternResult(&results[0], HostValue::String(std::move(value)));
    };
    auto object_result = [&](ObjectKind kind, int id = 0) {
        SetExternResult(&results[0], HostValue::Object(kind, id));
    };
    auto null_result = [&] {
        results[0].kind = WASMTIME_EXTERNREF;
        wasmtime_externref_set_null(&results[0].of.externref);
    };

    if (qualified_name == "wasm:js-string.length") {
        HostValue* host = GetHostValue(args[0]);
        results[0].kind = WASMTIME_I32;
        if (host && host->kind == HostValue::Kind::String) {
            results[0].of.i32 = static_cast<int32_t>(host->u16_string.size());
        } else {
            std::u16string u16 = Utf8ToUtf16(GetString(args[0]));
            results[0].of.i32 = static_cast<int32_t>(u16.size());
        }
    } else if (qualified_name == "wasm:js-string.concat") {
        HostValue* h1 = GetHostValue(args[0]);
        HostValue* h2 = GetHostValue(args[1]);
        std::u16string s1 = (h1 && h1->kind == HostValue::Kind::String) ? h1->u16_string : Utf8ToUtf16(GetString(args[0]));
        std::u16string s2 = (h2 && h2->kind == HostValue::Kind::String) ? h2->u16_string : Utf8ToUtf16(GetString(args[1]));
        auto* res = HostValue::U16String(s1 + s2);
        if (std::getenv("SKIKO_WASMTIME_TRACE") != nullptr) {
            std::cerr << "[js-string.concat] -> " << res->string << std::endl;
        }
        SetExternResult(&results[0], res);
    } else if (qualified_name == "wasm:js-string.charCodeAt") {
        HostValue* host = GetHostValue(args[0]);
        const size_t index = static_cast<uint32_t>(args[1].of.i32);
        results[0].kind = WASMTIME_I32;
        if (host && host->kind == HostValue::Kind::String) {
            results[0].of.i32 = index < host->u16_string.size()
                ? static_cast<uint16_t>(host->u16_string[index]) : 0;
        } else {
            std::u16string u16 = Utf8ToUtf16(GetString(args[0]));
            results[0].of.i32 = index < u16.size()
                ? static_cast<uint16_t>(u16[index]) : 0;
        }
    } else if (qualified_name == "wasm:js-string.substring") {
        HostValue* host = GetHostValue(args[0]);
        std::u16string value = (host && host->kind == HostValue::Kind::String)
            ? host->u16_string : Utf8ToUtf16(GetString(args[0]));
        const size_t begin = std::min<size_t>(
            static_cast<uint32_t>(args[1].of.i32), value.size());
        const size_t end = std::min<size_t>(
            static_cast<uint32_t>(args[2].of.i32), value.size());
        size_t sub_start = std::min(begin, end);
        size_t sub_len = std::max(begin, end) - sub_start;
        SetExternResult(&results[0], HostValue::U16String(value.substr(sub_start, sub_len)));
    } else if (qualified_name == "wasm:js-string.compare" ||
               qualified_name == "wasm:js-string.equals") {
        HostValue* h1 = GetHostValue(args[0]);
        HostValue* h2 = GetHostValue(args[1]);
        std::u16string s1 = (h1 && h1->kind == HostValue::Kind::String) ? h1->u16_string : Utf8ToUtf16(GetString(args[0]));
        std::u16string s2 = (h2 && h2->kind == HostValue::Kind::String) ? h2->u16_string : Utf8ToUtf16(GetString(args[1]));
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = qualified_name.ends_with("equals")
            ? (s1 == s2)
            : (s1 == s2 ? 0 : (s1 < s2 ? -1 : 1));
    } else if (qualified_name == "wasm:js-string.fromCharCodeArray") {
        wasmtime_arrayref_t array;
        if (!wasmtime_anyref_as_array(context_, &args[0].of.anyref, &array)) {
            return SkikoHost::Trap("fromCharCodeArray expected an array");
        }
        std::u16string value;
        for (uint32_t index = static_cast<uint32_t>(args[1].of.i32);
             index < static_cast<uint32_t>(args[2].of.i32); ++index) {
            wasmtime_val_t element;
            if (wasmtime_error_t* error =
                    wasmtime_arrayref_get(context_, &array, index, &element)) {
                wasmtime_arrayref_unroot(&array);
                throw std::runtime_error(
                    "Reading UTF-16 character array: " + ErrorMessage(error));
            }
            value.push_back(static_cast<char16_t>(element.of.i32));
            wasmtime_val_unroot(&element);
        }
        wasmtime_arrayref_unroot(&array);
        SetExternResult(&results[0], HostValue::U16String(std::move(value)));
    } else if (qualified_name == "wasm:js-string.intoCharCodeArray") {
        HostValue* host = GetHostValue(args[0]);
        std::u16string value = (host && host->kind == HostValue::Kind::String)
            ? host->u16_string : Utf8ToUtf16(GetString(args[0]));
        wasmtime_arrayref_t array;
        if (!wasmtime_anyref_as_array(context_, &args[1].of.anyref, &array)) {
            return SkikoHost::Trap("intoCharCodeArray expected an array");
        }
        uint32_t index = static_cast<uint32_t>(args[2].of.i32);
        for (char16_t character : value) {
            wasmtime_val_t element{};
            element.kind = WASMTIME_I32;
            element.of.i32 = static_cast<uint16_t>(character);
            if (wasmtime_error_t* error =
                    wasmtime_arrayref_set(context_, &array, index++, &element)) {
                wasmtime_arrayref_unroot(&array);
                throw std::runtime_error(
                    "Writing UTF-16 character array: " + ErrorMessage(error));
            }
        }
        wasmtime_arrayref_unroot(&array);
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = static_cast<int32_t>(value.size());
    } else if (name == "kotlin.wasm.internal.getJsEmptyString" ||
               name == "kotlin.js.stackPlaceHolder_js_code") {
        string_result("");
    } else if (name == "kotlin.wasm.internal.externrefToInt") {
        HostValue* value = GetHostValue(args[0]);
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = value && value->kind == HostValue::Kind::Number
            ? static_cast<int32_t>(value->number) : 0;
    } else if (name == "kotlin.wasm.internal.externrefToString" ||
               name == "kotlin.wasm.internal.itoa32_$external_fun" ||
               name == "kotlin.wasm.internal.utoa32_$external_fun") {
        if (args[0].kind == WASMTIME_I32) string_result(std::to_string(args[0].of.i32));
        else string_result(GetString(args[0]));
    } else if (name == "kotlin.wasm.internal.externrefEquals") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = GetString(args[0]) == GetString(args[1]);
    } else if (name == "kotlin.wasm.internal.externrefHashCode") {
        results[0].kind = WASMTIME_I32;
        uint32_t hash = 0;
        for (unsigned char c : GetString(args[0])) hash = hash * 31 + c;
        results[0].of.i32 = static_cast<int32_t>(hash);
    } else if (name == "kotlin.wasm.internal.isNullish") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = wasmtime_externref_is_null(&args[0].of.externref);
        if (std::getenv("SKIKO_WASMTIME_TRACE") != nullptr) {
            std::cerr
                << "[isNullish] kind=" << static_cast<int>(args[0].kind)
                << " store=" << args[0].of.externref.store_id
                << " result=" << results[0].of.i32 << '\n';
        }
    } else if (name == "kotlin.wasm.internal.kotlinUIntToJsNumberUnsafe") {
        SetExternResult(&results[0], HostValue::Number(
            static_cast<uint32_t>(args[0].of.i32)));
    } else if (name == "kotlin.wasm.internal.getCachedJsObject_$external_fun") {
        wasmtime_val_clone(&args[1], &results[0]);
    } else if (name == "kotlin.io.printlnImpl") {
        std::cout << GetString(args[0]) << '\n';
    } else if (name == "kotlin.random.initialSeed") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 0x534b494b;
    } else if (name.find("__convertKotlinClosureToJsClosure_") != std::string::npos) {
        auto callback = std::make_shared<Callback>();
        if (name.find("Double") != std::string::npos) {
            callback->signature = Callback::Signature::DoubleToUnit;
        } else if (name.find("(()->Unit)") != std::string::npos) {
            callback->signature = Callback::Signature::UnitToUnit;
        } else if (name.find("(Js?)->Js?") != std::string::npos || name.find("(Js)->Js?") != std::string::npos) {
            callback->signature = Callback::Signature::JsToJs;
        } else if (name.find("->Unit") != std::string::npos) {
            callback->signature = Callback::Signature::JsToUnit;
        }
        wasmtime_anyref_clone(&args[0].of.anyref, &callback->function);
        if (nargs >= 2 && args[1].kind == WASMTIME_FUNCREF) {
            callback->trampoline = args[1].of.funcref;
            callback->has_trampoline = true;
        }
        SetExternResult(&results[0], HostValue::CallbackValue(std::move(callback)));
    } else if (name == "kotlin.js.jsCatch" || name == "org.jetbrains.skiko.jsCatch") {
        HostValue* closure = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (closure && closure->callback) {
            last_thrown_exception_ = nullptr;
            bool ok = Invoke(closure->callback, 0.0);
            if (!ok || last_thrown_exception_ != nullptr) {
                HostValue* err = last_thrown_exception_;
                last_thrown_exception_ = nullptr;
                if (!err) {
                    err = HostValue::Object(ObjectKind::Error);
                    err->string = "Captured JS/Kotlin exception";
                }
                SetExternResult(&results[0], err);
                return nullptr;
            }
        }
        null_result();
        return nullptr;
    } else if (name == "kotlin.js.__callJsClosure_(()->Unit)" ||
               name == "kotlinx.coroutines.__callJsClosure_(()->Unit)") {
        HostValue* closure = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (closure) {
            if (closure->kind == HostValue::Kind::Poster) {
                if (closure->target_callback) {
                    ready_callbacks_.push_back({closure->target_callback, nullptr});
                } else if (coroutine_window_process_) {
                    ready_callbacks_.push_back({coroutine_window_process_, nullptr});
                }
            } else if (closure->callback) {
                Invoke(closure->callback, 0.0);
            }
        }
    } else if (name == "kotlinx.coroutines.tryGetProcess" ||
               name == "kotlinx.coroutines.tryGetWindow") {
        object_result(ObjectKind::Generic);
    } else if (name == "kotlinx.coroutines.nextTick_$external_fun") {
        HostValue* callback = nargs > 1 ? GetHostValue(args[1]) : (nargs > 0 ? GetHostValue(args[0]) : nullptr);
        if (callback && callback->callback) ready_callbacks_.push_back({callback->callback, nullptr});
    } else if (name == "kotlinx.coroutines.createScheduleMessagePoster") {
        HostValue* process_cb = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        SetExternResult(&results[0], HostValue::Poster(process_cb ? process_cb->callback : nullptr));
    } else if (name == "kotlinx.coroutines.createRescheduleMessagePoster") {
        SetExternResult(&results[0], HostValue::Poster(nullptr));
    } else if (name == "kotlinx.coroutines.subscribeToWindowMessages") {
        HostValue* process_cb = nargs > 1 ? GetHostValue(args[1]) : (nargs > 0 ? GetHostValue(args[0]) : nullptr);
        if (process_cb && process_cb->callback) {
            coroutine_window_process_ = process_cb->callback;
        }
    } else if (name == "kotlin.js.then_$external_fun" ||
               name == "kotlin.js.then_$external_fun_1" ||
               name.find("then_$external_fun") != std::string::npos ||
               name.find("catch_$external_fun") != std::string::npos) {
        HostValue* promise_val = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        HostValue* on_fulfilled = nargs > 1 ? GetHostValue(args[1]) : nullptr;
        if (!on_fulfilled && nargs > 0) {
            HostValue* maybe_cb = GetHostValue(args[0]);
            if (maybe_cb && maybe_cb->callback) on_fulfilled = maybe_cb;
        }

        HostValue* resolved_value = nullptr;
        if (promise_val && promise_val->promise_resolved && promise_val->promise_result) {
            resolved_value = promise_val->promise_result;
        }

        if (on_fulfilled && on_fulfilled->callback) {
            ready_callbacks_.push_back({on_fulfilled->callback, resolved_value});
        }

        auto* next_promise = HostValue::Object(ObjectKind::Promise);
        next_promise->promise_result = resolved_value;
        next_promise->promise_resolved = (resolved_value != nullptr);
        SetExternResult(&results[0], next_promise);
    } else if (name.find("__benchmarkArgs") != std::string::npos ||
               name.find("getSdlArgs") != std::string::npos ||
               name.find("getBenchmarkArgs") != std::string::npos ||
               name == "getBenchmarkArgs") {
        string_result(benchmark_args_);
    } else if (name.find("isNodeAddonRunning") != std::string::npos) {
        if (nresults > 0) {
            SetExternResult(&results[0], HostValue::BooleanValue(false));
        }
    } else if (name.find("isSdlEnvironment") != std::string::npos || name.find("isSdlEnv") != std::string::npos) {
        if (nresults > 0) {
            SetExternResult(&results[0], HostValue::BooleanValue(true));
        }
    } else if (name.find("isD8env") != std::string::npos) {
        if (nresults > 0) {
            SetExternResult(&results[0], HostValue::BooleanValue(false));
        }
    } else if (name.find("getCanvasWidth") != std::string::npos) {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 2048;
    } else if (name.find("getCanvasHeight") != std::string::npos) {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 1536;
    } else if (name.find("registerPointerCallback") != std::string::npos) {
        HostValue* callback_val = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (callback_val && callback_val->callback) {
            pointer_callback_ = callback_val->callback;
        }
    } else if (name == "org.w3c.performance.performance_$external_prop_getter" ||
               name == "performance_$external_prop_getter" ||
               name == "kotlin.time.tryGetPerformance" ||
               name.find("tryGetPerformance") != std::string::npos) {
        object_result(ObjectKind::Performance);
    } else if (name == "kotlin.system.currentTimeMillis" || name.find("currentTimeMillis") != std::string::npos) {
        if (nresults > 0) {
            const auto now = std::chrono::system_clock::now().time_since_epoch();
            results[0].kind = WASMTIME_I64;
            results[0].of.i64 = std::chrono::duration_cast<std::chrono::milliseconds>(now).count();
        }
    } else if (name == "org.w3c.performance.now_$external_fun" ||
               name == "now_$external_fun" ||
               name == "kotlin.time.getPerformanceNow" ||
               name.find("getPerformanceNow") != std::string::npos ||
               name == "kotlin.time.dateNow" ||
               name.find("dateNow") != std::string::npos ||
               name == "kotlin.time.getTime_$external_fun" ||
               name.find("getTime_$external_fun") != std::string::npos ||
               name == "org.jetbrains.skiko.currentNanoTimeWindowPerformance" ||
               name == "androidx.compose.material3.internal.now_$external_fun" ||
               name == "androidx.compose.ui.platform.timeRemaining_$external_fun" ||
               name == "org.w3c.dom.events.timeStamp_$external_prop_getter") {
        if (nresults > 0) {
            const auto now = std::chrono::steady_clock::now().time_since_epoch();
            results[0].kind = WASMTIME_F64;
            if (name == "org.jetbrains.skiko.currentNanoTimeWindowPerformance") {
                results[0].of.f64 = static_cast<double>(std::chrono::duration_cast<std::chrono::nanoseconds>(now).count());
            } else if (name == "androidx.compose.ui.platform.timeRemaining_$external_fun") {
                results[0].of.f64 = 16.6;
            } else {
                results[0].of.f64 = std::chrono::duration<double, std::milli>(now).count();
            }
        }
    } else if (name.find("setTimeout") != std::string::npos || name.find("setInterval") != std::string::npos) {
        HostValue* cb_val = nullptr;
        double delay_ms = 0.0;
        for (size_t i = 0; i < nargs; ++i) {
            HostValue* val = GetHostValue(args[i]);
            if (val && val->callback) {
                cb_val = val;
            } else if (args[i].kind == WASMTIME_I32) {
                delay_ms = args[i].of.i32;
            } else if (args[i].kind == WASMTIME_F64) {
                delay_ms = args[i].of.f64;
            }
        }
        if (cb_val && cb_val->callback) {
            const uint32_t id = next_timer_id_++;
            const bool is_int = name.find("setInterval") != std::string::npos;
            const uint32_t ms = static_cast<uint32_t>(std::max(0.0, delay_ms));
            const auto due = std::chrono::steady_clock::now() + std::chrono::milliseconds(ms);
            timers_.push_back({id, due, cb_val->callback, is_int, ms});
            if (nresults > 0) {
                results[0].kind = WASMTIME_I32;
                results[0].of.i32 = static_cast<int32_t>(id);
            }
        } else {
            if (nresults > 0) {
                results[0].kind = WASMTIME_I32;
                results[0].of.i32 = 0;
            }
        }
    } else if (name.find("clearTimeout") != std::string::npos || name.find("clearInterval") != std::string::npos) {
        int32_t timer_id = 0;
        for (size_t i = 0; i < nargs; ++i) {
            if (args[i].kind == WASMTIME_I32) timer_id = args[i].of.i32;
        }
        if (timer_id > 0) {
            timers_.erase(
                std::remove_if(timers_.begin(), timers_.end(), [timer_id](const Timer& t) {
                    return t.id == static_cast<uint32_t>(timer_id);
                }),
                timers_.end()
            );
        }
    } else if (name.find("schedulerPostTask") != std::string::npos ||
               name.find("requestIdleCallback") != std::string::npos) {
        HostValue* block = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (block && block->callback) {
            ready_callbacks_.push_back({block->callback, nullptr});
        }
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 1;
        }
    } else if (name.find("cancelIdleCallback") != std::string::npos) {
        // no-op
    } else if (name.find("didTimeout") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name == "org.jetbrains.skiko.wasm.awaitSkiko_$external_prop_getter") {
        auto* promise = HostValue::Object(ObjectKind::Promise);
        promise->promise_resolved = true;
        promise->promise_result = HostValue::BooleanValue(true);
        SetExternResult(&results[0], promise);
    } else if (name == "kotlinx.browser.document_$external_prop_getter" ||
               name == "org.w3c.dom.document_$external_prop_getter") {
        object_result(ObjectKind::Document);
    } else if (name == "org.w3c.dom.body_$external_prop_getter" ||
               name.find("body_$external_prop_getter") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("createElement_$external_fun") != std::string::npos ||
               name.find("createElement") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("getComputedStyle") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("style_$external_prop_getter") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("fontSize_$external_prop_getter") != std::string::npos) {
        string_result("16px");
    } else if (name.find("DOM_DELTA_PIXEL") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name.find("DOM_DELTA_LINE") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 1;
        }
    } else if (name.find("DOM_DELTA_PAGE") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 2;
        }
    } else if (name.find("deltaMode") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name.find("deltaX_$external_prop_getter") != std::string::npos ||
               name.find("deltaY_$external_prop_getter") != std::string::npos ||
               name.find("deltaZ_$external_prop_getter") != std::string::npos ||
               name.find("wheelDeltaX") != std::string::npos ||
               name.find("wheelDeltaY") != std::string::npos ||
               name.find("wheelDelta") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_F64;
            results[0].of.f64 = 0.0;
        }
    } else if (name == "kotlinx.browser.window_$external_prop_getter" ||
               name == "org.w3c.dom.window_$external_prop_getter") {
        object_result(ObjectKind::Generic);
    } else if (name == "org.w3c.dom.getElementById_$external_fun") {
        const std::string id = GetString(args[1]);
        if (id == "description") object_result(ObjectKind::Description);
        else object_result(ObjectKind::Canvas, 1);
    } else if (name == "org.w3c.dom.title_$external_prop_setter" ||
               name == "org.w3c.dom.innerHTML_$external_prop_setter") {
        title_ = GetString(args[1]);
    } else if (name == "org.w3c.dom.width_$external_prop_getter" ||
               name == "org.w3c.dom.height_$external_prop_getter") {
        HostValue* canvas = GetHostValue(args[0]);
        const bool width = name.find("width_") != std::string::npos;
        results[0].kind = WASMTIME_I32;
        if (!canvas || canvas->object != ObjectKind::Canvas) results[0].of.i32 = 0;
        else results[0].of.i32 = width ? 2048 : 1536;
    } else if (name == "org.w3c.dom.HTMLCanvasElement_$external_class_instanceof") {
        HostValue* value = GetHostValue(args[0]);
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = value && value->object == ObjectKind::Canvas;
    } else if (name == "org.w3c.dom.HTMLCanvasElement_$external_class_get") {
        object_result(ObjectKind::HtmlCanvasClass);
    } else if (name == "org.w3c.dom.Performance_$external_class_instanceof" ||
               name == "org.w3c.dom.Window_$external_class_instanceof" ||
               name == "org.w3c.dom.Document_$external_class_instanceof" ||
               name == "org.w3c.dom.EventTarget_$external_class_instanceof" ||
               name == "org.w3c.dom.Node_$external_class_instanceof" ||
               name == "org.w3c.dom.Element_$external_class_instanceof" ||
               name == "org.w3c.dom.HTMLElement_$external_class_instanceof") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 1;
    } else if (name == "org.jetbrains.skiko.GL_$external_prop_getter") {
        object_result(ObjectKind::Gl);
    } else if (name == "org.jetbrains.skiko.createContext_$external_fun" ||
               name == "createGlContextJs" || name.find("createGlContext") != std::string::npos) {
        int width = 2048;
        int height = 1536;
        if (nargs >= 2) {
            if (args[0].kind == WASMTIME_I32) width = args[0].of.i32;
            else if (HostValue* h = GetHostValue(args[0])) width = static_cast<int>(h->number);
            if (args[1].kind == WASMTIME_I32) height = args[1].of.i32;
            else if (HostValue* h = GetHostValue(args[1])) height = static_cast<int>(h->number);
        } else if (nargs == 1) {
            HostValue* canvas = GetHostValue(args[0]);
            if (canvas && canvas->object == ObjectKind::Canvas) {
                width = 2048;
                height = 1536;
            }
        }
        std::string error;
        const int id = skiko_->CreateCanvasContext(width, height, error);
        if (id == 0) return SkikoHost::Trap(error);
        if (name.find("createGlContext") != std::string::npos) {
            SetExternResult(&results[0], HostValue::Number(id));
        } else {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = id;
        }
    } else if (name == "org.jetbrains.skiko.makeContextCurrent_$external_fun" ||
               name == "makeGlContextCurrentJs" || name.find("makeGlContextCurrent") != std::string::npos) {
        int ctx_id = 1;
        if (nargs > 0) {
            if (args[0].kind == WASMTIME_I32) ctx_id = args[0].of.i32;
            else if (HostValue* h = GetHostValue(args[0])) ctx_id = static_cast<int>(h->number);
        }
        if (nargs > 1 && ctx_id == 0) {
            if (args[1].kind == WASMTIME_I32) ctx_id = args[1].of.i32;
            else if (HostValue* h = GetHostValue(args[1])) ctx_id = static_cast<int>(h->number);
        }
        std::string error;
        const bool ok = skiko_->MakeContextCurrent(ctx_id, error);
        if (!error.empty()) return SkikoHost::Trap(error);
        if (name.find("makeGlContextCurrent") != std::string::npos) {
            SetExternResult(&results[0], HostValue::BooleanValue(ok));
        } else {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = ok ? 1 : 0;
        }
    } else if (name == "deleteGlContextJs" || name.find("deleteGlContext") != std::string::npos) {
        SetExternResult(&results[0], HostValue::BooleanValue(true));
    } else if (name == "nativeGlPresent" || name.find("nativeGlPresent") != std::string::npos) {
        int ctx_id = skiko_->CurrentContextId();
        if (nargs > 0) {
            if (args[0].kind == WASMTIME_I32) ctx_id = args[0].of.i32;
            else if (HostValue* h = GetHostValue(args[0])) ctx_id = static_cast<int>(h->number);
        }
        if (ctx_id <= 0) ctx_id = 1;
        skiko_->FlushContexts();
        if (window_ != nullptr && window_->IsOpen()) {
            int win_w = 0, win_h = 0;
            window_->GetWindowSize(&win_w, &win_h);
            int src_w = skiko_->ContextWidth(ctx_id);
            int src_h = skiko_->ContextHeight(ctx_id);
            if (src_w <= 0) src_w = win_w * 2;
            if (src_h <= 0) src_h = win_h * 2;
            const std::vector<HostCanvasTexture> canvases = {
                {skiko_->ContextTexture(ctx_id), src_w, src_h, 0, 0, win_w, win_h}
            };
            window_->Present(canvases);
            std::string err;
            skiko_->MakeContextCurrent(ctx_id, err);
            window_->PollEvents(nullptr, nullptr);
        }
    } else if (name == "nativeGlFinish" || name.find("nativeGlFinish") != std::string::npos) {
#ifdef __APPLE__
        glFinish();
#endif
    } else if (name == "isSdlEnv" || name == "isSdlJs" || name.find("isSdl") != std::string::npos) {
        SetExternResult(&results[0], HostValue::BooleanValue(true));
    } else if (name == "isWasmtimeJs" || name == "isWasmtime" || name.find("isWasmtime") != std::string::npos) {
        SetExternResult(&results[0], HostValue::BooleanValue(true));
    } else if (name == "isNodeJs" || name == "isNodeJsJs" || name.find("isNodeJs") != std::string::npos) {
        SetExternResult(&results[0], HostValue::BooleanValue(false));
    } else if (name == "gc" || name == "window.gc" || name == "globalThis.gc" || name == "performJsGc" || name.find("performJsGc") != std::string::npos) {
        if (context_ != nullptr) {
            wasmtime_context_gc(context_);
        }
    } else if (name == "org.jetbrains.skiko.currentGLContext") {
        object_result(ObjectKind::GlContext, skiko_->CurrentContextId());
    } else if (name == "org.khronos.webgl.SAMPLES_$external_prop_getter") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 0x80A9;
    } else if (name == "org.khronos.webgl.getParameter_$external_fun") {
        SetExternResult(&results[0], HostValue::Number(0));
    } else if (name == "org.khronos.webgl.Companion_$external_object_getInstance") {
        object_result(ObjectKind::WebGlClass);
    } else if (name == "org.jetbrains.skiko.skikoRequestAnimationFrame" ||
               name == "org.jetbrains.skiko.windowRequestAnimationFrame" ||
               name == "org.w3c.dom.requestAnimationFrame_$external_fun" ||
               name.find("requestAnimationFrame") != std::string::npos) {
        HostValue* cb_val = nullptr;
        for (size_t i = 0; i < nargs; ++i) {
            HostValue* val = GetHostValue(args[i]);
            if (val && val->callback) {
                cb_val = val;
                break;
            }
        }
        if (cb_val && cb_val->callback) {
            animation_callbacks_.push_back(cb_val->callback);
        }
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = next_animation_id_++;
        }
    } else if (name.find("cancelAnimationFrame") != std::string::npos) {
        // no-op
    } else if (name == "kotlin.wasm.internal.getJsTrue") {
        SetExternResult(&results[0], HostValue::BooleanValue(true));
    } else if (name == "kotlin.wasm.internal.getJsFalse") {
        SetExternResult(&results[0], HostValue::BooleanValue(false));
    } else if (name == "kotlin.wasm.internal.getJsEmptyString") {
        string_result("");
    } else if (name == "kotlin.wasm.internal.intToExternref") {
        SetExternResult(&results[0], HostValue::Number(args[0].of.i32));
    } else if (name == "kotlin.wasm.internal.externrefToInt") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = host ? static_cast<int32_t>(host->number) : 0;
    } else if (name == "kotlin.wasm.internal.booleanToExternref") {
        const bool val = (nargs > 0 && args[0].kind == WASMTIME_I32) ? (args[0].of.i32 != 0) : false;
        SetExternResult(&results[0], HostValue::BooleanValue(val));
    } else if (name == "kotlin.wasm.internal.externrefToBoolean") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        if (!host) {
            results[0].of.i32 = 0;
        } else if (host->kind == HostValue::Kind::Boolean) {
            results[0].of.i32 = host->boolean ? 1 : 0;
        } else if (host->kind == HostValue::Kind::Number) {
            results[0].of.i32 = (host->number != 0.0) ? 1 : 0;
        } else if (host->kind == HostValue::Kind::String) {
            results[0].of.i32 = host->string.empty() ? 0 : 1;
        } else {
            results[0].of.i32 = 1;
        }
    } else if (name == "kotlin.wasm.internal.externrefToDouble") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_F64;
        results[0].of.f64 = host ? host->number : 0.0;
    } else if (name == "kotlin.wasm.internal.doubleToExternref") {
        const double val = (nargs > 0 && args[0].kind == WASMTIME_F64) ? args[0].of.f64 : 0.0;
        SetExternResult(&results[0], HostValue::Number(val));
    } else if (name == "kotlin.wasm.internal.externrefToFloat") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_F32;
        results[0].of.f32 = host ? static_cast<float>(host->number) : 0.0f;
    } else if (name == "kotlin.wasm.internal.floatToExternref") {
        const float val = (nargs > 0 && args[0].kind == WASMTIME_F32) ? args[0].of.f32 : 0.0f;
        SetExternResult(&results[0], HostValue::Number(val));
    } else if (name == "kotlin.wasm.internal.externrefToLong") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I64;
        results[0].of.i64 = host ? static_cast<int64_t>(host->number) : 0;
    } else if (name == "kotlin.wasm.internal.longToExternref") {
        const int64_t val = (nargs > 0 && args[0].kind == WASMTIME_I64) ? args[0].of.i64 : 0;
        SetExternResult(&results[0], HostValue::Number(static_cast<double>(val)));
    } else if (name == "kotlin.wasm.internal.externrefToByte" ||
               name == "kotlin.wasm.internal.externrefToShort" ||
               name == "kotlin.wasm.internal.externrefToChar") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = host ? static_cast<int32_t>(host->number) : 0;
    } else if (name == "kotlin.wasm.internal.byteToExternref" ||
               name == "kotlin.wasm.internal.shortToExternref" ||
               name == "kotlin.wasm.internal.charToExternref") {
        const int32_t val = (nargs > 0 && args[0].kind == WASMTIME_I32) ? args[0].of.i32 : 0;
        SetExternResult(&results[0], HostValue::Number(val));
    } else if (name == "kotlin.wasm.internal.externrefToString") {
        string_result(GetString(args[0]));
    } else if (name == "kotlin.wasm.internal.itoa32_$external_fun" ||
               name == "kotlin.wasm.internal.utoa32_$external_fun") {
        string_result(std::to_string(args[0].of.i32));
    } else if (name == "kotlin.wasm.internal.itoa64_$external_fun" ||
               name == "kotlin.wasm.internal.utoa64_$external_fun") {
        string_result(std::to_string(args[0].of.i64));
    } else if (name.find("newJsArray") != std::string::npos ||
               name.find("JsArray_$external_fun") != std::string::npos) {
        object_result(ObjectKind::Array);
    } else if (name.find("jsArrayPush") != std::string::npos) {
        // no-op
    } else if (name.find("jsArrayGet") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && host->object == ObjectKind::Array && nargs > 1 && args[1].kind == WASMTIME_I32) {
            const size_t index = static_cast<size_t>(args[1].of.i32);
            if (index < host->string_array.size()) {
                string_result(host->string_array[index]);
            } else {
                null_result();
            }
        } else {
            null_result();
        }
    } else if (name.find("jsArraySet") != std::string::npos) {
        // no-op
    } else if (name == "androidx.navigation.refBrowserWindow") {
        object_result(ObjectKind::Window);
    } else if (name == "androidx.navigation.location_$external_prop_getter") {
        object_result(ObjectKind::Location);
    } else if (name == "androidx.navigation.history_$external_prop_getter") {
        object_result(ObjectKind::History);
    } else if (name == "androidx.navigation.origin_$external_prop_getter") {
        string_result("http://localhost");
    } else if (name == "androidx.navigation.pathname_$external_prop_getter") {
        string_result("/");
    } else if (name == "androidx.navigation.hash_$external_prop_getter") {
        string_result("");
    } else if (name == "androidx.navigation.state_$external_prop_getter" ||
               name == "androidx.navigation.state_$external_prop_getter_1") {
        null_result();
    } else if (name == "androidx.navigation.pushState_$external_fun" ||
               name == "androidx.navigation.replaceState_$external_fun") {
        // no-op
    } else if (name == "androidx.navigation.decodeURIComponent_$external_fun" ||
               name == "androidx.navigation.encodeURIComponent_$external_fun") {
        string_result(nargs > 0 ? GetString(args[0]) : "");
    } else if (name.find("cancelable_$external_prop_getter") != std::string::npos ||
               name.find("defaultPrevented_$external_prop_getter") != std::string::npos ||
               name.find("bubbles_$external_prop_getter") != std::string::npos ||
               name.find("composed_$external_prop_getter") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name.find("stopPropagation") != std::string::npos ||
               name.find("preventDefault") != std::string::npos ||
               name.find("stopImmediatePropagation") != std::string::npos) {
        // no-op
    } else if (name == "org.jetbrains.skiko.devicePixelRatio" ||
               name == "org.w3c.dom.devicePixelRatio_$external_prop_getter") {
        results[0].kind = WASMTIME_F64;
        results[0].of.f64 = 1.0;
    } else if (name == "org.w3c.dom.navigator_$external_prop_getter") {
        object_result(ObjectKind::Generic);
    } else if (name == "org.jetbrains.skiko.wasm.contextAttributesWithDefaults") {
        object_result(ObjectKind::ContextAttributes);
    } else if (name == "org.jetbrains.skia.impl.FinalizationRegistry_$external_fun") {
        object_result(ObjectKind::FinalizationRegistry);
    } else if (name == "org.jetbrains.skia.impl.unregister_$external_fun") {
        if (nresults) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 1;
        }
    } else if (name == "org.jetbrains.skia.navigatorLanguage" ||
               name == "org.jetbrains.skiko.getNavigatorInfo" ||
               name == "org.jetbrains.skiko.getNavigatorUserAgent" ||
               name == "org.w3c.dom.userAgent_$external_prop_getter" ||
               name == "org.w3c.dom.language_$external_prop_getter") {
        string_result(name.ends_with("navigatorLanguage") || name.ends_with("language_$external_prop_getter") ? "en-US" : "macOS");
    } else if (name == "org.w3c.dom.languages_$external_prop_getter" ||
               name.find("languages_$external_prop_getter") != std::string::npos ||
               name.find("languages") != std::string::npos ||
               name.find("getUserPreferredLanguages") != std::string::npos ||
               name.find("userPreferredLanguages") != std::string::npos) {
        SetExternResult(&results[0], HostValue::StringArray({"en-US", "en"}));
    } else if (name.find("length_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        if (host && !host->xml_node_list.empty()) {
            results[0].of.i32 = static_cast<int32_t>(host->xml_node_list.size());
        } else if (host && host->object == ObjectKind::Array) {
            results[0].of.i32 = static_cast<int32_t>(host->string_array.size());
        } else {
            results[0].of.i32 = 0;
        }
    } else if (name.find("get_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && host->object == ObjectKind::Array && nargs > 1 && args[1].kind == WASMTIME_I32) {
            const size_t index = static_cast<size_t>(args[1].of.i32);
            if (index < host->string_array.size()) {
                string_result(host->string_array[index]);
            } else {
                null_result();
            }
        } else if (host && !host->xml_node_list.empty() && nargs > 1 && args[1].kind == WASMTIME_I32) {
            const size_t index = static_cast<size_t>(args[1].of.i32);
            if (index < host->xml_node_list.size()) {
                auto elem = HostValue::Object(ObjectKind::Element);
                elem->xml_node = host->xml_node_list[index];
                SetExternResult(&results[0], elem);
            } else {
                null_result();
            }
        } else {
            null_result();
        }
    } else if (name.find("set_$external_fun") != std::string::npos) {
        if (nresults > 0) null_result();
    } else if (name.find("has_$external_fun") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name.find("delete_$external_fun") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 0;
        }
    } else if (name.find("ok_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = (host == nullptr || host->boolean) ? 1 : 0;
    } else if (name.find("status_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = (host && host->number > 0) ? static_cast<int32_t>(host->number) : 200;
    } else if (name.find("statusText_$external_prop_getter") != std::string::npos) {
        string_result("OK");
    } else if (name.find("headers_$external_prop_getter") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("body_$external_prop_getter") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("clone_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        auto* cloned = HostValue::Object(host ? host->object : ObjectKind::Response);
        if (host) {
            cloned->byte_buffer = host->byte_buffer;
            cloned->boolean = host->boolean;
            cloned->number = host->number;
        }
        SetExternResult(&results[0], cloned);
    } else if (name.find("blob_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        auto* blob = HostValue::Object(ObjectKind::Blob);
        if (host) {
            blob->byte_buffer = host->byte_buffer;
        }
        auto* promise = HostValue::Object(ObjectKind::Promise);
        promise->promise_result = blob;
        promise->promise_resolved = true;
        SetExternResult(&results[0], promise);
    } else if (name == "org.w3c.dom.fetch_$external_fun" ||
               name == "org.w3c.fetch.fetch_$external_fun" ||
               name == "io.ktor.client.fetch.fetch_$external_fun" ||
               name.find("fetch_$external_fun") != std::string::npos ||
               name.find("jsFetchWithSignal") != std::string::npos ||
               name == "fetch" ||
               name == "window.fetch") {
        std::string path_str;
        for (size_t i = 0; i < nargs; ++i) {
            std::string s = GetString(args[i]);
            if (!s.empty() && s != "null" && s != "[object Object]") {
                path_str = s;
                break;
            }
        }
        std::vector<uint8_t> buffer = ReadResourceFile(path_str);
        auto* response = HostValue::Object(ObjectKind::Response);
        response->byte_buffer = std::move(buffer);
        response->boolean = true;
        response->number = 200;

        auto* promise = HostValue::Object(ObjectKind::Promise);
        promise->promise_result = response;
        promise->promise_resolved = true;
        SetExternResult(&results[0], promise);
    } else if (name == "org.w3c.files.slice_$external_fun" || name.find("slice_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        size_t start = nargs > 1 && args[1].kind == WASMTIME_I32 ? static_cast<size_t>(std::max(0, args[1].of.i32)) : 0;
        size_t end = nargs > 2 && args[2].kind == WASMTIME_I32 ? static_cast<size_t>(std::max(0, args[2].of.i32)) : (host ? host->byte_buffer.size() : 0);
        auto* sliced = HostValue::Object(ObjectKind::Blob);
        if (host && start < host->byte_buffer.size()) {
            end = std::min(end, host->byte_buffer.size());
            if (end > start) {
                sliced->byte_buffer.assign(host->byte_buffer.begin() + start, host->byte_buffer.begin() + end);
            }
        }
        SetExternResult(&results[0], sliced);
    } else if (name == "org.w3c.fetch.arrayBuffer_$external_fun" ||
               name == "org.jetbrains.compose.resources.jsExportBlobAsArrayBuffer" ||
               name.find("arrayBuffer_$external_fun") != std::string::npos ||
               name.find("jsExportBlobAsArrayBuffer") != std::string::npos ||
               name.find("arrayBuffer") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        auto* array_buf = HostValue::Object(ObjectKind::ArrayBuffer);
        if (host) {
            array_buf->byte_buffer = host->byte_buffer;
        }
        auto* promise = HostValue::Object(ObjectKind::Promise);
        promise->promise_result = array_buf;
        promise->promise_resolved = true;
        SetExternResult(&results[0], promise);
    } else if (name.find("copyArrayBufferToWasmMemory") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        int32_t ptr = nargs > 1 && args[1].kind == WASMTIME_I32 ? args[1].of.i32 : 0;
        if (host && caller && !host->byte_buffer.empty()) {
            wasmtime_extern_t mem_ext;
            if (wasmtime_caller_export_get(caller, "memory", 6, &mem_ext) && mem_ext.kind == WASMTIME_EXTERN_MEMORY) {
                uint8_t* mem_data = wasmtime_memory_data(context_, &mem_ext.of.memory);
                size_t mem_size = wasmtime_memory_data_size(context_, &mem_ext.of.memory);
                if (ptr >= 0 && static_cast<size_t>(ptr) + host->byte_buffer.size() <= mem_size) {
                    std::memcpy(mem_data + ptr, host->byte_buffer.data(), host->byte_buffer.size());
                }
            }
        }
    } else if (name.find("supportsCacheApi") != std::string::npos ||
               name.find("isInTestEnvironment") != std::string::npos) {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 0;
    } else if (name.find("byteLength_$external_prop_getter") != std::string::npos ||
               name.find("byteLength") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = host ? static_cast<int32_t>(host->byte_buffer.size()) : 0;
    } else if (name == "org.w3c.fetch.Response_$external_class_instanceof") {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 1;
    } else if (name.find("XMLHttpRequest") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name == "org.w3c.xhr.open_$external_fun" || name.find("open_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        std::string url = nargs > 2 ? GetString(args[2]) : (nargs > 1 ? GetString(args[1]) : "");
        if (host) {
            host->string = url;
        }
    } else if (name == "org.w3c.xhr.send_$external_fun" || name.find("send_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && !host->string.empty()) {
            host->byte_buffer = ReadResourceFile(host->string);
            host->number = 200;
        }
    } else if (name == "org.w3c.xhr.overrideMimeType_$external_fun") {
        // no-op
    } else if (name.find("responseText_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        std::string text = host ? std::string(host->byte_buffer.begin(), host->byte_buffer.end()) : "";
        string_result(std::move(text));
    } else if (name.find("requestResponseAsByteArray") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        auto* array_buf = HostValue::Object(ObjectKind::ArrayBuffer);
        if (host) array_buf->byte_buffer = host->byte_buffer;
        SetExternResult(&results[0], array_buf);
    } else if (name == "org.w3c.dom.parsing.DOMParser_$external_fun") {
        object_result(ObjectKind::Generic);
    } else if (name == "org.w3c.dom.parsing.parseFromString_$external_fun" ||
               name.find("parseFromString_$external_fun") != std::string::npos) {
        std::string xml_str = nargs > 1 ? GetString(args[1]) : (nargs > 0 ? GetString(args[0]) : "");
        auto doc = HostValue::Object(ObjectKind::Document);
        doc->xml_node = ParseXml(xml_str);
        SetExternResult(&results[0], doc);
    } else if (name == "org.w3c.dom.documentElement_$external_prop_getter" ||
               name.find("documentElement_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && host->xml_node) {
            auto elem = HostValue::Object(ObjectKind::Element);
            elem->xml_node = host->xml_node;
            SetExternResult(&results[0], elem);
        } else {
            null_result();
        }
    } else if (name == "org.w3c.dom.localName_$external_prop_getter" ||
               name.find("localName_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        string_result(host && host->xml_node ? host->xml_node->local_name : "");
    } else if (name == "org.w3c.dom.nodeName_$external_prop_getter" ||
               name.find("nodeName_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        string_result(host && host->xml_node ? host->xml_node->tag_name : "");
    } else if (name == "org.w3c.dom.namespaceURI_$external_prop_getter" ||
               name.find("namespaceURI_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        string_result(host && host->xml_node ? host->xml_node->namespace_uri : "");
    } else if (name == "org.w3c.dom.getAttribute_$external_fun" ||
               name.find("getAttribute_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        std::string attr_name = nargs > 1 ? GetString(args[1]) : "";
        std::string val = "";
        if (host && host->xml_node) {
            for (const auto& attr : host->xml_node->attributes) {
                if (attr.name == attr_name || attr.local_name == attr_name) {
                    val = attr.value;
                    break;
                }
            }
        }
        string_result(val);
    } else if (name == "org.w3c.dom.getAttributeNS_$external_fun" ||
               name.find("getAttributeNS_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        std::string local_name = nargs > 2 ? GetString(args[2]) : (nargs > 1 ? GetString(args[1]) : "");
        std::string val = "";
        if (host && host->xml_node) {
            for (const auto& attr : host->xml_node->attributes) {
                if (attr.local_name == local_name || attr.name == local_name || attr.name == "android:" + local_name) {
                    val = attr.value;
                    break;
                }
            }
        }
        string_result(val);
    } else if (name == "org.w3c.dom.childNodes_$external_prop_getter" ||
               name.find("childNodes_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        auto list = HostValue::Object(ObjectKind::NodeList);
        if (host && host->xml_node) {
            list->xml_node_list = host->xml_node->children;
        }
        SetExternResult(&results[0], list);
    } else if (name == "org.w3c.dom.length_$external_prop_getter" ||
               name == "org.w3c.dom.length_$external_prop_getter_1" ||
               name == "length_$external_prop_getter" ||
               name.find("length_$external_prop_getter") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        if (host && !host->xml_node_list.empty()) {
            results[0].of.i32 = static_cast<int32_t>(host->xml_node_list.size());
        } else if (host && !host->string_array.empty()) {
            results[0].of.i32 = static_cast<int32_t>(host->string_array.size());
        } else {
            results[0].of.i32 = 0;
        }
    } else if (name == "org.w3c.dom.item_$external_fun" ||
               name.find("item_$external_fun") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        int32_t idx = nargs > 1 && args[1].kind == WASMTIME_I32 ? args[1].of.i32 : 0;
        if (host && idx >= 0 && static_cast<size_t>(idx) < host->xml_node_list.size()) {
            auto elem = HostValue::Object(ObjectKind::Element);
            elem->xml_node = host->xml_node_list[static_cast<size_t>(idx)];
            SetExternResult(&results[0], elem);
        } else {
            null_result();
        }
    } else if (name == "org.w3c.dom.lookupPrefix_$external_fun" ||
               name.find("lookupPrefix_$external_fun") != std::string::npos) {
        string_result("android");
    } else if (name == "org.w3c.dom.Element_$external_class_instanceof") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = (host && host->xml_node != nullptr) ? 1 : 0;
    } else if (name == "kotlin.wasm.internal.getJsClassName") {
        string_result("Object");
    } else if (name.find("jsTypeOf") != std::string::npos || name.find("js_typeof") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        std::string type_name = "undefined";
        if (host) {
            if (host->kind == HostValue::Kind::String) type_name = "string";
            else if (host->kind == HostValue::Kind::Number) type_name = "number";
            else if (host->kind == HostValue::Kind::Boolean) type_name = "boolean";
            else if (host->kind == HostValue::Kind::Callback || host->kind == HostValue::Kind::Poster) type_name = "function";
            else type_name = "object";
        }
        string_result(type_name);
    } else if (name == "kotlin.wasm.internal.getConstructor") {
        object_result(ObjectKind::Generic);
    } else if (name == "kotlin.createJsError") {
        auto* error = HostValue::Object(ObjectKind::Error);
        error->string = nargs == 0 ? "Kotlin error" : GetString(args[0]);
        if (std::getenv("SKIKO_WASMTIME_TRACE") != nullptr) {
            std::cerr << "[createJsError] message=" << error->string << '\n';
        }
        SetExternResult(&results[0], error);
    } else if (name == "kotlin.js.JsError_$external_class_instanceof") {
        HostValue* value = GetHostValue(args[0]);
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = value && value->object == ObjectKind::Error;
    } else if (name == "kotlin.js.message_$external_prop_getter") {
        string_result(GetString(args[0]));
    } else if (name == "kotlin.js.kotlinException_$external_prop_getter") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && host->has_kotlin_exception) {
            wasmtime_val_clone(&host->kotlin_exception, &results[0]);
        } else {
            null_result();
        }
    } else if (name == "kotlin.js.kotlinException_$external_prop_setter") {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && nargs > 1) {
            if (host->has_kotlin_exception) {
                wasmtime_val_unroot(&host->kotlin_exception);
                host->has_kotlin_exception = false;
            }
            wasmtime_val_clone(&args[1], &host->kotlin_exception);
            host->has_kotlin_exception = true;
        }
    } else if (name == "kotlin.wasm.internal.jsThrow" ||
               name == "kotlin.js.jsThrow" ||
               name == "org.jetbrains.skiko.jsThrow") {
        HostValue* host = (nargs > 0) ? GetHostValue(args[0]) : nullptr;
        if (host != nullptr) {
            last_thrown_exception_ = host;
        } else if (nargs > 0) {
            auto* err = HostValue::Object(ObjectKind::Error);
            wasmtime_val_clone(&args[0], &err->kotlin_exception);
            err->has_kotlin_exception = true;
            last_thrown_exception_ = err;
        }
        return nullptr;
    } else if (name.find("WeakRef") != std::string::npos && name.find("deref") == std::string::npos) {
        if (nargs > 0) {
            SetExternResult(&results[0], HostValue::WeakRefValue(args[0]));
        } else {
            object_result(ObjectKind::Generic);
        }
    } else if (name.find("deref") != std::string::npos) {
        HostValue* host = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        if (host && host->has_target_val) {
            wasmtime_val_clone(&host->target_val, &results[0]);
        } else {
            null_result();
        }
    } else if (name.find("FinalizationRegistry") != std::string::npos) {
        object_result(ObjectKind::FinalizationRegistry);
    } else if (name.find("unregister") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 1;
        }
    } else if (name.find("parseLanguageTagToIntlLocale") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("Locale_$external_fun") != std::string::npos) {
        object_result(ObjectKind::Generic);
    } else if (name.find("_script_$external_prop_getter") != std::string::npos ||
               name == "org.jetbrains.compose.resources.script_$external_prop_getter") {
        null_result();
    } else if (name.find("_baseName_$external_prop_getter") != std::string::npos ||
               name.find("baseName_$external_prop_getter") != std::string::npos) {
        string_result("en-US");
    } else if (name.find("_language_$external_prop_getter") != std::string::npos ||
               name == "org.jetbrains.compose.resources.language_$external_prop_getter") {
        string_result("en");
    } else if (name.find("_region_$external_prop_getter") != std::string::npos ||
               name == "org.jetbrains.compose.resources.region_$external_prop_getter") {
        string_result("US");
    } else if (name.find("doesJsArrayContainValue") != std::string::npos) {
        if (nresults > 0) {
            results[0].kind = WASMTIME_I32;
            results[0].of.i32 = 1;
        }
    } else if (name.find("toLocaleUpperCase") != std::string::npos) {
        string_result(nargs > 0 ? GetString(args[0]) : "");
    } else if (name.find("isSecureContext") != std::string::npos) {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 1;
    } else if (name.find("isSchedulerApiSupported") != std::string::npos ||
               name.find("isIdleApiSupported") != std::string::npos ||
               name.find("isFullWakeLockApiSupported") != std::string::npos ||
               name.find("isVibrationSupported") != std::string::npos ||
               name.find("hasVirtualKeyboard") != std::string::npos ||
               name.find("isClipboardReadSupported") != std::string::npos ||
               name.find("isClipboardWriteSupported") != std::string::npos ||
               name.find("isFallbackWriteTextApiAvailable") != std::string::npos ||
               name.find("isFullClipboardApiSupported") != std::string::npos ||
               name.find("webWakeLockSupported") != std::string::npos ||
               name.find("isInTestEnvironment") != std::string::npos ||
               name.find("supportsCacheApi") != std::string::npos ||
               name.find("isMouseEvent") != std::string::npos ||
               name.find("isTouchEvent") != std::string::npos ||
               name.find("isTypedEvent") != std::string::npos ||
               name.find("documentIsVisible") != std::string::npos) {
        results[0].kind = WASMTIME_I32;
        results[0].of.i32 = 0;
    } else if (name.find("instanceof") != std::string::npos ||
               name.find("Instanceof") != std::string::npos) {
        HostValue* value = nargs > 0 ? GetHostValue(args[0]) : nullptr;
        results[0].kind = WASMTIME_I32;
        if (name.find("JsError") != std::string::npos || name.find("Error") != std::string::npos) {
            results[0].of.i32 = (value && value->object == ObjectKind::Error) ? 1 : 0;
        } else {
            results[0].of.i32 = 1;
        }
    } else if (name == "kotlin.system.exitProcess" ||
               name == "kotlin.wasm.internal.exitProcess" ||
               name.find("exitProcess") != std::string::npos ||
               name.find("exitSdlHost") != std::string::npos ||
               name == "exit" ||
               name == "process.exit") {
        std::cout << "[KotlinAppHost] Benchmark suite execution complete. Exiting cleanly." << std::endl;
        std::exit(nargs > 0 && args[0].kind == WASMTIME_I32 ? args[0].of.i32 : 0);
    } else if (name == "org.w3c.dom.close_$external_fun") {
        should_exit_ = true;
    } else if (name == "org.jetbrains.skiko.wasm.patchWebGlContext" ||
               name.find("register_$external_fun") != std::string::npos ||
               name.find("unregister_$external_fun") != std::string::npos ||
               name == "org.w3c.dom.addEventListener_$external_fun" ||
               name == "org.w3c.dom.removeEventListener_$external_fun" ||
               name == "org.w3c.dom.setAttribute_$external_fun" ||
               name == "org.w3c.dom.focus_$external_fun" ||
               name == "org.w3c.dom.blur_$external_fun" ||
               name == "kotlin.js.name_$external_prop_setter") {
        // Deliberate no-op host shims.
    } else {
        if (nresults > 0) {
            if (name.find("instanceof") != std::string::npos ||
                name.find("Instanceof") != std::string::npos ||
                name.find("matches_$external_fun") != std::string::npos ||
                name.find("contains_$external_fun") != std::string::npos ||
                name.find("has_$external_fun") != std::string::npos ||
                name.find("delete_$external_fun") != std::string::npos) {
                results[0].kind = WASMTIME_I32;
                results[0].of.i32 = 0;
            } else {
                object_result(ObjectKind::Generic);
            }
        }
    }
    return nullptr;
}
