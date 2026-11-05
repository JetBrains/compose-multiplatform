/**
 * Routes the published (Emscripten) Skiko runtime's GL imports to native_skiko.node (wasm-sdl only).
 *
 * The published skiko.wasm imports `env.emscripten_glX` (plus env.glGetString/glGetStringi/glGetIntegerv)
 * with the plain C GL ABI: pointers are Wasm32 offsets into the Skiko linear memory, i64 timeouts are BigInt.
 * native_skiko.node implements the same C ABI through `addon.glX` (explicit adapters) or
 * `addon.getGLFunction("glX")` once `attachWasmRuntime(memory, malloc)` points it at the Skiko memory.
 * skiko.wasm and the Skiko klib stay the published, ABI-matching pair; only the JS loader is adapted.
 */
import fs from 'fs';
import nodeModule from 'module';
import { pathToFileURL } from 'url';

const AWAIT_PATTERN = 'export const awaitSkiko = loadSkikoWASM().then(';
const AWAIT_REPLACEMENT = 'export const awaitSkiko = loadSkikoWASM(globalThis.__skikoEmscriptenModuleArg ?? {}).then(';

const GL_VERTEX_ARRAY_BINDING = 0x85B5;
const GL_COMPILE_STATUS = 0x8B81;
const GL_LINK_STATUS = 0x8B82;

// OES_vertex_array_object and EXT_disjoint_timer_query entry points are identical to their core GL counterparts.
const ALIASES = {
    glGenVertexArraysOES: 'glGenVertexArrays',
    glDeleteVertexArraysOES: 'glDeleteVertexArrays',
    glBindVertexArrayOES: 'glBindVertexArray',
    glGenQueriesEXT: 'glGenQueries',
    glDeleteQueriesEXT: 'glDeleteQueries',
    glBeginQueryEXT: 'glBeginQuery',
    glEndQueryEXT: 'glEndQuery',
    glGetQueryivEXT: 'glGetQueryiv',
    glGetQueryObjectuivEXT: 'glGetQueryObjectuiv',
    glQueryCounterEXT: 'glQueryCounter',
    glGetQueryObjectui64vEXT: 'glGetQueryObjectui64v',
    glGetQueryObjecti64vEXT: 'glGetQueryObjecti64v'
};

/**
 * Makes the eager `loadSkikoWASM()` call of the published skiko.mjs accept a module argument,
 * so that `instantiateWasm` can replace the WebGL-backed imports before the Wasm instance is created.
 * Must be called before skiko.mjs is imported. Runtimes that already export setSkikoNativeGLHost are left untouched.
 */
export function registerSkikoRuntimeTransform(skikoPath) {
    if (typeof nodeModule.registerHooks !== 'function') {
        throw new Error(`[SkikoNativeGL] node:module.registerHooks is required (running Node ${process.version})`);
    }
    const target = pathToFileURL(skikoPath).href;
    const state = { transformed: false, nativeHostRuntime: false };
    nodeModule.registerHooks({
        load(url, context, nextLoad) {
            const result = nextLoad(url, context);
            if (url.split('?')[0] !== target) {
                return result;
            }
            const source = typeof result.source === 'string' ? result.source : Buffer.from(result.source).toString('utf8');
            if (source.includes('setSkikoNativeGLHost')) {
                state.nativeHostRuntime = true;
                return result;
            }
            if (!source.includes(AWAIT_PATTERN)) {
                throw new Error(`[SkikoNativeGL] ${skikoPath} has neither setSkikoNativeGLHost nor the Emscripten loader pattern "${AWAIT_PATTERN}"`);
            }
            state.transformed = true;
            return { ...result, format: 'module', source: source.replace(AWAIT_PATTERN, AWAIT_REPLACEMENT) };
        }
    });
    return state;
}

function resolveNative(addon, name) {
    if (Object.prototype.hasOwnProperty.call(addon, name) && typeof addon[name] === 'function') {
        return addon[name];
    }
    const fn = addon.getGLFunction(name);
    return typeof fn === 'function' ? fn : null;
}

/**
 * GLSL ES 3.00 -> desktop GLSL 3.30 core. Skia's WebGL backend emits `#version 300 es`;
 * the macOS CGL core profile created by native_skiko.node only accepts desktop GLSL versions.
 */
