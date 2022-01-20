import androidx.compose.runtime.AtomicInt
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

actual val MClock: MonotonicFrameClock = DefaultMonotonicFrameClock

internal actual object GlobalSnapshotManager {
    private val started = AtomicInteger(0)

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

actual fun _runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }
