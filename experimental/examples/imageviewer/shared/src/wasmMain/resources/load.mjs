import { instantiate } from './imageviewer.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });