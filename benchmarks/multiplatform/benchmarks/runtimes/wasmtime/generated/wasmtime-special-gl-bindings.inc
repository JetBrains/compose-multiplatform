// Explicit adapters for Wasm arguments that are not native GL ABI values.

namespace {

#define SPECIAL_GL(name)                                                   \
    wasm_trap_t* Special_##name(                                           \
        void* environment,                                                 \
        wasmtime_caller_t* caller,                                         \
        wasmtime_val_raw_t* slots,                                         \
        size_t slot_count                                                  \
    )

const void* BufferOffset(uint32_t offset) {
    return reinterpret_cast<const void*>(static_cast<uintptr_t>(offset));
}

uint64_t JoinU64(uint32_t low, uint32_t high) {
    return static_cast<uint64_t>(low) |
           (static_cast<uint64_t>(high) << 32u);
}

const void* PixelUnpackPointer(
    SkikoHost* host,
    wasmtime_caller_t* caller,
    uint32_t offset
) {
    if (offset == 0) return nullptr;
    GrGLint unpack_buffer = 0;
    host->CurrentGL()->fFunctions.fGetIntegerv(
        GL_PIXEL_UNPACK_BUFFER_BINDING,
        &unpack_buffer
    );
    return unpack_buffer != 0
        ? BufferOffset(offset)
        : host->WasmPointer<const uint8_t>(caller, offset);
}

void* PixelPackPointer(
    SkikoHost* host,
    wasmtime_caller_t* caller,
    uint32_t offset
) {
    if (offset == 0) return nullptr;
    GrGLint pack_buffer = 0;
    host->CurrentGL()->fFunctions.fGetIntegerv(
        GL_PIXEL_PACK_BUFFER_BINDING,
        &pack_buffer
    );
    return pack_buffer != 0
        ? const_cast<void*>(BufferOffset(offset))
        : host->WasmPointer<uint8_t>(caller, offset);
}

std::unordered_map<std::string, uint32_t> wasm_strings;

uint32_t CopyStringToWasm(
    SkikoHost* host,
    wasmtime_caller_t* caller,
    const GrGLubyte* value,
    wasm_trap_t** trap
) {
    if (value == nullptr) return 0;
    const char* text = reinterpret_cast<const char*>(value);
    const auto existing = wasm_strings.find(text);
    if (existing != wasm_strings.end()) return existing->second;

    const size_t length = std::strlen(text) + 1;
    const uint32_t offset = host->WasmAllocate(
        caller,
        static_cast<uint32_t>(length),
        trap
    );
    if (*trap != nullptr || offset == 0) return 0;

    uint8_t* destination = host->WasmSpan<uint8_t>(caller, offset, length);
    if (destination == nullptr) {
        *trap = SkikoHost::Trap("Allocated string is outside Wasm memory");
        return 0;
    }
    std::memcpy(destination, text, length);
    wasm_strings.emplace(text, offset);
    return offset;
}

struct MappedBufferBridge {
    void* native_pointer = nullptr;
    uint32_t wasm_offset = 0;
    size_t length = 0;
    size_t capacity = 0;
    bool writable = false;
};

std::unordered_map<GrGLenum, MappedBufferBridge> mapped_buffers;

uint32_t MapNativeBufferToWasm(
    SkikoHost* host,
    wasmtime_caller_t* caller,
    GrGLenum target,
    void* native_pointer,
    size_t length,
    bool readable,
    bool writable,
    wasm_trap_t** trap
) {
    if (native_pointer == nullptr) return 0;
    auto& bridge = mapped_buffers[target];
    if (bridge.wasm_offset == 0 || bridge.capacity < length) {
        bridge.wasm_offset = host->WasmAllocate(
            caller,
            static_cast<uint32_t>(length),
            trap
        );
        if (*trap != nullptr || bridge.wasm_offset == 0) return 0;
        bridge.capacity = length;
    }
    bridge.native_pointer = native_pointer;
    bridge.length = length;
    bridge.writable = writable;
    if (readable && length != 0) {
        uint8_t* destination = host->WasmSpan<uint8_t>(
            caller,
            bridge.wasm_offset,
            length
        );
        if (destination == nullptr) {
            *trap = SkikoHost::Trap("Mapped buffer is outside Wasm memory");
            return 0;
        }
        std::memcpy(destination, native_pointer, length);
    }
    return bridge.wasm_offset;
}

std::unordered_map<uint32_t, GrGLsync> sync_handles;
uint32_t next_sync_handle = 1;

