/**
 * Automated Multi-Launcher Benchmark Runner for Compose Wasm in Native Window (SDL2 / Native Host).
 *
 * Supported Launchers:
 * - 'node' / 'sdl' (Node.js with SDL2 and native_skiko.node addon)
 * - 'wasmtime' (Wasmtime CLI with WASI runtime engine)
 */

import fs from 'fs';
import { spawn, spawnSync } from 'child_process';
import { createRequire } from 'module';
import path from 'path';
import { inspect } from 'util';
import { fileURLToPath, pathToFileURL } from 'url';
import { registerSkikoRuntimeTransform, createEmscriptenNativeGLBridge } from './skiko_emscripten_native_gl.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function canonicalPath(targetPath) {
    let existingPath = path.resolve(targetPath);
    const remainingSegments = [];
    while (!fs.existsSync(existingPath)) {
        remainingSegments.unshift(path.basename(existingPath));
        existingPath = path.dirname(existingPath);
    }
    return path.resolve(fs.realpathSync(existingPath), ...remainingSegments);
}

// ==========================================
// Launcher Abstraction Layer
// ==========================================

export class BenchmarkLauncher {
    constructor(name) {
        this.name = name;
    }

    async initialize(options) {
        throw new Error('initialize() must be implemented by launcher');
    }

    async runBenchmarks(benchmarkNames, options) {
        throw new Error('runBenchmarks() must be implemented by launcher');
    }

    parseDurationMs(val) {
        if (!val || val === 'N/A') return null;
        if (typeof val === 'number') return val;
        if (typeof val === 'string' && val.startsWith('PT')) {
            const seconds = parseFloat(val.replace('PT', '').replace('S', ''));
            if (!isNaN(seconds)) {
                return seconds * 1000;
            }
        }
        const num = parseFloat(val);
        return isNaN(num) ? null : num;
    }

    formatReportFromStats(name, stats) {
        const frameInfo = stats.averageFrameInfo || {};
        const fpsInfo = stats.averageFPSInfo || {};
        const conditions = stats.conditions || {};
        const cpuMs = this.parseDurationMs(frameInfo.cpuTime);
        const gpuMs = this.parseDurationMs(frameInfo.gpuTime);
        const totalMs = (cpuMs !== null && gpuMs !== null) ? (cpuMs + gpuMs) : (this.parseDurationMs(frameInfo.totalTime) || null);
        return {
            benchmark: name,
            launcher: this.name,
            iterations: conditions.frameCount ?? 1000,
            warmup: conditions.warmupCount ?? 100,
            effectiveFps: fpsInfo.fps ? Number(fpsInfo.fps.toFixed(1)) : (totalMs ? Number((1000 / totalMs).toFixed(1)) : 0),
            cpuAvgMs: cpuMs !== null ? `${cpuMs.toFixed(3)} ms` : 'N/A',
            gpuAvgMs: gpuMs !== null ? `${gpuMs.toFixed(3)} ms` : 'N/A',
            totalAvgMs: totalMs !== null ? `${totalMs.toFixed(3)} ms` : 'N/A'
        };
    }

    async cleanup() {
        // Optional hook
    }
}

const ALL_AVAILABLE_BENCHMARKS = [
    "AnimatedVisibility",
    "CanvasDrawing",
    "HeavyShader",
    "LazyGrid",
    "LazyGrid-ItemLaunchedEffect",
    "LazyGrid-SmoothScroll",
    "LazyGrid-SmoothScroll-ItemLaunchedEffect",
    "LazyList",
    "MultipleComponents",
    "MultipleComponents-NoVectorGraphics",
    "TextLayout",
    "VisualEffects"
];

/**
 * SNAPSHOT native-host Skiko looks up GL entry points as `nativeGLHost[name]`.
 * The addon exposes a few special wrappers by name and the rest via getGLFunction(name),
 * which already interprets Wasm linear-memory offsets. This host is that lookup table,
 * not a browser WebGL2 adapter.
 */
function createSkikoNativeGLHost(addon) {
    if (!addon) {
        throw new Error('[NodeSdlLauncher] Native Skiko addon is required to build the GL host');
    }
    const resolved = new Map();
    return new Proxy(addon, {
        get(target, prop, receiver) {
            if (typeof prop !== 'string') {
                return Reflect.get(target, prop, receiver);
            }
            // NAPI exports are non-configurable; returning a bound copy violates Proxy invariants.
            if (Reflect.has(target, prop)) {
                return Reflect.get(target, prop, receiver);
            }
            if (resolved.has(prop)) {
                return resolved.get(prop);
            }
            if (prop.startsWith('gl') && typeof target.getGLFunction === 'function') {
                const fn = target.getGLFunction(prop);
                if (typeof fn === 'function') {
                    resolved.set(prop, fn);
                    return fn;
                }
            }
            return undefined;
        }
    });
}

function formatWasmError(error) {
    if (error instanceof Error) {
        return error.stack || `${error.name}: ${error.message}`;
    }
    const message = error && (error.message || error.stack);
    if (typeof message === 'string' && message) {
        return message;
    }
    const isWasmException = typeof WebAssembly !== 'undefined' && WebAssembly.Exception && error instanceof WebAssembly.Exception;
    if (isWasmException || (error && typeof error === 'object' && Object.keys(error).length === 0)) {
        // Kotlin/Wasm exceptions are opaque to JS; Main.wasmJs.kt prints message + stack trace on the Kotlin side.
        return '<opaque Kotlin/Wasm exception; see "Benchmark exception during execution" above for the Kotlin message and stack trace>';
    }
    return inspect(error, { depth: 8, breakLength: 120, getters: true, showHidden: true });
}

export class NodeCanvas {
    constructor(description = {}) {
        this.width = description.width || 1920;
        this.height = description.height || 1080;
        this.clientWidth = this.width;
        this.clientHeight = this.height;
        this.offsetWidth = this.width;
        this.offsetHeight = this.height;
        this.scrollWidth = this.width;
        this.scrollHeight = this.height;
        this.id = description.id || '';
        this.tagName = 'CANVAS';
        this.nodeName = 'CANVAS';
        this.nodeType = 1;
        this.childNodes = [];
        this.children = [];
        this.parentNode = null;
        this.parentElement = null;
        this.ownerDocument = null;
        Object.assign(this, description);
        this.style = description.style || {};
        this.__skikoNativeContext = 0;
    }

