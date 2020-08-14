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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.Remeasurement
import androidx.compose.ui.RemeasurementModifier
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.roundToInt

private inline class ScrollDirection(val isForward: Boolean)

@Suppress("NOTHING_TO_INLINE")
internal inline class DataIndex(val value: Int) {
    inline operator fun inc(): DataIndex = DataIndex(value + 1)
    inline operator fun dec(): DataIndex = DataIndex(value - 1)
    inline operator fun plus(i: Int): DataIndex = DataIndex(value + i)
    inline operator fun minus(i: Int): DataIndex = DataIndex(value - i)
    inline operator fun minus(i: DataIndex): DataIndex = DataIndex(value - i.value)
    inline operator fun compareTo(other: DataIndex): Int = value - other.value
}

@OptIn(ExperimentalSubcomposeLayoutApi::class)
internal class LazyForState(val isVertical: Boolean) {
    /**
     * The index of the first item that is composed into the layout tree
     */
    private var firstComposedItem = DataIndex(0)
    /**
     * The index of the last item that is composed into the layout tree
     */
    private var lastComposedItem = DataIndex(-1) // obviously-bogus sentinel value
    /**
     * Scrolling forward is positive - i.e., the amount that the item is offset backwards
     */
    private var firstItemScrollOffset = 0f
    /**
     * The amount of space remaining in the last item
     */
    private var lastItemRemainingSpace = 0f
    /**
     * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    private var scrollToBeConsumed = 0f
    /**
     * The children that have been measured this measure pass.
     * Used to avoid measuring twice in a single pass, which is illegal
     */
    private val measuredThisPass: MutableMap<DataIndex, List<Placeable>> = mutableMapOf()

    /**
     * The listener to be passed to onScrollDeltaConsumptionRequested.
     * Cached to avoid recreations
     */
    val onScrollDelta: (Float) -> Float = { onScroll(it) }

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the list.
     */
    var scrollableController: ScrollableController? = null

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    private lateinit var remeasurement: Remeasurement

