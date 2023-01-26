/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextFieldScrollState
import androidx.compose.foundation.v2.LazyGridScrollbarAdapter
import androidx.compose.foundation.v2.LazyListScrollbarAdapter
import androidx.compose.foundation.v2.ScrollableScrollbarAdapter
import androidx.compose.foundation.v2.SliderAdapter
import androidx.compose.foundation.v2.TextFieldScrollbarAdapter
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.delay

/**
 * [CompositionLocal] used to pass [ScrollbarStyle] down the tree.
 * This value is typically set in some "Theme" composable function
 * (DesktopTheme, MaterialTheme)
 */
val LocalScrollbarStyle = staticCompositionLocalOf { defaultScrollbarStyle() }

/**
 * Defines visual style of scrollbars (thickness, shapes, colors, etc).
 * Can be passed as a parameter of scrollbar through [LocalScrollbarStyle]
 */
@Immutable
data class ScrollbarStyle(
    val minimalHeight: Dp,
    val thickness: Dp,
    val shape: Shape,
    val hoverDurationMillis: Int,
    val unhoverColor: Color,
    val hoverColor: Color
)

/**
 * Simple default [ScrollbarStyle] without applying MaterialTheme.
 */
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 300,
    unhoverColor = Color.Black.copy(alpha = 0.12f),
    hoverColor = Color.Black.copy(alpha = 0.50f)
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the bottom of the container.
 * It is usually used in pair with `LazyColumn(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Deprecated("Use VerticalScrollbar(" +
    "adapter: androidx.compose.foundation.v2.ScrollbarAdapter)" +
    " instead")
@Composable
fun VerticalScrollbar(
    @Suppress("DEPRECATION") adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = OldScrollbar(
    adapter,
    modifier,
    reverseLayout,
    style,
    interactionSource,
    isVertical = true
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.horizontalScroll(state)) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the end of the container.
 * It is usually used in pair with `LazyRow(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Deprecated("Use HorizontalScrollbar(" +
    "adapter: androidx.compose.foundation.v2.ScrollbarAdapter) instead")
@Composable
fun HorizontalScrollbar(
    @Suppress("DEPRECATION") adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = OldScrollbar(
    adapter,
    modifier,
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) !reverseLayout else reverseLayout,
    style,
    interactionSource,
    isVertical = false
)

@Suppress("DEPRECATION")
@Composable
private fun OldScrollbar(
    oldAdapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean
) = OldOrNewScrollbar(
    oldOrNewAdapter = oldAdapter,
    newScrollbarAdapterFactory = ScrollbarAdapter::asNewAdapter,
    modifier = modifier,
    reverseLayout = reverseLayout,
    style = style,
    interactionSource = interactionSource,
    isVertical = isVertical
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 *
 * @param adapter [androidx.compose.foundation.v2.ScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the bottom of the container.
 * It is usually used in pair with `LazyColumn(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun VerticalScrollbar(
    adapter: androidx.compose.foundation.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    reverseLayout,
    style,
    interactionSource,
    isVertical = true
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
 *         )
 *     }
 *
 * @param adapter [androidx.compose.foundation.v2.ScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the end of the container.
 * It is usually used in pair with `LazyRow(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun HorizontalScrollbar(
    adapter: androidx.compose.foundation.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) !reverseLayout else reverseLayout,
    style,
    interactionSource,
    isVertical = false
)

@Composable
private fun NewScrollbar(
    newAdapter: androidx.compose.foundation.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean
) = OldOrNewScrollbar(
    oldOrNewAdapter = newAdapter,
    newScrollbarAdapterFactory = { adapter, _ -> adapter },
    modifier = modifier,
    reverseLayout = reverseLayout,
    style = style,
    interactionSource = interactionSource,
    isVertical = isVertical
)

private typealias NewScrollbarAdapterFactory<T> = (
    adapter: T,
    trackSize: Int,
) -> androidx.compose.foundation.v2.ScrollbarAdapter


/**
 * The actual implementation of the scrollbar.
 *
 * Takes the scroll adapter (old or new) and a function that converts it to the new scrollbar
 * adapter interface. This allows both the old (left for backwards compatibility) and new
 * implementations to use the same code.
 */
@Composable
internal fun <T> OldOrNewScrollbar(
    oldOrNewAdapter: T,
    // We need an adapter factory because we can't convert an old to a new
    // adapter until we have the track/container size
    newScrollbarAdapterFactory: NewScrollbarAdapterFactory<T>,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean,
) = with(LocalDensity.current) {
    val dragInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            dragInteraction.value?.let { interaction ->
                interactionSource.tryEmit(DragInteraction.Cancel(interaction))
                dragInteraction.value = null
            }
        }
    }

    var containerSize by remember { mutableStateOf(0) }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isHighlighted by remember {
        derivedStateOf {
            isHovered || dragInteraction.value is DragInteraction.Start
        }
    }

    val minimalHeight = style.minimalHeight.toPx()

    val adapter = remember(oldOrNewAdapter, containerSize){
        newScrollbarAdapterFactory(oldOrNewAdapter, containerSize)
    }
    val sliderAdapter = remember(adapter, containerSize, minimalHeight, reverseLayout, isVertical) {
        SliderAdapter(adapter, containerSize, minimalHeight, reverseLayout, isVertical)
    }

    val scrollThickness = style.thickness.roundToPx()
    val measurePolicy = if (isVertical) {
        remember(sliderAdapter, scrollThickness) {
            verticalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    } else {
        remember(sliderAdapter, scrollThickness) {
            horizontalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    }

    val color by animateColorAsState(
        if (isHighlighted) style.hoverColor else style.unhoverColor,
        animationSpec = TweenSpec(durationMillis = style.hoverDurationMillis)
    )

    val isVisible = sliderAdapter.thumbSize < containerSize

    Layout(
        {
            Box(
                Modifier
                    .background(if (isVisible) color else Color.Transparent, style.shape)
                    .scrollbarDrag(
                        interactionSource = interactionSource,
                        draggedInteraction = dragInteraction,
                        sliderAdapter = sliderAdapter,
                    )
            )
        },
        modifier
            .hoverable(interactionSource = interactionSource)
            .scrollOnPressOutsideThumb(isVertical, sliderAdapter, adapter),
        measurePolicy
    )
}

/**
 * Adapts an old [ScrollbarAdapter] to the new interface, under the assumption that the
 * track size is equal to the viewport size.
 */
private class OldScrollbarAdapterAsNew(
    @Suppress("DEPRECATION") val oldAdapter: ScrollbarAdapter,
    private val trackSize: Int
) : androidx.compose.foundation.v2.ScrollbarAdapter {

    override val scrollOffset: Double
        get() = oldAdapter.scrollOffset.toDouble()

    override val contentSize: Double
        get() = (oldAdapter.maxScrollOffset(trackSize) + trackSize).toDouble()

    override val viewportSize: Double
        get() = trackSize.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        oldAdapter.scrollTo(trackSize, scrollOffset.toFloat())
    }

}

/**
 * Converts an instance of the old scrollbar adapter to a new one.
 *
 * If the old one is in fact just a [NewScrollbarAdapterAsOld], then simply unwrap it.
 * This allows users that simply passed our own (old) implementations back to
 * us to seamlessly use the new implementations, and enjoy all their benefits.
 */
@Suppress("DEPRECATION")
private fun ScrollbarAdapter.asNewAdapter(
    trackSize: Int
): androidx.compose.foundation.v2.ScrollbarAdapter =
    if (this is NewScrollbarAdapterAsOld)
        this.newAdapter  // Just unwrap
    else
        OldScrollbarAdapterAsNew(this, trackSize)

/**
 * Adapts a new scrollbar adapter to the old interface.
 */
@Suppress("DEPRECATION")
private class NewScrollbarAdapterAsOld(
    val newAdapter: androidx.compose.foundation.v2.ScrollbarAdapter
): ScrollbarAdapter {

    override val scrollOffset: Float
        get() = newAdapter.scrollOffset.toFloat()

    override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
        newAdapter.scrollTo(scrollOffset.toDouble())
    }

    override fun maxScrollOffset(containerSize: Int): Float {
        return newAdapter.maxScrollOffset.toFloat()
    }

}

/**
 * Converts an instance of the new scrollbar adapter to an old one.
 */
@Suppress("DEPRECATION")
private fun androidx.compose.foundation.v2.ScrollbarAdapter.asOldAdapter(): ScrollbarAdapter =
    if (this is OldScrollbarAdapterAsNew)
        this.oldAdapter  // Just unwrap
    else
        NewScrollbarAdapterAsOld(this)

/**
 * Create and [remember] (old) [ScrollbarAdapter] for scrollable container and current instance of
 * [scrollState]
 */
@Deprecated(
    message = "Use rememberScrollbarAdapter instead",
    replaceWith = ReplaceWith(
        expression = "rememberScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.rememberScrollbarAdapter"
    )
)
@JvmName("rememberScrollbarAdapter")
@Suppress("DEPRECATION")
@Composable
fun rememberOldScrollbarAdapter(
    scrollState: ScrollState
): ScrollbarAdapter = remember(scrollState) {
    OldScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] (old) [ScrollbarAdapter] for lazy scrollable container and current instance
 * of [scrollState]
 */
@Deprecated(
    message ="Use rememberScrollbarAdapter instead",
    replaceWith = ReplaceWith(
        expression = "rememberScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.rememberScrollbarAdapter"
    )
)
@JvmName("rememberScrollbarAdapter")
@Suppress("DEPRECATION")
@Composable
fun rememberOldScrollbarAdapter(
    scrollState: LazyListState,
): ScrollbarAdapter {
    return remember(scrollState) {
        OldScrollbarAdapter(scrollState)
    }
}

/**
 * ScrollbarAdapter for Modifier.verticalScroll and Modifier.horizontalScroll
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@Deprecated(
    message = "Use ScrollbarAdapter() instead",
    replaceWith = ReplaceWith(
        expression = "ScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.ScrollbarAdapter"
    )
)
@JvmName("ScrollbarAdapter")
@Suppress("DEPRECATION")
fun OldScrollbarAdapter(
    scrollState: ScrollState
): ScrollbarAdapter = ScrollbarAdapter(scrollState).asOldAdapter()

/**
 * ScrollbarAdapter for lazy lists.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@Deprecated(
    message = "Use ScrollbarAdapter() instead",
    replaceWith = ReplaceWith(
        expression = "ScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.ScrollbarAdapter"
    )
)
@JvmName("ScrollbarAdapter")
@Suppress("DEPRECATION")
fun OldScrollbarAdapter(
    scrollState: LazyListState
): ScrollbarAdapter = ScrollbarAdapter(scrollState).asOldAdapter()

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for
 * scrollable container with the given instance [ScrollState].
 */
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: ScrollState
): androidx.compose.foundation.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for
 * lazy scrollable container with the given instance [LazyListState].
 */
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyListState,
): androidx.compose.foundation.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for lazy grid with
 * the given instance of [LazyGridState].
 */
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyGridState,
): androidx.compose.foundation.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for text field with
 * the given instance of [TextFieldScrollState].
 */
