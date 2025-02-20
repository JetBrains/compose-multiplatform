import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

fun main(args: Array<String>) {
    println("Args = ${args.joinToString(separator = ", ")}")
    if (isSpecialJetstream3Build().toBoolean()) {
        val hardcodedArgs =
            "benchmarks=" +
                    "AnimatedVisibility(100000)," +
                    "LazyGrid(2000)," +
                    "LazyGrid-ItemLaunchedEffect(2000)," +
                    "LazyGrid-SmoothScroll(5000)," +
                    "LazyGrid-SmoothScroll-ItemLaunchedEffect(5000)," +
                    "VisualEffects(10000)"
        // TODO: fix and enable Example1 for Wasm target

        Args.parseArgs(arrayOf(hardcodedArgs))
    }
    Args.enableModes(Mode.CPU)

    val jsOne = 1.toJsNumber()

    eventLoop = object : EventLoop {
        override suspend fun runMicrotasks() {
            suspendCoroutine { c ->
                Promise.resolve(jsOne).then {
                    c.resumeWith(Result.success(Unit))
                    it
                }
            }
        }
    }

    MainScope().launch {
        runBenchmarks()
        println("Completed!")
    }
}

private fun isSpecialJetstream3Build(): JsBoolean =
    js("isWasmBuildForJetstream3 == true")