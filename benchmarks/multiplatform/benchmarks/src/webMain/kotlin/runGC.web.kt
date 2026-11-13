private fun performJsGc(): Unit =
    js("(function() { if (typeof gc === 'function') { gc(); } else if (typeof globalThis.gc === 'function') { globalThis.gc(); } else { console.log('Manual GC is not available. Ensure that the browser was started with the appropriate flags.'); } })()")

actual fun runGC() {
    performJsGc()
}