import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.Promise

val jsOne = 1.toJsNumber()

private fun isNodeJsJs(): JsBoolean =
    js("Boolean(typeof process !== 'undefined' && Boolean(process.release) && process.release.name === 'node')")

private fun isWasmtimeJs(): JsBoolean =
    js("Boolean(typeof process === 'undefined' && (Boolean(globalThis.__isBenchmarkMode) || Boolean(globalThis.isSdl) || Boolean(globalThis.__skikoNativeGL)))")

private fun isNodeJs(): Boolean = isNodeJsJs().toBoolean()
private fun isWasmtime(): Boolean = isWasmtimeJs().toBoolean()

fun main() {
    if (isWasmtime()) {
        mainSdl(getSdlArgs())
    } else if (isD8env().toBoolean()) {
        mainD8(emptyArray())
    } else if (!isNodeJs()) {
        mainBrowser()
    }
}

fun main(args: Array<String>) {
    if (isWasmtime()) {
        mainSdl(if (args.isNotEmpty()) args else getSdlArgs())
    }
}

private external fun getBenchmarkArgs(): String

private fun getSdlArgs(): Array<String> {
    val argsStr = try { getBenchmarkArgs() } catch (t: Throwable) { "" }
    if (argsStr.isBlank()) return emptyArray()
    return argsStr.split(" ").filter { it.isNotBlank() }.toTypedArray()
}

fun mainSdl(args: Array<String>) {
    val cleanArgs = args.filter { !it.startsWith("--wasm") && !it.startsWith("--skiko") && !it.endsWith(".wasm") }.toTypedArray()
    var config = Args.parseArgs(cleanArgs)
    if (cleanArgs.none { it.startsWith("modes=", ignoreCase = true) }) {
        config = config.copy(modes = setOf(Mode.SIMPLE))
    }
    config = config.copy(saveStatsToJSON = true)
    Config.setGlobal(config)

    MainScope().launch {
        try {
            awaitSkikoWasm()
            runBenchmarks(graphicsContext = wasmGraphicsContext())
            exitSdlHost()
        } catch (t: Throwable) {
            println("Benchmark exception during execution: ${t.message}")
            println(t.stackTraceToString())
            exitSdlHost()
        }
    }
}

private fun exitSdlHost(): Unit =
    js("(function() { if (typeof process !== 'undefined' && typeof process.exit === 'function') { process.exit(0); } else if (typeof window !== 'undefined' && typeof window.close === 'function') { window.close(); } })()")

@JsExport
fun sdlBenchmarksRunner(args: String): Promise<JsAny?> {
    val argArray = if (args.isBlank()) emptyArray() else args.split(" ").filter { it.isNotBlank() }.toTypedArray()
    var config = Args.parseArgs(argArray)
    if (argArray.none { it.startsWith("modes=", ignoreCase = true) }) {
        config = config.copy(modes = setOf(Mode.SIMPLE))
    }
    config = config.copy(saveStatsToJSON = true)
    Config.setGlobal(config)

    return MainScope().promise {
        try {
            awaitSkikoWasm()
            runBenchmarks(graphicsContext = wasmGraphicsContext())
            jsOne
        } catch (t: Throwable) {
            println("Benchmark exception during execution: ${t.message}")
            println(t.stackTraceToString())
            throw t
        }
    }
}

internal fun isSdlEnv(): JsBoolean =
    js("Boolean(globalThis.__skikoNativeGL || globalThis.isSdl || (typeof process !== 'undefined' && Boolean(process.release) && process.release.name === 'node'))")

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
    js("Boolean(typeof isD8 !== 'undefined' && isD8)")