@ExperimentalFoundationApi
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: TextFieldScrollState,
): androidx.compose.foundation.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * ScrollbarAdapter for Modifier.verticalScroll and Modifier.horizontalScroll
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: ScrollState
): androidx.compose.foundation.v2.ScrollbarAdapter = ScrollableScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for lazy lists.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: LazyListState
): androidx.compose.foundation.v2.ScrollbarAdapter = LazyListScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for lazy grids.
 *
 * [scrollState] is instance of [LazyGridState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyGridState()
 *
 *         LazyVerticalGrid(columns = ..., state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: LazyGridState
): androidx.compose.foundation.v2.ScrollbarAdapter = LazyGridScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for text fields.
 *
 * [scrollState] is instance of [TextFieldScrollState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val scrollState = rememberTextFieldVerticalScrollState()
 *
 *         BasicTextField(
 *             value = ...,
 *             onValueChange = ...,
 *             scrollState = state
 *         ) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(scrollState)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@ExperimentalFoundationApi
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: TextFieldScrollState
): androidx.compose.foundation.v2.ScrollbarAdapter = TextFieldScrollbarAdapter(scrollState)

/**
 * Defines how to scroll the scrollable component
 */
@Deprecated("Use androidx.compose.foundation.v2.ScrollbarAdapter instead")
interface ScrollbarAdapter {

    /**
     * Scroll offset of the content inside the scrollable component.
     * Offset "100" means that the content is scrolled by 100 pixels from the start.
     */
    val scrollOffset: Float

    /**
     * Instantly jump to [scrollOffset] in pixels
     *
     * @param containerSize size of the scrollable container
     *  (for example, it is height of ScrollableColumn if we use VerticalScrollbar)
     * @param scrollOffset target value in pixels to jump to,
     *  value will be coerced to 0..maxScrollOffset
     */
    suspend fun scrollTo(containerSize: Int, scrollOffset: Float)

    /**
     * Maximum scroll offset of the content inside the scrollable component
     *
     * @param containerSize size of the scrollable component
     *  (for example, it is height of ScrollableColumn if we use VerticalScrollbar)
     */
    fun maxScrollOffset(containerSize: Int): Float

}

private fun computeSlidePositionAndSize(sliderAdapter: SliderAdapter): Pair<Int, Int> {
    val adapterPosition = sliderAdapter.position
    val position = adapterPosition.roundToInt()
    val size = (sliderAdapter.thumbSize + adapterPosition - position).roundToInt()

    return Pair(position, size)
}

private fun verticalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxHeight)
    val (position, height) = computeSlidePositionAndSize(sliderAdapter)

    val placeable = measurables.first().measure(
        Constraints.fixed(
            constraints.constrainWidth(scrollThickness),
            height
        )
    )
    layout(placeable.width, constraints.maxHeight) {
        placeable.place(0, position)
    }
}

