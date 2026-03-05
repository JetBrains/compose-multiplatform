import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.url.URLSearchParams
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString


@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun mainBrowser() = MainScope().launch {
    awaitSkikoWasm()

    val urlParams = URLSearchParams(window.location.search.toJsString())
    var i = 0
    val args = generateSequence { urlParams.get("arg${i++}") }.toList().toTypedArray()

    Config.setGlobalFromArgs(args)

    val composeRoot = document.getElementById("root") ?: error("No root element found")
    if (Config.isModeEnabled(Mode.REAL)) {
        val frameRate = 120 // can we get this from device?
        ComposeViewport("root") {
            BenchmarkRunner(getBenchmarks(), frameRate, onExit = { composeRoot.remove() })
        }
    } else {
        composeRoot.remove()
        println("Wait for the benchmarks to complete...\n")
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
}

private suspend fun awaitSkikoWasm() {
    suspendCancellableCoroutine { c ->
        @Suppress("INVISIBLE_REFERENCE")
        androidx.compose.ui.window.onSkikoReady {
            c.resumeWith(Result.success(Unit))
        }
    }
}
