/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import android.view.Choreographer
import android.view.Display
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.trace
import java.util.concurrent.TimeUnit

@ExperimentalFoundationApi
@Composable
internal actual fun LazyLayoutPrefetcher(
    prefetchPolicy: LazyLayoutPrefetchPolicy,
    itemContentFactory: LazyLayoutItemContentFactory,
    subcomposeLayoutState: SubcomposeLayoutState
) {
    val view = LocalView.current
    remember(subcomposeLayoutState, prefetchPolicy, view) {
        LazyLayoutPrefetcher(
            prefetchPolicy,
            subcomposeLayoutState,
            itemContentFactory,
            view
        )
    }
}

/**
 * Android specific prefetch implementation. The only platform specific things are:
 * 1) Calculating the deadline
 * 2) Posting the delayed runnable
 * This could be refactored in the future in order to keep the most logic platform agnostic to
 * enable the prefetching on desktop.
 *
 * The differences with the implementation in RecyclerView:
 *
 * 1) Prefetch is per-list-index, and performed on whole item.
 *    With RecyclerView, nested scrolling RecyclerViews would prefetch incrementally, e.g. items
 *    like the following in a scrolling vertical list could be broken up within a frame:
 *    [Row1 [a], [b], [c]]
 *    [Row2 [d], [e]]
 *    [Row3 [f], [g], [h]]
 *    You could have frames that break up this work arbitrarily:
 *    Frame 1 - prefetch [a]
 *    Frame 2 - prefetch [b], [c]
 *    Frame 3 - prefetch [d]
 *    Frame 4 - prefetch [e], [f]
 *    Something similar is not possible with LazyColumn yet.
 *
 * 2) Prefetching time estimation only captured during the prefetch.
 *    We currently don't track the time of the regular subcompose call happened during the regular
 *    measure pass, only the ones which are done during the prefetching. The downside is we build
 *    our prefetch information only after scrolling has started and items are showing up. Your very
 *    first scroll won't know if it's safe to prefetch. Why:
 *    a) SubcomposeLayout is not exposing an API to understand if subcompose() call is going to
 *    do the real work. The work could be skipped if the same lambda was passed as for the
 *    previous invocation or if there were no recompositions scheduled. We could workaround it
 *    by keeping the extra state in LazyListState about what items we already composed and to
 *    only measure the first composition for the given slot, or consider exposing extra
 *    information in SubcomposeLayoutState API.
 *    b) It allows us to nicely decouple the logic, now the prefetching logic is build on
 *    top of the regular LazyColumn measuring functionallity and the main logic knows nothing
 *    about prefetch
 *    c) Maybe the better approach would be to wait till the low-level runtime infra is ready to
 *    do subcompositions on the different threads which illuminates the need to calculate the
 *    deadlines completely.
 *    Tracking bug: b/187393381.
 *
 * 3) Prefetch is not aware of item type.
 *    RecyclerView separates timing metadata about different item types. For example, in play
 *    store style UI, this allows RecyclerView to separately estimate the cost of a header,
 *    separator, and item row. In this implementation, all of these would be averaged together in
 *    the same estimation.
 *    There is no view type concept in LazyColumn at all. What can we possible do:
 *    a) Think of different item/items calls in the builder dsl as different view types
 *    automatically. It is close enough but still not entirely correct if the user have something
 *    like a list of elements which are objects of some sealed classes and they consider
 *    different classes as completely different types
 *    b) Maybe if we would be able to precompose on the different thread this issue is also not
 *    so critical given that we don't need to calculate the deadline.
 *    Tracking bug: 187393922
 */
@ExperimentalFoundationApi
internal class LazyLayoutPrefetcher(
    private val prefetchPolicy: LazyLayoutPrefetchPolicy,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val view: View
) : RememberObserver,
    LazyLayoutPrefetchPolicy.Subscriber,
    Runnable,
    Choreographer.FrameCallback {

    /**
     * The indices scheduled to be prefetched. The items with these indices will be precomposed
     * one by one during subsequent [run]s, before doing a common premeasure on the next [run].
     * When an item is precomposed, the resulting [PrecomposedSlotHandle] is added to
     * [precomposedSlotsHandles] and the index is not removed from [indicesToPrefetch].
     */
    private val indicesToPrefetch = arrayListOf<Int>()

    /**
     * The constraints used to premeasure the prefetched items.
     */
    private val premeasureConstraints = arrayListOf<Constraints>()

    /**
     * [PrecomposedSlotHandle]s corresponding to the already precomposed items with indices from
     * [indicesToPrefetch]. While there are still items to precompose, every [run] will add a new
     * [PrecomposedSlotHandle], corresponding to the next index of [indicesToPrefetch] not
     * precomposed.
     */
    private val precomposedSlotsHandles = arrayListOf<PrecomposedSlotHandle>()

    /**
     * Average time the prefetching operations takes. Keeping it allows us to not start the work
     * if in this frame we are most likely not going to finish the work in time to not delay the
     * next frame.
     */
    private var averagePrecomposeTimeNs: Long = 0
    private var averagePremeasureTimeNs: Long = 0

    private var prefetchScheduled = false

    private val choreographer = Choreographer.getInstance()

    /** Is true when LazyList was composed and not yet disposed. */
    private var isActive = false

    init {
        calculateFrameIntervalIfNeeded(view)
    }

    /**
     * Callback to be executed when the prefetching is needed.
     * [indicesToPrefetch] will be used as an input.
     */
    override fun run() {
        if (indicesToPrefetch.isEmpty() || !prefetchScheduled || !isActive) {
            // incorrect input. ignore
            return
        }
        if (precomposedSlotsHandles.size < indicesToPrefetch.size) {
            trace("compose:lazylist:prefetch:compose") {
                val latestFrameVsyncNs = TimeUnit.MILLISECONDS.toNanos(view.drawingTime)
                val nextFrameNs = latestFrameVsyncNs + frameIntervalNs
                val itemsProvider = itemContentFactory.itemsProvider()
                var schedulePrefetch = true
                while (precomposedSlotsHandles.size < indicesToPrefetch.size) {
                    val index = indicesToPrefetch[precomposedSlotsHandles.size]
                    if (view.windowVisibility != View.VISIBLE ||
                        index !in 0 until itemsProvider.itemsCount) {
                        schedulePrefetch = false
                        break
                    }
                    val beforeNs = System.nanoTime()
                    // check if there is enough time left in this frame. otherwise, we schedule
                    // a next frame callback in which we will post the message in the handler again.
                    if (!(beforeNs > nextFrameNs ||
                        beforeNs + averagePrecomposeTimeNs < nextFrameNs)) {
                        break
                    }

                    val key = itemsProvider.getKey(index)
                    val content = itemContentFactory.getContent(index, key)
                    val handle = subcomposeLayoutState.precompose(key, content)
                    precomposedSlotsHandles.add(handle)
                    averagePrecomposeTimeNs = calculateAverageTime(
                        System.nanoTime() - beforeNs,
                        averagePrecomposeTimeNs
                    )
                }
                if (schedulePrefetch) {
                    // now schedule another precompose or the premeasure on the next frame
                    choreographer.postFrameCallback(this)
                } else {
                    prefetchScheduled = false
                }
            }
        } else {
            trace("compose:lazylist:prefetch:measure") {
                // the precomposition happened already. premeasure now
                val latestFrameVsyncNs = TimeUnit.MILLISECONDS.toNanos(view.drawingTime)
                val nextFrameNs = latestFrameVsyncNs + frameIntervalNs
                val beforeNs = System.nanoTime()
                if (beforeNs > nextFrameNs || beforeNs + averagePremeasureTimeNs < nextFrameNs) {
                    if (view.windowVisibility == View.VISIBLE &&
                        precomposedSlotsHandles.isNotEmpty()
                    ) {
                        precomposedSlotsHandles.fastForEachIndexed { handleIndex, handle ->
                            repeat(handle.placeablesCount) { placeableIndex ->
                                handle.premeasure(
                                    placeableIndex,
                                    premeasureConstraints[handleIndex]
                                )
                            }
                        }

                        averagePremeasureTimeNs = calculateAverageTime(
                            System.nanoTime() - beforeNs,
                            averagePremeasureTimeNs
                        )
                    }
                    prefetchScheduled = false
                } else {
                    // there is not enough time left in this frame. we schedule a next frame callback
                    // in which we are going to post the message in the handler again.
                    choreographer.postFrameCallback(this)
                }
            }
        }
    }

    /**
     * Choreographer frame callback. It will be called when during the previous frame we didn't
     * have enough time left. We will post a new message in the handler in order to try to
     * prefetch again after this frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        if (isActive) {
            view.post(this)
        }
    }

    private fun calculateAverageTime(new: Long, current: Long): Long {
        // Calculate a weighted moving average of time taken to compose an item. We use weighted
        // moving average to bias toward more recent measurements, and to minimize storage /
        // computation cost. (the idea is taken from RecycledViewPool)
        return if (current == 0L) {
            new
        } else {
            current / 4 * 3 + new / 4
        }
    }

    override fun scheduleForPrefetch(indices: List<Pair<Int, Constraints>>) {
        indicesToPrefetch.clear()
        premeasureConstraints.clear()
        indices.fastForEach {
            indicesToPrefetch.add(it.first)
            premeasureConstraints.add(it.second)
        }
        precomposedSlotsHandles.clear()
        if (!prefetchScheduled) {
            prefetchScheduled = true
            // schedule the prefetching
            view.post(this)
        }
    }

    override fun cancelScheduledPrefetch() {
        precomposedSlotsHandles.fastForEach { it.dispose() }
        indicesToPrefetch.clear()
        premeasureConstraints.clear()
    }

    override fun onRemembered() {
        prefetchPolicy.prefetcher = this
        isActive = true
    }

    override fun onForgotten() {
        isActive = false
        prefetchPolicy.prefetcher = null
        view.removeCallbacks(this)
        choreographer.removeFrameCallback(this)
    }

    override fun onAbandoned() {}

    companion object {

        /**
         * The static cache in order to not gather the display refresh rate to often (expensive operation).
         */
        private var frameIntervalNs: Long = 0

        private fun calculateFrameIntervalIfNeeded(view: View) {
            // we only do this query once, statically, because it's very expensive (> 1ms)
            if (frameIntervalNs == 0L) {
                val display: Display? = view.display
                var refreshRate = 60f
                if (!view.isInEditMode && display != null) {
                    val displayRefreshRate = display.refreshRate
                    if (displayRefreshRate >= 30f) {
                        // break 60 fps assumption if data from display appears valid
                        refreshRate = displayRefreshRate
                    }
                }
                frameIntervalNs = (1000000000 / refreshRate).toLong()
            }
        }
    }
}
