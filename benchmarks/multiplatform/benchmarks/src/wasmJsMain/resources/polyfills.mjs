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
        return this._uint8.buffer;
    }
}

globalThis.asyncReadBuffer = async (s) => {
    return readbuffer(s);
}

globalThis.fetch = async (p) => {
    let data;
    try {
        // data = await asyncReadBuffer("drawable/img.png");
        data = readbuffer("drawable/img.png");
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