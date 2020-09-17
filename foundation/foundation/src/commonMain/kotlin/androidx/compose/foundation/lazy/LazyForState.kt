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

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.Remeasurement
import androidx.compose.ui.RemeasurementModifier
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.roundToInt

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
internal class LazyForState(
    flingConfig: FlingConfig,
    animationClock: AnimationClockObservable
) {
    /**
     * The index of the first item that is composed into the layout tree
     */
    private var firstVisibleItemIndex = DataIndex(0)
    /**
     * Scrolling forward is positive - i.e., the amount that the item is offset backwards
     */
    private var firstVisibleItemScrollOffset = 0
    /**
     * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    private var scrollToBeConsumed = 0f

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the list.
     */
    internal val scrollableController =
        ScrollableController(
            flingConfig = flingConfig,
            animationClock = animationClock,
            consumeScrollDelta = { onScroll(it) })

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    private lateinit var remeasurement: Remeasurement

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyForState.remeasurement = remeasurement
        }
    }

    /**
     * The cached instance of the scope to be used for composing items.
     */
    private var itemScope = InitialLazyItemsScopeImpl

    private fun onScroll(distance: Float): Float {
        check(abs(scrollToBeConsumed) < 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed += distance
        remeasurement.forceRemeasure()

        if (abs(scrollToBeConsumed) < 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            scrollableController.stopAnimation()
            return scrollConsumed
        }
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
        isVertical: Boolean,
        horizontalAlignment: Alignment.Horizontal,
        verticalAlignment: Alignment.Vertical,
        itemsCount: Int,
        itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
    ): MeasureScope.MeasureResult = with(scope) {
        constraints.assertNotNestingScrollableContainers(isVertical)
        if (itemsCount <= 0) {
            // empty data set. reset the current scroll and report zero size
            firstVisibleItemIndex = DataIndex(0)
            firstVisibleItemScrollOffset = 0
            layout(constraints.constrainWidth(0), constraints.constrainHeight(0)) {}
        } else {
            // this will update the scope object if the constrains have been changed
            updateItemScope(constraints)

            // assert for the incorrect initial state
            require(firstVisibleItemScrollOffset >= 0f)
            require(firstVisibleItemIndex.value >= 0f)

            if (firstVisibleItemIndex.value >= itemsCount) {
                // the data set has been updated and now we have less items that we were
                // scrolled to before
                firstVisibleItemIndex = DataIndex(itemsCount - 1)
                firstVisibleItemScrollOffset = 0
            }

            // represents the real amount of consumed pixels
            var consumedScroll = scrollToBeConsumed.roundToInt()

            // applying the whole requested scroll offset. we will figure out if we can't consume
            // all of it later
            firstVisibleItemScrollOffset -= consumedScroll

            // if the current scroll offset is less than minimally possible
            if (firstVisibleItemIndex == DataIndex(0) && firstVisibleItemScrollOffset < 0) {
                consumedScroll += firstVisibleItemScrollOffset
                firstVisibleItemScrollOffset = 0
            }

            // the constraints we will measure child with. the cross axis are not restricted
            val childConstraints = Constraints(
                maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
                maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
            )
            // saving it into the field as we first go backward and after that want to go forward
            // again from the initial position
            val goingForwardInitialIndex = firstVisibleItemIndex
            var goingForwardInitialScrollOffset = firstVisibleItemScrollOffset

            // this will contain all the placeables representing the visible items
            val visibleItemsPlaceables = mutableListOf<Placeable>()

            // we had scrolled backward, which means items before current firstItemScrollOffset
            // became visible. compose them and update firstItemScrollOffset
            while (firstVisibleItemScrollOffset < 0 && firstVisibleItemIndex > DataIndex(0)) {
                val previous = DataIndex(firstVisibleItemIndex.value - 1)
                val placeables =
                    subcompose(previous, itemScope.itemContentFactory(previous.value)).fastMap {
                        it.measure(childConstraints)
                    }
                visibleItemsPlaceables.addAll(0, placeables)
                val size = placeables.fastSumBy { if (isVertical) it.height else it.width }
                firstVisibleItemScrollOffset += size
                firstVisibleItemIndex = previous
            }
            // if we were scrolled backward, but there were not enough items before. this means
            // not the whole scroll was consumed
            if (firstVisibleItemScrollOffset < 0) {
                consumedScroll += firstVisibleItemScrollOffset
                goingForwardInitialScrollOffset += firstVisibleItemScrollOffset
                firstVisibleItemScrollOffset = 0
            }

            // remembers the composed placeables which we are not currently placing as they are out
            // of screen. it is possible we will need to place them if the remaining items will
            // not fill the whole viewport and we will need to scroll back
            var notUsedButComposedItems: MutableList<List<Placeable>>? = null

            // composing visible items starting from goingForwardInitialIndex until we fill the
            // whole viewport
            var index = goingForwardInitialIndex
            val maxMainAxis = if (isVertical) constraints.maxHeight else constraints.maxWidth
            var mainAxisUsed = -goingForwardInitialScrollOffset
            var maxCrossAxis = 0
            while (mainAxisUsed <= maxMainAxis && index.value < itemsCount) {
                val placeables =
                    subcompose(index, itemScope.itemContentFactory(index.value)).fastMap {
                        it.measure(childConstraints)
                    }
                var size = 0
                placeables.fastForEach {
                    size += if (isVertical) it.height else it.width
                    maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) it.height else it.width)
                }
                mainAxisUsed += size

                if (mainAxisUsed < 0f) {
                    // this item is offscreen and will not be placed. advance firstVisibleItemIndex
                    firstVisibleItemIndex = index + 1
                    firstVisibleItemScrollOffset -= size
                    // but remember the corresponding placeables in case we will be forced to
                    // scroll back as there were not enough items to fill the viewport
                    if (notUsedButComposedItems == null) {
                        notUsedButComposedItems = mutableListOf()
                    }
                    notUsedButComposedItems.add(placeables)
                } else {
                    visibleItemsPlaceables.addAll(placeables)
                }

                index++
            }

            // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
            // lets try to scroll back if we have enough items before firstVisibleItemIndex.
            if (mainAxisUsed < maxMainAxis) {
                val toScrollBack = maxMainAxis - mainAxisUsed
                firstVisibleItemScrollOffset -= toScrollBack
                mainAxisUsed += toScrollBack
                while (firstVisibleItemScrollOffset < 0 && firstVisibleItemIndex > DataIndex(0)) {
                    val previous = DataIndex(firstVisibleItemIndex.value - 1)
                    val alreadyComposedIndex = notUsedButComposedItems?.lastIndex ?: -1
                    val placeables = if (alreadyComposedIndex >= 0) {
                        notUsedButComposedItems!!.removeAt(alreadyComposedIndex)
                    } else {
                        subcompose(previous, itemScope.itemContentFactory(previous.value)).fastMap {
                            it.measure(childConstraints)
                        }
                    }
                    visibleItemsPlaceables.addAll(0, placeables)
                    val size = placeables.fastSumBy { if (isVertical) it.height else it.width }
                    firstVisibleItemScrollOffset += size
                    firstVisibleItemIndex = previous
                }
                consumedScroll += toScrollBack
                if (firstVisibleItemScrollOffset < 0) {
                    consumedScroll += firstVisibleItemScrollOffset
                    mainAxisUsed += firstVisibleItemScrollOffset
                    firstVisibleItemScrollOffset = 0
                }
            }

            // report the amount of pixels we consumed
            scrollToBeConsumed -= consumedScroll

            // Wrap the content of the children
            val layoutWidth = constraints.constrainWidth(
                if (isVertical) maxCrossAxis else mainAxisUsed
            )
            val layoutHeight = constraints.constrainHeight(
                if (!isVertical) maxCrossAxis else mainAxisUsed
            )

            return layout(layoutWidth, layoutHeight) {
                var currentMainAxis = -firstVisibleItemScrollOffset
                visibleItemsPlaceables.fastForEach {
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
    }
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
