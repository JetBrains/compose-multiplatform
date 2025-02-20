actual fun runGC() {
    js("(typeof gc === 'function') ? gc() : ((isD8 != true) ? console.log('Manual GC is not available. Ensure that the browser was started with the appropriate flags.') : 0)")
}