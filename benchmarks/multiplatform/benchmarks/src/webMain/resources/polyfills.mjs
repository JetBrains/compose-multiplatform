// polyfills.mjs
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
}

globalThis.fetch = async (p) => {
    let data;
    try {
        let path = p.replace(/^\.\//, '');
        console.log('fetch', path);
        data = read(path, 'binary');
    } catch (err) {
        console.log('error', err);
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