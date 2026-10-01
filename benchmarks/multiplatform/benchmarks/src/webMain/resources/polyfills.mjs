// polyfills.mjs
globalThis.isD8 = true;

if (typeof globalThis.window === 'undefined') {
    globalThis.window = globalThis;
}
if (typeof globalThis.navigator === 'undefined') {
    globalThis.navigator = {};
}
if (!globalThis.navigator.languages) {
    globalThis.navigator.languages = ['en-US', 'en'];
    globalThis.navigator.userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';
    globalThis.navigator.platform = "MacIntel";
}

// Compose reads `window.isSecureContext` in its Clipboard feature:
globalThis.isSecureContext = false;

if (!globalThis.gc) {
    // No GC control in D8
    globalThis.gc = () => {
        // console.log('gc called');
    };
}

if (typeof globalThis.AbortController === 'undefined') {
    globalThis.AbortController = class {
        constructor() {
            this.signal = {};
        }
        abort() {}
    };
}

// Minimal Blob polyfill
class BlobPolyfill {
    constructor(uint8, type = '') {
        this._uint8 = uint8;
        this._type = type;
    }
    get size() {
        return this._uint8.byteLength;
    }
    get type() { return this._type; }
    async arrayBuffer() {
        console.log('arrayBuffer called');
        return this._uint8.buffer;
    }
    slice(start = 0, end = this.size, type = this.type) {
        return new BlobPolyfill(this._uint8.slice(start, end), type);
    }
}

const runtimeDirectory = String(import.meta.url)
    .replace(/^file:\/\//, '')
    .replace(/\/[^/]+$/, '');

globalThis.fetch = async (request) => {
    const resourcePath = String(request)
        .replace(/^file:\/\//, '')
        .replace(/^\.\//, '');
    const filePath = resourcePath.startsWith('/')
        ? resourcePath
        : `${runtimeDirectory}/${resourcePath}`;
    console.log('fetch', resourcePath);
    let data;
    try {
        data = read(filePath, 'binary');
    } catch (error) {
        console.error(`Failed to read D8 resource ${filePath}:`, error);
        throw error;
    }

    const uint8 = new Uint8Array(data);

    return {
        ok: true,
        status: 200,
        async blob() {
            return new BlobPolyfill(uint8, 'application/xml');
        },
    };
};
