import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

actual val MClock: MonotonicFrameClock = if (js("typeof window !== \"undefined\"") == true) {
    println("Using DefaultMonotonicFrameClock")
    DefaultMonotonicFrameClock
} else {
    println("Using NodeJsMonotonicFrameClock")
    NodeJsMonotonicFrameClock
}

internal actual object GlobalSnapshotManager {
    private var started = false

    actual fun ensureStarted() {
        if (!started) {
            started = true
            val channel = Channel<Unit>(Channel.CONFLATED)
            CoroutineScope(JsMicrotasksDispatcher()).launch {
                channel.consumeEach {
                    Snapshot.sendApplyNotifications()
                }
            }
            Snapshot.registerGlobalWriteObserver {
                channel.trySend(Unit)
            }
        }
    }
}


private object NodeJsMonotonicFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        val ns = (js("new Date().getTime()") as Number).toLong() * 1000_000
        return onFrame(ns)
    }
}

private val testScope = MainScope()

actual fun _runTest(block: suspend CoroutineScope.() -> Unit): dynamic = testScope.promise {
    block()
}
