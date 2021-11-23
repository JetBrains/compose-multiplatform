import androidx.compose.runtime.*
import kotlinx.coroutines.*

class UnitApplier : Applier<Unit> {
    override val current: Unit
        get() = Unit

    override fun down(node: Unit) {}
    override fun up() {}
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun clear() {}
}

fun createRecomposer(): Recomposer {
    val mainScope = CoroutineScope(
        NonCancellable + Dispatchers.Main + DefaultMonotonicFrameClock
    )

    return Recomposer(mainScope.coroutineContext).also {
        mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
            it.runRecomposeAndApplyChanges()
        }
    }
}


actual fun callComposable(content: @Composable () -> Unit) {
    val c = ControlledComposition(UnitApplier(), createRecomposer())
    c.setContent(content)
}
