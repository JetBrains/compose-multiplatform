// Generated file. Do not edit manually.

namespace {

uint32_t U32(const wasmtime_val_raw_t& value) {
    return static_cast<uint32_t>(value.i32);
}

int32_t I32(const wasmtime_val_raw_t& value) {
    return value.i32;
}

wasm_trap_t* Generated_glActiveTexture(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glActiveTexture: no active GL context");
    }

    gl->fFunctions.fActiveTexture(
            U32(slots[0])
        );
    host->RecordGLCall("glActiveTexture");
    return nullptr;
}

wasm_trap_t* Generated_glAttachShader(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glAttachShader: no active GL context");
    }

    gl->fFunctions.fAttachShader(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glAttachShader");
    return nullptr;
}

wasm_trap_t* Generated_glBeginQuery(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBeginQuery: no active GL context");
    }

    gl->fFunctions.fBeginQuery(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBeginQuery");
    return nullptr;
}

wasm_trap_t* Generated_glBindAttribLocation(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindAttribLocation: no active GL context");
    }

    gl->fFunctions.fBindAttribLocation(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<const GrGLchar>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glBindAttribLocation");
    return nullptr;
}

wasm_trap_t* Generated_glBindBuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindBuffer: no active GL context");
    }

    gl->fFunctions.fBindBuffer(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBindBuffer");
    return nullptr;
}

wasm_trap_t* Generated_glBindFragDataLocation(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindFragDataLocation: no active GL context");
    }

    gl->fFunctions.fBindFragDataLocation(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<const GrGLchar>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glBindFragDataLocation");
    return nullptr;
}

wasm_trap_t* Generated_glBindFragDataLocationIndexed(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindFragDataLocationIndexed: no active GL context");
    }

    gl->fFunctions.fBindFragDataLocationIndexed(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            host->WasmPointer<const GrGLchar>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glBindFragDataLocationIndexed");
    return nullptr;
}

wasm_trap_t* Generated_glBindRenderbuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindRenderbuffer: no active GL context");
    }

    gl->fFunctions.fBindRenderbuffer(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBindRenderbuffer");
    return nullptr;
}

wasm_trap_t* Generated_glBindSampler(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindSampler: no active GL context");
    }

    gl->fFunctions.fBindSampler(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBindSampler");
    return nullptr;
}

wasm_trap_t* Generated_glBindTexture(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindTexture: no active GL context");
    }

    gl->fFunctions.fBindTexture(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBindTexture");
    return nullptr;
}

wasm_trap_t* Generated_glBindVertexArray(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBindVertexArray: no active GL context");
    }

    gl->fFunctions.fBindVertexArray(
            U32(slots[0])
        );
    host->RecordGLCall("glBindVertexArray");
    return nullptr;
}

wasm_trap_t* Generated_glBlendColor(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBlendColor: no active GL context");
    }

    gl->fFunctions.fBlendColor(
            slots[0].f32,
            slots[1].f32,
            slots[2].f32,
            slots[3].f32
        );
    host->RecordGLCall("glBlendColor");
    return nullptr;
}

wasm_trap_t* Generated_glBlendEquation(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBlendEquation: no active GL context");
    }

    gl->fFunctions.fBlendEquation(
            U32(slots[0])
        );
    host->RecordGLCall("glBlendEquation");
    return nullptr;
}

wasm_trap_t* Generated_glBlendFunc(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBlendFunc: no active GL context");
    }

    gl->fFunctions.fBlendFunc(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glBlendFunc");
    return nullptr;
}

wasm_trap_t* Generated_glBlitFramebuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBlitFramebuffer: no active GL context");
    }

    gl->fFunctions.fBlitFramebuffer(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3]),
            I32(slots[4]),
            I32(slots[5]),
            I32(slots[6]),
            I32(slots[7]),
            U32(slots[8]),
            U32(slots[9])
        );
    host->RecordGLCall("glBlitFramebuffer");
    return nullptr;
}

wasm_trap_t* Generated_glBufferData(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBufferData: no active GL context");
    }

    gl->fFunctions.fBufferData(
            U32(slots[0]),
            static_cast<int64_t>(I32(slots[1])),
            host->WasmPointer<const uint8_t>(caller, U32(slots[2])),
            U32(slots[3])
        );
    host->RecordGLCall("glBufferData");
    return nullptr;
}

wasm_trap_t* Generated_glBufferSubData(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glBufferSubData: no active GL context");
    }

    gl->fFunctions.fBufferSubData(
            U32(slots[0]),
            static_cast<int64_t>(I32(slots[1])),
            static_cast<int64_t>(I32(slots[2])),
            host->WasmPointer<const uint8_t>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glBufferSubData");
    return nullptr;
}

wasm_trap_t* Generated_glCheckFramebufferStatus(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCheckFramebufferStatus: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fCheckFramebufferStatus(
            U32(slots[0])
        ));
    host->RecordGLCall("glCheckFramebufferStatus");
    return nullptr;
}

wasm_trap_t* Generated_glClear(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glClear: no active GL context");
    }

    gl->fFunctions.fClear(
            U32(slots[0])
        );
    host->RecordGLCall("glClear");
    return nullptr;
}

wasm_trap_t* Generated_glClearColor(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glClearColor: no active GL context");
    }

    gl->fFunctions.fClearColor(
            slots[0].f32,
            slots[1].f32,
            slots[2].f32,
            slots[3].f32
        );
    host->RecordGLCall("glClearColor");
    return nullptr;
}

wasm_trap_t* Generated_glClearStencil(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glClearStencil: no active GL context");
    }

    gl->fFunctions.fClearStencil(
            I32(slots[0])
        );
    host->RecordGLCall("glClearStencil");
    return nullptr;
}

