@file:OptIn(ExperimentalJsExport::class)

import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Promise

val jsOne = 1.toJsNumber()

fun main(args: Array<String>) {
    if (isD8env().toBoolean()) {
        mainD8(args)
    } else {
        mainBrowser()
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

private val basicConfigForD8 = Config(
    // Using only SIMPLE mode, because VSYNC_EMULATION calls delay(...),
    // which is implemented via setTimeout on web targets.
    // setTimeout implementation is simplified in D8, making
    // the VSYNC_EMULATION mode meaningless when running in D8
    modes = setOf(Mode.SIMPLE),
    // MultipleComponents is unsupported, because it uses vector icons. D8 doesn't provide XML parsing APIs.
    // But there is an alternative workload called 'MultipleComponents-NoVectorGraphics'
    disabledBenchmarks = setOf("MultipleComponents"),
)

@JsExport
fun customLaunch(benchmarkName: String, frameCount: Int): Promise<JsAny?> {
    val config = basicConfigForD8.copy(
        benchmarks = mapOf(benchmarkName.uppercase() to frameCount)
    )
    Config.setGlobal(config)

    return MainScope().promise {
        runBenchmarks(warmupCount = 0)
        jsOne
    }
}

@JsExport
fun d8BenchmarksRunner(args: String): Promise<JsAny?> {
    val config = Args.parseArgs(args.split(" ").toTypedArray())
        .copy(
            modes = basicConfigForD8.modes,
            disabledBenchmarks = basicConfigForD8.disabledBenchmarks
        )

    Config.setGlobal(config)

    return MainScope().promise {
        runBenchmarks()
        jsOne
    }
}

private fun isD8env(): JsBoolean =
    js("typeof isD8 !== 'undefined'")