    getContext(type) {
        return null;
    }

    setAttribute(key, value) { this[key] = value; }
    getAttribute(key) { return this[key] ?? null; }
    removeAttribute(key) { delete this[key]; }
    addEventListener() {}
    removeEventListener() {}
    remove() {
        if (this.parentNode && typeof this.parentNode.removeChild === 'function') {
            this.parentNode.removeChild(this);
        }
    }
    appendChild(child) {
        this.childNodes.push(child);
        this.children.push(child);
        if (child) {
            child.parentNode = this;
            child.parentElement = this;
        }
        return child;
    }
    removeChild(child) {
        this.childNodes = this.childNodes.filter(c => c !== child);
        this.children = this.children.filter(c => c !== child);
        if (child) {
            child.parentNode = null;
            child.parentElement = null;
        }
        return child;
    }
    focus() {}
    blur() {}
    getBoundingClientRect() {
        return {
            top: 0,
            left: 0,
            width: this.width,
            height: this.height,
            right: this.width,
            bottom: this.height
        };
    }
}

function buildKotlinArgs(rawArgs = [], options = {}) {
    const iterations = options.iterations || 1000;
    const warmup = options.warmup || 100;
    const kotlinArgs = [];
    let hasFrameCount = false;
    let hasWarmup = false;
    let hasSaveStats = false;
    let hasBenchmarksArg = false;

    for (const arg of rawArgs) {
        if (typeof arg !== 'string') continue;
        if (arg.startsWith('launcher=') || arg.startsWith('--launcher=') ||
            arg.startsWith('nativeSkikoPath=') || arg.startsWith('--nativeSkikoPath=') ||
            arg.startsWith('reportsDir=') || arg.startsWith('--reportsDir=')) {
            continue;
        }
        const clean = arg.startsWith('--') ? arg.slice(2) : arg;
        if (clean.startsWith('frameCount=') || clean.startsWith('iterations=')) {
            hasFrameCount = true;
            kotlinArgs.push(`frameCount=${clean.split('=')[1]}`);
        } else if (clean.startsWith('warmupCount=') || clean.startsWith('warmup=')) {
            hasWarmup = true;
            kotlinArgs.push(`warmupCount=${clean.split('=')[1]}`);
        } else if (clean.startsWith('saveStatsToJSON=')) {
            hasSaveStats = true;
            kotlinArgs.push(clean);
        } else if (clean.startsWith('benchmarks=') || clean.startsWith('benchmark=')) {
            hasBenchmarksArg = true;
            kotlinArgs.push(`benchmarks=${clean.split('=')[1]}`);
        } else {
            kotlinArgs.push(clean);
        }
    }

    if (!hasFrameCount) kotlinArgs.push(`frameCount=${iterations}`);
    if (!hasWarmup) kotlinArgs.push(`warmupCount=${warmup}`);
    if (!hasSaveStats) kotlinArgs.push('saveStatsToJSON=true');
    if (!hasBenchmarksArg && options.benchmarkArg) {
        kotlinArgs.push(`benchmarks=${options.benchmarkArg}`);
    } else if (!hasBenchmarksArg && Array.isArray(options.benchmarkNames) && options.benchmarkNames.length > 0) {
        kotlinArgs.push(`benchmarks=${options.benchmarkNames.join(',')}`);
    }

    return kotlinArgs;
}

/**
 * Node.js + SDL2 + Native Skiko OpenGL Launcher
 */
export class NodeSdlBenchmarkLauncher extends BenchmarkLauncher {
    constructor() {
        super('node_sdl');
        this.nativeGL = null;
        this.nativeGLContextId = null;
        this.wasmExports = null;
        this.canvases = new Map();
        this.isCleanedUp = false;
        this._exitHandler = () => this.cleanupSync();
        process.once('exit', this._exitHandler);
    }

    async initialize(options) {
        globalThis.isSdl = true;
        globalThis.__isBenchmarkMode = true;
        globalThis.isNodeAddonRunning = () => true;
        globalThis.getBenchmarkArgs = () => '';

        const nativeSkikoPath = options.nativeSkikoPath || process.env.SKIKO_NATIVE_GL_ADDON;

        if (!nativeSkikoPath) {
            throw new Error('[NodeSdlLauncher] Native Skiko addon path is required. Set SKIKO_NATIVE_GL_ADDON or pass -Pbenchmark.nativeSkiko=/path/to/native_skiko.node to Gradle.');
        }

        if (!fs.existsSync(nativeSkikoPath)) {
            throw new Error(`[NodeSdlLauncher] Native Skiko addon not found at path: ${nativeSkikoPath}`);
        }

        const require = createRequire(import.meta.url);
        try {
            this.nativeGL = require(nativeSkikoPath);
        } catch (err) {
            throw new Error(`[NodeSdlLauncher] Failed to load native Skiko addon from ${nativeSkikoPath}: ${err.message}`, { cause: err });
        }

        if (!this.nativeGL) {
            throw new Error(`[NodeSdlLauncher] Native Skiko addon is null after require`);
        }

        this.nativeGLHost = createSkikoNativeGLHost(this.nativeGL);
        globalThis.__skikoNativeGL = this.nativeGLHost;

        const width = options.width || 1920;
        const height = options.height || 1080;
        const title = "Compose Wasm Benchmark";

        try {
            this.nativeGL.createWindow(width, height, title);
        } catch (e) {
            throw new Error(`[NodeSdlLauncher] Native GL window creation failed: ${e.message}`, { cause: e });
        }

        this.setupShims(options);
    }

