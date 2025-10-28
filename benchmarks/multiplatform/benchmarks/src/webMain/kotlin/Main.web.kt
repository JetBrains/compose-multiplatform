import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams
import kotlin.js.JsBoolean
import kotlin.js.js
import kotlin.js.toBoolean
import kotlin.js.toJsString

fun main(args: Array<String>) {
    if (isD8env().toBoolean()) {
        mainD8(args)
    } else {
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        androidx.compose.ui.window.onSkikoReady {
            mainBrowser()
        }
    }
}

fun mainBrowser() {
    val urlParams = URLSearchParams(window.location.search.toJsString())
    var i = 0
    val args = generateSequence { urlParams.get("arg${i++}") }.toList().toTypedArray()

    Config.setGlobalFromArgs(args)

    MainScope().launch {
        if (Config.saveStats() && !BenchmarksSaveServerClient.isServerAlive()) {
            println("No benchmark server found.")
            return@launch
        }
        runBenchmarks()
        println("Completed!")
        if (Config.saveStats()) {
            GlobalScope.launch {
                BenchmarksSaveServerClient.stopServer()
            }
        }
    }
}


// Currently, the initialization can't be adjusted to avoid calling the fun main, but
// we don't want use the default fun main, because Jetstream3 requires running the workloads separately / independently of each other.
// Also, they require that a benchmark completes before the function exists, which is not possible with if they just call fun main.
// Therefore, they'll rely on fun customLaunch, which returns a Promise (can be awaited for).
fun mainD8(args: Array<String>) {
    println("mainD8 is intentionally doing nothing. Read the comments in main.wasmJs.kt")
}

private fun isD8env(): JsBoolean =
    js("typeof isD8 !== 'undefined'")