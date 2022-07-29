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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import kotlin.math.abs

@ExperimentalFoundationApi
internal class LazyStaggeredGridState : ScrollableState {
    var firstVisibleItems: IntArray by mutableStateOf(IntArray(0))
        private set

    var firstVisibleItemScrollOffsets: IntArray by mutableStateOf(IntArray(0))
        private set

    internal val spans: SpanLookup = SpanLookup()

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
            remeasurement?.forceRemeasure()
            // todo(b/182882362): notify prefetch
//            if (prefetchingEnabled) {
//                val leftoverScroll = preScrollToBeConsumed - scrollToBeConsumed
//                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
//            }
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

    internal fun applyMeasureResult(result: LazyStaggeredGridMeasureResult) {
        scrollToBeConsumed -= result.consumedScroll
        firstVisibleItems = result.firstVisibleItemIndices
        firstVisibleItemScrollOffsets = result.firstVisibleItemScrollOffsets
        canScrollBackward = result.canScrollBackward
        canScrollForward = result.canScrollForward
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress
}

internal class SpanLookup {
    private var spans = IntArray(16)

    fun setSpan(item: Int, span: Int) {
        ensureCapacity(item + 1)
        spans[item] = span + 1
    }

    fun getSpan(item: Int): Int =
        spans[item] - 1

    fun capacity(): Int =
        spans.size

    private fun ensureCapacity(capacity: Int) {
        if (spans.size < capacity) {
            spans = spans.copyInto(IntArray(spans.size * 2))
        }
    }

    fun reset() {
        spans.fill(0)
    }

    companion object {
        internal const val SpanUnset = -1
    }
}