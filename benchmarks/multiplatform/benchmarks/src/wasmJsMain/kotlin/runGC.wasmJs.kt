actual fun runGC() {
    js("(typeof gc === 'function')? gc() : console.log('Manual GC is not available. Ensure that the browser was started with the appropriate flags.')")
}