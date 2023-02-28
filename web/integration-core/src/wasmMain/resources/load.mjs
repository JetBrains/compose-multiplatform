import { instantiate } from './myapp.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });
