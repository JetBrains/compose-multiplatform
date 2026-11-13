#include "skiko_host.h"

#include <cstring>
#include <cstdlib>
#include <iostream>
#include <unordered_map>
#include <utility>
#include <vector>

#include "include/gpu/ganesh/gl/mac/GrGLMakeMacInterface.h"

namespace {

void DeleteFramebufferObjects(
    GLuint framebuffer,
    GLuint color,
    GLuint depth_stencil,
    GLuint default_vao
) {
    if (default_vao != 0) {
        glDeleteVertexArrays(1, &default_vao);
    }
    if (depth_stencil != 0) {
        glDeleteRenderbuffers(1, &depth_stencil);
    }
    if (color != 0) {
        glDeleteTextures(1, &color);
    }
    if (framebuffer != 0) {
        glDeleteFramebuffers(1, &framebuffer);
    }
}

} // namespace

#ifdef __APPLE__
struct SkikoHost::OffscreenContext {
    CGLContextObj context = nullptr;
    GLuint framebuffer = 0;
    GLuint color = 0;
    GLuint depth_stencil = 0;
    GLuint default_vao = 0;
    int width = 0;
    int height = 0;
    sk_sp<const GrGLInterface> gl_interface;
    uint64_t gl_calls = 0;
    uint64_t clears = 0;
    uint64_t draw_arrays = 0;
    uint64_t draw_elements = 0;
    uint64_t shader_compiles = 0;
    uint64_t program_links = 0;
    GrGLenum first_error = 0;
    std::string first_error_call;
};
#endif

SkikoHost::SkikoHost(void* share_context)
    : share_context_(static_cast<CGLContextObj>(share_context)) {}

SkikoHost::~SkikoHost() {
    for (auto& item : contexts_) {
        CGLSetCurrentContext(item->context);
        DeleteFramebufferObjects(
            item->framebuffer,
            item->color,
            item->depth_stencil,
            item->default_vao
        );
        CGLDestroyContext(item->context);
    }
    CGLSetCurrentContext(nullptr);
}

bool SkikoHost::CreateOffscreenContext(
    int width,
    int height,
    std::string& error
) {
    auto item = std::make_unique<OffscreenContext>();
    CGLPixelFormatAttribute attributes[] = {
        kCGLPFAOpenGLProfile,
        static_cast<CGLPixelFormatAttribute>(kCGLOGLPVersion_3_2_Core),
        kCGLPFAAccelerated,
        kCGLPFAColorSize,
        static_cast<CGLPixelFormatAttribute>(24),
        kCGLPFAAlphaSize,
        static_cast<CGLPixelFormatAttribute>(8),
        kCGLPFADepthSize,
        static_cast<CGLPixelFormatAttribute>(24),
        kCGLPFAStencilSize,
        static_cast<CGLPixelFormatAttribute>(8),
        static_cast<CGLPixelFormatAttribute>(0),
    };

    CGLPixelFormatObj pixel_format = nullptr;
    GLint count = 0;
    if (CGLChoosePixelFormat(attributes, &pixel_format, &count) != kCGLNoError ||
        pixel_format == nullptr) {
        error = "CGLChoosePixelFormat failed";
        return false;
    }

    const CGLError create_result =
        CGLCreateContext(pixel_format, share_context_, &item->context);
    CGLDestroyPixelFormat(pixel_format);
    if (create_result != kCGLNoError || item->context == nullptr) {
        error = "CGLCreateContext failed";
        return false;
    }

    if (CGLSetCurrentContext(item->context) != kCGLNoError) {
        error = "CGLSetCurrentContext failed";
        return false;
    }

    item->gl_interface = GrGLInterfaces::MakeMac();
    if (!item->gl_interface || !item->gl_interface->validate()) {
        error = "Skia could not create a valid native GrGLInterface";
        return false;
    }

    item->width = width;
    item->height = height;

    glGenFramebuffers(1, &item->framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, item->framebuffer);

    glGenTextures(1, &item->color);
    glBindTexture(GL_TEXTURE_2D, item->color);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(
        GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
        GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    glFramebufferTexture2D(
        GL_FRAMEBUFFER,
        GL_COLOR_ATTACHMENT0,
        GL_TEXTURE_2D,
        item->color,
        0
    );

    glGenRenderbuffers(1, &item->depth_stencil);
    glBindRenderbuffer(GL_RENDERBUFFER, item->depth_stencil);
    glRenderbufferStorage(
        GL_RENDERBUFFER,
        GL_DEPTH24_STENCIL8,
        width,
        height
    );
    glFramebufferRenderbuffer(
        GL_FRAMEBUFFER,
        GL_DEPTH_STENCIL_ATTACHMENT,
        GL_RENDERBUFFER,
        item->depth_stencil
    );

    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) !=
        GL_FRAMEBUFFER_COMPLETE) {
        error = "Native OpenGL framebuffer is incomplete";
        return false;
    }

    glGenVertexArrays(1, &item->default_vao);
    glBindVertexArray(item->default_vao);

    glViewport(0, 0, width, height);
    current_ = item.get();
    contexts_.push_back(std::move(item));
    return true;
}

int SkikoHost::CreateCanvasContext(
    int width,
    int height,
    std::string& error
) {
    if (!bootstrap_context_claimed_ && contexts_.size() == 1 &&
        contexts_[0]->width == width && contexts_[0]->height == height) {
        bootstrap_context_claimed_ = true;
        if (!MakeContextCurrent(1, error)) return 0;
        return 1;
    }
    if (!CreateOffscreenContext(width, height, error)) return 0;
    bootstrap_context_claimed_ = true;
    return static_cast<int>(contexts_.size());
}

bool SkikoHost::MakeContextCurrent(int context_id, std::string& error) {
    if (context_id <= 0 ||
        static_cast<size_t>(context_id) > contexts_.size()) {
        error = "Invalid GL context id " + std::to_string(context_id);
        return false;
    }
    OffscreenContext* item = contexts_[static_cast<size_t>(context_id - 1)].get();
    if (CGLSetCurrentContext(item->context) != kCGLNoError) {
        error = "CGLSetCurrentContext failed";
        return false;
    }
    current_ = item;
    glBindFramebuffer(GL_FRAMEBUFFER, item->framebuffer);
    glBindVertexArray(item->default_vao);
    glViewport(0, 0, item->width, item->height);
    return true;
}

GLuint SkikoHost::ResolveVertexArray(GLuint vao) const {
    if (vao == 0 && current_ != nullptr) {
        return current_->default_vao;
    }
    return vao;
}

GLuint SkikoHost::DefaultVAO() const {
    return current_ != nullptr ? current_->default_vao : 0;
}

int SkikoHost::CurrentContextId() const {
    for (size_t index = 0; index < contexts_.size(); ++index) {
        if (contexts_[index].get() == current_) {
            return static_cast<int>(index + 1);
        }
    }
    return 0;
}

const GrGLInterface* SkikoHost::CurrentGL() const {
    return current_ == nullptr ? nullptr : current_->gl_interface.get();
}

bool SkikoHost::AttachRuntime(
    wasmtime_context_t* context,
    const wasmtime_instance_t& instance,
    std::string& error
) {
    wasmtime_extern_t item;
    if (!wasmtime_instance_export_get(
            context,
            &instance,
            "memory",
            sizeof("memory") - 1,
            &item) ||
        item.kind != WASMTIME_EXTERN_MEMORY) {
        error = "skiko.wasm does not export memory";
        return false;
    }
    memory_ = item.of.memory;
    has_memory_ = true;

    if (!wasmtime_instance_export_get(
            context,
            &instance,
            "malloc",
            sizeof("malloc") - 1,
            &item) ||
        item.kind != WASMTIME_EXTERN_FUNC) {
        error = "skiko.wasm does not export malloc";
        return false;
    }
    malloc_ = item.of.func;
    has_malloc_ = true;
    return true;
}

GLuint SkikoHost::ResolveFramebuffer(GLuint framebuffer) const {
    return framebuffer == 0 && current_ != nullptr
        ? current_->framebuffer
        : framebuffer;
}

GLuint SkikoHost::Framebuffer() const {
    return current_ == nullptr ? 0 : current_->framebuffer;
}

GLuint SkikoHost::ContextTexture(int context_id) const {
    if (context_id <= 0 ||
        static_cast<size_t>(context_id) > contexts_.size()) return 0;
    return contexts_[static_cast<size_t>(context_id - 1)]->color;
}

int SkikoHost::ContextWidth(int context_id) const {
    if (context_id <= 0 ||
        static_cast<size_t>(context_id) > contexts_.size()) return 0;
    return contexts_[static_cast<size_t>(context_id - 1)]->width;
}

int SkikoHost::ContextHeight(int context_id) const {
    if (context_id <= 0 ||
        static_cast<size_t>(context_id) > contexts_.size()) return 0;
    return contexts_[static_cast<size_t>(context_id - 1)]->height;
}

void SkikoHost::FlushContexts() {
    for (const auto& item : contexts_) {
        CGLSetCurrentContext(item->context);
        glFlush();
    }
}