wasm_trap_t* Generated_glColorMask(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glColorMask: no active GL context");
    }

    gl->fFunctions.fColorMask(
            static_cast<uint8_t>(U32(slots[0])),
            static_cast<uint8_t>(U32(slots[1])),
            static_cast<uint8_t>(U32(slots[2])),
            static_cast<uint8_t>(U32(slots[3]))
        );
    host->RecordGLCall("glColorMask");
    return nullptr;
}

wasm_trap_t* Generated_glCompileShader(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCompileShader: no active GL context");
    }

    gl->fFunctions.fCompileShader(
            U32(slots[0])
        );
    host->RecordGLCall("glCompileShader", U32(slots[0]));
    return nullptr;
}

wasm_trap_t* Generated_glCopyBufferSubData(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCopyBufferSubData: no active GL context");
    }

    gl->fFunctions.fCopyBufferSubData(
            U32(slots[0]),
            U32(slots[1]),
            static_cast<int64_t>(I32(slots[2])),
            static_cast<int64_t>(I32(slots[3])),
            static_cast<int64_t>(I32(slots[4]))
        );
    host->RecordGLCall("glCopyBufferSubData");
    return nullptr;
}

wasm_trap_t* Generated_glCopyTexSubImage2D(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCopyTexSubImage2D: no active GL context");
    }

    gl->fFunctions.fCopyTexSubImage2D(
            U32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3]),
            I32(slots[4]),
            I32(slots[5]),
            I32(slots[6]),
            I32(slots[7])
        );
    host->RecordGLCall("glCopyTexSubImage2D");
    return nullptr;
}

wasm_trap_t* Generated_glCreateProgram(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCreateProgram: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fCreateProgram());
    host->RecordGLCall("glCreateProgram");
    return nullptr;
}

wasm_trap_t* Generated_glCreateShader(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCreateShader: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fCreateShader(
            U32(slots[0])
        ));
    host->RecordGLCall("glCreateShader");
    return nullptr;
}

wasm_trap_t* Generated_glCullFace(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glCullFace: no active GL context");
    }

    gl->fFunctions.fCullFace(
            U32(slots[0])
        );
    host->RecordGLCall("glCullFace");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteBuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteBuffers: no active GL context");
    }

    gl->fFunctions.fDeleteBuffers(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteBuffers");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteFramebuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteFramebuffers: no active GL context");
    }

    gl->fFunctions.fDeleteFramebuffers(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteFramebuffers");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteProgram(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteProgram: no active GL context");
    }

    gl->fFunctions.fDeleteProgram(
            U32(slots[0])
        );
    host->RecordGLCall("glDeleteProgram");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteQueries(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteQueries: no active GL context");
    }

    gl->fFunctions.fDeleteQueries(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteQueries");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteRenderbuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteRenderbuffers: no active GL context");
    }

    gl->fFunctions.fDeleteRenderbuffers(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteRenderbuffers");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteSamplers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteSamplers: no active GL context");
    }

    gl->fFunctions.fDeleteSamplers(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteSamplers");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteShader(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteShader: no active GL context");
    }

    gl->fFunctions.fDeleteShader(
            U32(slots[0])
        );
    host->RecordGLCall("glDeleteShader");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteTextures(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteTextures: no active GL context");
    }

    gl->fFunctions.fDeleteTextures(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteTextures");
    return nullptr;
}

wasm_trap_t* Generated_glDeleteVertexArrays(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDeleteVertexArrays: no active GL context");
    }

    gl->fFunctions.fDeleteVertexArrays(
            I32(slots[0]),
            host->WasmPointer<const GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDeleteVertexArrays");
    return nullptr;
}

wasm_trap_t* Generated_glDepthMask(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDepthMask: no active GL context");
    }

    gl->fFunctions.fDepthMask(
            static_cast<uint8_t>(U32(slots[0]))
        );
    host->RecordGLCall("glDepthMask");
    return nullptr;
}

wasm_trap_t* Generated_glDisable(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDisable: no active GL context");
    }

    gl->fFunctions.fDisable(
            U32(slots[0])
        );
    host->RecordGLCall("glDisable");
    return nullptr;
}

wasm_trap_t* Generated_glDisableVertexAttribArray(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDisableVertexAttribArray: no active GL context");
    }

    gl->fFunctions.fDisableVertexAttribArray(
            U32(slots[0])
        );
    host->RecordGLCall("glDisableVertexAttribArray");
    return nullptr;
}

wasm_trap_t* Generated_glDrawArrays(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDrawArrays: no active GL context");
    }

    gl->fFunctions.fDrawArrays(
            U32(slots[0]),
            I32(slots[1]),
            I32(slots[2])
        );
    host->RecordGLCall("glDrawArrays");
    return nullptr;
}

wasm_trap_t* Generated_glDrawArraysInstanced(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDrawArraysInstanced: no active GL context");
    }

    gl->fFunctions.fDrawArraysInstanced(
            U32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3])
        );
    host->RecordGLCall("glDrawArraysInstanced");
    return nullptr;
}

wasm_trap_t* Generated_glDrawBuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDrawBuffer: no active GL context");
    }

    gl->fFunctions.fDrawBuffer(
            U32(slots[0])
        );
    host->RecordGLCall("glDrawBuffer");
    return nullptr;
}

wasm_trap_t* Generated_glDrawBuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glDrawBuffers: no active GL context");
    }

    gl->fFunctions.fDrawBuffers(
            I32(slots[0]),
            host->WasmPointer<const GrGLenum>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glDrawBuffers");
    return nullptr;
}

wasm_trap_t* Generated_glEnable(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glEnable: no active GL context");
    }

    gl->fFunctions.fEnable(
            U32(slots[0])
        );
    host->RecordGLCall("glEnable");
    return nullptr;
}

