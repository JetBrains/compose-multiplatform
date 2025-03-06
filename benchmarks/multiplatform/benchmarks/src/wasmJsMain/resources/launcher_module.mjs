globalThis.skipFunMain = true;
globalThis.isWasmBuildForJetstream3 = false;
globalThis.isD8 = true;

import * as Li9za2lrby5tanM from './skikod8.mjs';
import { instantiate } from './compose-benchmarks-benchmarks-wasm-js.uninstantiated.mjs';

const exports = (await instantiate({
    './skiko.mjs': Li9za2lrby5tanM
})).exports;

await import('./polyfills.mjs');

/*
    AnimatedVisibility,
    LazyGrid,
    LazyGrid-ItemLaunchedEffect,
    LazyGrid-SmoothScroll,
    LazyGrid-SmoothScroll-ItemLaunchedEffect,
    VisualEffects
 */
let name = arguments[0] ? arguments[0] : 'AnimatedVisibility';
let frameCount = arguments[1] ? parseInt(arguments[1]) : 1000;
exports.customLaunch(name, frameCount);
console.log('Finished');