export function translateGlslEsToCore(source) {
    return source.replace(/^\s*#version\s+300\s+es\s*$/m, '#version 330 core');
}

export function createEmscriptenNativeGLBridge({ addon, wasmPath, translateShaders = true, checkErrors = false, log = console.log }) {
    if (!addon || typeof addon.attachWasmRuntime !== 'function' || typeof addon.getGLFunction !== 'function') {
        throw new Error('[SkikoNativeGL] native_skiko.node with attachWasmRuntime/getGLFunction is required');
    }
    const usage = new Map();
    const unmapped = [];
    const shaderHeads = [];
    const runtime = { memory: null, exports: null, attachedBuffer: null, current: 0, contexts: new Map() };
    let translatedShaders = 0;

    const finishFn = resolveNative(addon, 'glFinish');
    if (typeof finishFn === 'function') {
        globalThis.__skikoGlFinish = () => finishFn.call(addon);
    }

    // The addon caches the memory base; re-attach whenever memory growth replaced the ArrayBuffer.
    const syncMemory = () => {
        if (runtime.memory.buffer !== runtime.attachedBuffer) {
            addon.attachWasmRuntime(runtime.memory, runtime.exports.malloc);
            runtime.attachedBuffer = runtime.memory.buffer;
        }
    };
    const count = (name) => usage.set(name, (usage.get(name) || 0) + 1);
    let nativeGetError = null;
    // Validation mode: any native GL error raised by a routed call aborts the run.
    const checkError = (glName) => {
        if (!checkErrors || glName === 'glGetError') return;
        nativeGetError ??= resolveNative(addon, 'glGetError');
        const error = nativeGetError();
        if (error !== 0) {
            throw new Error(`[SkikoNativeGL] native GL error 0x${error.toString(16)} after ${glName}`);
        }
    };

    const readCString = (ptr, len) => {
        const u8 = new Uint8Array(runtime.memory.buffer);
        let end = ptr;
        if (len === undefined || len < 0) {
            while (u8[end] !== 0) end++;
        } else {
            end = ptr + len;
        }
        return Buffer.from(u8.subarray(ptr, end)).toString('utf8');
    };

    function makeImport(importName, glName) {
        const fn = resolveNative(addon, ALIASES[glName] || glName);
        if (!fn) {
            unmapped.push(importName);
            return () => {
                throw new Error(`[SkikoNativeGL] Skia called GL import ${importName}, which has no native_skiko.node binding`);
            };
        }
        if (glName === 'glClientWaitSync' || glName === 'glWaitSync') {
            // Published import: (sync, flags, BigInt timeout); addon: (sync, flags, timeoutLow, timeoutHigh).
            return (sync, flags, timeout) => {
                syncMemory(); count(glName);
                const t = BigInt.asUintN(64, BigInt(timeout));
                const result = fn(sync, flags, Number(t & 0xFFFFFFFFn), Number(t >> 32n));
                checkError(glName);
                return result;
            };
        }
        if (glName === 'glBindVertexArray' || glName === 'glBindVertexArrayOES') {
            // A core profile has no usable VAO 0, while Skia's WebGL backend uses 0 as its default vertex state.
            return (vao) => {
                syncMemory(); count(glName);
                const ctx = runtime.contexts.get(runtime.current);
                const result = fn(vao === 0 && ctx ? ctx.defaultVao : vao);
                checkError(glName);
                return result;
            };
        }
        if (glName === 'glGetIntegerv') {
            return (pname, ptr) => {
                syncMemory(); count(glName);
                const result = fn(pname, ptr);
                checkError(glName);
                if (pname === GL_VERTEX_ARRAY_BINDING && ptr) {
                    const ctx = runtime.contexts.get(runtime.current);
                    const i32 = new Int32Array(runtime.memory.buffer);
                    if (ctx && i32[ptr >> 2] === ctx.defaultVao) i32[ptr >> 2] = 0;
                }
                return result;
            };
        }
        if (glName === 'glShaderSource') {
            return (shader, stringCount, strings, lengths) => {
                syncMemory(); count(glName);
                const u32 = new Uint32Array(runtime.memory.buffer);
                const i32 = new Int32Array(runtime.memory.buffer);
                let source = '';
                for (let i = 0; i < stringCount; i++) {
                    source += readCString(u32[(strings >> 2) + i], lengths ? i32[(lengths >> 2) + i] : undefined);
                }
                if (shaderHeads.length < 2) shaderHeads.push(source.split('\n')[0]);
                if (!translateShaders) {
                    const result = fn(shader, stringCount, strings, lengths);
                    checkError(glName);
                    return result;
                }
                const encoded = Buffer.from(translateGlslEsToCore(source) + '\0', 'utf8');
                const stringPtr = runtime.exports.malloc(encoded.length);
                const arrayPtr = runtime.exports.malloc(4);
                syncMemory();
                new Uint8Array(runtime.memory.buffer).set(encoded, stringPtr);
                new Uint32Array(runtime.memory.buffer)[arrayPtr >> 2] = stringPtr;
                translatedShaders++;
                try {
                    const result = fn(shader, 1, arrayPtr, 0);
                    checkError(glName);
                    return result;
                } finally {
                    runtime.exports.free(arrayPtr);
                    runtime.exports.free(stringPtr);
                }
            };
        }
        if (glName === 'glGetShaderiv' || glName === 'glGetProgramiv') {
            // Skia silently skips draws whose program fails to build; a benchmark must fail instead.
            const isShader = glName === 'glGetShaderiv';
            const statusEnum = isShader ? GL_COMPILE_STATUS : GL_LINK_STATUS;
            const infoLog = resolveNative(addon, isShader ? 'glGetShaderInfoLog' : 'glGetProgramInfoLog');
            return (object, pname, ptr) => {
                syncMemory(); count(glName);
                const result = fn(object, pname, ptr);
                checkError(glName);
                if (pname === statusEnum && ptr && new Int32Array(runtime.memory.buffer)[ptr >> 2] === 0) {
                    const size = 4096;
                    const buffer = runtime.exports.malloc(size);
                    syncMemory();
                    infoLog(object, size, 0, buffer);
                    checkError(isShader ? 'glGetShaderInfoLog' : 'glGetProgramInfoLog');
                    const message = readCString(buffer);
                    runtime.exports.free(buffer);
                    throw new Error(`[SkikoNativeGL] native GL ${isShader ? 'shader compile' : 'program link'} failed: ${message}`);
                }
                return result;
            };
        }
        if (checkErrors) {
            return (...args) => {
                syncMemory();
                count(glName);
                const result = fn(...args);
                checkError(glName);
                return result;
            };
        }
        return (...args) => {
            syncMemory();
            count(glName);
            return fn(...args);
        };
    }

    const moduleArg = {
        locateFile: (file, dir) => (file.endsWith('.wasm') ? wasmPath : dir + file),
        instantiateWasm(info, receiveInstance) {
            const module = new WebAssembly.Module(fs.readFileSync(wasmPath));
            let mapped = 0;
            for (const imp of WebAssembly.Module.imports(module)) {
                if (imp.kind !== 'function') continue;
                const match = /^(?:emscripten_)?(gl[A-Z].*)$/.exec(imp.name);
                if (!match) continue;
                info[imp.module][imp.name] = makeImport(imp.name, match[1]);
                mapped++;
            }
            const instance = new WebAssembly.Instance(module, info);
            runtime.exports = instance.exports;
            runtime.memory = instance.exports.memory;
            syncMemory();
            log(`[SkikoNativeGL] skiko.wasm instantiated: ${mapped} GL imports routed to native_skiko.node; ` +
                `${unmapped.length} without native binding (throw if called): ${unmapped.join(', ') || '-'}`);
            receiveInstance(instance, module);
            return instance.exports;
        }
    };

    /**
     * Replaces Emscripten's WebGL context management with native_skiko.node contexts.
     * Must be called after `awaitSkiko` resolved (Emscripten assigns Module.GL during evaluation).
     */
    function installNativeContexts() {
        const GL = moduleArg.GL;
        if (!GL || typeof GL.createContext !== 'function' || !runtime.exports) {
            throw new Error('[SkikoNativeGL] Emscripten Module.GL / skiko.wasm instance is not available; was skiko.mjs transformed?');
        }
        GL.createContext = (canvas, attributes = {}) => {
            const width = canvas && canvas.width;
            const height = canvas && canvas.height;
            if (!Number.isInteger(width) || !Number.isInteger(height) || width <= 0 || height <= 0) {
                throw new Error(`[SkikoNativeGL] canvas dimensions must be positive integers, got ${width}x${height}`);
            }
            const id = addon.createContext({ width, height });
            if (!id) {
                throw new Error('[SkikoNativeGL] native createContext returned 0');
            }
            addon.makeContextCurrent(id);
            syncMemory();
            const vaoPtr = runtime.exports.malloc(4);
            syncMemory();
            resolveNative(addon, 'glGenVertexArrays')(1, vaoPtr);
            const defaultVao = new Uint32Array(runtime.memory.buffer)[vaoPtr >> 2];
            runtime.exports.free(vaoPtr);
            if (!defaultVao) {
                throw new Error('[SkikoNativeGL] glGenVertexArrays returned 0 for the default VAO');
            }
            resolveNative(addon, 'glBindVertexArray')(defaultVao);
            const ctx = { handle: id, attributes, version: 2, GLctx: null, native: true, defaultVao, width, height, canvas };
            runtime.contexts.set(id, ctx);
            GL.contexts[id] = ctx;
            runtime.current = id;
            GL.currentContext = ctx;
            if (canvas) canvas.__skikoNativeContext = id;
            log(`[SkikoNativeGL] native GL context ${id} created (${width}x${height})`);
            return id;
        };
        GL.makeContextCurrent = (handle) => {
            if (!handle) {
                runtime.current = 0;
                GL.currentContext = null;
                return true;
            }
            const ctx = runtime.contexts.get(handle);
            if (!ctx) {
                throw new Error(`[SkikoNativeGL] unknown native GL context handle ${handle}`);
            }
            addon.makeContextCurrent(handle);
            runtime.current = handle;
            GL.currentContext = ctx;
            return true;
        };
        GL.deleteContext = (handle) => {
            const ctx = runtime.contexts.get(handle);
            if (!ctx) return;
            addon.destroyContext(handle);
            runtime.contexts.delete(handle);
            delete GL.contexts[handle];
            if (ctx.canvas && ctx.canvas.__skikoNativeContext === handle) {
                ctx.canvas.__skikoNativeContext = 0;
            }
            if (runtime.current === handle) {
                runtime.current = 0;
                GL.currentContext = null;
            }
            return true;
        };
        return GL;
    }

    /**
     * Reads the context framebuffer back and summarizes it (evidence that the GPU actually drew something).
     */
    function readbackSummary(handle) {
        const ctx = runtime.contexts.get(handle);
        if (!ctx) {
            throw new Error(`[SkikoNativeGL] unknown native GL context handle ${handle}`);
        }
        if (runtime.current !== handle) {
            addon.makeContextCurrent(handle);
            runtime.current = handle;
        }
        resolveNative(addon, 'glFinish')();
        const size = ctx.width * ctx.height * 4;
        const ptr = runtime.exports.malloc(size);
        syncMemory();
        // GL_RGBA / GL_UNSIGNED_BYTE from logical framebuffer 0 (the addon maps it to the context FBO).
        addon.glBindFramebuffer(0x8D40, 0);
        addon.glReadPixels(0, 0, ctx.width, ctx.height, 0x1908, 0x1401, ptr);
        const pixels = new Uint32Array(runtime.memory.buffer, ptr, ctx.width * ctx.height);
        const colors = new Map();
        for (let i = 0; i < pixels.length; i += 7) {
            colors.set(pixels[i], (colors.get(pixels[i]) || 0) + 1);
        }
        const top = [...colors.entries()].sort((a, b) => b[1] - a[1]).slice(0, 3)
            .map(([c, n]) => `#${(c >>> 0).toString(16).padStart(8, '0')}x${n}`);
        runtime.exports.free(ptr);
        return { width: ctx.width, height: ctx.height, sampledDistinctColors: colors.size, topSampledColorsABGR: top };
    }

    return {
        moduleArg,
        installNativeContexts,
        readbackSummary,
        contextIds: () => [...runtime.contexts.keys()],
        report: () => ({
            unmapped: unmapped.slice(),
            translateShaders,
            translatedShaders,
            shaderHeads: shaderHeads.slice(),
            usage: Object.fromEntries([...usage.entries()].sort((a, b) => b[1] - a[1]))
        })
    };
}