wasm_trap_t* Generated_glEnableVertexAttribArray(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glEnableVertexAttribArray: no active GL context");
    }

    gl->fFunctions.fEnableVertexAttribArray(
            U32(slots[0])
        );
    host->RecordGLCall("glEnableVertexAttribArray");
    return nullptr;
}

wasm_trap_t* Generated_glEndQuery(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glEndQuery: no active GL context");
    }

    gl->fFunctions.fEndQuery(
            U32(slots[0])
        );
    host->RecordGLCall("glEndQuery");
    return nullptr;
}

wasm_trap_t* Generated_glFinish(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFinish: no active GL context");
    }

    gl->fFunctions.fFinish();
    host->RecordGLCall("glFinish");
    return nullptr;
}

wasm_trap_t* Generated_glFlush(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFlush: no active GL context");
    }

    gl->fFunctions.fFlush();
    host->RecordGLCall("glFlush");
    return nullptr;
}

wasm_trap_t* Generated_glFlushMappedBufferRange(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFlushMappedBufferRange: no active GL context");
    }

    gl->fFunctions.fFlushMappedBufferRange(
            U32(slots[0]),
            static_cast<int64_t>(I32(slots[1])),
            static_cast<int64_t>(I32(slots[2]))
        );
    host->RecordGLCall("glFlushMappedBufferRange");
    return nullptr;
}

wasm_trap_t* Generated_glFramebufferRenderbuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFramebufferRenderbuffer: no active GL context");
    }

    gl->fFunctions.fFramebufferRenderbuffer(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            U32(slots[3])
        );
    host->RecordGLCall("glFramebufferRenderbuffer");
    return nullptr;
}

wasm_trap_t* Generated_glFramebufferTexture2D(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFramebufferTexture2D: no active GL context");
    }

    gl->fFunctions.fFramebufferTexture2D(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            U32(slots[3]),
            I32(slots[4])
        );
    host->RecordGLCall("glFramebufferTexture2D");
    return nullptr;
}

wasm_trap_t* Generated_glFrontFace(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glFrontFace: no active GL context");
    }

    gl->fFunctions.fFrontFace(
            U32(slots[0])
        );
    host->RecordGLCall("glFrontFace");
    return nullptr;
}

wasm_trap_t* Generated_glGenBuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenBuffers: no active GL context");
    }

    gl->fFunctions.fGenBuffers(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenBuffers");
    return nullptr;
}

wasm_trap_t* Generated_glGenFramebuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenFramebuffers: no active GL context");
    }

    gl->fFunctions.fGenFramebuffers(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenFramebuffers");
    return nullptr;
}

wasm_trap_t* Generated_glGenQueries(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenQueries: no active GL context");
    }

    gl->fFunctions.fGenQueries(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenQueries");
    return nullptr;
}

wasm_trap_t* Generated_glGenRenderbuffers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenRenderbuffers: no active GL context");
    }

    gl->fFunctions.fGenRenderbuffers(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenRenderbuffers");
    return nullptr;
}

wasm_trap_t* Generated_glGenSamplers(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenSamplers: no active GL context");
    }

    gl->fFunctions.fGenSamplers(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenSamplers");
    return nullptr;
}

wasm_trap_t* Generated_glGenTextures(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenTextures: no active GL context");
    }

    gl->fFunctions.fGenTextures(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenTextures");
    return nullptr;
}

wasm_trap_t* Generated_glGenVertexArrays(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenVertexArrays: no active GL context");
    }

    gl->fFunctions.fGenVertexArrays(
            I32(slots[0]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGenVertexArrays");
    return nullptr;
}

wasm_trap_t* Generated_glGenerateMipmap(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGenerateMipmap: no active GL context");
    }

    gl->fFunctions.fGenerateMipmap(
            U32(slots[0])
        );
    host->RecordGLCall("glGenerateMipmap");
    return nullptr;
}

wasm_trap_t* Generated_glGetBufferParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetBufferParameteriv: no active GL context");
    }

    gl->fFunctions.fGetBufferParameteriv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetBufferParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glGetError(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetError: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fGetError());
    host->RecordGLCall("glGetError");
    return nullptr;
}

wasm_trap_t* Generated_glGetFloatv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetFloatv: no active GL context");
    }

    gl->fFunctions.fGetFloatv(
            U32(slots[0]),
            host->WasmPointer<GrGLfloat>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glGetFloatv");
    return nullptr;
}

wasm_trap_t* Generated_glGetFramebufferAttachmentParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetFramebufferAttachmentParameteriv: no active GL context");
    }

    gl->fFunctions.fGetFramebufferAttachmentParameteriv(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            host->WasmPointer<GrGLint>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glGetFramebufferAttachmentParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glGetInternalformativ(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetInternalformativ: no active GL context");
    }

    gl->fFunctions.fGetInternalformativ(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            I32(slots[3]),
            host->WasmPointer<GrGLint>(caller, U32(slots[4]))
        );
    host->RecordGLCall("glGetInternalformativ");
    return nullptr;
}

wasm_trap_t* Generated_glGetMultisamplefv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetMultisamplefv: no active GL context");
    }

    gl->fFunctions.fGetMultisamplefv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetMultisamplefv");
    return nullptr;
}

wasm_trap_t* Generated_glGetProgramBinary(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetProgramBinary: no active GL context");
    }

    gl->fFunctions.fGetProgramBinary(
            U32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<GrGLsizei>(caller, U32(slots[2])),
            host->WasmPointer<GrGLenum>(caller, U32(slots[3])),
            host->WasmPointer<uint8_t>(caller, U32(slots[4]))
        );
    host->RecordGLCall("glGetProgramBinary");
    return nullptr;
}