    /**
     * The modifier which provides [remeasurement].
     */
    val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyForState.remeasurement = remeasurement
        }
    }

    /**
     * The cached instance of the scope to be used for composing items.
     */
    private var itemScope = InitialLazyItemsScopeImpl

    private val Placeable.mainAxisSize get() = if (isVertical) height else width
    private val Placeable.crossAxisSize get() = if (!isVertical) height else width

    // TODO: really want an Int here
    private fun onScroll(distance: Float): Float {
        check(abs(scrollToBeConsumed) < 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed = distance
        remeasurement.forceRemeasure()
        val scrollConsumed = distance - scrollToBeConsumed

        if (abs(scrollToBeConsumed) < 0.5) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            scrollableController!!.stopAnimation()
            return scrollConsumed
        }
    }

    private fun SubcomposeMeasureScope<DataIndex>.consumePendingScroll(
        childConstraints: Constraints,
        itemsCount: Int,
        itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
    ) {
        val scrollDirection = ScrollDirection(isForward = scrollToBeConsumed < 0f)

        while (true) {
            // General outline:
            // Consume as much of the drag as possible via adjusting the scroll offset
            scrollToBeConsumed = consumeScrollViaOffset(scrollToBeConsumed)

            // TODO: What's the correct way to handle half a pixel of unconsumed scroll?

            // Allow up to half a pixel of scroll to remain unconsumed
            if (abs(scrollToBeConsumed) >= 0.5f) {
                // We need to bring another item onscreen. Can we?
                if (!composeAndMeasureNextItem(
                        childConstraints,
                        scrollDirection,
                        itemsCount,
                        itemContentFactory
                    )
                ) {
                    // Nope. Break out and return the rest of the drag
                    break
                }
                // Yay, we got another item! Our scroll offsets are populated again, go back and
                // consume them in the next round.
            } else {
                // We've consumed the whole scroll
                break
            }
        }
    }

    /**
     * @return The amount of scroll remaining unconsumed
     */
    private fun consumeScrollViaOffset(delta: Float): Float {
        if (delta < 0) {
            // Scrolling forward, content moves up
            // Consume via space at end
            // Remember: delta is *negative*
            if (lastItemRemainingSpace >= -delta) {
                // We can consume it all
                updateScrollOffsets(delta)
                return 0f
            } else {
                // All offset consumed, return the remaining offset to the caller
                // delta is negative, prevRemainingSpace/lastItemRemainingSpace are positive
                val prevRemainingSpace = lastItemRemainingSpace
                updateScrollOffsets(-prevRemainingSpace)
                return delta + prevRemainingSpace
            }
        } else {
            // Scrolling backward, content moves down
            // Consume via initial offset
            if (firstItemScrollOffset >= delta) {
                // We can consume it all
                updateScrollOffsets(delta)
                return 0f
            } else {
                // All offset consumed, return the remaining offset to the caller
                val prevRemainingSpace = firstItemScrollOffset
                updateScrollOffsets(prevRemainingSpace)
                return delta - prevRemainingSpace
            }
        }
    }

    /**
     * Must be called within a measure pass.
     *
     * @return `true` if an item was composed and measured, `false` if there are no more items in
     * the scroll direction
     */
    private fun SubcomposeMeasureScope<DataIndex>.composeAndMeasureNextItem(
        childConstraints: Constraints,
        scrollDirection: ScrollDirection,
        itemsCount: Int,
        itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
    ): Boolean {
        val nextItemIndex = if (scrollDirection.isForward) {
            if (itemsCount > lastComposedItem.value + 1) {
                ++lastComposedItem
            } else {
                return false
            }
        } else {
            if (firstComposedItem.value > 0) {
                --firstComposedItem
            } else {
                return false
            }
        }

        val nextItems = composeChildForDataIndex(nextItemIndex, itemContentFactory).map {
            it.measure(childConstraints)
        }

        measuredThisPass[nextItemIndex] = nextItems

        val childSize = nextItems.fastSumBy { it.mainAxisSize }

        // Add in our newly composed space so that it may be consumed
        if (scrollDirection.isForward) {
            lastItemRemainingSpace += childSize
        } else {
            firstItemScrollOffset += childSize
        }

        return true
    }

    /**
     * Does no bounds checking, just moves the start and last offsets in sync.
     * Assumes the caller has checked bounds.
     */
    private fun updateScrollOffsets(delta: Float) {
        // Scrolling forward is negative delta and consumes space, so add the negative
        lastItemRemainingSpace += delta
        // Scrolling forward is negative delta and adds offset, so subtract the negative
        firstItemScrollOffset -= delta
    }

    /**
     * Updates the [itemScope] with the last [constraints] we got from the parent
     */
    private fun Density.updateItemScope(constraints: Constraints) {
        val width = constraints.maxWidth.toDp()
        val height = constraints.maxHeight.toDp()
        if (width != itemScope.maxWidth || height != itemScope.maxHeight) {
            itemScope = LazyItemScopeImpl(width, height)
        }
    }

    /**
     * Measures and positions currently visible items using [itemContentFactory] for subcomposing.
     */
    fun measure(
        scope: SubcomposeMeasureScope<DataIndex>,
        constraints: Constraints,
        horizontalAlignment: Alignment.Horizontal,
        verticalAlignment: Alignment.Vertical,
        itemsCount: Int,
        itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
    ): MeasureScope.MeasureResult = with(scope) {
        updateItemScope(constraints)
        measuredThisPass.clear()
        val maxMainAxis = if (isVertical) constraints.maxHeight else constraints.maxWidth
        val childConstraints = Constraints(
            maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
            maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
        )

        // We're being asked to consume scroll by the Scrollable
        if (abs(scrollToBeConsumed) >= 0.5f) {
            // Consume it in advance, because it simplifies the rest of this method if we
            // know exactly how much scroll we've consumed - for instance, we can safely
            // discard anything off the start of the viewport, because we know we can fill
            // it, assuming nothing has shrunken on us (which has to be handled separately
            // anyway)
            consumePendingScroll(childConstraints, itemsCount, itemContentFactory)
        }

        var mainAxisUsed = (-firstItemScrollOffset).roundToInt()
        var maxCrossAxis = 0

        // The index of the first item that should be displayed, regardless of what is
        // currently displayed.  Will be moved forward as we determine what's offscreen
        var index = firstComposedItem

        // TODO: handle the case where we can't fill the viewport due to children shrinking,
        //  but there are more items at the start that we could fill with
        val allPlaceables = mutableListOf<Placeable>()
        while (mainAxisUsed <= maxMainAxis && index.value < itemsCount) {
            val placeables = measuredThisPass.getOrPut(index) {
                composeChildForDataIndex(index, itemContentFactory).fastMap {
                    it.measure(childConstraints)
                }
            }
            var size = 0
            placeables.fastForEach {
                size += it.mainAxisSize
                maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize)
            }
            mainAxisUsed += size

            if (mainAxisUsed < 0f) {
                // this item is offscreen, remove it and the offset it took up
                firstComposedItem = index + 1
                firstItemScrollOffset -= size
            } else {
                allPlaceables.addAll(placeables)
            }

            index++
        }
        lastComposedItem = index - 1 // index is incremented after the last iteration

        lastItemRemainingSpace = if (mainAxisUsed > maxMainAxis) {
            (mainAxisUsed - maxMainAxis).toFloat()
        } else {
            0f
        }

        // Wrap the content of the children
        val layoutWidth = constraints.constrainWidth(
            if (isVertical) maxCrossAxis else mainAxisUsed
        )
        val layoutHeight = constraints.constrainHeight(
            if (!isVertical) maxCrossAxis else mainAxisUsed
        )

        return layout(layoutWidth, layoutHeight) {
            var currentMainAxis = (-firstItemScrollOffset).roundToInt()
            allPlaceables.fastForEach {
                if (isVertical) {
                    val x = horizontalAlignment.align(layoutWidth - it.width, layoutDirection)
                    if (currentMainAxis + it.height > 0 && currentMainAxis < layoutHeight) {
                        it.place(x, currentMainAxis)
                    }
                    currentMainAxis += it.height
                } else {
                    val y = verticalAlignment.align(layoutHeight - it.height)
                    if (currentMainAxis + it.width > 0 && currentMainAxis < layoutWidth) {
                        it.placeRelative(currentMainAxis, y)
                    }
                    currentMainAxis += it.width
                }
            }
        }
    }

    private fun SubcomposeMeasureScope<DataIndex>.composeChildForDataIndex(
        dataIndex: DataIndex,
        itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
    ): List<Measurable> = subcompose(dataIndex, itemScope.itemContentFactory(dataIndex.value))
}

/**
 * Pre-allocated initial value for [LazyItemScopeImpl] to not have it nullable and avoid using
 * late init.
 */
private val InitialLazyItemsScopeImpl = LazyItemScopeImpl(0.dp, 0.dp)

private data class LazyItemScopeImpl(
    val maxWidth: Dp,
    val maxHeight: Dp
) : LazyItemScope {
    override fun Modifier.fillParentMaxSize() = size(maxWidth, maxHeight)
    override fun Modifier.fillParentMaxWidth() = width(maxWidth)
    override fun Modifier.fillParentMaxHeight() = height(maxHeight)
}
