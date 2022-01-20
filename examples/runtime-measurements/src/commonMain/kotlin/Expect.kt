import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope

expect val MClock: MonotonicFrameClock


internal expect object GlobalSnapshotManager {
    fun ensureStarted()
}


expect fun _runTest(block: suspend CoroutineScope.() -> Unit)
