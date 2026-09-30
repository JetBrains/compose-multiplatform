import './polyfills.mjs';
import { customLaunch } from './compose-benchmarks-benchmarks.mjs';

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
await customLaunch(name, frameCount);
console.log('Finished');
