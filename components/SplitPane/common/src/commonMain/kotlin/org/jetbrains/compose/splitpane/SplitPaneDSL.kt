package org.jetbrains.compose.splitpane

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface SplitPaneScope {

    fun first(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    fun second(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    fun splitter(
        block: SplitterScope.() -> Unit
    )
}

interface SplitterScope {
    val isHorizontal: Boolean
    fun Modifier.markAsHandle(): Modifier
    fun content(content: @Composable () -> Unit)
}

internal class SplitterScopeImpl(
    override val isHorizontal: Boolean,
    private val splitPaneState: SplitPaneState
) : SplitterScope {

    internal var splitter: ComposableSlot? = null
        private set

    override fun Modifier.markAsHandle(): Modifier =
        this.pointerInput(splitPaneState.splitterState) {
            detectDragGestures { change, _ ->
                change.consumeAllChanges()
                splitPaneState.splitterState.dispatchRawMovement(
                    if (isHorizontal) change.position.x else change.position.y
                )
            }
        }

    override fun content(
        content: @Composable () -> Unit
    ) {
        splitter = content
    }
}

private typealias ComposableSlot = @Composable () -> Unit

internal class SplitPaneScopeImpl(
    private val isHorizontal: Boolean,
    private val splitPaneState: SplitPaneState
) : SplitPaneScope {

    private var firstPlaceableMinimalSize: Dp = 0.dp
    private var secondPlaceableMinimalSize: Dp = 0.dp

    internal val minimalSizes: MinimalSizes
        get() = MinimalSizes(firstPlaceableMinimalSize, secondPlaceableMinimalSize)

    internal var firstPlaceableContent: ComposableSlot? = null
        private set
    internal var secondPlaceableContent: ComposableSlot? = null
        private set
    internal var splitter: ComposableSlot? = null
        private set

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

    override fun splitter(
        block: SplitterScope.() -> Unit
    ) {
        SplitterScopeImpl(
            isHorizontal,
            splitPaneState
        ).apply {
            block()
            this@SplitPaneScopeImpl.splitter = splitter
        }

    }
}

@Composable
fun rememberSplitPaneState(
    initial: Dp = 0.dp,
    moveEnabled: Boolean = true,
    interactionState: InteractionState = InteractionState()
): SplitPaneState {
    return remember {
        SplitPaneState(
            SplitterState(
                initialPosition = initial.value,
                interactionState = interactionState
            ),
            enabled = moveEnabled
        )
    }
}