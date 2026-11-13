/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "skiko_host.h"
#include "kotlin_app_host.h"
#include "../../core/skiko_sdl_core.h"

#include <chrono>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <string_view>
#include <thread>
#include <vector>

#include <wasi.h>
#include <wasmtime.h>

namespace fs = std::filesystem;

namespace {

template <typename T, void (*Delete)(T*)>
struct HandleDeleter {
    void operator()(T* value) const {
        if (value != nullptr) {
            Delete(value);
        }
    }
};

template <typename T, void (*Delete)(T*)>
using Handle = std::unique_ptr<T, HandleDeleter<T, Delete>>;

using Engine = Handle<wasm_engine_t, wasm_engine_delete>;
using Store = Handle<wasmtime_store_t, wasmtime_store_delete>;
using Module = Handle<wasmtime_module_t, wasmtime_module_delete>;
using Linker = Handle<wasmtime_linker_t, wasmtime_linker_delete>;

std::vector<uint8_t> ReadFile(const std::string& path) {
    std::ifstream input(path, std::ios::binary | std::ios::ate);
    if (!input) {
        throw std::runtime_error("Cannot open " + path);
    }

    const std::streamsize size = input.tellg();
    input.seekg(0, std::ios::beg);
    std::vector<uint8_t> bytes(static_cast<size_t>(size));
    if (!input.read(reinterpret_cast<char*>(bytes.data()), size)) {
        throw std::runtime_error("Cannot read " + path);
    }
    return bytes;
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

void Check(wasmtime_error_t* error, std::string_view operation) {
    if (error != nullptr) {
        throw std::runtime_error(std::string(operation) + ": " + ErrorMessage(error));
    }
}

wasm_trap_t* NoOpImport(
    void*,
    wasmtime_caller_t*,
    wasmtime_val_raw_t* slots,
    size_t
) {
    if (slots != nullptr) {
        slots[0].i64 = 0;
    }
    return nullptr;
}

wasm_trap_t* SetJmp(
    void*,
    wasmtime_caller_t*,
    wasmtime_val_raw_t* slots,
    size_t
) {
    slots[0].i32 = 0;
    return nullptr;
}

wasm_trap_t* EmscriptenResizeHeap(
    void*,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t
) {
    if (caller == nullptr || slots == nullptr) {
        if (slots != nullptr) slots[0].i32 = 0;
        return nullptr;
    }
    const uint32_t requested_size = slots[0].i32;
    wasmtime_extern_t item;
    if (!wasmtime_caller_export_get(
            caller,
            "memory",
            sizeof("memory") - 1,
            &item) ||
        item.kind != WASMTIME_EXTERN_MEMORY) {
        slots[0].i32 = 0;
        return nullptr;
    }

    wasmtime_context_t* context = wasmtime_caller_context(caller);
    const size_t current_bytes = wasmtime_memory_data_size(context, &item.of.memory);
    if (requested_size <= current_bytes) {
        slots[0].i32 = 1;
        return nullptr;
    }

    const uint64_t current_pages = wasmtime_memory_size(context, &item.of.memory);
    const uint64_t needed_pages = (static_cast<uint64_t>(requested_size) + 65535) / 65536;
    if (needed_pages <= current_pages) {
        slots[0].i32 = 1;
        return nullptr;
    }

    const uint64_t delta_pages = needed_pages - current_pages;
    uint64_t prev_pages = 0;
    wasmtime_error_t* error = wasmtime_memory_grow(
        context,
        &item.of.memory,
        delta_pages,
        &prev_pages
    );
    if (error != nullptr) {
        wasmtime_error_delete(error);
        slots[0].i32 = 0;
        return nullptr;
    }

    slots[0].i32 = 1;
    return nullptr;
}

wasm_trap_t* EmscriptenDateNow(
    void*,
    wasmtime_caller_t*,
    wasmtime_val_raw_t* slots,
    size_t
) {
    const auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    slots[0].f64 = static_cast<double>(now);
    return nullptr;
}

wasm_trap_t* AbortJs(
    void*,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* args,
    size_t nargs
) {
    std::string message;
    if (nargs > 0 && caller != nullptr) {
        wasmtime_context_t* context = wasmtime_caller_context(caller);
        wasmtime_extern_t item;
        if (wasmtime_caller_export_get(caller, "memory", 6, &item) && item.kind == WASMTIME_EXTERN_MEMORY) {
            uint8_t* mem = wasmtime_memory_data(context, &item.of.memory);
            size_t size = wasmtime_memory_data_size(context, &item.of.memory);
            int32_t ptr = args[0].i32;
            if (ptr >= 0 && static_cast<size_t>(ptr) < size) {
                const char* str = reinterpret_cast<const char*>(mem + ptr);
                size_t len = strnlen(str, 1024);
                message = std::string(str, len);
            }
        }
    }
    std::cerr << "skiko abort() called" << (message.empty() ? "" : ": ") << message << "\n";
    return nullptr;
}

struct ImportRegistration {
    size_t ordinary_gl = 0;
    size_t special_gl = 0;
    size_t ignored = 0;
};

ImportRegistration RegisterEnvironmentImports(
    wasmtime_linker_t* linker,
    const wasmtime_module_t* module,
    SkikoHost* host
) {
    ImportRegistration result;
    wasm_importtype_vec_t imports;
    wasmtime_module_imports(module, &imports);

    for (size_t index = 0; index < imports.size; ++index) {
        const wasm_importtype_t* import = imports.data[index];
        const wasm_name_t* module_name = wasm_importtype_module(import);
        const wasm_name_t* item_name = wasm_importtype_name(import);

        if (module_name == nullptr || item_name == nullptr) {
            continue;
        }

        const std::string_view module_view(module_name->data, module_name->size);
        const std::string name(item_name->data, item_name->size);

        if (module_view != "env") {
            continue;
        }

        if (name == "setjmp") {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    SetJmp,
                    nullptr,
                    nullptr
                ),
                "Defining env.setjmp"
            );
            continue;
        }

        if (name == "emscripten_resize_heap") {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    EmscriptenResizeHeap,
                    nullptr,
                    nullptr
                ),
                "Defining env.emscripten_resize_heap"
            );
            continue;
        }

        if (name == "emscripten_date_now") {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    EmscriptenDateNow,
                    nullptr,
                    nullptr
                ),
                "Defining env.emscripten_date_now"
            );
            continue;
        }

        if (name == "_abort_js" || name == "abort") {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    AbortJs,
                    nullptr,
                    nullptr
                ),
                "Defining env." + name
            );
            continue;
        }

        const GLBinding* binding = FindSpecialGLBinding(name);
        if (binding == nullptr && name.rfind("emscripten_", 0) == 0) {
            binding = FindSpecialGLBinding(name.substr(11));
        }
        if (binding != nullptr) {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    binding->callback,
                    host,
                    nullptr
                ),
                "Defining special GL import " + name
            );
            result.special_gl += 1;
            continue;
        }

        binding = FindGeneratedGLBinding(name);
        if (binding == nullptr && name.rfind("emscripten_", 0) == 0) {
            binding = FindGeneratedGLBinding(name.substr(11));
        }
        if (binding != nullptr) {
            const wasm_externtype_t* extern_type = wasm_importtype_type(import);
            const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
            Check(
                wasmtime_linker_define_func_unchecked(
                    linker,
                    module_name->data,
                    module_name->size,
                    item_name->data,
                    item_name->size,
                    function_type,
                    binding->callback,
                    host,
                    nullptr
                ),
                "Defining generated GL import " + name
            );
            result.ordinary_gl += 1;
            continue;
        }

        const wasm_externtype_t* extern_type = wasm_importtype_type(import);
        const wasm_functype_t* function_type = wasm_externtype_as_functype_const(extern_type);
        Check(
            wasmtime_linker_define_func_unchecked(
                linker,
                module_name->data,
                module_name->size,
                item_name->data,
                item_name->size,
                function_type,
                NoOpImport,
                nullptr,
                nullptr
            ),
            "Defining fallback import " + name
        );
        result.ignored += 1;
    }

    wasm_importtype_vec_delete(&imports);
    return result;
}

