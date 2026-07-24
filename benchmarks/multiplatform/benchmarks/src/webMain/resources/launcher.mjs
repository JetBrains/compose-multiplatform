globalThis.isD8 = true;

import * as skiko from './skikod8.mjs';
import { instantiate } from './compose-benchmarks-benchmarks.uninstantiated.mjs';

const exports = (await instantiate({
    './skiko.mjs': skiko
})).exports;

await import('./polyfills.mjs');

await exports.d8BenchmarksRunner(Array.from(arguments).join(' '));
console.log('Finished');