void SkikoHost::RecordGLCall(const char* name, GLuint object) {
    if (current_ == nullptr) return;
    ++current_->gl_calls;
    if (std::strcmp(name, "glClear") == 0) ++current_->clears;
    if (std::strncmp(name, "glDrawArrays", 12) == 0) {
        ++current_->draw_arrays;
    }
    if (std::strncmp(name, "glDrawElements", 14) == 0 ||
        std::strcmp(name, "glDrawRangeElements") == 0) {
        ++current_->draw_elements;
    }
    if (std::strcmp(name, "glCompileShader") == 0) {
        ++current_->shader_compiles;
    }
    if (std::strcmp(name, "glLinkProgram") == 0) ++current_->program_links;

    // Error polling is intentionally opt-in because glGetError consumes state.
    const bool diagnostics =
        std::getenv("SKIKO_WASMTIME_GL_DIAGNOSTICS") != nullptr;
    if (current_->first_error == 0 && diagnostics) {
        const GrGLenum error = current_->gl_interface->fFunctions.fGetError();
        if (error != 0) {
            current_->first_error = error;
            current_->first_error_call = name;
        }
    }

    constexpr GrGLenum kCompileStatus = 0x8B81;
    constexpr GrGLenum kLinkStatus = 0x8B82;
    constexpr GrGLenum kInfoLogLength = 0x8B84;
    if (diagnostics && object != 0 &&
        (std::strcmp(name, "glCompileShader") == 0 ||
         std::strcmp(name, "glLinkProgram") == 0)) {
        GrGLint status = 0;
        GrGLint length = 0;
        if (std::strcmp(name, "glCompileShader") == 0) {
            current_->gl_interface->fFunctions.fGetShaderiv(
                object, kCompileStatus, &status);
            current_->gl_interface->fFunctions.fGetShaderiv(
                object, kInfoLogLength, &length);
        } else {
            current_->gl_interface->fFunctions.fGetProgramiv(
                object, kLinkStatus, &status);
            current_->gl_interface->fFunctions.fGetProgramiv(
                object, kInfoLogLength, &length);
        }
        if (status == 0) {
            std::vector<GrGLchar> log(
                static_cast<size_t>(length > 1 ? length : 1), 0);
            GrGLsizei written = 0;
            if (std::strcmp(name, "glCompileShader") == 0) {
                current_->gl_interface->fFunctions.fGetShaderInfoLog(
                    object, length, &written, log.data());
            } else {
                current_->gl_interface->fFunctions.fGetProgramInfoLog(
                    object, length, &written, log.data());
            }
            std::cerr << "[GL context " << CurrentContextId() << "] "
                      << name << " failed: " << log.data() << "\n";
        }
    }
}

void SkikoHost::PrintGLDiagnostics(int context_id) const {
    if (context_id <= 0 ||
        static_cast<size_t>(context_id) > contexts_.size()) return;
    const OffscreenContext* item =
        contexts_[static_cast<size_t>(context_id - 1)].get();
    std::cerr << "[GL context " << context_id << "] calls=" << item->gl_calls
              << " clears=" << item->clears
              << " drawArrays=" << item->draw_arrays
              << " drawElements=" << item->draw_elements
              << " shaderCompiles=" << item->shader_compiles
              << " programLinks=" << item->program_links
              << " firstError=0x" << std::hex << item->first_error
              << std::dec;
    if (!item->first_error_call.empty()) {
        std::cerr << " after=" << item->first_error_call;
    }
    std::cerr << "\n";
}

uint32_t SkikoHost::WasmAllocate(
    wasmtime_caller_t* caller,
    uint32_t size,
    wasm_trap_t** trap
) const {
    if (!has_malloc_) {
        *trap = Trap("Wasm malloc is not attached");
        return 0;
    }

    wasmtime_val_raw_t slot{};
    slot.i32 = static_cast<int32_t>(size);
    wasmtime_error_t* error = wasmtime_func_call_unchecked(
        wasmtime_caller_context(caller),
        &malloc_,
        &slot,
        1,
        trap
    );
    if (error != nullptr) {
        wasm_byte_vec_t message;
        wasmtime_error_message(error, &message);
        const std::string text(message.data, message.size);
        wasm_byte_vec_delete(&message);
        wasmtime_error_delete(error);
        *trap = Trap("malloc failed: " + text);
        return 0;
    }
    if (*trap != nullptr) {
        return 0;
    }
    return static_cast<uint32_t>(slot.i32);
}

wasm_trap_t* SkikoHost::Trap(const std::string& message) {
    return wasmtime_trap_new(message.data(), message.size());
}

#include "wasmtime-gl-bindings.inc"
#include "wasmtime-special-gl-bindings.inc"
