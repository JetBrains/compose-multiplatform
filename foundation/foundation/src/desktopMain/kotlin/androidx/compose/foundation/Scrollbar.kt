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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.PressTimeoutMillis
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasuringIntrinsicsMeasureBlocks
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.sign

/**
 * Ambient used to pass [ScrollbarStyle] down the tree.
 * This value is typically set in some "Theme" composable function
 * (DesktopTheme, MaterialTheme)
 */
val ScrollbarStyleAmbient = staticCompositionLocalOf { defaultScrollbarStyle() }

/**
 * Defines visual style of scrollbars (thickness, shapes, colors, etc).
 * Can be passed as a parameter of scrollbar through [ScrollbarStyleAmbient]
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
 * Simple default [ScrollbarStyle] without hover effects and without applying MaterialTheme.
 */
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RectangleShape,
    hoverDurationMillis = 0,
    unhoverColor = Color.Black.copy(alpha = 0.12f),
    hoverColor = Color.Black.copy(alpha = 0.12f)
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0f)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         ScrollableColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *             rememberScrollbarAdapter(state)
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionState [InteractionState] that will be updated when the element with this
 * state is being dragged, using [Interaction.Dragged]
 */
@Composable
fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = ScrollbarStyleAmbient.current,
    interactionState: InteractionState = remember { InteractionState() }
) = Scrollbar(
    adapter,
    modifier,
    style,
    interactionState,
    isVertical = true
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (ScrollableRow, LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0f)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         ScrollableRow(state = state) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
 *             rememberScrollbarAdapter(state)
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionState [InteractionState] that will be updated when the element with this
 * state is being dragged, using [Interaction.Dragged]
 */
@Composable
fun HorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = ScrollbarStyleAmbient.current,
    interactionState: InteractionState = remember { InteractionState() }
) = Scrollbar(
    adapter,
    modifier,
    style,
    interactionState,
    isVertical = false
)