    setupShims(options) {
        const originalFetch = globalThis.fetch;
        globalThis.fetch = async function(resource, init) {
            let urlStr = typeof resource === 'string' ? resource : (resource?.url || resource?.toString?.() || '');
            let filePath = null;
            if (urlStr.startsWith('http://localhost/') || urlStr.startsWith('http://localhost:8080/')) {
                urlStr = urlStr.replace(/^https?:\/\/localhost(:\d+)?\//, '');
            }
            if (urlStr.startsWith('file://')) {
                try {
                    filePath = new URL(urlStr).pathname;
                } catch (e) {}
            } else if (!urlStr.startsWith('http://') && !urlStr.startsWith('https://')) {
                const searchBases = [
                    __dirname,
                    process.cwd(),
                    path.resolve(process.cwd(), 'build/wasm/packages/compose-benchmarks-benchmarks/kotlin'),
                    path.resolve(process.cwd(), 'benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin'),
                    path.resolve(__dirname, '../../../../benchmarks/src/commonMain')
                ];
                for (const base of searchBases) {
                    const candidate = path.resolve(base, urlStr);
                    if (fs.existsSync(candidate)) {
                        filePath = candidate;
                        break;
                    }
                }
                if (!filePath) {
                    filePath = path.resolve(__dirname, urlStr);
                }
            }
            if (filePath && fs.existsSync(filePath)) {
                const buffer = fs.readFileSync(filePath);
                const contentType = filePath.endsWith('.wasm') ? 'application/wasm' : (filePath.endsWith('.png') ? 'image/png' : 'application/octet-stream');
                return new Response(buffer, {
                    status: 200,
                    statusText: 'OK',
                    headers: { 'Content-Type': contentType }
                });
            }
            if (originalFetch) {
                return originalFetch(resource, init);
            }
            throw new Error(`Cannot fetch ${urlStr}`);
        };

        if (typeof globalThis.read === 'undefined') {
            globalThis.read = function(filePath, type) {
                if (filePath.startsWith('file://')) {
                    try { filePath = new URL(filePath).pathname; } catch (e) {}
                }
                if (type === 'binary') {
                    const buf = fs.readFileSync(filePath);
                    return new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
                }
                return fs.readFileSync(filePath, 'utf8');
            };
        }

        if (typeof globalThis.readbuffer === 'undefined') {
            globalThis.readbuffer = function(filePath) {
                if (filePath.startsWith('file://')) {
                    try { filePath = new URL(filePath).pathname; } catch (e) {}
                }
                const buf = fs.readFileSync(filePath);
                return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
            };
        }

        if (typeof globalThis.XMLHttpRequest === 'undefined') {
            class XMLHttpRequestShim {
                constructor() {
                    this.status = 0;
                    this.response = null;
                    this.onload = null;
                    this.onerror = null;
                    this.readyState = 0;
                    this.responseType = '';
                }
                open(method, url, async = true) {
                    this._url = url;
                }
                send() {
                    let filePath = this._url;
                    if (filePath.startsWith('file://')) {
                        try {
                            filePath = new URL(filePath).pathname;
                        } catch (e) {}
                    } else if (!filePath.startsWith('http://') && !filePath.startsWith('https://')) {
                        filePath = path.resolve(__dirname, filePath);
                    }
                    try {
                        const buf = fs.readFileSync(filePath);
                        this.status = 200;
                        this.response = this.responseType === 'arraybuffer' 
                            ? buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength)
                            : buf.toString('utf8');
                        this.readyState = 4;
                        if (this.onload) setTimeout(() => this.onload(), 0);
                    } catch (e) {
                        this.status = 404;
                        if (this.onerror) setTimeout(() => this.onerror(e), 0);
                    }
                }
            }
            globalThis.XMLHttpRequest = XMLHttpRequestShim;
        }

        const navigatorShim = {
            languages: ['en-US'],
            language: 'en-US',
            platform: process.platform,
            userAgent: `Mozilla/5.0 (Node.js ${process.version})`
        };
        try {
            Object.defineProperty(globalThis, "navigator", {
                configurable: true,
                value: navigatorShim
            });
        } catch {
            globalThis.navigator = navigatorShim;
        }

        globalThis.EventTarget = class EventTarget {
            addEventListener() {}
            removeEventListener() {}
            dispatchEvent() { return true; }
        };
        globalThis.Node = class Node extends globalThis.EventTarget {};
        globalThis.Element = class Element extends globalThis.Node {};
        globalThis.HTMLElement = class HTMLElement extends globalThis.Element {};
        globalThis.HTMLDivElement = class HTMLDivElement extends globalThis.HTMLElement {};
        globalThis.HTMLCanvasElement = NodeCanvas;
        Object.setPrototypeOf(NodeCanvas.prototype, globalThis.HTMLElement.prototype);
        globalThis.HTMLTextAreaElement = class HTMLTextAreaElement extends globalThis.HTMLElement {};
        globalThis.HTMLInputElement = class HTMLInputElement extends globalThis.HTMLElement {};
        globalThis.HTMLStyleElement = class HTMLStyleElement extends globalThis.HTMLElement {};
        globalThis.HTMLHeadElement = class HTMLHeadElement extends globalThis.HTMLElement {};
        globalThis.HTMLBodyElement = class HTMLBodyElement extends globalThis.HTMLElement {};
        globalThis.HTMLHtmlElement = class HTMLHtmlElement extends globalThis.HTMLElement {};
        globalThis.DocumentFragment = class DocumentFragment extends globalThis.Node {};
        globalThis.ShadowRoot = class ShadowRoot extends globalThis.DocumentFragment {};

        globalThis.window = globalThis;
        globalThis.self = globalThis;
        globalThis.devicePixelRatio = 1.0;
        globalThis.innerWidth = options.width || 1920;
        globalThis.innerHeight = options.height || 1080;
        globalThis.location = globalThis.location || { href: 'http://localhost/', search: '' };
        globalThis.isSecureContext = false;

        this.canvases = new Map();
        let docTitle = "Compose Wasm Benchmark";

        const elementWidth = options.width || 1920;
        const elementHeight = options.height || 1080;

        const makeStyle = () => {
            const s = {};
            s.setProperty = (k, v) => { s[k] = v; };
            s.getPropertyValue = (k) => s[k] ?? '';
            s.removeProperty = (k) => { const v = s[k]; delete s[k]; return v ?? ''; };
            return s;
        };

        const makeElement = (tag = 'div') => {
            const tagName = (tag || 'div').toUpperCase();
            if (tagName === 'CANVAS') {
                const canvas = new NodeCanvas({ width: elementWidth, height: elementHeight });
                canvas.style = makeStyle();
                canvas.setPointerCapture = () => {};
                canvas.releasePointerCapture = () => {};
                canvas.hasPointerCapture = () => false;
                canvas.replaceChildren = () => {};
                canvas.getRootNode = () => globalThis.document;
                canvas.ownerDocument = globalThis.document;
                return canvas;
            }
            let proto = globalThis.HTMLElement.prototype;
            if (tagName === 'DIV') proto = globalThis.HTMLDivElement.prototype;
            else if (tagName === 'TEXTAREA') proto = globalThis.HTMLTextAreaElement.prototype;
            else if (tagName === 'INPUT') proto = globalThis.HTMLInputElement.prototype;
            else if (tagName === 'STYLE') proto = globalThis.HTMLStyleElement.prototype;
            else if (tagName === 'HEAD') proto = globalThis.HTMLHeadElement.prototype;
            else if (tagName === 'BODY') proto = globalThis.HTMLBodyElement.prototype;
            else if (tagName === 'HTML') proto = globalThis.HTMLHtmlElement.prototype;

            const el = Object.create(proto);
            Object.assign(el, {
                style: makeStyle(),
                width: elementWidth,
                height: elementHeight,
                clientWidth: elementWidth,
                clientHeight: elementHeight,
                offsetWidth: elementWidth,
                offsetHeight: elementHeight,
                scrollWidth: elementWidth,
                scrollHeight: elementHeight,
                id: '',
                tagName: tagName,
                nodeName: tagName,
                nodeType: 1,
                childNodes: [],
                children: [],
                parentNode: null,
                parentElement: null,
                ownerDocument: null,
                setAttribute(key, value) { el[key] = value; },
                getAttribute(key) { return el[key] ?? null; },
                removeAttribute(key) { delete el[key]; },
                addEventListener() {},
                removeEventListener() {},
                remove() {
                    if (el.parentNode && typeof el.parentNode.removeChild === 'function') {
                        el.parentNode.removeChild(el);
                    }
                },
                appendChild: (child) => {
                    el.childNodes.push(child);
                    el.children.push(child);
                    if (child) {
                        child.parentNode = el;
                        child.parentElement = el;
                        if (child.id) this.canvases.set(child.id, child);
                    }
                    return child;
                },
                removeChild: (child) => {
                    el.childNodes = el.childNodes.filter(c => c !== child);
                    el.children = el.children.filter(c => c !== child);
                    if (child) {
                        child.parentNode = null;
                        child.parentElement = null;
                    }
                    return child;
                },
                replaceChildren() {
                    el.childNodes = [];
                    el.children = [];
                },
                attachShadow: () => {
                    const shadow = makeElement('div');
                    Object.setPrototypeOf(shadow, globalThis.ShadowRoot.prototype);
                    shadow.host = el;
                    el.shadowRoot = shadow;
                    return shadow;
                },
                shadowRoot: null,
                focus() {},
                blur() {},
                setPointerCapture() {},
                releasePointerCapture() {},
                hasPointerCapture: () => false,
                getRootNode: () => globalThis.document,
                getBoundingClientRect() {
                    return {
                        top: 0,
                        left: 0,
                        width: elementWidth,
                        height: elementHeight,
                        right: elementWidth,
                        bottom: elementHeight
                    };
                }
            });
            return el;
        };

        const docElement = makeElement('html');
        const docBody = makeElement('body');
        const docHead = makeElement('head');
        const rootElement = makeElement('div');
        rootElement.id = 'root';
        docBody.appendChild(rootElement);

        this.canvases.set('root', rootElement);
        this.canvases.set(rootElement, rootElement);

        globalThis.document = {
            get title() {
                return docTitle;
            },
            set title(value) {
                docTitle = String(value);
                if (globalThis.__skikoNativeGL && typeof globalThis.__skikoNativeGL.setWindowTitle === 'function') {
                    try { globalThis.__skikoNativeGL.setWindowTitle(docTitle); } catch (err) {
                        console.warn(`[NodeSdlLauncher] Failed to set window title:`, err);
                    }
                }
            },
            visibilityState: 'visible',
            hidden: false,
            activeElement: null,
            documentElement: docElement,
            body: docBody,
            head: docHead,
            defaultView: globalThis.window,
            getElementById: (id) => this.canvases.get(id) || null,
            createElement: (tag) => {
                const el = makeElement(tag);
                el.ownerDocument = globalThis.document;
                if (el instanceof NodeCanvas) {
                    if (el.id) {
                        this.canvases.set(el.id, el);
                    }
                    this.canvases.set(el, el);
                }
                return el;
            },
            addEventListener: () => {},
            removeEventListener: () => {},
            contains: () => true,
            hasFocus: () => true
        };

        docElement.ownerDocument = globalThis.document;
        docBody.ownerDocument = globalThis.document;
        docHead.ownerDocument = globalThis.document;
        rootElement.ownerDocument = globalThis.document;

        globalThis.window.matchMedia = (query) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: () => {},
            removeListener: () => {},
            addEventListener: () => {},
            removeEventListener: () => {},
            dispatchEvent: () => false
        });
        globalThis.matchMedia = globalThis.window.matchMedia;

        if (typeof globalThis.ResizeObserver === 'undefined') {
            globalThis.ResizeObserver = class {
                observe() {}
                unobserve() {}
                disconnect() {}
            };
        }
        if (typeof globalThis.MutationObserver === 'undefined') {
            globalThis.MutationObserver = class {
                observe() {}
                disconnect() {}
                takeRecords() { return []; }
            };
        }

        let rafQueue = [];
        let rafScheduled = false;

        const pumpRaf = () => {
            rafScheduled = false;
            if (this.isCleanedUp) return;

            if (this.nativeGL && typeof this.nativeGL.pollEvents === 'function') {
                try { this.nativeGL.pollEvents(); } catch (e) {}
            }

            if (rafQueue.length > 0) {
                const batch = rafQueue;
                rafQueue = [];
                const now = performance.now();
                for (const item of batch) {
                    try {
                        item.cb(now);
                    } catch (err) {
                        console.error('[NodeSdlLauncher] Error in requestAnimationFrame callback:', err);
                    }
                }
                if (this.nativeGL && typeof this.nativeGL.present === 'function') {
                    try {
                        const ids = this.emscriptenBridge ? this.emscriptenBridge.contextIds() : [1];
                        for (const id of ids) {
                            this.nativeGL.present(id);
                            if (typeof this.nativeGL.makeContextCurrent === 'function') {
                                this.nativeGL.makeContextCurrent(id);
                            }
                        }
                    } catch (e) {}
                }
            }

            if (rafQueue.length > 0 && !rafScheduled) {
                rafScheduled = true;
                setImmediate(pumpRaf);
            }
        };

        globalThis.requestAnimationFrame = (cb) => {
            const id = Symbol();
            rafQueue.push({ id, cb });
            if (!rafScheduled) {
                rafScheduled = true;
                setImmediate(pumpRaf);
            }
            return id;
        };

        globalThis.cancelAnimationFrame = (id) => {
            rafQueue = rafQueue.filter(item => item.id !== id);
        };
        globalThis.__skikoRequestAnimationFrame = globalThis.requestAnimationFrame;
        globalThis.__skikoCancelAnimationFrame = globalThis.cancelAnimationFrame;
        if (!globalThis.addEventListener) {
            globalThis.addEventListener = () => {};
        }
        if (!globalThis.removeEventListener) {
            globalThis.removeEventListener = () => {};
        }
    }

    async runBenchmarks(benchmarkNames, options = {}) {
        const iterations = options.iterations || 1000;
        const warmup = options.warmup || 100;
        const rawArgs = options.rawArgs || [];

        const kotlinArgs = buildKotlinArgs(rawArgs, { iterations, warmup, benchmarkArg: options.benchmarkArg, benchmarkNames });
        const kotlinArgsStr = kotlinArgs.join(' ');
        let wasmRunSucceeded = false;

        const explicitReportsDir = process.env.BENCHMARK_REPORTS_DIR ||
            rawArgs.find(a => a.startsWith('reportsDir=') || a.startsWith('--reportsDir='))?.split('=')[1];

        if (explicitReportsDir) {
            const reportsDir = path.resolve(explicitReportsDir);
            const kotlinReportsDir = path.resolve(process.cwd(), 'build/benchmarks/json-reports');
            if (canonicalPath(reportsDir) !== canonicalPath(kotlinReportsDir)) {
                throw new Error(`[NodeSdlLauncher] reportsDir must match Kotlin's output directory (${kotlinReportsDir}); got ${reportsDir}`);
            }
        }

        const candidateOutputDirs = explicitReportsDir
            ? [path.resolve(explicitReportsDir)]
            : [
                path.resolve(process.cwd(), 'build/benchmarks/json-reports'),
                path.resolve(process.cwd(), '../../../../../benchmarks/build/benchmarks/json-reports'),
                path.resolve(process.cwd(), 'benchmarks/build/benchmarks/json-reports'),
                path.resolve(__dirname, 'build/benchmarks/json-reports'),
                path.resolve(__dirname, '../../../../../benchmarks/build/benchmarks/json-reports')
            ];

        // Clean stale reports before running to prevent accepting outdated results
        for (const dir of candidateOutputDirs) {
            if (fs.existsSync(dir)) {
                for (const name of benchmarkNames) {
                    const target = path.join(dir, `${name}.json`);
                    if (fs.existsSync(target)) {
                        fs.unlinkSync(target);
                    }
                }
            }
        }

        // Try executing compiled Wasm bundle if present
        try {
            const polyfillsPath = path.resolve(__dirname, './polyfills.mjs');
            if (fs.existsSync(polyfillsPath)) {
                await import(polyfillsPath);
            }

            const searchPaths = [
                path.resolve(__dirname, './compose-benchmarks-benchmarks.mjs'),
                path.resolve(process.cwd(), 'build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.mjs'),
                path.resolve(process.cwd(), 'benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.mjs'),
                path.resolve(process.cwd(), '../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.mjs'),
                path.resolve(__dirname, '../../../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.mjs')
            ];

            const modulePath = searchPaths.find(p => fs.existsSync(p));
            if (!modulePath) {
                throw new Error(`Could not find compiled Wasm module: compose-benchmarks-benchmarks.mjs in search paths: ${searchPaths.join(', ')}`);
            }

            const jodaDir = path.resolve(path.dirname(modulePath), 'node_modules/@js-joda/core');
            if (!fs.existsSync(jodaDir) || !fs.existsSync(path.join(jodaDir, 'package.json'))) {
                fs.mkdirSync(jodaDir, { recursive: true });
                fs.writeFileSync(path.join(jodaDir, 'package.json'), JSON.stringify({
                    name: '@js-joda/core',
                    version: '1.0.0',
                    type: 'module',
                    main: './index.js',
                    exports: {
                        '.': './index.js',
                        './*': './*.js'
                    }
                }, null, 2));
                fs.writeFileSync(path.join(jodaDir, 'index.js'), 'export default {};\n');
            }
            const resolvedJodaPath = createRequire(modulePath).resolve('@js-joda/core');
            if (!resolvedJodaPath.startsWith(path.resolve(path.dirname(modulePath), 'node_modules') + path.sep)) {
                throw new Error(`[NodeSdlLauncher] @js-joda/core resolved outside the benchmark package: ${resolvedJodaPath}`);
            }

            const skikoPath = path.resolve(path.dirname(modulePath), 'skiko.mjs');
            if (!fs.existsSync(skikoPath)) {
                throw new Error(`[NodeSdlLauncher] skiko.mjs not found next to the benchmark module (${skikoPath}); wasm-sdl needs the packaged Skiko runtime (skiko.mjs + skiko.wasm)`);
            }
            // The published (Emscripten) runtime is adapted at load time; a native-host runtime is used as is.
            const transformState = registerSkikoRuntimeTransform(skikoPath);
            this.emscriptenBridge = createEmscriptenNativeGLBridge({
                addon: this.nativeGL,
                wasmPath: path.resolve(path.dirname(modulePath), 'skiko.wasm'),
                translateShaders: process.env.SKIKO_NATIVE_GL_TRANSLATE_SHADERS !== '0',
                checkErrors: process.env.SKIKO_NATIVE_GL_CHECK_ERRORS === '1'
            });
            globalThis.__skikoEmscriptenModuleArg = this.emscriptenBridge.moduleArg;
            const skiko = await import(pathToFileURL(skikoPath).href);
            this.skikoModule = skiko;
            if (transformState.nativeHostRuntime) {
                if (typeof skiko.setSkikoNativeGLHost !== 'function') {
                    throw new Error(`[NodeSdlLauncher] Native-host skiko.mjs does not export setSkikoNativeGLHost (${skikoPath})`);
                }
                this.emscriptenBridge = null;
                const nativeHost = this.nativeGLHost || createSkikoNativeGLHost(this.nativeGL);
                this.nativeGLHost = nativeHost;
                globalThis.__skikoNativeGL = nativeHost;
                skiko.setSkikoNativeGLHost(nativeHost);
                if (!skiko.GL || typeof skiko.GL.createContext !== 'function' || typeof skiko.GL.makeContextCurrent !== 'function') {
                    throw new Error('[NodeSdlLauncher] Native-host Skiko runtime did not export GL.createContext/makeContextCurrent');
                }
                await skiko.awaitSkiko;
                globalThis.__skikoGL = skiko.GL;
                console.log('[NodeSdlLauncher] Native-host Skiko GL attached via setSkikoNativeGLHost');
            } else if (transformState.transformed) {
                await skiko.awaitSkiko;
                globalThis.__skikoGL = this.emscriptenBridge.installNativeContexts();
                console.log('[NodeSdlLauncher] Published Skiko runtime attached to native_skiko.node via instantiateWasm');
            } else {
                throw new Error(`[NodeSdlLauncher] skiko.mjs was not loaded through the native GL transform (${skikoPath})`);
            }

            const wasmModule = await import(modulePath);
            if (wasmModule.sdlBenchmarksRunner) {
                await wasmModule.sdlBenchmarksRunner(kotlinArgsStr);
                wasmRunSucceeded = true;
            } else {
                throw new Error(`Wasm module does not export sdlBenchmarksRunner`);
            }
            this.logGLUsage(console.log, 'Skiko GL import usage');
            if (this.emscriptenBridge) {
                for (const id of this.emscriptenBridge.contextIds()) {
                    console.log(`[NodeSdlLauncher] Native GL context ${id} readback:`, JSON.stringify(this.emscriptenBridge.readbackSummary(id)));
                }
            }
        } catch (e) {
            this.logGLUsage(console.error, 'Skiko GL import usage at failure');
            console.error(`[NodeSdlLauncher] Wasm execution error:`, formatWasmError(e));
            throw e;
        }

        const mainReportsDir = explicitReportsDir
            ? path.resolve(explicitReportsDir)
            : (candidateOutputDirs.find(d => fs.existsSync(path.dirname(d)) && d.includes('benchmarks/build/benchmarks/json-reports')) || candidateOutputDirs[1]);

        const reports = [];
        const missing = [];
        if (wasmRunSucceeded) {
            for (const benchmarkName of benchmarkNames) {
                let reportJson = null;
                for (const dir of candidateOutputDirs) {
                    const reportPath = path.join(dir, `${benchmarkName}.json`);
                    if (fs.existsSync(reportPath)) {
                        const content = fs.readFileSync(reportPath, 'utf-8');
                        try {
                            reportJson = JSON.parse(content);
                        } catch (parseErr) {
                            throw new Error(`Failed to parse benchmark report JSON for ${benchmarkName} at ${reportPath}: ${parseErr.message}`);
                        }
                        if (!fs.existsSync(mainReportsDir)) fs.mkdirSync(mainReportsDir, { recursive: true });
                        const dstPath = path.join(mainReportsDir, `${benchmarkName}.json`);
                        if (reportPath !== dstPath) {
                            fs.copyFileSync(reportPath, dstPath);
                        }
                        break;
                    }
                }
                if (reportJson) {
                    reports.push(this.formatReportFromStats(benchmarkName, reportJson));
                } else {
                    missing.push(benchmarkName);
                }
            }
        }

        if (missing.length > 0) {
            throw new Error(`Benchmark execution failed to produce reports for: ${missing.join(', ')}`);
        }
        if (reports.length === 0) {
            throw new Error(`No benchmark reports generated for: ${benchmarkNames.join(', ')}`);
        }

        return reports;
    }

    logGLUsage(print, title) {
        try {
            if (this.emscriptenBridge) {
                print(`[NodeSdlLauncher] ${title}:`, JSON.stringify(this.emscriptenBridge.report()));
            } else if (this.skikoModule && typeof this.skikoModule.getSkikoGLImportUsage === 'function') {
                print(`[NodeSdlLauncher] ${title}:`, JSON.stringify(this.skikoModule.getSkikoGLImportUsage()));
            }
        } catch {}
    }

    parseDurationMs(val) {
        if (!val || val === 'N/A') return null;
        if (typeof val === 'number') return val;
        if (typeof val === 'string' && val.startsWith('PT')) {
            const seconds = parseFloat(val.replace('PT', '').replace('S', ''));
            if (!isNaN(seconds)) {
                return seconds * 1000;
            }
        }
        const num = parseFloat(val);
        return isNaN(num) ? null : num;
    }

    formatReportFromStats(name, stats) {
        const frameInfo = stats.averageFrameInfo || {};
        const fpsInfo = stats.averageFPSInfo || {};
        const conditions = stats.conditions || {};
        const cpuMs = this.parseDurationMs(frameInfo.cpuTime);
        const gpuMs = this.parseDurationMs(frameInfo.gpuTime);
        const totalMs = (cpuMs !== null && gpuMs !== null) ? (cpuMs + gpuMs) : (this.parseDurationMs(frameInfo.totalTime) || null);
        return {
            benchmark: name,
            launcher: this.name,
            iterations: conditions.frameCount ?? 1000,
            warmup: conditions.warmupCount ?? 100,
            effectiveFps: fpsInfo.fps ? Number(fpsInfo.fps.toFixed(1)) : (totalMs ? Number((1000 / totalMs).toFixed(1)) : 0),
            cpuAvgMs: cpuMs !== null ? `${cpuMs.toFixed(3)} ms` : 'N/A',
            gpuAvgMs: gpuMs !== null ? `${gpuMs.toFixed(3)} ms` : 'N/A',
            totalAvgMs: totalMs !== null ? `${totalMs.toFixed(3)} ms` : 'N/A'
        };
    }

    cleanupSync() {
        if (this.isCleanedUp) return;
        this.isCleanedUp = true;
        if (this._exitHandler) {
            process.removeListener('exit', this._exitHandler);
            this._exitHandler = null;
        }
        if (this.canvases && this.nativeGL && typeof this.nativeGL.destroyContext === 'function') {
            for (const canvas of this.canvases.values()) {
                if (canvas && canvas.__skikoNativeContext) {
                    try {
                        this.nativeGL.destroyContext(canvas.__skikoNativeContext);
                    } catch (e) {
                        console.warn('Native context teardown error:', e);
                    }
                    canvas.__skikoNativeContext = 0;
                }
            }
        }
        if (this.nativeGLContextId && this.nativeGL && typeof this.nativeGL.destroyContext === 'function') {
            try {
                this.nativeGL.destroyContext(this.nativeGLContextId);
            } catch (e) {
                console.warn('Native launcher context teardown error:', e);
            }
            this.nativeGLContextId = null;
        }
        if (this.nativeGL && typeof this.nativeGL.destroyWindow === 'function') {
            try {
                this.nativeGL.destroyWindow();
            } catch (e) {
                console.warn('Native window teardown error:', e);
            }
        }
    }

    async cleanup() {
        // Native GL context and window destruction are deferred to the process 'exit' handler.
        // This keeps the native context alive during event loop wind-down so that any
        // FinalizationRegistry callbacks on Skia Managed objects can complete without encountering a destroyed GL context.
    }
}