void CallInitializer(
    wasmtime_context_t* context,
    const wasmtime_instance_t& instance
) {
    const char* names[] = {"_initialize", "__wasm_call_ctors"};
    for (const char* name : names) {
        wasmtime_extern_t item;
        if (!wasmtime_instance_export_get(
                context,
                &instance,
                name,
                std::strlen(name),
                &item) ||
            item.kind != WASMTIME_EXTERN_FUNC) {
            continue;
        }

        wasm_trap_t* trap = nullptr;
        wasmtime_error_t* error = wasmtime_func_call(
            context,
            &item.of.func,
            nullptr,
            0,
            nullptr,
            0,
            &trap
        );
        if (error != nullptr) {
            throw std::runtime_error(std::string("Calling ") + name + ": " + ErrorMessage(error));
        }
        if (trap != nullptr) {
            throw std::runtime_error(std::string("Calling ") + name + ": " + TrapMessage(trap));
        }
        return;
    }
}

Module LoadModule(
    wasm_engine_t* engine,
    const std::string& path,
    const std::string& label
) {
    std::string cwasm_path = path;
    if (cwasm_path.ends_with(".wasm")) {
        cwasm_path.replace(cwasm_path.size() - 5, 5, ".cwasm");
    }
    if (fs::exists(cwasm_path) && fs::last_write_time(cwasm_path) >= fs::last_write_time(path)) {
        wasmtime_module_t* module_val = nullptr;
        wasmtime_error_t* err = wasmtime_module_deserialize_file(engine, cwasm_path.c_str(), &module_val);
        if (err == nullptr && module_val != nullptr) {
            std::cout << "Loaded precompiled " << label << " from " << cwasm_path << std::endl;
            return Module(module_val);
        }
        if (err != nullptr) wasmtime_error_delete(err);
    }

    std::string actual_path = path;
    std::string stripped_path = path;
    if (stripped_path.ends_with(".wasm")) {
        stripped_path.replace(stripped_path.size() - 5, 5, "-opt.wasm");
    }
    if (fs::exists(stripped_path) && fs::last_write_time(stripped_path) >= fs::last_write_time(path)) {
        actual_path = stripped_path;
    }

    std::cout << "Reading " << label << " from " << actual_path << "..." << std::endl;
    std::vector<uint8_t> bytes = ReadFile(actual_path);
    wasmtime_module_t* module_val = nullptr;
    std::cout << "Compiling " << label << " (" << bytes.size() << " bytes)..." << std::endl;
    wasmtime_error_t* comp_err = wasmtime_module_new(engine, bytes.data(), bytes.size(), &module_val);
    if (comp_err != nullptr) {
        std::string err_msg = ErrorMessage(comp_err);
        if (err_msg.find("legacy_exceptions") != std::string::npos || err_msg.find("try instruction") != std::string::npos) {
            std::cout << "Attempting to strip legacy exceptions via wasm-opt for " << label << "..." << std::endl;
            std::string cmd = "wasm-opt --all-features --strip-eh \"" + path + "\" -o \"" + stripped_path + "\"";
            if (std::system(cmd.c_str()) == 0 && fs::exists(stripped_path)) {
                actual_path = stripped_path;
                bytes = ReadFile(actual_path);
                std::cout << "Recompiling stripped " << label << " (" << bytes.size() << " bytes)..." << std::endl;
                Check(
                    wasmtime_module_new(engine, bytes.data(), bytes.size(), &module_val),
                    "Compiling " + label
                );
            } else {
                throw std::runtime_error("Compiling " + label + ": " + err_msg);
            }
        } else {
            throw std::runtime_error("Compiling " + label + ": " + err_msg);
        }
    }

    // Cache precompiled artifact
    wasm_byte_vec_t serialized;
    wasmtime_error_t* ser_err = wasmtime_module_serialize(module_val, &serialized);
    if (ser_err == nullptr) {
        std::ofstream out(cwasm_path, std::ios::binary);
        if (out) {
            out.write(serialized.data, serialized.size);
            std::cout << "Saved precompiled " << label << " cache to " << cwasm_path << std::endl;
        }
        wasm_byte_vec_delete(&serialized);
    } else {
        wasmtime_error_delete(ser_err);
    }

    return Module(module_val);
}

} // namespace

