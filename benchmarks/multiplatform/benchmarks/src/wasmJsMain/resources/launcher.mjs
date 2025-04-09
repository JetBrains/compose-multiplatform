globalThis.isD8 = true;

import * as Li9za2lrby5tanM from './skikod8.mjs';
import { instantiate } from './compose-benchmarks-benchmarks-wasm-js.uninstantiated.mjs';

const exports = (await instantiate({
    './skiko.mjs': Li9za2lrby5tanM
})).exports;

await import('./polyfills.mjs');

await exports.d8BenchmarksRunner(Array.from(arguments).join(' '));
console.log('Finished');
