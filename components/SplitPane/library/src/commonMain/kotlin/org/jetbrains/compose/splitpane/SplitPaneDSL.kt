package org.jetbrains.compose.splitpane

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Receiver scope which is used by [HorizontalSplitPane] and [VerticalSplitPane] */
@ExperimentalSplitPaneApi
interface SplitPaneScope {

    /**
     * Set up first composable item if SplitPane, for [HorizontalSplitPane] it will be
     * Left part, for [VerticalSplitPane] it will be Top part
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
     * For [HorizontalSplitPane] it will be Right part, for [VerticalSplitPane] it will be Bottom part
     * @param minSize a minimal size of composable item.
     * For [HorizontalSplitPane] it will be minimal width, for [VerticalSplitPane] it wil be minimal Heights.
     * In this context minimal mean that this composable item could not be smaller than specified value.
     * @param content composable item content.
     * */
    fun second(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
    )

    fun splitter(block: SplitterScope.() -> Unit)

}

/** Receiver scope which is used by [SplitterScope] */
@ExperimentalSplitPaneApi
interface HandleScope {
    /** allow mark composable as movable handle */
    fun Modifier.markAsHandle(): Modifier
}

/** Receiver scope which is used by [SplitPaneScope] */
@ExperimentalSplitPaneApi
interface SplitterScope {
    /**
     * Set up visible part of splitter. This part will be measured and placed between split pane
     * parts (first and second)
     *
     * @param content composable item content
     * */
    fun visiblePart(content: @Composable () -> Unit)

    /**
     * Set up handle part, this part of splitter would be measured and placed above [visiblePart] content.
     * Size of handle will have no effect on split pane parts (first and second) sizes.
     *
     * @param alignment alignment of handle according to [visiblePart] could be:
     * * [SplitterHandleAlignment.BEFORE] if you place handle before [visiblePart],
     * * [SplitterHandleAlignment.ABOVE] if you place handle above [visiblePart] (will be centered)
     * * and [SplitterHandleAlignment.AFTER] if you place handle after [visiblePart].
     *
     * @param content composable item content provider. Uses [HandleScope] to allow mark any provided composable part
     * as handle.
     * [content] will be placed only if [SplitPaneState.moveEnabled] is true
     */
    fun handle(
        alignment: SplitterHandleAlignment = SplitterHandleAlignment.ABOVE,
        content: @Composable HandleScope.() -> Unit
    )
}

@OptIn(ExperimentalSplitPaneApi::class)
internal class HandleScopeImpl(
    private val containerScope: SplitPaneScopeImpl
) : HandleScope {
    override fun Modifier.markAsHandle(): Modifier = composed {
        val layoutDirection = LocalLayoutDirection.current
        pointerInput(containerScope.splitPaneState) {
            detectDragGestures { change, _ ->
                change.consume()
                containerScope.splitPaneState.dispatchRawMovement(
                    if (containerScope.isHorizontal)
                        if (layoutDirection == LayoutDirection.Ltr) change.position.x else -change.position.x
                    else change.position.y
                )
            }
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
internal class SplitterScopeImpl(
    private val containerScope: SplitPaneScopeImpl
) : SplitterScope {

    override fun visiblePart(content: @Composable () -> Unit) {
        containerScope.visiblePart = content
    }

    override fun handle(
        alignment: SplitterHandleAlignment,
        content: @Composable HandleScope.() -> Unit
    ) {
        containerScope.handle = { HandleScopeImpl(containerScope).content() }
        containerScope.alignment = alignment
    }
}

private typealias ComposableSlot = @Composable () -> Unit

@OptIn(ExperimentalSplitPaneApi::class)
internal class SplitPaneScopeImpl(
    internal val isHorizontal: Boolean,
    internal val splitPaneState: SplitPaneState
) : SplitPaneScope {

    private var firstPlaceableMinimalSize: Dp = 0.dp
    private var secondPlaceableMinimalSize: Dp = 0.dp

    internal val minimalSizes: MinimalSizes
        get() = MinimalSizes(firstPlaceableMinimalSize, secondPlaceableMinimalSize)

    internal var firstPlaceableContent: ComposableSlot? = null
        private set
    internal var secondPlaceableContent: ComposableSlot? = null
        private set

    internal lateinit var visiblePart: ComposableSlot
    internal lateinit var handle: ComposableSlot
    internal var alignment: SplitterHandleAlignment = SplitterHandleAlignment.ABOVE
    internal val splitter
        get() =
            if (this::visiblePart.isInitialized && this::handle.isInitialized) {
                Splitter(visiblePart, handle, alignment)
            } else {
                defaultSplitter(isHorizontal, splitPaneState)
            }

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

    override fun splitter(block: SplitterScope.() -> Unit) {
        SplitterScopeImpl(this).block()
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
 * */
@ExperimentalSplitPaneApi
@Composable
fun rememberSplitPaneState(
    initialPositionPercentage: Float = 0f,
    moveEnabled: Boolean = true
): SplitPaneState {
    return remember {
        SplitPaneState(
            moveEnabled = moveEnabled,
            initialPositionPercentage = initialPositionPercentage
        )
    }
}