int main(int argc, char** argv) {
    try {
        std::string skiko_wasm_path;
        std::string application_wasm_path;

        for (int i = 1; i < argc; ++i) {
            const std::string_view arg(argv[i]);
            if (arg == "--wasm" && i + 1 < argc) {
                application_wasm_path = argv[++i];
            } else if (arg == "--skiko" && i + 1 < argc) {
                skiko_wasm_path = argv[++i];
            } else if (skiko_wasm_path.empty()) {
                skiko_wasm_path = argv[i];
            } else if (application_wasm_path.empty()) {
                application_wasm_path = argv[i];
            }
        }

        // Auto-discover skiko.wasm and application wasm if not passed
        if (skiko_wasm_path.empty()) {
            if (const char* env_skiko = std::getenv("SKIKO_WASM")) {
                if (fs::exists(env_skiko)) {
                    skiko_wasm_path = env_skiko;
                }
            }
        }
        if (skiko_wasm_path.empty()) {
            const std::vector<std::string> search_candidates = {
                "skiko.wasm",
                "wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm",
                "build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm",
                "benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm",
                "benchmarks/multiplatform/benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm",
                "../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm",
                "/Users/dustin.feucht/projects/skiko/samples/SkiaNonWebSample/build/compose/skiko-wasmJs-runtime/skiko.wasm",
                "/Users/dustin.feucht/projects/skiko/samples/SkiaNonWebSample/build/processedResources/wasmJs/main/skiko.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/compose/skiko-wasmJs-runtime/skiko.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/processedResources/wasmJs/main/skiko.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/skiko.wasm",
                "compose/mpp/demo/build/compose/skiko-wasmJs-runtime/skiko.wasm",
                "compose/mpp/demo/build/processedResources/wasmJs/main/skiko.wasm",
                "compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/skiko.wasm",
                "build/compose/skiko-wasmJs-runtime/skiko.wasm",
                "build/processedResources/wasmJs/main/skiko.wasm",
                "build/compileSync/wasmJs/main/developmentExecutable/kotlin/skiko.wasm"
            };
            for (const auto& candidate : search_candidates) {
                if (fs::exists(candidate)) {
                    skiko_wasm_path = candidate;
                    break;
                }
            }
        }

        if (application_wasm_path.empty()) {
            const std::vector<std::string> search_candidates = {
                "compose-benchmarks-benchmarks.wasm",
                "wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm",
                "build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm",
                "benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm",
                "benchmarks/multiplatform/benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm",
                "../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/mpp-demo.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/compileSync/wasmJs/main/productionExecutable/kotlin/mpp-demo.wasm",
                "out/compose-multiplatform-core/compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/compose-multiplatform-core-compose-mpp-demo.wasm",
                "compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/mpp-demo.wasm",
                "compose/mpp/demo/build/compileSync/wasmJs/main/productionExecutable/kotlin/mpp-demo.wasm",
                "compose/mpp/demo/build/compileSync/wasmJs/main/developmentExecutable/kotlin/compose-multiplatform-core-compose-mpp-demo.wasm",
                "build/compileSync/wasmJs/main/developmentExecutable/kotlin/mpp-demo.wasm",
                "build/compileSync/wasmJs/main/developmentExecutable/kotlin/compose-multiplatform-core-compose-mpp-demo.wasm"
            };
            for (const auto& candidate : search_candidates) {
                if (fs::exists(candidate)) {
                    application_wasm_path = candidate;
                    break;
                }
            }
        }

        if (skiko_wasm_path.empty() || !fs::exists(skiko_wasm_path)) {
            std::cerr << "skiko.wasm not found. Please provide path as argument.\n";
            return 1;
        }

        std::cout << "Loading Skiko WASM from: " << skiko_wasm_path << std::endl;
        if (!application_wasm_path.empty()) {
            std::cout << "Loading Compose App WASM from: " << application_wasm_path << std::endl;
        }

        std::cout << "Creating wasm engine..." << std::endl;
        wasm_config_t* config = wasm_config_new();
        if (config == nullptr) {
            throw std::runtime_error("wasm_config_new failed");
        }
        wasmtime_config_wasm_gc_set(config, true);
        wasmtime_config_wasm_function_references_set(config, true);
        wasmtime_config_wasm_exceptions_set(config, true);
        wasmtime_config_parallel_compilation_set(config, true);
        wasmtime_config_cranelift_opt_level_set(config, WASMTIME_OPT_LEVEL_SPEED);
        wasmtime_error_t* cache_err = wasmtime_config_cache_config_load(config, nullptr);
        if (cache_err != nullptr) {
            wasmtime_error_delete(cache_err);
        }
        Engine engine(wasm_engine_new_with_config(config));
        if (!engine) {
            throw std::runtime_error("wasm_engine_new failed");
        }

        Module skiko_module = LoadModule(engine.get(), skiko_wasm_path, "skiko.wasm");
        std::unique_ptr<Module> application_module;
        if (!application_wasm_path.empty()) {
            application_module = std::make_unique<Module>(
                LoadModule(engine.get(), application_wasm_path, "Compose application")
            );
        }

        std::cout << "Creating SDL Window..." << std::endl;
        constexpr int window_width = 1024;
        constexpr int window_height = 768;
        skiko::core::SkikoSdlWindow window(
            window_width, window_height, "Compose MPP Demo (WASM + Wasmtime Native SDL)"
        );

        std::cout << "Creating SkikoHost..." << std::endl;
        SkikoHost host(window.NativeGLContext());
        std::string gl_error;
        if (!host.CreateOffscreenContext(window_width * 2, window_height * 2, gl_error)) {
            throw std::runtime_error(gl_error);
        }

        std::cout << "Creating Store..." << std::endl;
        Store store(wasmtime_store_new(engine.get(), &host, nullptr));
        if (!store) {
            throw std::runtime_error("wasmtime_store_new failed");
        }
        wasmtime_context_t* context = wasmtime_store_context(store.get());

        std::cout << "Configuring WASI..." << std::endl;
        wasi_config_t* wasi = wasi_config_new();
        if (wasi == nullptr) {
            throw std::runtime_error("wasi_config_new failed");
        }
        wasi_config_inherit_stdout(wasi);
        wasi_config_inherit_stderr(wasi);
        wasi_config_inherit_stdin(wasi);
        wasi_config_inherit_argv(wasi);
        wasi_config_inherit_env(wasi);
        Check(wasmtime_context_set_wasi(context, wasi), "Configuring WASI");

        std::cout << "Creating Linker..." << std::endl;
        Linker linker(wasmtime_linker_new(engine.get()));
        if (!linker) {
            throw std::runtime_error("wasmtime_linker_new failed");
        }
        Check(wasmtime_linker_define_wasi(linker.get()), "Defining WASI");
        RegisterEnvironmentImports(linker.get(), skiko_module.get(), &host);

        std::cout << "Instantiating skiko..." << std::endl;
        wasmtime_instance_t skiko_instance;
        wasm_trap_t* trap = nullptr;
        wasmtime_error_t* instantiate_error = wasmtime_linker_instantiate(
            linker.get(),
            context,
            skiko_module.get(),
            &skiko_instance,
            &trap
        );
        if (instantiate_error != nullptr) {
            throw std::runtime_error("Instantiating skiko.wasm: " + ErrorMessage(instantiate_error));
        }
        if (trap != nullptr) {
            throw std::runtime_error("Instantiating skiko.wasm: " + TrapMessage(trap));
        }

        std::cout << "Attaching runtime..." << std::endl;
        std::string runtime_error;
        if (!host.AttachRuntime(context, skiko_instance, runtime_error)) {
            throw std::runtime_error(runtime_error);
        }

        std::cout << "Calling initializer..." << std::endl;
        CallInitializer(context, skiko_instance);

        if (application_module) {
            std::cout << "Linking skiko.mjs..." << std::endl;
            Check(
                wasmtime_linker_define_instance(
                    linker.get(),
                    context,
                    "./skiko.mjs",
                    sizeof("./skiko.mjs") - 1,
                    &skiko_instance
                ),
                "Linking skiko.wasm exports to the Compose application"
            );

            std::string benchmark_args;
            for (int i = 1; i < argc; ++i) {
                std::string arg = argv[i];
                if (arg == "--wasm" || arg == "--skiko") {
                    ++i;
                    continue;
                }
                if (!benchmark_args.empty()) benchmark_args += " ";
                benchmark_args += arg;
            }

            std::cout << "Registering KotlinAppHost imports..." << std::endl;
            KotlinAppHost application(context, &host, benchmark_args, &window);
            application.RegisterImports(linker.get(), application_module->get());

            std::cout << "Instantiating application..." << std::endl;
            wasmtime_instance_t application_instance;
            trap = nullptr;
            wasmtime_error_t* application_error = wasmtime_linker_instantiate(
                linker.get(),
                context,
                application_module->get(),
                &application_instance,
                &trap
            );
            if (application_error != nullptr) {
                throw std::runtime_error("Instantiating Compose application: " + ErrorMessage(application_error));
            }
            if (trap != nullptr) {
                throw std::runtime_error("Instantiating Compose application: " + TrapMessage(trap));
            }

            std::cout << "Attaching application instance..." << std::endl;
            application.AttachInstance(application_instance);
            std::cout << "Starting application..." << std::endl;
            application.Start();

            std::cout << "Compose Wasmtime Native SDL host running. Close the window to exit." << std::endl;

            auto on_pointer_event = [](
                int32_t event_type,
                double x,
                double y,
                int32_t button,
                double scroll_delta_x,
                double scroll_delta_y,
                void* user_data
            ) {
                auto* app = static_cast<KotlinAppHost*>(user_data);
                if (app != nullptr) {
                    app->DispatchPointerEvent(event_type, x, y, button, scroll_delta_x, scroll_delta_y);
                }
            };

            const auto start = std::chrono::steady_clock::now();
            auto next_frame = start + std::chrono::microseconds(16667);
            uint64_t frame_index = 0;

            while (window.IsOpen() && !application.ShouldExit()) {
                if (window.IsOpen()) {
                    window.PollEvents(on_pointer_event, &application);
                }

                const auto now = std::chrono::steady_clock::now();
                application.RunExpiredTimers(now);
                application.DrainReadyCallbacks();

                if (now >= next_frame) {
                    const double timestamp = std::chrono::duration<double, std::milli>(now - start).count();
                    const size_t callback_count = application.AnimationCallbackCount();
                    if (std::getenv("SKIKO_WASMTIME_TEST_CLICK") != nullptr && frame_index == 15) {
                        application.DispatchPointerEvent(1, 400.0, 400.0, 1, 0.0, 0.0);
                        application.DispatchPointerEvent(2, 400.0, 400.0, 1, 0.0, 0.0);
                    }
                    if (std::getenv("SKIKO_WASMTIME_TEST_SCROLL") != nullptr && (frame_index == 15 || frame_index == 20)) {
                        application.DispatchPointerEvent(6, 400.0, 400.0, 0, 0.0, -20.0);
                    }
                    for (size_t index = 0; index < callback_count; ++index) {
                        try {
                            application.RunNextAnimationCallback(timestamp);
                        } catch (const std::exception& e) {
                            std::cerr << "[KotlinAppHost] Animation callback error: " << e.what() << std::endl;
                        }
                    }
                    application.RunExpiredTimers(now);
                    application.DrainReadyCallbacks();

                    if (window.IsOpen() && callback_count > 0) {
                        host.FlushContexts();
                        const int target_id = host.CurrentContextId() > 0 ? host.CurrentContextId() : 1;
                        int src_w = host.ContextWidth(target_id);
                        int src_h = host.ContextHeight(target_id);
                        if (src_w <= 0) src_w = window_width * 2;
                        if (src_h <= 0) src_h = window_height * 2;
                        const std::vector<HostCanvasTexture> canvases = {
                            {host.ContextTexture(target_id), src_w, src_h, 0, 0, window_width, window_height}
                        };
                        window.Present(canvases);
                        std::string err;
                        host.MakeContextCurrent(target_id, err);
                    }
                    ++frame_index;

                    do {
                        next_frame += std::chrono::microseconds(16667);
                    } while (next_frame <= now);
                }

                const auto wake_deadline = application.NextDeadline(next_frame);
                const auto current_time = std::chrono::steady_clock::now();
                if (wake_deadline > current_time) {
                    std::this_thread::sleep_until(std::min(wake_deadline, current_time + std::chrono::milliseconds(4)));
                }
            }
        }

        std::cout << "Wasmtime SDL Host exited cleanly.\n";
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "wasmtime-sdl-runner error: " << e.what() << "\n";
        return 1;
    }
}
