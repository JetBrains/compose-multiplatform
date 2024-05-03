import { instantiate } from './jetsnackwasmapp.uninstantiated.mjs';

await wasmSetup;

instantiate({ skia: Module['asm'] });
