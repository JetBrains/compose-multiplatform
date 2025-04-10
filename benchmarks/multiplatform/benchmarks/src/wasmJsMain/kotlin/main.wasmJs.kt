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
    eventLoop = object : EventLoop {
        override suspend fun runMicrotasks() {
            yield()
        }
    }

    val urlParams = URLSearchParams(window.location.search.toJsString())
    var i = 0
    val args = generateSequence { urlParams.get("arg${i++}") }.toList().toTypedArray()
    Args.parseArgs(args)

    Args.enableModes(Mode.SIMPLE)

    MainScope().launch {
        runBenchmarks()
        println("Completed!")
    }
}


// Currently, the initialization can't be adjusted to avoid calling the fun main, but
// we don't want use the default fun main, because Jetstream3 requires running the workloads separately / independently of each other.
// Also, they require that a benchmark completes before the function exists, which is not possible with if they just call fun main.
// Therefore, they'll rely on fun customLaunch, which returns a Promise (can be awaited for).
fun mainD8(args: Array<String>) {
    println("mainD8 is intentionally doing nothing. Read the comments in main.wasmJs.kt")

    // Example1 must use xml resources, but D8 doesn't have Xml parsing API,
    // consider checking it in a browser
    Args.skipBenchmark("Example1")
}

@JsExport
fun customLaunch(benchmarkName: String, frameCount: Int): Promise<JsAny?> {
    val args = "benchmarks=$benchmarkName($frameCount)"
    Args.parseArgs(arrayOf(args))
    Args.enableModes(Mode.SIMPLE)

    eventLoop = object : EventLoop {
        override suspend fun runMicrotasks() {
            yield()
        }
    }

    return MainScope().promise {
        runBenchmarks(warmupCount = 0)
        jsOne
    }
}

@JsExport
fun d8BenchmarksRunner(args: String): Promise<JsAny?> {
    if (args.isNotBlank()) {
        println("Args = $args")
        Args.parseArgs(args.split(" ").toTypedArray())
    }
    Args.enableModes(Mode.SIMPLE)

    eventLoop = object : EventLoop {
        override suspend fun runMicrotasks() {
            yield()
        }
    }

    return MainScope().promise {
        runBenchmarks()
        jsOne
    }
}

private fun isD8env(): JsBoolean =
    js("typeof isD8 !== 'undefined'")