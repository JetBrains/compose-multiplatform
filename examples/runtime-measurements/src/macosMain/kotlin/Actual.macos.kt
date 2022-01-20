import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.getTimeNanos

private object SixtyFpsMonotonicFrameClock : MonotonicFrameClock {
    private const val fps = 60

    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        delay(1000L / fps)
        return onFrame(getTimeNanos())
    }
}

actual val MClock: MonotonicFrameClock = SixtyFpsMonotonicFrameClock

internal actual object GlobalSnapshotManager {
    private val started = kotlin.native.concurrent.AtomicInt(0)

    actual fun ensureStarted() {
        if (started.compareAndSet(0, 1)) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            CoroutineScope(Dispatchers.Default).launch {
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

actual fun _runTest(block: suspend CoroutineScope.() -> Unit) =
    runBlocking(EmptyCoroutineContext, block)