private fun horizontalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxWidth)
    val (position, width) = computeSlidePositionAndSize(sliderAdapter)

    val placeable = measurables.first().measure(
        Constraints.fixed(
            width,
            constraints.constrainHeight(scrollThickness)
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(position, 0)
    }
}

private fun Modifier.scrollbarDrag(
    interactionSource: MutableInteractionSource,
    draggedInteraction: MutableState<DragInteraction.Start?>,
    sliderAdapter: SliderAdapter,
): Modifier = composed {
    val currentInteractionSource by rememberUpdatedState(interactionSource)
    val currentDraggedInteraction by rememberUpdatedState(draggedInteraction)
    val currentSliderAdapter by rememberUpdatedState(sliderAdapter)

    pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown(requireUnconsumed = false)
                val interaction = DragInteraction.Start()
                currentInteractionSource.tryEmit(interaction)
                currentDraggedInteraction.value = interaction
                currentSliderAdapter.onDragStarted()
                val isSuccess = drag(down.id) { change ->
                    currentSliderAdapter.onDragDelta(change.positionChange())
                    change.consume()
                }
                val finishInteraction = if (isSuccess) {
                    DragInteraction.Stop(interaction)
                } else {
                    DragInteraction.Cancel(interaction)
                }
                currentInteractionSource.tryEmit(finishInteraction)
                currentDraggedInteraction.value = null
            }
        }
    }
}

