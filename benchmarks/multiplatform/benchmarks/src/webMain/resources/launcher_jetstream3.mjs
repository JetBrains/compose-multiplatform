globalThis.isD8 = true;

import * as skiko from './skikod8.mjs';
import { instantiate } from './compose-benchmarks-benchmarks-wasm-js.uninstantiated.mjs';

const exports = (await instantiate({
    './skiko.mjs': skiko
})).exports;

await import('./polyfills.mjs');

/*
    AnimatedVisibility,
    LazyGrid,
    LazyGrid-ItemLaunchedEffect,
    LazyGrid-SmoothScroll,
    LazyGrid-SmoothScroll-ItemLaunchedEffect,
    VisualEffects,
    MultipleComponents-NoVectorGraphics
 */
let name = arguments[0] ? arguments[0] : 'AnimatedVisibility';
let frameCount = arguments[1] ? parseInt(arguments[1]) : 1000;
await exports.customLaunch(name, frameCount);
console.log('Finished');
