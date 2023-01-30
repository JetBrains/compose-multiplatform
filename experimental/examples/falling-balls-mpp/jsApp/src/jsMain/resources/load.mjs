import { instantiate } from './jsApp-wasm.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });
