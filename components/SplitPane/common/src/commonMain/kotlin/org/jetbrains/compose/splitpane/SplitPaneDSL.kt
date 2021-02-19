package org.jetbrains.compose.splitpane

import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
}

private typealias ComposableSlot = @Composable () -> Unit

internal class SplitPaneScopeImpl : SplitPaneScope {

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
}

/**
 * creates a [SplitPaneState] and remembers it across composition
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialPositionPercentage the initial value for [SplitPaneState.positionPercentage]
 * @param moveEnabled the initial value for [SplitPaneState.moveEnabled]
 * @param interactionState the initial value for [SplitPaneState.interactionState]
 * */
@Composable
fun rememberSplitPaneState(
    initialPositionPercentage: Float = 0f,
    moveEnabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() }
): SplitPaneState {
    return remember {
        SplitPaneState(
            moveEnabled = moveEnabled,
            initialPositionPercentage = initialPositionPercentage,
            interactionState = interactionState
        )
    }
}