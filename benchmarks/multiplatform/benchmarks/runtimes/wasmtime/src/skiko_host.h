#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include <wasmtime.h>

#ifdef __APPLE__
#include <OpenGL/OpenGL.h>
#include <OpenGL/gl3.h>
#include "include/gpu/ganesh/gl/GrGLInterface.h"
#else
class GrGLInterface;
using GLuint = unsigned int;
#endif

class SkikoHost {
public:
    explicit SkikoHost(void* share_context = nullptr);
    ~SkikoHost();

    SkikoHost(const SkikoHost&) = delete;
    SkikoHost& operator=(const SkikoHost&) = delete;

    bool CreateOffscreenContext(int width, int height, std::string& error);
    int CreateCanvasContext(int width, int height, std::string& error);
    bool MakeContextCurrent(int context_id, std::string& error);
    int CurrentContextId() const;
    bool AttachRuntime(
        wasmtime_context_t* context,
        const wasmtime_instance_t& instance,
        std::string& error
    );
    const GrGLInterface* CurrentGL() const;
    GLuint ResolveFramebuffer(GLuint framebuffer) const;
    GLuint Framebuffer() const;
    GLuint ResolveVertexArray(GLuint vao) const;
    GLuint DefaultVAO() const;
    GLuint ContextTexture(int context_id) const;
    int ContextWidth(int context_id) const;
    int ContextHeight(int context_id) const;
    void FlushContexts();
    void RecordGLCall(const char* name, GLuint object = 0);
    void PrintGLDiagnostics(int context_id) const;
    uint32_t WasmAllocate(
        wasmtime_caller_t* caller,
        uint32_t size,
        wasm_trap_t** trap
    ) const;

    template <typename T>
    T* WasmPointer(wasmtime_caller_t* caller, uint32_t offset) const {
        if (offset == 0) {
            return nullptr;
        }

        wasmtime_extern_t item;
        if (!wasmtime_caller_export_get(
                caller,
                "memory",
                sizeof("memory") - 1,
                &item) ||
            item.kind != WASMTIME_EXTERN_MEMORY) {
            return nullptr;
        }

        wasmtime_context_t* context = wasmtime_caller_context(caller);
        uint8_t* base = wasmtime_memory_data(context, &item.of.memory);
        const size_t size = wasmtime_memory_data_size(context, &item.of.memory);
        if (static_cast<size_t>(offset) > size || sizeof(T) > size - offset) {
            return nullptr;
        }

        return reinterpret_cast<T*>(base + offset);
    }

    template <typename T>
    T* WasmSpan(
        wasmtime_caller_t* caller,
        uint32_t offset,
        size_t count
    ) const {
        if (offset == 0) {
            return nullptr;
        }
        if (count > SIZE_MAX / sizeof(T)) {
            return nullptr;
        }

        wasmtime_extern_t item;
        if (!wasmtime_caller_export_get(
                caller,
                "memory",
                sizeof("memory") - 1,
                &item) ||
            item.kind != WASMTIME_EXTERN_MEMORY) {
            return nullptr;
        }

        wasmtime_context_t* context = wasmtime_caller_context(caller);
        uint8_t* base = wasmtime_memory_data(context, &item.of.memory);
        const size_t size = wasmtime_memory_data_size(context, &item.of.memory);
        const size_t length = count * sizeof(T);
        if (static_cast<size_t>(offset) > size || length > size - offset) {
            return nullptr;
        }
        return reinterpret_cast<T*>(base + offset);
    }

    static wasm_trap_t* Trap(const std::string& message);

private:
#ifdef __APPLE__
    struct OffscreenContext;
    std::vector<std::unique_ptr<OffscreenContext>> contexts_;
    OffscreenContext* current_ = nullptr;
    bool bootstrap_context_claimed_ = false;
    CGLContextObj share_context_ = nullptr;
#endif
    bool has_memory_ = false;
    bool has_malloc_ = false;
    wasmtime_memory_t memory_{};
    wasmtime_func_t malloc_{};
};

using GLCallback = wasmtime_func_unchecked_callback_t;

struct GLBinding {
    const char* name;
    GLCallback callback;
};

const GLBinding* FindGeneratedGLBinding(const std::string& name);
const GLBinding* FindSpecialGLBinding(const std::string& name);