// TODO(demin): do we need to stop dragging if cursor is beyond constraints?
// TODO(demin): add Interaction.Hovered to interactionState
@Composable
private fun Scrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle,
    interactionState: InteractionState,
    isVertical: Boolean
) = with(LocalDensity.current) {
    DisposableEffect(interactionState) {
        onDispose {
            interactionState.removeInteraction(Interaction.Dragged)
        }
    }

    var containerSize by remember { mutableStateOf(0) }
    var isHover by remember { mutableStateOf(false) }

    val minimalHeight = style.minimalHeight.toPx()
    val sliderAdapter = remember(adapter, containerSize, minimalHeight) {
        SliderAdapter(adapter, containerSize, minimalHeight)
    }

    val scrollThickness = style.thickness.toIntPx()
    val measureBlocks = if (isVertical) {
        remember(sliderAdapter, scrollThickness) {
            verticalMeasureBlocks(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    } else {
        remember(sliderAdapter, scrollThickness) {
            horizontalMeasureBlocks(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    }

    val dragObserver = object : DragObserver {
        override fun onStart(downPosition: Offset) {
            interactionState.addInteraction(Interaction.Dragged)
        }

        override fun onStop(velocity: Offset) {
            interactionState.removeInteraction(Interaction.Dragged)
        }

        override fun onCancel() {
            interactionState.removeInteraction(Interaction.Dragged)
        }

        override fun onDrag(dragDistance: Offset): Offset {
            sliderAdapter.position += if (isVertical) dragDistance.y else dragDistance.x
            return dragDistance
        }
    }

    val color by animateColorAsState(
        if (isHover) style.hoverColor else style.unhoverColor,
        animationSpec = TweenSpec(durationMillis = style.hoverDurationMillis)
    )

    val isVisible = sliderAdapter.size < containerSize

    Layout(
        {
            Box(
                Modifier
                    .background(if (isVisible) color else Color.Transparent, style.shape)
                    .rawDragGestureFilter(dragObserver)
            )
        },
        measureBlocks,
        modifier
            .pointerMoveFilter(
                onExit = { isHover = false; true },
                onEnter = { isHover = true; true }
            )
            .scrollOnPressOutsideSlider(isVertical, sliderAdapter, adapter, containerSize)
    )
}

@Suppress("DEPRECATION") // press gesture filter
private fun Modifier.scrollOnPressOutsideSlider(
    isVertical: Boolean,
    sliderAdapter: SliderAdapter,
    scrollbarAdapter: ScrollbarAdapter,
    containerSize: Int
) = composed {
    var targetOffset: Offset? by remember { mutableStateOf(null) }

    if (targetOffset != null) {
        val targetPosition = if (isVertical) targetOffset!!.y else targetOffset!!.x

        LaunchedEffect(targetPosition) {
            var delay = PressTimeoutMillis * 3
            while (targetPosition !in sliderAdapter.bounds) {
                val oldSign = sign(targetPosition - sliderAdapter.position)
                scrollbarAdapter.scrollTo(
                    containerSize,
                    scrollbarAdapter.scrollOffset + oldSign * containerSize
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

    pressIndicatorGestureFilter(
        onStart = { targetOffset = it },
        onStop = { targetOffset = null },
        onCancel = { targetOffset = null }
    )
}

/**
 * Create and [remember] [ScrollbarAdapter] for scrollable container and current instance of
 * [scrollState]
 */
@Composable
fun rememberScrollbarAdapter(
    scrollState: ScrollState
): ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [ScrollbarAdapter] for lazy scrollable container and current instance of
 * [scrollState] and item configuration
 */
@ExperimentalFoundationApi
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyListState,
    itemCount: Int,
    averageItemSize: Dp
): ScrollbarAdapter {
    val averageItemSizePx = with(LocalDensity.current) {
        averageItemSize.toPx()
    }
    return remember(scrollState, itemCount, averageItemSizePx) {
        ScrollbarAdapter(scrollState, itemCount, averageItemSizePx)
    }
}

/**
 * ScrollbarAdapter for ScrollableColumn and ScrollableRow
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0f)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         ScrollableColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *             rememberScrollbarAdapter(state)
 *         )
 *     }
 */
fun ScrollbarAdapter(
    scrollState: ScrollState
): ScrollbarAdapter = ScrollableScrollbarAdapter(scrollState)

private class ScrollableScrollbarAdapter(
    private val scrollState: ScrollState
) : ScrollbarAdapter {
    override val scrollOffset: Float get() = scrollState.value

    override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
        scrollState.scrollTo(scrollOffset)
    }

    override fun maxScrollOffset(containerSize: Int) =
        scrollState.maxValue
}

// TODO(demin): if item height is different then slider will have wrong
//  position when we dragging it (we can drag it to the beginning, but content will not be at the
//  beginning). We can implement adaptive scrollbar height after b/170472532

/**
 * Experimental ScrollbarAdapter for lazy lists. Doesn't work stable with non-fixed item height.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Scrollbar size and position will be calculated by passed [itemCount] and [averageItemSize]
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *         val itemCount = 100
 *         val itemHeight = 20.dp
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             Modifier.align(Alignment.CenterEnd),
 *             rememberScrollbarAdapter(state, itemCount, itemHeight)
 *         )
 *     }
 */
@ExperimentalFoundationApi
fun ScrollbarAdapter(
    scrollState: LazyListState,
    itemCount: Int,
    averageItemSize: Float
): ScrollbarAdapter = LazyScrollbarAdapter(
    scrollState, itemCount, averageItemSize
)

private class LazyScrollbarAdapter(
    private val scrollState: LazyListState,
    private val itemCount: Int,
    private val averageItemSize: Float
) : ScrollbarAdapter {
    init {
        require(itemCount >= 0f) { "itemCount should be non-negative ($itemCount)" }
        require(averageItemSize > 0f) { "averageItemSize should be positive ($averageItemSize)" }
    }

    override val scrollOffset: Float
        get() = scrollState.firstVisibleItemIndex * averageItemSize +
            scrollState.firstVisibleItemScrollOffset

    override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
        val scrollOffsetCoerced = scrollOffset.coerceIn(0f, maxScrollOffset(containerSize))

        val index = (scrollOffsetCoerced / averageItemSize)
            .toInt()
            .coerceAtLeast(0)
            .coerceAtMost(itemCount - 1)

        scrollState.snapToItemIndex(
            index = index,
            scrollOffset = (scrollOffsetCoerced - index * averageItemSize).toInt()
        )
    }

    override fun maxScrollOffset(containerSize: Int) =
        averageItemSize * itemCount - containerSize
}

/**
 * Defines how to scroll the scrollable component
 */
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

private class SliderAdapter(
    val adapter: ScrollbarAdapter,
    val containerSize: Int,
    val minHeight: Float
) {
    private val contentSize get() = adapter.maxScrollOffset(containerSize) + containerSize
    private val visiblePart get() = containerSize.toFloat() / contentSize

    val size
        get() = (containerSize * visiblePart)
            .coerceAtLeast(minHeight)
            .coerceAtMost(containerSize.toFloat())

    private val scrollScale get() = (containerSize - size) / (contentSize - containerSize)

    var position: Float
        get() = scrollScale * adapter.scrollOffset
        set(value) {
            runBlocking {
                adapter.scrollTo(containerSize, value / scrollScale)
            }
        }

    val bounds get() = position..position + size
}

private fun verticalMeasureBlocks(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasuringIntrinsicsMeasureBlocks { measurables, constraints ->
    setContainerSize(constraints.maxHeight)
    val height = sliderAdapter.size.toInt()
    val placeable = measurables.first().measure(
        Constraints.fixed(
            constraints.constrainWidth(scrollThickness),
            height
        )
    )
    layout(placeable.width, constraints.maxHeight) {
        placeable.place(0, sliderAdapter.position.toInt())
    }
}

private fun horizontalMeasureBlocks(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasuringIntrinsicsMeasureBlocks { measurables, constraints ->
    setContainerSize(constraints.maxWidth)
    val width = sliderAdapter.size.toInt()
    val placeable = measurables.first().measure(
        Constraints.fixed(
            width,
            constraints.constrainHeight(scrollThickness)
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(sliderAdapter.position.toInt(), 0)
    }
}