wasm_trap_t* Generated_glGetProgramInfoLog(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetProgramInfoLog: no active GL context");
    }

    gl->fFunctions.fGetProgramInfoLog(
            U32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<GrGLsizei>(caller, U32(slots[2])),
            host->WasmPointer<GrGLchar>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glGetProgramInfoLog");
    return nullptr;
}

wasm_trap_t* Generated_glGetProgramiv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetProgramiv: no active GL context");
    }

    gl->fFunctions.fGetProgramiv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetProgramiv");
    return nullptr;
}

wasm_trap_t* Generated_glGetQueryObjecti64v(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetQueryObjecti64v: no active GL context");
    }

    gl->fFunctions.fGetQueryObjecti64v(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint64>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetQueryObjecti64v");
    return nullptr;
}

wasm_trap_t* Generated_glGetQueryObjectui64v(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetQueryObjectui64v: no active GL context");
    }

    gl->fFunctions.fGetQueryObjectui64v(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLuint64>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetQueryObjectui64v");
    return nullptr;
}

wasm_trap_t* Generated_glGetQueryObjectuiv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetQueryObjectuiv: no active GL context");
    }

    gl->fFunctions.fGetQueryObjectuiv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLuint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetQueryObjectuiv");
    return nullptr;
}

wasm_trap_t* Generated_glGetQueryiv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetQueryiv: no active GL context");
    }

    gl->fFunctions.fGetQueryiv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetQueryiv");
    return nullptr;
}

wasm_trap_t* Generated_glGetRenderbufferParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetRenderbufferParameteriv: no active GL context");
    }

    gl->fFunctions.fGetRenderbufferParameteriv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetRenderbufferParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glGetShaderInfoLog(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetShaderInfoLog: no active GL context");
    }

    gl->fFunctions.fGetShaderInfoLog(
            U32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<GrGLsizei>(caller, U32(slots[2])),
            host->WasmPointer<GrGLchar>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glGetShaderInfoLog");
    return nullptr;
}

wasm_trap_t* Generated_glGetShaderPrecisionFormat(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetShaderPrecisionFormat: no active GL context");
    }

    gl->fFunctions.fGetShaderPrecisionFormat(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2])),
            host->WasmPointer<GrGLint>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glGetShaderPrecisionFormat");
    return nullptr;
}

wasm_trap_t* Generated_glGetShaderiv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetShaderiv: no active GL context");
    }

    gl->fFunctions.fGetShaderiv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glGetShaderiv");
    return nullptr;
}

wasm_trap_t* Generated_glGetTexLevelParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetTexLevelParameteriv: no active GL context");
    }

    gl->fFunctions.fGetTexLevelParameteriv(
            U32(slots[0]),
            I32(slots[1]),
            U32(slots[2]),
            host->WasmPointer<GrGLint>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glGetTexLevelParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glGetUniformLocation(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glGetUniformLocation: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fGetUniformLocation(
            U32(slots[0]),
            host->WasmPointer<const GrGLchar>(caller, U32(slots[1]))
        ));
    host->RecordGLCall("glGetUniformLocation");
    return nullptr;
}

wasm_trap_t* Generated_glInsertEventMarkerEXT(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glInsertEventMarkerEXT: no active GL context");
    }

    gl->fFunctions.fInsertEventMarker(
            I32(slots[0]),
            host->WasmPointer<const char>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glInsertEventMarkerEXT");
    return nullptr;
}

wasm_trap_t* Generated_glIsTexture(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glIsTexture: no active GL context");
    }

    slots[0].i32 = static_cast<int32_t>(gl->fFunctions.fIsTexture(
            U32(slots[0])
        ));
    host->RecordGLCall("glIsTexture");
    return nullptr;
}

wasm_trap_t* Generated_glLineWidth(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glLineWidth: no active GL context");
    }

    gl->fFunctions.fLineWidth(
            slots[0].f32
        );
    host->RecordGLCall("glLineWidth");
    return nullptr;
}

wasm_trap_t* Generated_glLinkProgram(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glLinkProgram: no active GL context");
    }

    gl->fFunctions.fLinkProgram(
            U32(slots[0])
        );
    host->RecordGLCall("glLinkProgram", U32(slots[0]));
    return nullptr;
}

wasm_trap_t* Generated_glPatchParameteri(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glPatchParameteri: no active GL context");
    }

    gl->fFunctions.fPatchParameteri(
            U32(slots[0]),
            I32(slots[1])
        );
    host->RecordGLCall("glPatchParameteri");
    return nullptr;
}

wasm_trap_t* Generated_glPixelStorei(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glPixelStorei: no active GL context");
    }

    gl->fFunctions.fPixelStorei(
            U32(slots[0]),
            I32(slots[1])
        );
    host->RecordGLCall("glPixelStorei");
    return nullptr;
}

wasm_trap_t* Generated_glPolygonMode(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glPolygonMode: no active GL context");
    }

    gl->fFunctions.fPolygonMode(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glPolygonMode");
    return nullptr;
}

wasm_trap_t* Generated_glPopGroupMarkerEXT(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glPopGroupMarkerEXT: no active GL context");
    }

    gl->fFunctions.fPopGroupMarker();
    host->RecordGLCall("glPopGroupMarkerEXT");
    return nullptr;
}

wasm_trap_t* Generated_glProgramBinary(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glProgramBinary: no active GL context");
    }

    gl->fFunctions.fProgramBinary(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<uint8_t>(caller, U32(slots[2])),
            I32(slots[3])
        );
    host->RecordGLCall("glProgramBinary");
    return nullptr;
}

