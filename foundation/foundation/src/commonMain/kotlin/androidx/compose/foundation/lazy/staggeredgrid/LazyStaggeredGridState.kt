/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs

@ExperimentalFoundationApi
@Composable
internal fun rememberLazyStaggeredGridState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemOffset: Int = 0
): LazyStaggeredGridState =
    rememberSaveable(saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState(
            firstVisibleItemIndex,
            firstVisibleItemOffset
        )
    }

@ExperimentalFoundationApi
internal class LazyStaggeredGridState(
    initialFirstVisibleItems: IntArray = IntArray(0),
    initialFirstVisibleOffsets: IntArray = IntArray(0)
) : ScrollableState {
    constructor(initialFirstVisibleItemIndex: Int, initialFirstVisibleItemOffset: Int) : this(
        intArrayOf(initialFirstVisibleItemIndex),
        intArrayOf(initialFirstVisibleItemOffset)
    )

    internal var firstVisibleItems by mutableStateOf(initialFirstVisibleItems)

    internal var firstVisibleItemScrollOffsets by mutableStateOf(initialFirstVisibleOffsets)

    private val layoutInfoState: MutableState<LazyStaggeredGridLayoutInfo> =
        mutableStateOf(LazyStaggeredGridLayoutInfo.Empty)

    internal val spans = LazyStaggeredGridSpans()

    private var canScrollForward = true
    private var canScrollBackward = true

    private var remeasurement: Remeasurement? = null

    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyStaggeredGridState.remeasurement = remeasurement
        }
    }

    internal val prefetchState: LazyLayoutPrefetchState = LazyLayoutPrefetchState()

    internal var prefetchingEnabled = true

    private val scrollableState = ScrollableState { -onScroll(-it) }

    internal var scrollToBeConsumed = 0f
        private set

    /* @VisibleForTesting */
    internal var measurePassCount = 0

    private var wasScrollingForward = true
    internal var isVertical = false
    internal var prefetchLaneWidths: IntArray = IntArray(0)
    private var prefetchBaseIndex: Int = -1
    private val currentItemPrefetchHandles = mutableVectorOf<PrefetchHandle>()

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        scrollableState.scroll(scrollPriority, block)
    }

    private fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        check(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            remeasurement?.forceRemeasure()
            if (prefetchingEnabled) {
                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    private fun notifyPrefetch(delta: Float) {
        if (!prefetchingEnabled) {
            return
        }
        val info = layoutInfoState.value
        if (info.visibleItemsInfo.isNotEmpty()) {
            val scrollingForward = delta < 0

            if (wasScrollingForward != scrollingForward) {
                // the scrolling direction has been changed which means the last prefetched item
                // is not going to be reached anytime soon so it is safer to dispose it.
                // if this item is already visible it is safe to call the method anyway
                // as it will be a no-op
                currentItemPrefetchHandles.forEach { it.cancel() }
            }

            wasScrollingForward = scrollingForward
            val prefetchIndex = if (scrollingForward) {
                info.visibleItemsInfo.last().index
            } else {
                info.visibleItemsInfo.first().index
            }

            if (prefetchIndex == prefetchBaseIndex) {
                // Already prefetched based on this index
                return
            }
            prefetchBaseIndex = prefetchIndex
            currentItemPrefetchHandles.clear()

            var targetIndex = prefetchIndex
            for (lane in prefetchLaneWidths.indices) {
                val previousIndex = targetIndex

                // find the next item for each line and prefetch if it is valid
                targetIndex = if (scrollingForward) {
                    spans.findNextItemIndex(previousIndex, lane)
                } else {
                    spans.findPreviousItemIndex(previousIndex, lane)
                }
                if (
                    targetIndex !in (0 until info.totalItemsCount) ||
                        previousIndex == targetIndex
                ) {
                    return
                }

                val crossAxisSize = prefetchLaneWidths[lane] -
                    if (lane == 0) 0 else prefetchLaneWidths[lane - 1]

                val constraints = if (isVertical) {
                    Constraints.fixedWidth(crossAxisSize)
                } else {
                    Constraints.fixedHeight(crossAxisSize)
                }

                currentItemPrefetchHandles.add(
                    prefetchState.schedulePrefetch(
                        index = targetIndex,
                        constraints = constraints
                    )
                )
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: LazyStaggeredGridLayoutInfo) {
        val items = info.visibleItemsInfo
        if (prefetchBaseIndex != -1 && items.isNotEmpty()) {
            if (prefetchBaseIndex !in items.first().index..items.last().index) {
                prefetchBaseIndex = -1
                currentItemPrefetchHandles.forEach { it.cancel() }
                currentItemPrefetchHandles.clear()
            }
        }
    }

    internal fun applyMeasureResult(result: LazyStaggeredGridMeasureResult) {
        scrollToBeConsumed -= result.consumedScroll
        firstVisibleItems = result.firstVisibleItemIndices
        firstVisibleItemScrollOffsets = result.firstVisibleItemScrollOffsets
        canScrollBackward = result.canScrollBackward
        canScrollForward = result.canScrollForward
        layoutInfoState.value = result
        cancelPrefetchIfVisibleItemsChanged(result)

        measurePassCount++
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    companion object {
        val Saver = listSaver<LazyStaggeredGridState, IntArray>(
            save = { state ->
                listOf(
                    state.firstVisibleItems,
                    state.firstVisibleItemScrollOffsets
                    // todo: save spans as well?
                )
            },
            restore = {
                LazyStaggeredGridState(it[0], it[1])
            }
        )
    }
}