/**
 * Wasmtime Standalone CLI Launcher
 */
export class WasmtimeBenchmarkLauncher extends BenchmarkLauncher {
    constructor() {
        super('wasmtime');
        this.runnerPath = null;
    }

    async initialize(options = {}) {
        console.log(`[WasmtimeLauncher] Initializing standalone Wasmtime native execution harness...`);
        const searchPaths = [
            process.env.WASMTIME_SDL_RUNNER,
            path.resolve(process.cwd(), 'wasmtime-sdl-runner'),
            path.resolve(process.cwd(), 'build/wasmtime-sdl-runner/wasmtime-sdl-runner'),
            path.resolve(process.cwd(), 'benchmarks/build/wasmtime-sdl-runner/wasmtime-sdl-runner'),
            path.resolve(process.cwd(), '../../benchmarks/build/wasmtime-sdl-runner/wasmtime-sdl-runner'),
            path.resolve(__dirname, '../../../../build/wasmtime-sdl-runner/wasmtime-sdl-runner'),
            path.resolve(__dirname, '../../../../benchmarks/build/wasmtime-sdl-runner/wasmtime-sdl-runner')
        ].filter(Boolean);

        this.runnerPath = searchPaths.find(p => fs.existsSync(p));
        if (!this.runnerPath) {
            console.warn(`[WasmtimeLauncher] wasmtime-sdl-runner executable not found in search paths: ${searchPaths.join(', ')}`);
        } else {
            console.log(`[WasmtimeLauncher] Found wasmtime-sdl-runner at: ${this.runnerPath}`);
        }
    }

    async runBenchmarks(benchmarkNames, options = {}) {
        const { iterations = 1000, warmup = 100, rawArgs = [] } = options;
        const explicitReportsDir = process.env.BENCHMARK_REPORTS_DIR ||
            rawArgs.find(a => a.startsWith('reportsDir=') || a.startsWith('--reportsDir='))?.split('=')[1];

        const candidateOutputDirs = explicitReportsDir
            ? [path.resolve(explicitReportsDir)]
            : [
                path.resolve(process.cwd(), 'build/benchmarks/json-reports'),
                path.resolve(process.cwd(), '../../../../../benchmarks/build/benchmarks/json-reports'),
                path.resolve(process.cwd(), 'benchmarks/build/benchmarks/json-reports'),
                path.resolve(__dirname, 'build/benchmarks/json-reports'),
                path.resolve(__dirname, '../../../../../benchmarks/build/benchmarks/json-reports')
            ];

        // Clean stale reports before running to prevent accepting outdated results
        for (const dir of candidateOutputDirs) {
            if (fs.existsSync(dir)) {
                for (const name of benchmarkNames) {
                    const target = path.join(dir, `${name}.json`);
                    if (fs.existsSync(target)) {
                        fs.unlinkSync(target);
                    }
                }
            }
        }

        const wasmSearchPaths = [
            path.resolve(__dirname, './compose-benchmarks-benchmarks.wasm'),
            path.resolve(process.cwd(), 'build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm'),
            path.resolve(process.cwd(), 'benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm'),
            path.resolve(process.cwd(), '../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm'),
            path.resolve(__dirname, '../../../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/compose-benchmarks-benchmarks.wasm')
        ];
        const wasmPath = wasmSearchPaths.find(p => fs.existsSync(p));

        const skikoSearchPaths = [
            process.env.SKIKO_WASM,
            path.resolve(__dirname, './skiko.wasm'),
            path.resolve(process.cwd(), 'skiko.wasm'),
            path.resolve(process.cwd(), 'build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm'),
            path.resolve(process.cwd(), 'benchmarks/build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm'),
            path.resolve(process.cwd(), '../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm'),
            path.resolve(__dirname, '../../../../build/wasm/packages/compose-benchmarks-benchmarks/kotlin/skiko.wasm'),
            '/Users/dustin.feucht/projects/skiko/samples/SkiaNonWebSample/build/compose/skiko-wasmJs-runtime/skiko.wasm',
            '/Users/dustin.feucht/projects/skiko/samples/SkiaNonWebSample/build/processedResources/wasmJs/main/skiko.wasm'
        ].filter(Boolean);
        const skikoPath = skikoSearchPaths.find(p => fs.existsSync(p));

        let wasmRunSucceeded = false;
        const generatedFiles = [];

        if (this.runnerPath && wasmPath && skikoPath) {
            console.log(`[WasmtimeLauncher] Spawning wasmtime-sdl-runner...`);
            const kotlinArgs = buildKotlinArgs(rawArgs, { iterations, warmup, benchmarkArg: options.benchmarkArg, benchmarkNames });
            const runnerArgs = ['--wasm', wasmPath, '--skiko', skikoPath, ...kotlinArgs];

            await new Promise((resolve, reject) => {
                const child = spawn(this.runnerPath, runnerArgs, {
                    env: {
                        ...process.env,
                        BENCHMARK_MODE: 'true',
                        BENCHMARK_ITERATIONS: String(iterations),
                        BENCHMARK_WARMUP: String(warmup)
                    }
                });

                let stdoutBuffer = '';
                let inJson = false;
                let jsonBuffer = '';

                child.stdout.on('data', (chunk) => {
                    const text = chunk.toString();
                    process.stdout.write(text);
                    stdoutBuffer += text;

                    const lines = stdoutBuffer.split('\n');
                    stdoutBuffer = lines.pop() || '';

                    for (const line of lines) {
                        if (line.includes('JSON_START')) {
                            inJson = true;
                            jsonBuffer = '';
                        } else if (line.includes('JSON_END')) {
                            inJson = false;
                            try {
                                const parsed = JSON.parse(jsonBuffer);
                                if (parsed && parsed.name) {
                                    for (const d of candidateOutputDirs) {
                                        if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
                                        const filePath = path.join(d, `${parsed.name}.json`);
                                        fs.writeFileSync(filePath, JSON.stringify(parsed, null, 2));
                                        generatedFiles.push(filePath);
                                    }
                                }
                            } catch (e) {}
                        } else if (inJson) {
                            jsonBuffer += line + '\n';
                        }
                    }
                });

                child.stderr.on('data', (chunk) => {
                    process.stderr.write(chunk.toString());
                });

                child.on('error', (err) => {
                    console.warn(`[WasmtimeLauncher] Process error: ${err.message}`);
                    for (const f of generatedFiles) {
                        if (fs.existsSync(f)) try { fs.unlinkSync(f); } catch {}
                    }
                    reject(err);
                });

                child.on('close', (code, signal) => {
                    if (stdoutBuffer) {
                        const lines = [stdoutBuffer];
                        for (const line of lines) {
                            if (line.includes('JSON_START')) {
                                inJson = true;
                                jsonBuffer = '';
                            } else if (line.includes('JSON_END')) {
                                inJson = false;
                                try {
                                    const parsed = JSON.parse(jsonBuffer);
                                    if (parsed && parsed.name) {
                                        for (const d of candidateOutputDirs) {
                                            if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
                                            const filePath = path.join(d, `${parsed.name}.json`);
                                            fs.writeFileSync(filePath, JSON.stringify(parsed, null, 2));
                                            generatedFiles.push(filePath);
                                        }
                                    }
                                } catch (e) {}
                            } else if (inJson) {
                                jsonBuffer += line + '\n';
                            }
                        }
                    }

                    if (signal) {
                        for (const f of generatedFiles) {
                            if (fs.existsSync(f)) try { fs.unlinkSync(f); } catch {}
                        }
                        reject(new Error(`[WasmtimeLauncher] wasmtime-sdl-runner was terminated by signal ${signal}`));
                    } else if (code !== 0 && code !== null) {
                        for (const f of generatedFiles) {
                            if (fs.existsSync(f)) try { fs.unlinkSync(f); } catch {}
                        }
                        reject(new Error(`[WasmtimeLauncher] wasmtime-sdl-runner exited with error code ${code}`));
                    } else {
                        wasmRunSucceeded = true;
                        resolve();
                    }
                });
            });
        } else {
            throw new Error(`[WasmtimeLauncher] Missing required components (runner: ${this.runnerPath}, wasm: ${wasmPath}, skiko: ${skikoPath})`);
        }

        const mainReportsDir = explicitReportsDir
            ? path.resolve(explicitReportsDir)
            : (candidateOutputDirs.find(d => fs.existsSync(path.dirname(d)) && d.includes('benchmarks/build/benchmarks/json-reports')) || candidateOutputDirs[0]);

        const reports = [];
        const missing = [];
        if (wasmRunSucceeded) {
            for (const benchmarkName of benchmarkNames) {
                let reportJson = null;
                for (const dir of candidateOutputDirs) {
                    const reportPath = path.join(dir, `${benchmarkName}.json`);
                    if (fs.existsSync(reportPath)) {
                        try {
                            reportJson = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
                        } catch (parseErr) {
                            throw new Error(`Failed to parse benchmark report JSON for ${benchmarkName} at ${reportPath}: ${parseErr.message}`);
                        }
                        if (!fs.existsSync(mainReportsDir)) fs.mkdirSync(mainReportsDir, { recursive: true });
                        const dstPath = path.join(mainReportsDir, `${benchmarkName}.json`);
                        if (reportPath !== dstPath) {
                            fs.copyFileSync(reportPath, dstPath);
                        }
                        break;
                    }
                }
                if (reportJson) {
                    reports.push(this.formatReportFromStats(benchmarkName, reportJson));
                } else {
                    missing.push(benchmarkName);
                }
            }
        }

        if (missing.length > 0) {
            throw new Error(`Benchmark execution failed to produce reports for: ${missing.join(', ')}`);
        }
        if (reports.length === 0) {
            throw new Error(`No benchmark reports generated for: ${benchmarkNames.join(', ')}`);
        }

        return reports;
    }

    async cleanup() {
    }
}

// ==========================================
// Launcher Registry & CLI Orchestrator
// ==========================================

const LAUNCHERS = {
    'node': NodeSdlBenchmarkLauncher,
    'sdl': NodeSdlBenchmarkLauncher,
    'wasmtime': WasmtimeBenchmarkLauncher
};

export async function runSdlBenchmarkCli(argv = process.argv.slice(2)) {
    const args = parseArgs(argv);
    const launcherName = args.launcher || 'node';
    const benchmarkArg = args.benchmark || args.benchmarks || null;
    const iterations = parseInt(args.iterations || args.frameCount || '1000', 10);
    const warmup = parseInt(args.warmup || args.warmupCount || '100', 10);

    const benchmarksToRun = benchmarkArg
        ? benchmarkArg.split(',').map(s => s.trim()).filter(Boolean)
        : ALL_AVAILABLE_BENCHMARKS;

    const LauncherClass = LAUNCHERS[launcherName.toLowerCase()] || NodeSdlBenchmarkLauncher;
    const launcher = new LauncherClass();

    try {
        await launcher.initialize({
            nativeSkikoPath: args.nativeSkikoPath,
            width: parseInt(args.width || '1024', 10),
            height: parseInt(args.height || '768', 10)
        });

        const reports = await launcher.runBenchmarks(benchmarksToRun, {
            iterations,
            warmup,
            benchmarkArg,
            rawArgs: argv
        });

        console.log('\n==========================================');
        console.log(`Compose Wasm in Native Window Benchmark Results`);
        console.log('==========================================');
        console.table(reports);

        console.log();
        return reports;
    } finally {
        await launcher.cleanup();
    }
}

function parseArgs(args) {
    const tokens = [];
    for (const arg of args) {
        if (typeof arg === 'string') {
            tokens.push(...arg.split(/\s+/).filter(Boolean));
        }
    }
    const result = {};
    for (const arg of tokens) {
        if (arg.startsWith('--')) {
            const [k, ...v] = arg.slice(2).split('=');
            result[k] = v.length > 0 ? v.join('=') : 'true';
        } else if (arg.includes('=')) {
            const [k, ...v] = arg.split('=');
            result[k] = v.join('=');
        }
    }
    return result;
}

if (process.argv[1] && process.argv[1].endsWith('launcher_sdl.mjs')) {
    runSdlBenchmarkCli().catch(err => {
        console.error('Benchmark execution error:', err);
        process.exit(1);
    });
}
