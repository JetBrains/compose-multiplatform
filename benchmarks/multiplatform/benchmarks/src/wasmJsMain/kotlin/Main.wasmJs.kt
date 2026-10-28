import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.Promise

val jsOne = 1.toJsNumber()

fun main(args: Array<String>) {
    if (isSdlEnv().toBoolean()) {
        mainSdl(args)
    } else if (isD8env().toBoolean()) {
        mainD8(args)
    } else {
        mainBrowser()
    }
}

fun mainSdl(args: Array<String>) {
    println("mainSdl is intentionally doing nothing. Read comments in main.wasmJs.kt")
}

// HeavyShader and CanvasDrawing perform heavy shader blending and thousands of complex vector paths.
// In headless Wasm runs without hardware GPU acceleration, Skia software rasterization (Surface.makeRasterN32Premul)
// causes each frame to take hundreds to thousands of milliseconds, stalling default full suite runs for hours.
// They can still be executed on demand by explicitly passing e.g. benchmarks=HeavyShader or benchmarks=CanvasDrawing.
private val defaultDisabledBenchmarksForSdl = setOf(
    "HeavyShader",
    "CanvasDrawing"
)

@JsExport
fun sdlBenchmarksRunner(args: String): Promise<JsAny?> {
    val argArray = if (args.isBlank()) emptyArray() else args.split(" ").filter { it.isNotBlank() }.toTypedArray()
    var config = Args.parseArgs(argArray)
    if (argArray.none { it.startsWith("modes=", ignoreCase = true) }) {
        config = config.copy(modes = setOf(Mode.SIMPLE))
    }
    if (argArray.none { it.startsWith("disabledBenchmarks=", ignoreCase = true) } && config.benchmarks.isEmpty()) {
        config = config.copy(disabledBenchmarks = config.disabledBenchmarks + defaultDisabledBenchmarksForSdl.map { it.uppercase() })
    }

    Config.setGlobal(config)

    return MainScope().promise {
        awaitSkikoWasm()
        runBenchmarks()
        jsOne
    }
}

private fun isSdlEnv(): JsBoolean =
    js("typeof globalThis.__skikoNativeGL !== 'undefined' || typeof globalThis.isSdl !== 'undefined' || (typeof process !== 'undefined' && Boolean(process.release) && process.release.name === 'node')")

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