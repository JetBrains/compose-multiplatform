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

/** Receiver scope which is used by [HorizontalSplitPane] and [VerticalSplitPane] */
interface SplitPaneScope {

    /**
     * Set up first composable item if SplitPane, for [HorizontalSplitPane] it will be
     * Right part, for [VerticalSplitPane] it will be Top part
     * @param minSize a minimal size of composable item.
     * For [HorizontalSplitPane] it will be minimal width, for [VerticalSplitPane] it wil be minimal Heights.
     * In this context minimal mean that this composable item could not be smaller than specified value.
     * @param content composable item content.
     * */
    fun first(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    /**
     * Set up second composable item if SplitPane.
     * For [HorizontalSplitPane] it will be Left part, for [VerticalSplitPane] it will be Bottom part
     * @param minSize a minimal size of composable item.
     * For [HorizontalSplitPane] it will be minimal width, for [VerticalSplitPane] it wil be minimal Heights.
     * In this context minimal mean that this composable item could not be smaller than specified value.
     * @param content composable item content.
     * */
    fun second(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    /**
     * Set up splitter that will split first and second parts of split pane.
     * By default for [HorizontalSplitPane] it will be vertical line that divides available width in some place.
     * For [VerticalSplitPane] it wil be horizontal line that divides available heights in some place.
     * @param block specify configuration lambda for [SplitterScope] receiver
     * which wil allow to configure custom composable for splitter and move handle
     * */
    fun splitter(
        block: SplitterScope.() -> Unit
    )
}

/** Receiver scope which is used by [HorizontalSplitPane] and [VerticalSplitPane] by [SplitPaneScope] */
interface SplitterScope {

    /**
     * Value provided into composable configurations.
     * It will allow to check if configure custom splitter for [HorizontalSplitPane] or [VerticalSplitPane]
     * */
    val isHorizontal: Boolean

    /**
     * [Modifier] extension function that will allow to mark any composable configured inside
     * [SplitterScope] as move handle.
     * That will allow use them to move splitter along split pane direction.
     * */
    fun Modifier.markAsHandle(): Modifier

    /**
     * Set up composable content for custom splitter.
     * */
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

/**
 * creates a [SplitPaneState] and remembers it across composition
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initial the initial value for [SplitterState.position]
 * @param moveEnabled the initial value for [SplitPaneState.moveEnabled]
 * @param interactionState the initial value for [SplitterState.interactionState]
 * */
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