uint32_t StoreSync(GrGLsync sync) {
    if (sync == nullptr) return 0;
    const uint32_t handle = next_sync_handle++;
    sync_handles.emplace(handle, sync);
    return handle;
}

GrGLsync LookupSync(uint32_t handle) {
    if (handle == 0) return nullptr;
    const auto found = sync_handles.find(handle);
    return found == sync_handles.end() ? nullptr : found->second;
}

SPECIAL_GL(glBindFramebuffer) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fBindFramebuffer(
        U32(slots[0]),
        host->ResolveFramebuffer(U32(slots[1]))
    );
    host->RecordGLCall("glBindFramebuffer");
    return nullptr;
}

SPECIAL_GL(glBindVertexArray) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fBindVertexArray(
        host->ResolveVertexArray(U32(slots[0]))
    );
    host->RecordGLCall("glBindVertexArray");
    return nullptr;
}

SPECIAL_GL(glGetIntegerv) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLenum pname = U32(slots[0]);
    GrGLint* params = host->WasmPointer<GrGLint>(caller, U32(slots[1]));
    if (params == nullptr) return SkikoHost::Trap("glGetIntegerv: invalid output");
    host->CurrentGL()->fFunctions.fGetIntegerv(pname, params);
    host->RecordGLCall("glGetIntegerv");
    const bool framebuffer_binding =
        pname == GL_FRAMEBUFFER_BINDING ||
        pname == GL_DRAW_FRAMEBUFFER_BINDING ||
        pname == GL_READ_FRAMEBUFFER_BINDING;
    if (framebuffer_binding && static_cast<GLuint>(*params) == host->Framebuffer()) {
        *params = 0;
    }
    if (pname == 0x85B5 /* GL_VERTEX_ARRAY_BINDING */ && static_cast<GLuint>(*params) == host->DefaultVAO()) {
        *params = 0;
    }
    return nullptr;
}

SPECIAL_GL(glGetShaderiv) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLuint shader = U32(slots[0]);
    const GrGLenum pname = U32(slots[1]);
    GrGLint* params = host->WasmPointer<GrGLint>(caller, U32(slots[2]));
    if (params == nullptr) return SkikoHost::Trap("glGetShaderiv: invalid output");
    host->CurrentGL()->fFunctions.fGetShaderiv(shader, pname, params);
    host->RecordGLCall("glGetShaderiv");
    if (pname == 0x8B81 /* GL_COMPILE_STATUS */ && *params == 0) {
        char log_buffer[4096] = {0};
        GLsizei log_len = 0;
        glGetShaderInfoLog(shader, sizeof(log_buffer), &log_len, log_buffer);
        std::cerr << "[Wasmtime GL] Shader compile error: " << log_buffer << std::endl;
    }
    return nullptr;
}

SPECIAL_GL(glGetProgramiv) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLuint program = U32(slots[0]);
    const GrGLenum pname = U32(slots[1]);
    GrGLint* params = host->WasmPointer<GrGLint>(caller, U32(slots[2]));
    if (params == nullptr) return SkikoHost::Trap("glGetProgramiv: invalid output");
    host->CurrentGL()->fFunctions.fGetProgramiv(program, pname, params);
    host->RecordGLCall("glGetProgramiv");
    if (pname == 0x8B82 /* GL_LINK_STATUS */ && *params == 0) {
        char log_buffer[4096] = {0};
        GLsizei log_len = 0;
        glGetProgramInfoLog(program, sizeof(log_buffer), &log_len, log_buffer);
        std::cerr << "[Wasmtime GL] Program link error: " << log_buffer << std::endl;
    }
    return nullptr;
}

SPECIAL_GL(glGetString) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    wasm_trap_t* trap = nullptr;
    const GrGLenum pname = U32(slots[0]);
    const char* override_str = nullptr;
    if (pname == 0x1F00 /* GL_VENDOR */) {
        override_str = "WebKit";
    } else if (pname == 0x1F01 /* GL_RENDERER */) {
        override_str = "WebKit WebGL";
    } else if (pname == 0x1F02 /* GL_VERSION */) {
        override_str = "OpenGL ES 3.0 (WebGL 2.0 (OpenGL ES 3.0 Chromium))";
    } else if (pname == 0x1F03 /* GL_EXTENSIONS */) {
        override_str = "EXT_color_buffer_float OES_texture_float EXT_texture_filter_anisotropic WEBGL_draw_buffers";
    } else if (pname == 0x8B8C /* GL_SHADING_LANGUAGE_VERSION */) {
        override_str = "OpenGL ES GLSL ES 3.00 (WebGL GLSL ES 3.00 (OpenGL ES GLSL ES 3.0 Chromium))";
    }

    const GrGLubyte* native_val = override_str ? reinterpret_cast<const GrGLubyte*>(override_str) : (host->CurrentGL() ? host->CurrentGL()->fFunctions.fGetString(pname) : nullptr);
    slots[0].i32 = static_cast<int32_t>(CopyStringToWasm(
        host,
        caller,
        native_val,
        &trap
    ));
    host->RecordGLCall("glGetString");
    return trap;
}

