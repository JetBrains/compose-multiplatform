import { instantiate } from './compose-benchmarks-wasm-js.uninstantiated.mjs';

await wasmSetup;
await instantiate({ skia: Module['asm'] });