wasm_trap_t* Generated_glProgramParameteri(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glProgramParameteri: no active GL context");
    }

    gl->fFunctions.fProgramParameteri(
            U32(slots[0]),
            U32(slots[1]),
            I32(slots[2])
        );
    host->RecordGLCall("glProgramParameteri");
    return nullptr;
}

wasm_trap_t* Generated_glPushGroupMarkerEXT(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glPushGroupMarkerEXT: no active GL context");
    }

    gl->fFunctions.fPushGroupMarker(
            I32(slots[0]),
            host->WasmPointer<const char>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glPushGroupMarkerEXT");
    return nullptr;
}

wasm_trap_t* Generated_glQueryCounter(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glQueryCounter: no active GL context");
    }

    gl->fFunctions.fQueryCounter(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glQueryCounter");
    return nullptr;
}

wasm_trap_t* Generated_glReadBuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glReadBuffer: no active GL context");
    }

    gl->fFunctions.fReadBuffer(
            U32(slots[0])
        );
    host->RecordGLCall("glReadBuffer");
    return nullptr;
}

wasm_trap_t* Generated_glRenderbufferStorage(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glRenderbufferStorage: no active GL context");
    }

    gl->fFunctions.fRenderbufferStorage(
            U32(slots[0]),
            U32(slots[1]),
            I32(slots[2]),
            I32(slots[3])
        );
    host->RecordGLCall("glRenderbufferStorage");
    return nullptr;
}

wasm_trap_t* Generated_glRenderbufferStorageMultisample(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glRenderbufferStorageMultisample: no active GL context");
    }

    gl->fFunctions.fRenderbufferStorageMultisample(
            U32(slots[0]),
            I32(slots[1]),
            U32(slots[2]),
            I32(slots[3]),
            I32(slots[4])
        );
    host->RecordGLCall("glRenderbufferStorageMultisample");
    return nullptr;
}

wasm_trap_t* Generated_glSamplerParameterf(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glSamplerParameterf: no active GL context");
    }

    gl->fFunctions.fSamplerParameterf(
            U32(slots[0]),
            U32(slots[1]),
            slots[2].f32
        );
    host->RecordGLCall("glSamplerParameterf");
    return nullptr;
}

wasm_trap_t* Generated_glSamplerParameteri(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glSamplerParameteri: no active GL context");
    }

    gl->fFunctions.fSamplerParameteri(
            U32(slots[0]),
            U32(slots[1]),
            I32(slots[2])
        );
    host->RecordGLCall("glSamplerParameteri");
    return nullptr;
}

wasm_trap_t* Generated_glSamplerParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glSamplerParameteriv: no active GL context");
    }

    gl->fFunctions.fSamplerParameteriv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glSamplerParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glScissor(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glScissor: no active GL context");
    }

    gl->fFunctions.fScissor(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3])
        );
    host->RecordGLCall("glScissor");
    return nullptr;
}

wasm_trap_t* Generated_glStencilFunc(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilFunc: no active GL context");
    }

    gl->fFunctions.fStencilFunc(
            U32(slots[0]),
            I32(slots[1]),
            U32(slots[2])
        );
    host->RecordGLCall("glStencilFunc");
    return nullptr;
}

wasm_trap_t* Generated_glStencilFuncSeparate(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilFuncSeparate: no active GL context");
    }

    gl->fFunctions.fStencilFuncSeparate(
            U32(slots[0]),
            U32(slots[1]),
            I32(slots[2]),
            U32(slots[3])
        );
    host->RecordGLCall("glStencilFuncSeparate");
    return nullptr;
}

wasm_trap_t* Generated_glStencilMask(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilMask: no active GL context");
    }

    gl->fFunctions.fStencilMask(
            U32(slots[0])
        );
    host->RecordGLCall("glStencilMask");
    return nullptr;
}

wasm_trap_t* Generated_glStencilMaskSeparate(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilMaskSeparate: no active GL context");
    }

    gl->fFunctions.fStencilMaskSeparate(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glStencilMaskSeparate");
    return nullptr;
}

wasm_trap_t* Generated_glStencilOp(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilOp: no active GL context");
    }

    gl->fFunctions.fStencilOp(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2])
        );
    host->RecordGLCall("glStencilOp");
    return nullptr;
}

wasm_trap_t* Generated_glStencilOpSeparate(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glStencilOpSeparate: no active GL context");
    }

    gl->fFunctions.fStencilOpSeparate(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2]),
            U32(slots[3])
        );
    host->RecordGLCall("glStencilOpSeparate");
    return nullptr;
}

wasm_trap_t* Generated_glTexBuffer(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexBuffer: no active GL context");
    }

    gl->fFunctions.fTexBuffer(
            U32(slots[0]),
            U32(slots[1]),
            U32(slots[2])
        );
    host->RecordGLCall("glTexBuffer");
    return nullptr;
}

wasm_trap_t* Generated_glTexParameterf(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexParameterf: no active GL context");
    }

    gl->fFunctions.fTexParameterf(
            U32(slots[0]),
            U32(slots[1]),
            slots[2].f32
        );
    host->RecordGLCall("glTexParameterf");
    return nullptr;
}

wasm_trap_t* Generated_glTexParameterfv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexParameterfv: no active GL context");
    }

    gl->fFunctions.fTexParameterfv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glTexParameterfv");
    return nullptr;
}

wasm_trap_t* Generated_glTexParameteri(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexParameteri: no active GL context");
    }

    gl->fFunctions.fTexParameteri(
            U32(slots[0]),
            U32(slots[1]),
            I32(slots[2])
        );
    host->RecordGLCall("glTexParameteri");
    return nullptr;
}