SPECIAL_GL(glGetStringi) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    wasm_trap_t* trap = nullptr;
    slots[0].i32 = static_cast<int32_t>(CopyStringToWasm(
        host,
        caller,
        host->CurrentGL()->fFunctions.fGetStringi(
            U32(slots[0]),
            U32(slots[1])
        ),
        &trap
    ));
    host->RecordGLCall("glGetStringi");
    return trap;
}

std::string AdaptShaderSource(const char* src, size_t length) {
    std::string s(src, length);
    size_t pos = s.find("#version 300 es");
    if (pos != std::string::npos) {
        s.replace(pos, 15, "#version 150\n#define highp\n#define mediump\n#define lowp\n");
    }
    pos = 0;
    while ((pos = s.find("precision ", pos)) != std::string::npos) {
        if (pos == 0 || s[pos - 1] == '\n' || s[pos - 1] == ' ' || s[pos - 1] == '\t') {
            s.insert(pos, "// ");
            pos += 3;
        }
        pos += 10;
    }
    return s;
}

SPECIAL_GL(glShaderSource) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLuint shader = U32(slots[0]);
    const GrGLsizei count = I32(slots[1]);
    const uint32_t* offsets = host->WasmSpan<const uint32_t>(
        caller,
        U32(slots[2]),
        static_cast<size_t>(count)
    );
    if (count > 0 && offsets == nullptr) {
        return SkikoHost::Trap("glShaderSource: invalid string offsets");
    }
    std::vector<std::string> adapted_storage(static_cast<size_t>(count));
    std::vector<const GrGLchar*> strings(static_cast<size_t>(count));
    std::vector<GrGLint> lengths(static_cast<size_t>(count));
    const GrGLint* orig_lengths = U32(slots[3]) == 0
        ? nullptr
        : host->WasmSpan<const GrGLint>(
            caller,
            U32(slots[3]),
            static_cast<size_t>(count)
        );
    if (U32(slots[3]) != 0 && orig_lengths == nullptr) {
        return SkikoHost::Trap("glShaderSource: invalid lengths");
    }

    for (GrGLsizei index = 0; index < count; ++index) {
        const char* raw = host->WasmPointer<const GrGLchar>(caller, offsets[index]);
        if (raw == nullptr) {
            return SkikoHost::Trap("glShaderSource: invalid string");
        }
        size_t len = (orig_lengths && orig_lengths[index] >= 0) ? static_cast<size_t>(orig_lengths[index]) : std::strlen(raw);
        adapted_storage[static_cast<size_t>(index)] = AdaptShaderSource(raw, len);
        strings[static_cast<size_t>(index)] = adapted_storage[static_cast<size_t>(index)].c_str();
        lengths[static_cast<size_t>(index)] = static_cast<GrGLint>(adapted_storage[static_cast<size_t>(index)].size());
    }

    host->CurrentGL()->fFunctions.fShaderSource(
        shader,
        count,
        strings.data(),
        lengths.data()
    );
    host->RecordGLCall("glShaderSource");
    return nullptr;
}

SPECIAL_GL(glVertexAttribPointer) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fVertexAttribPointer(
        U32(slots[0]), I32(slots[1]), U32(slots[2]),
        static_cast<GrGLboolean>(U32(slots[3])), I32(slots[4]),
        BufferOffset(U32(slots[5]))
    );
    host->RecordGLCall("glVertexAttribPointer");
    return nullptr;
}

SPECIAL_GL(glVertexAttribIPointer) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fVertexAttribIPointer(
        U32(slots[0]), I32(slots[1]), U32(slots[2]), I32(slots[3]),
        BufferOffset(U32(slots[4]))
    );
    host->RecordGLCall("glVertexAttribIPointer");
    return nullptr;
}

SPECIAL_GL(glDrawElements) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fDrawElements(
        U32(slots[0]), I32(slots[1]), U32(slots[2]),
        BufferOffset(U32(slots[3]))
    );
    host->RecordGLCall("glDrawElements");
    return nullptr;
}

SPECIAL_GL(glDrawElementsInstanced) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fDrawElementsInstanced(
        U32(slots[0]), I32(slots[1]), U32(slots[2]),
        BufferOffset(U32(slots[3])), I32(slots[4])
    );
    host->RecordGLCall("glDrawElementsInstanced");
    return nullptr;
}

SPECIAL_GL(glDrawRangeElements) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fDrawRangeElements(
        U32(slots[0]), U32(slots[1]), U32(slots[2]), I32(slots[3]),
        U32(slots[4]), BufferOffset(U32(slots[5]))
    );
    host->RecordGLCall("glDrawRangeElements");
    return nullptr;
}

SPECIAL_GL(glDrawArraysIndirect) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fDrawArraysIndirect(
        U32(slots[0]), BufferOffset(U32(slots[1]))
    );
    host->RecordGLCall("glDrawArraysIndirect");
    return nullptr;
}

SPECIAL_GL(glDrawElementsIndirect) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fDrawElementsIndirect(
        U32(slots[0]), U32(slots[1]), BufferOffset(U32(slots[2]))
    );
    host->RecordGLCall("glDrawElementsIndirect");
    return nullptr;
}

SPECIAL_GL(glTexImage2D) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fTexImage2D(
        U32(slots[0]), I32(slots[1]), I32(slots[2]), I32(slots[3]),
        I32(slots[4]), I32(slots[5]), U32(slots[6]), U32(slots[7]),
        PixelUnpackPointer(host, caller, U32(slots[8]))
    );
    host->RecordGLCall("glTexImage2D");
    return nullptr;
}

SPECIAL_GL(glTexSubImage2D) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fTexSubImage2D(
        U32(slots[0]), I32(slots[1]), I32(slots[2]), I32(slots[3]),
        I32(slots[4]), I32(slots[5]), U32(slots[6]), U32(slots[7]),
        PixelUnpackPointer(host, caller, U32(slots[8]))
    );
    host->RecordGLCall("glTexSubImage2D");
    return nullptr;
}

SPECIAL_GL(glCompressedTexImage2D) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fCompressedTexImage2D(
        U32(slots[0]), I32(slots[1]), U32(slots[2]), I32(slots[3]),
        I32(slots[4]), I32(slots[5]), I32(slots[6]),
        PixelUnpackPointer(host, caller, U32(slots[7]))
    );
    host->RecordGLCall("glCompressedTexImage2D");
    return nullptr;
}

SPECIAL_GL(glCompressedTexSubImage2D) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fCompressedTexSubImage2D(
        U32(slots[0]), I32(slots[1]), I32(slots[2]), I32(slots[3]),
        I32(slots[4]), I32(slots[5]), U32(slots[6]), I32(slots[7]),
        PixelUnpackPointer(host, caller, U32(slots[8]))
    );
    host->RecordGLCall("glCompressedTexSubImage2D");
    return nullptr;
}

SPECIAL_GL(glReadPixels) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fReadPixels(
        I32(slots[0]), I32(slots[1]), I32(slots[2]), I32(slots[3]),
        U32(slots[4]), U32(slots[5]),
        PixelPackPointer(host, caller, U32(slots[6]))
    );
    host->RecordGLCall("glReadPixels");
    return nullptr;
}

SPECIAL_GL(glMapBuffer) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLenum target = U32(slots[0]);
    const GrGLenum access = U32(slots[1]);
    GrGLint size = 0;
    host->CurrentGL()->fFunctions.fGetBufferParameteriv(
        target, GL_BUFFER_SIZE, &size
    );
    void* pointer = host->CurrentGL()->fFunctions.fMapBuffer(target, access);
    wasm_trap_t* trap = nullptr;
    slots[0].i32 = static_cast<int32_t>(MapNativeBufferToWasm(
        host, caller, target, pointer, static_cast<size_t>(size),
        access != GL_WRITE_ONLY, access != GL_READ_ONLY, &trap
    ));
    return trap;
}

SPECIAL_GL(glMapBufferRange) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLenum target = U32(slots[0]);
    const GrGLintptr offset = I32(slots[1]);
    const GrGLsizeiptr length = I32(slots[2]);
    const GrGLbitfield access = U32(slots[3]);
    void* pointer = host->CurrentGL()->fFunctions.fMapBufferRange(
        target, offset, length, access
    );
    wasm_trap_t* trap = nullptr;
    slots[0].i32 = static_cast<int32_t>(MapNativeBufferToWasm(
        host, caller, target, pointer, static_cast<size_t>(length),
        (access & GL_MAP_READ_BIT) != 0,
        (access & GL_MAP_WRITE_BIT) != 0,
        &trap
    ));
    return trap;
}

SPECIAL_GL(glUnmapBuffer) {
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLenum target = U32(slots[0]);
    auto& bridge = mapped_buffers[target];
    if (bridge.native_pointer && bridge.writable && bridge.length != 0) {
        const uint8_t* source = host->WasmSpan<const uint8_t>(
            caller,
            bridge.wasm_offset,
            bridge.length
        );
        if (source == nullptr) {
            return SkikoHost::Trap("glUnmapBuffer: invalid mapped data");
        }
        std::memcpy(bridge.native_pointer, source, bridge.length);
    }
    bridge.native_pointer = nullptr;
    bridge.length = 0;
    bridge.writable = false;
    slots[0].i32 = static_cast<int32_t>(
        host->CurrentGL()->fFunctions.fUnmapBuffer(target)
    );
    return nullptr;
}

SPECIAL_GL(glFenceSync) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    slots[0].i32 = static_cast<int32_t>(StoreSync(
        host->CurrentGL()->fFunctions.fFenceSync(
            U32(slots[0]), U32(slots[1])
        )
    ));
    return nullptr;
}

SPECIAL_GL(glIsSync) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    slots[0].i32 = static_cast<int32_t>(
        host->CurrentGL()->fFunctions.fIsSync(LookupSync(U32(slots[0])))
    );
    return nullptr;
}

SPECIAL_GL(glClientWaitSync) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    slots[0].i32 = static_cast<int32_t>(
        host->CurrentGL()->fFunctions.fClientWaitSync(
            LookupSync(U32(slots[0])), U32(slots[1]),
            JoinU64(U32(slots[2]), U32(slots[3]))
        )
    );
    return nullptr;
}

SPECIAL_GL(glWaitSync) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    host->CurrentGL()->fFunctions.fWaitSync(
        LookupSync(U32(slots[0])), U32(slots[1]),
        JoinU64(U32(slots[2]), U32(slots[3]))
    );
    return nullptr;
}

SPECIAL_GL(glDeleteSync) {
    (void)caller; (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const uint32_t handle = U32(slots[0]);
    host->CurrentGL()->fFunctions.fDeleteSync(LookupSync(handle));
    sync_handles.erase(handle);
    return nullptr;
}

const GLBinding kSpecialGLBindings[] = {
    {"glBindFramebuffer", Special_glBindFramebuffer},
    {"glBindVertexArray", Special_glBindVertexArray},
    {"glBindVertexArrayOES", Special_glBindVertexArray},
    {"glClientWaitSync", Special_glClientWaitSync},
    {"glCompressedTexImage2D", Special_glCompressedTexImage2D},
    {"glCompressedTexSubImage2D", Special_glCompressedTexSubImage2D},
    {"glDeleteSync", Special_glDeleteSync},
    {"glDrawArraysIndirect", Special_glDrawArraysIndirect},
    {"glDrawElements", Special_glDrawElements},
    {"glDrawElementsIndirect", Special_glDrawElementsIndirect},
    {"glDrawElementsInstanced", Special_glDrawElementsInstanced},
    {"glDrawRangeElements", Special_glDrawRangeElements},
    {"glFenceSync", Special_glFenceSync},
    {"glGetIntegerv", Special_glGetIntegerv},
    {"glGetProgramiv", Special_glGetProgramiv},
    {"glGetShaderiv", Special_glGetShaderiv},
    {"glGetString", Special_glGetString},
    {"glGetStringi", Special_glGetStringi},
    {"glIsSync", Special_glIsSync},
    {"glMapBuffer", Special_glMapBuffer},
    {"glMapBufferRange", Special_glMapBufferRange},
    {"glReadPixels", Special_glReadPixels},
    {"glShaderSource", Special_glShaderSource},
    {"glTexImage2D", Special_glTexImage2D},
    {"glTexSubImage2D", Special_glTexSubImage2D},
    {"glUnmapBuffer", Special_glUnmapBuffer},
    {"glVertexAttribIPointer", Special_glVertexAttribIPointer},
    {"glVertexAttribPointer", Special_glVertexAttribPointer},
    {"glWaitSync", Special_glWaitSync},
};

#undef SPECIAL_GL

} // namespace

const GLBinding* FindSpecialGLBinding(const std::string& name) {
    for (const GLBinding& binding : kSpecialGLBindings) {
        if (name == binding.name) return &binding;
    }
    return nullptr;
}
