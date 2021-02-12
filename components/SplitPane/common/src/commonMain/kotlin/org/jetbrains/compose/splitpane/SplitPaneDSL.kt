package org.jetbrains.compose.splitpane

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.compose.movable.SingleDirectionMovable
import org.jetbrains.compose.movable.SplitterState
import kotlin.coroutines.coroutineContext

interface SplitPaneScope {

    fun first(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    fun second(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )
}

interface SeparatorScope {
    val isHorizontal: Boolean
    fun Modifier.markAsHandle()
    fun separator(content: @Composable () -> Unit)
}

internal class SeparatorScopeImpl(
    override val isHorizontal: Boolean,
    private val splitterState: SingleDirectionMovable
) : SeparatorScope {

    override fun Modifier.markAsHandle() {
        this.pointerInput(splitterState) {
            detectDragGestures { change, _ ->
                change.consumeAllChanges()
                LaunchedEffect(splitterState) {
                    splitterState.move {
                        moveBy(if (isHorizontal) change.position.x else change.position.y)
                    }
                }
//                splitterState.dispatchRawMovement(if (isHorizontal) change.position.x else change.position.y)
            }
        }
    }

    override fun separator(content: () -> Unit) {
        TODO("Not yet implemented")
    }
}

private typealias ComposableSlot = @Composable () -> Unit

internal class SplitPaneScopeImpl : SplitPaneScope {

    private var firstPlaceableMinimalSize: Dp = 0.dp
    private var secondPlaceableMinimalSize: Dp = 0.dp

    internal val minimalSizes: MinimalSizes
        get() = MinimalSizes(firstPlaceableMinimalSize, secondPlaceableMinimalSize)

    internal var firstPlaceableContent: ComposableSlot? = null
    internal var secondPlaceableContent: ComposableSlot? = null

        override fun first(
            minSize: Dp,
            content: @Composable () -> Unit
        ) {
            firstPlaceableMinimalSize = minSize
            firstPlaceableContent = content
        }

        override fun second(
            minSize: Dp,
            content: @Composable () -> Unit
        ) {
            secondPlaceableMinimalSize = minSize
            secondPlaceableContent = content
        }

}

@Composable
fun rememberSplitterState(
    initial: Dp = 0.dp,
    interactionState: InteractionState? = null
): SplitterState {
    return remember {
        SplitterState(
            initialPosition = initial.value,
//            interactionState = interactionState
        )
    }
}