wasm_trap_t* Generated_glTexParameteriv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexParameteriv: no active GL context");
    }

    gl->fFunctions.fTexParameteriv(
            U32(slots[0]),
            U32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glTexParameteriv");
    return nullptr;
}

wasm_trap_t* Generated_glTexStorage2D(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTexStorage2D: no active GL context");
    }

    gl->fFunctions.fTexStorage2D(
            U32(slots[0]),
            I32(slots[1]),
            U32(slots[2]),
            I32(slots[3]),
            I32(slots[4])
        );
    host->RecordGLCall("glTexStorage2D");
    return nullptr;
}

wasm_trap_t* Generated_glTextureBarrierNV(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glTextureBarrierNV: no active GL context");
    }

    gl->fFunctions.fTextureBarrier();
    host->RecordGLCall("glTextureBarrierNV");
    return nullptr;
}

wasm_trap_t* Generated_glUniform1f(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform1f: no active GL context");
    }

    gl->fFunctions.fUniform1f(
            I32(slots[0]),
            slots[1].f32
        );
    host->RecordGLCall("glUniform1f");
    return nullptr;
}

wasm_trap_t* Generated_glUniform1fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform1fv: no active GL context");
    }

    gl->fFunctions.fUniform1fv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform1fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform1i(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform1i: no active GL context");
    }

    gl->fFunctions.fUniform1i(
            I32(slots[0]),
            I32(slots[1])
        );
    host->RecordGLCall("glUniform1i");
    return nullptr;
}

wasm_trap_t* Generated_glUniform1iv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform1iv: no active GL context");
    }

    gl->fFunctions.fUniform1iv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform1iv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform2f(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform2f: no active GL context");
    }

    gl->fFunctions.fUniform2f(
            I32(slots[0]),
            slots[1].f32,
            slots[2].f32
        );
    host->RecordGLCall("glUniform2f");
    return nullptr;
}

wasm_trap_t* Generated_glUniform2fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform2fv: no active GL context");
    }

    gl->fFunctions.fUniform2fv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform2fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform2i(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform2i: no active GL context");
    }

    gl->fFunctions.fUniform2i(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2])
        );
    host->RecordGLCall("glUniform2i");
    return nullptr;
}

wasm_trap_t* Generated_glUniform2iv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform2iv: no active GL context");
    }

    gl->fFunctions.fUniform2iv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform2iv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform3f(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform3f: no active GL context");
    }

    gl->fFunctions.fUniform3f(
            I32(slots[0]),
            slots[1].f32,
            slots[2].f32,
            slots[3].f32
        );
    host->RecordGLCall("glUniform3f");
    return nullptr;
}

wasm_trap_t* Generated_glUniform3fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform3fv: no active GL context");
    }

    gl->fFunctions.fUniform3fv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform3fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform3i(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform3i: no active GL context");
    }

    gl->fFunctions.fUniform3i(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3])
        );
    host->RecordGLCall("glUniform3i");
    return nullptr;
}

wasm_trap_t* Generated_glUniform3iv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform3iv: no active GL context");
    }

    gl->fFunctions.fUniform3iv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform3iv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform4f(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform4f: no active GL context");
    }

    gl->fFunctions.fUniform4f(
            I32(slots[0]),
            slots[1].f32,
            slots[2].f32,
            slots[3].f32,
            slots[4].f32
        );
    host->RecordGLCall("glUniform4f");
    return nullptr;
}

wasm_trap_t* Generated_glUniform4fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform4fv: no active GL context");
    }

    gl->fFunctions.fUniform4fv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform4fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniform4i(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform4i: no active GL context");
    }

    gl->fFunctions.fUniform4i(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3]),
            I32(slots[4])
        );
    host->RecordGLCall("glUniform4i");
    return nullptr;
}

wasm_trap_t* Generated_glUniform4iv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniform4iv: no active GL context");
    }

    gl->fFunctions.fUniform4iv(
            I32(slots[0]),
            I32(slots[1]),
            host->WasmPointer<const GrGLint>(caller, U32(slots[2]))
        );
    host->RecordGLCall("glUniform4iv");
    return nullptr;
}

wasm_trap_t* Generated_glUniformMatrix2fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniformMatrix2fv: no active GL context");
    }

    gl->fFunctions.fUniformMatrix2fv(
            I32(slots[0]),
            I32(slots[1]),
            static_cast<uint8_t>(U32(slots[2])),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glUniformMatrix2fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniformMatrix3fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniformMatrix3fv: no active GL context");
    }

    gl->fFunctions.fUniformMatrix3fv(
            I32(slots[0]),
            I32(slots[1]),
            static_cast<uint8_t>(U32(slots[2])),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glUniformMatrix3fv");
    return nullptr;
}

wasm_trap_t* Generated_glUniformMatrix4fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUniformMatrix4fv: no active GL context");
    }

    gl->fFunctions.fUniformMatrix4fv(
            I32(slots[0]),
            I32(slots[1]),
            static_cast<uint8_t>(U32(slots[2])),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[3]))
        );
    host->RecordGLCall("glUniformMatrix4fv");
    return nullptr;
}

wasm_trap_t* Generated_glUseProgram(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glUseProgram: no active GL context");
    }

    gl->fFunctions.fUseProgram(
            U32(slots[0])
        );
    host->RecordGLCall("glUseProgram");
    return nullptr;
}

wasm_trap_t* Generated_glVertexAttrib1f(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glVertexAttrib1f: no active GL context");
    }

    gl->fFunctions.fVertexAttrib1f(
            U32(slots[0]),
            slots[1].f32
        );
    host->RecordGLCall("glVertexAttrib1f");
    return nullptr;
}

