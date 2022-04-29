import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.util.concurrent.atomic.AtomicBoolean

val LocalContainer = compositionLocalOf<ExternalContainer> { error("undefined") }

@Composable
fun ComposeExternalTextField(text: String, onChange: (String) -> Unit) {
    val container = LocalContainer.current
    val widget = remember { ExternalTextField() }

    DisposableEffect(Unit) {
        container.children.add(widget)

        onDispose {
            container.children.remove(widget)
        }
    }

    SideEffect {
        widget.text = text
        widget.onUserInput = onChange
    }
}

open class ComposeContainer : ExternalContainer() {
    private val scope = CoroutineScope(UIDispatcher)
    private val recomposer = Recomposer(UIDispatcher)
    private var composition: Composition? = null

    init {
        GlobalSnapshotManager.ensureStarted()
        scope.launch(YieldFrameClock, start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        composition?.dispose()
        composition = Composition(EmptyApplier, recomposer)
        composition?.setContent {
            CompositionLocalProvider(LocalContainer provides this) {
                content()
            }
        }
    }

    override fun dispose() {
        composition?.dispose()
        recomposer.cancel()
        scope.cancel()
    }
}

private object YieldFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ): R {
        // We call `yield` to avoid blocking UI thread. If we don't call this then application
        // can be frozen for the user in some cases as it will not receive any input events.
        //
        // Swing dispatcher will process all pending events and resume after `yield`.
        yield()
        return onFrame(System.nanoTime())
    }
}

private object EmptyApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) = Unit
    override fun up() = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun clear() = Unit
    override fun onEndChanges() = Unit
}

internal object GlobalSnapshotManager {
    private val started = AtomicBoolean(false)

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            CoroutineScope(UIDispatcher).launch {
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