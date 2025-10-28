@file:OptIn(ExperimentalJsExport::class)

import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Promise

val jsOne = 1.toJsNumber()

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