wasm_trap_t* Generated_glVertexAttrib2fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glVertexAttrib2fv: no active GL context");
    }

    gl->fFunctions.fVertexAttrib2fv(
            U32(slots[0]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glVertexAttrib2fv");
    return nullptr;
}

wasm_trap_t* Generated_glVertexAttrib3fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glVertexAttrib3fv: no active GL context");
    }

    gl->fFunctions.fVertexAttrib3fv(
            U32(slots[0]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glVertexAttrib3fv");
    return nullptr;
}

wasm_trap_t* Generated_glVertexAttrib4fv(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glVertexAttrib4fv: no active GL context");
    }

    gl->fFunctions.fVertexAttrib4fv(
            U32(slots[0]),
            host->WasmPointer<const GrGLfloat>(caller, U32(slots[1]))
        );
    host->RecordGLCall("glVertexAttrib4fv");
    return nullptr;
}

wasm_trap_t* Generated_glVertexAttribDivisor(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glVertexAttribDivisor: no active GL context");
    }

    gl->fFunctions.fVertexAttribDivisor(
            U32(slots[0]),
            U32(slots[1])
        );
    host->RecordGLCall("glVertexAttribDivisor");
    return nullptr;
}

wasm_trap_t* Generated_glViewport(
    void* environment,
    wasmtime_caller_t* caller,
    wasmtime_val_raw_t* slots,
    size_t slot_count
) {
    (void)caller;
    (void)slots;
    (void)slot_count;
    auto* host = static_cast<SkikoHost*>(environment);
    const GrGLInterface* gl = host->CurrentGL();
    if (gl == nullptr) {
        return SkikoHost::Trap("glViewport: no active GL context");
    }

    gl->fFunctions.fViewport(
            I32(slots[0]),
            I32(slots[1]),
            I32(slots[2]),
            I32(slots[3])
        );
    host->RecordGLCall("glViewport");
    return nullptr;
}