private fun Modifier.scrollOnPressOutsideThumb(
    isVertical: Boolean,
    sliderAdapter: SliderAdapter,
    scrollbarAdapter: androidx.compose.foundation.v2.ScrollbarAdapter,
) = composed {
    var targetOffset: Offset? by remember { mutableStateOf(null) }

    if (targetOffset != null) {
        val targetPosition = if (isVertical) targetOffset!!.y else targetOffset!!.x

        LaunchedEffect(targetPosition) {
            var delay = PressTimeoutMillis * 3
            while (targetPosition !in sliderAdapter.bounds) {
                val oldSign = sign(targetPosition - sliderAdapter.position)
                scrollbarAdapter.scrollTo(
                    scrollbarAdapter.scrollOffset + oldSign * scrollbarAdapter.viewportSize
                )
                val newSign = sign(targetPosition - sliderAdapter.position)

                if (oldSign != newSign) {
                    break
                }

                delay(delay)
                delay = PressTimeoutMillis
            }
        }
    }
    Modifier.pointerInput(Unit) {
        detectTapAndPress(
            onPress = { offset ->
                targetOffset = offset
                tryAwaitRelease()
                targetOffset = null
            },
            onTap = {}
        )
    }
}

/**
 * The time that must elapse before a tap gesture sends onTapDown, if there's
 * any doubt that the gesture is a tap.
 */
private const val PressTimeoutMillis: Long = 100L
