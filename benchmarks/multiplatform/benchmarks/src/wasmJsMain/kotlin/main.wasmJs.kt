import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

fun main(args: Array<String>) {
    println("Args = ${args.joinToString(separator = ", ")}")
    // Args.parseArgs(args)
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