const GLBinding kGeneratedGLBindings[] = {
    {"glActiveTexture", Generated_glActiveTexture},
    {"glAttachShader", Generated_glAttachShader},
    {"glBeginQuery", Generated_glBeginQuery},
    {"glBindAttribLocation", Generated_glBindAttribLocation},
    {"glBindBuffer", Generated_glBindBuffer},
    {"glBindFragDataLocation", Generated_glBindFragDataLocation},
    {"glBindFragDataLocationIndexed", Generated_glBindFragDataLocationIndexed},
    {"glBindRenderbuffer", Generated_glBindRenderbuffer},
    {"glBindSampler", Generated_glBindSampler},
    {"glBindTexture", Generated_glBindTexture},
    {"glBindVertexArray", Generated_glBindVertexArray},
    {"glBlendColor", Generated_glBlendColor},
    {"glBlendEquation", Generated_glBlendEquation},
    {"glBlendFunc", Generated_glBlendFunc},
    {"glBlitFramebuffer", Generated_glBlitFramebuffer},
    {"glBufferData", Generated_glBufferData},
    {"glBufferSubData", Generated_glBufferSubData},
    {"glCheckFramebufferStatus", Generated_glCheckFramebufferStatus},
    {"glClear", Generated_glClear},
    {"glClearColor", Generated_glClearColor},
    {"glClearStencil", Generated_glClearStencil},
    {"glColorMask", Generated_glColorMask},
    {"glCompileShader", Generated_glCompileShader},
    {"glCopyBufferSubData", Generated_glCopyBufferSubData},
    {"glCopyTexSubImage2D", Generated_glCopyTexSubImage2D},
    {"glCreateProgram", Generated_glCreateProgram},
    {"glCreateShader", Generated_glCreateShader},
    {"glCullFace", Generated_glCullFace},
    {"glDeleteBuffers", Generated_glDeleteBuffers},
    {"glDeleteFramebuffers", Generated_glDeleteFramebuffers},
    {"glDeleteProgram", Generated_glDeleteProgram},
    {"glDeleteQueries", Generated_glDeleteQueries},
    {"glDeleteRenderbuffers", Generated_glDeleteRenderbuffers},
    {"glDeleteSamplers", Generated_glDeleteSamplers},
    {"glDeleteShader", Generated_glDeleteShader},
    {"glDeleteTextures", Generated_glDeleteTextures},
    {"glDeleteVertexArrays", Generated_glDeleteVertexArrays},
    {"glDepthMask", Generated_glDepthMask},
    {"glDisable", Generated_glDisable},
    {"glDisableVertexAttribArray", Generated_glDisableVertexAttribArray},
    {"glDrawArrays", Generated_glDrawArrays},
    {"glDrawArraysInstanced", Generated_glDrawArraysInstanced},
    {"glDrawBuffer", Generated_glDrawBuffer},
    {"glDrawBuffers", Generated_glDrawBuffers},
    {"glEnable", Generated_glEnable},
    {"glEnableVertexAttribArray", Generated_glEnableVertexAttribArray},
    {"glEndQuery", Generated_glEndQuery},
    {"glFinish", Generated_glFinish},
    {"glFlush", Generated_glFlush},
    {"glFlushMappedBufferRange", Generated_glFlushMappedBufferRange},
    {"glFramebufferRenderbuffer", Generated_glFramebufferRenderbuffer},
    {"glFramebufferTexture2D", Generated_glFramebufferTexture2D},
    {"glFrontFace", Generated_glFrontFace},
    {"glGenBuffers", Generated_glGenBuffers},
    {"glGenFramebuffers", Generated_glGenFramebuffers},
    {"glGenQueries", Generated_glGenQueries},
    {"glGenRenderbuffers", Generated_glGenRenderbuffers},
    {"glGenSamplers", Generated_glGenSamplers},
    {"glGenTextures", Generated_glGenTextures},
    {"glGenVertexArrays", Generated_glGenVertexArrays},
    {"glGenerateMipmap", Generated_glGenerateMipmap},
    {"glGetBufferParameteriv", Generated_glGetBufferParameteriv},
    {"glGetError", Generated_glGetError},
    {"glGetFloatv", Generated_glGetFloatv},
    {"glGetFramebufferAttachmentParameteriv", Generated_glGetFramebufferAttachmentParameteriv},
    {"glGetInternalformativ", Generated_glGetInternalformativ},
    {"glGetMultisamplefv", Generated_glGetMultisamplefv},
    {"glGetProgramBinary", Generated_glGetProgramBinary},
    {"glGetProgramInfoLog", Generated_glGetProgramInfoLog},
    {"glGetProgramiv", Generated_glGetProgramiv},
    {"glGetQueryObjecti64v", Generated_glGetQueryObjecti64v},
    {"glGetQueryObjectui64v", Generated_glGetQueryObjectui64v},
    {"glGetQueryObjectuiv", Generated_glGetQueryObjectuiv},
    {"glGetQueryiv", Generated_glGetQueryiv},
    {"glGetRenderbufferParameteriv", Generated_glGetRenderbufferParameteriv},
    {"glGetShaderInfoLog", Generated_glGetShaderInfoLog},
    {"glGetShaderPrecisionFormat", Generated_glGetShaderPrecisionFormat},
    {"glGetShaderiv", Generated_glGetShaderiv},
    {"glGetTexLevelParameteriv", Generated_glGetTexLevelParameteriv},
    {"glGetUniformLocation", Generated_glGetUniformLocation},
    {"glInsertEventMarkerEXT", Generated_glInsertEventMarkerEXT},
    {"glIsTexture", Generated_glIsTexture},
    {"glLineWidth", Generated_glLineWidth},
    {"glLinkProgram", Generated_glLinkProgram},
    {"glPatchParameteri", Generated_glPatchParameteri},
    {"glPixelStorei", Generated_glPixelStorei},
    {"glPolygonMode", Generated_glPolygonMode},
    {"glPopGroupMarkerEXT", Generated_glPopGroupMarkerEXT},
    {"glProgramBinary", Generated_glProgramBinary},
    {"glProgramParameteri", Generated_glProgramParameteri},
    {"glPushGroupMarkerEXT", Generated_glPushGroupMarkerEXT},
    {"glQueryCounter", Generated_glQueryCounter},
    {"glReadBuffer", Generated_glReadBuffer},
    {"glRenderbufferStorage", Generated_glRenderbufferStorage},
    {"glRenderbufferStorageMultisample", Generated_glRenderbufferStorageMultisample},
    {"glSamplerParameterf", Generated_glSamplerParameterf},
    {"glSamplerParameteri", Generated_glSamplerParameteri},
    {"glSamplerParameteriv", Generated_glSamplerParameteriv},
    {"glScissor", Generated_glScissor},
    {"glStencilFunc", Generated_glStencilFunc},
    {"glStencilFuncSeparate", Generated_glStencilFuncSeparate},
    {"glStencilMask", Generated_glStencilMask},
    {"glStencilMaskSeparate", Generated_glStencilMaskSeparate},
    {"glStencilOp", Generated_glStencilOp},
    {"glStencilOpSeparate", Generated_glStencilOpSeparate},
    {"glTexBuffer", Generated_glTexBuffer},
    {"glTexParameterf", Generated_glTexParameterf},
    {"glTexParameterfv", Generated_glTexParameterfv},
    {"glTexParameteri", Generated_glTexParameteri},
    {"glTexParameteriv", Generated_glTexParameteriv},
    {"glTexStorage2D", Generated_glTexStorage2D},
    {"glTextureBarrierNV", Generated_glTextureBarrierNV},
    {"glUniform1f", Generated_glUniform1f},
    {"glUniform1fv", Generated_glUniform1fv},
    {"glUniform1i", Generated_glUniform1i},
    {"glUniform1iv", Generated_glUniform1iv},
    {"glUniform2f", Generated_glUniform2f},
    {"glUniform2fv", Generated_glUniform2fv},
    {"glUniform2i", Generated_glUniform2i},
    {"glUniform2iv", Generated_glUniform2iv},
    {"glUniform3f", Generated_glUniform3f},
    {"glUniform3fv", Generated_glUniform3fv},
    {"glUniform3i", Generated_glUniform3i},
    {"glUniform3iv", Generated_glUniform3iv},
    {"glUniform4f", Generated_glUniform4f},
    {"glUniform4fv", Generated_glUniform4fv},
    {"glUniform4i", Generated_glUniform4i},
    {"glUniform4iv", Generated_glUniform4iv},
    {"glUniformMatrix2fv", Generated_glUniformMatrix2fv},
    {"glUniformMatrix3fv", Generated_glUniformMatrix3fv},
    {"glUniformMatrix4fv", Generated_glUniformMatrix4fv},
    {"glUseProgram", Generated_glUseProgram},
    {"glVertexAttrib1f", Generated_glVertexAttrib1f},
    {"glVertexAttrib2fv", Generated_glVertexAttrib2fv},
    {"glVertexAttrib3fv", Generated_glVertexAttrib3fv},
    {"glVertexAttrib4fv", Generated_glVertexAttrib4fv},
    {"glVertexAttribDivisor", Generated_glVertexAttribDivisor},
    {"glViewport", Generated_glViewport},
};

} // namespace

const GLBinding* FindGeneratedGLBinding(const std::string& name) {
    for (const GLBinding& binding : kGeneratedGLBindings) {
        if (name == binding.name) {
            return &binding;
        }
    }
    return nullptr;
}
