@file:OptIn(ExperimentalJsExport::class)

import kotlinx.coroutines.*
import kotlin.js.Promise

val jsOne = 1.toJsNumber()

fun main(args: Array<String>) {
    if (isD8env().toBoolean()) {
        mainD8(args)
    } else {
        mainBrowser(args)
    }
}

fun mainBrowser(args: Array<String>) {
    eventLoop = object : EventLoop {
        override suspend fun runMicrotasks() {
            yield()
        }
    }

    Args.parseArgs(args)
    Args.enableModes(Mode.CPU)

    MainScope().launch {
        runBenchmarks()
        println("Completed!")
    }
}


// To run all the benchmarks in d8:
// ./gradlew :benchmarks:wasmJsProductionExecutableCompileSync --rerun-tasks
// cd compose-multiplatform/benchmarks/multiplatform/build/js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin
// ~/.gradle/d8/v8-mac-arm64-rel-12.5.170/d8 --module launcher.mjs
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
    Args.enableModes(Mode.CPU)

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
    Args.enableModes(Mode.CPU)

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