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

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.listSaver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Placeable
import androidx.compose.ui.Remeasurement
import androidx.compose.ui.RemeasurementModifier
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_FEATURE_WARNING")
internal inline class DataIndex(val value: Int) {
    inline operator fun inc(): DataIndex = DataIndex(value + 1)
    inline operator fun dec(): DataIndex = DataIndex(value - 1)
    inline operator fun plus(i: Int): DataIndex = DataIndex(value + i)
    inline operator fun minus(i: Int): DataIndex = DataIndex(value - i)
    inline operator fun minus(i: DataIndex): DataIndex = DataIndex(value - i.value)
    inline operator fun compareTo(other: DataIndex): Int = value - other.value
}

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 * [LazyListState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val config = defaultFlingConfig()

    // Avoid creating a new instance every invocation
    val saver = remember(config, clock) {
        LazyListState.Saver(config, clock)
    }

    return rememberSavedInstanceState(config, clock, saver = saver) {
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            config,
            clock
        )
    }
}

@OptIn(ExperimentalSubcomposeLayoutApi::class)
/**
 * A state object that can be hoisted to control and observe scrolling
 *
 * In most cases, this will be created via [rememberLazyListState].
 */
@Stable
class LazyListState constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    flingConfig: FlingConfig,
    animationClock: AnimationClockObservable
) {
    /**
     * The index of the first item that is visible
     */
    var firstVisibleItemIndex: Int by mutableStateOf(firstVisibleItemIndex)
        private set

    /**
     * Internal copy to avoid model reads triggering unnecessary remeasures
     */
    private var _firstVisibleItemIndex = DataIndex(firstVisibleItemIndex)

    /**
     * Scrolling forward is positive - i.e., the amount that the item is offset backwards
     */
    var firstVisibleItemScrollOffset by mutableStateOf(firstVisibleItemScrollOffset)
        internal set

    /**
     * Internal copy to avoid model reads triggering unnecessary remeasures
     */
    private var _firstVisibleItemScrollOffset = firstVisibleItemScrollOffset

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
     * Only used for testing to confirm that we're not making too many measure passes
     */
    @VisibleForTesting
    internal var numMeasurePasses: Int = 0

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyListState.remeasurement = remeasurement
        }
    }

    // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
    //  fine-grained control over scrolling
    @VisibleForTesting
    internal fun onScroll(distance: Float): Float {
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
     * Measures and positions currently visible items using [itemContentFactory] for subcomposing.
     */
    internal fun measure(
        scope: SubcomposeMeasureScope<DataIndex>,
        constraints: Constraints,
        isVertical: Boolean,
        horizontalAlignment: Alignment.Horizontal,
        verticalAlignment: Alignment.Vertical,
        itemsCount: Int,
        itemContentFactory: (Int) -> @Composable () -> Unit
    ): MeasureScope.MeasureResult = with(scope) {
        numMeasurePasses++
        constraints.assertNotNestingScrollableContainers(isVertical)
        if (itemsCount <= 0) {
            // empty data set. reset the current scroll and report zero size
            _firstVisibleItemIndex = DataIndex(0)
            _firstVisibleItemScrollOffset = 0
            layout(constraints.constrainWidth(0), constraints.constrainHeight(0)) {}
        } else {
            // assert for the incorrect initial state
            require(_firstVisibleItemScrollOffset >= 0f)
            require(_firstVisibleItemIndex.value >= 0f)

            if (_firstVisibleItemIndex.value >= itemsCount) {
                // the data set has been updated and now we have less items that we were
                // scrolled to before
                _firstVisibleItemIndex = DataIndex(itemsCount - 1)
                _firstVisibleItemScrollOffset = 0
            }

            // represents the real amount of consumed pixels
            var consumedScroll = scrollToBeConsumed.roundToInt()

            // applying the whole requested scroll offset. we will figure out if we can't consume
            // all of it later
            _firstVisibleItemScrollOffset -= consumedScroll

            // if the current scroll offset is less than minimally possible
            if (_firstVisibleItemIndex == DataIndex(0) && _firstVisibleItemScrollOffset < 0) {
                consumedScroll += _firstVisibleItemScrollOffset
                _firstVisibleItemScrollOffset = 0
            }

            // the constraints we will measure child with. the cross axis are not restricted
            val childConstraints = Constraints(
                maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
                maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
            )
            // saving it into the field as we first go backward and after that want to go forward
            // again from the initial position
            val goingForwardInitialIndex = _firstVisibleItemIndex
            var goingForwardInitialScrollOffset = _firstVisibleItemScrollOffset

            // this will contain all the placeables representing the visible items
            val visibleItemsPlaceables = mutableListOf<Placeable>()

            // we had scrolled backward, which means items before current firstItemScrollOffset
            // became visible. compose them and update firstItemScrollOffset
            while (_firstVisibleItemScrollOffset < 0 && _firstVisibleItemIndex > DataIndex(0)) {
                val previous = DataIndex(_firstVisibleItemIndex.value - 1)
                val placeables =
                    subcompose(previous, itemContentFactory(previous.value)).fastMap {
                        it.measure(childConstraints)
                    }
                visibleItemsPlaceables.addAll(0, placeables)
                val size = placeables.fastSumBy { if (isVertical) it.height else it.width }
                _firstVisibleItemScrollOffset += size
                _firstVisibleItemIndex = previous
            }
            // if we were scrolled backward, but there were not enough items before. this means
            // not the whole scroll was consumed
            if (_firstVisibleItemScrollOffset < 0) {
                consumedScroll += _firstVisibleItemScrollOffset
                goingForwardInitialScrollOffset += _firstVisibleItemScrollOffset
                _firstVisibleItemScrollOffset = 0
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
                    subcompose(index, itemContentFactory(index.value)).fastMap {
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
                    _firstVisibleItemIndex = index + 1
                    _firstVisibleItemScrollOffset -= size
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
                _firstVisibleItemScrollOffset -= toScrollBack
                mainAxisUsed += toScrollBack
                while (_firstVisibleItemScrollOffset < 0 && _firstVisibleItemIndex > DataIndex(0)) {
                    val previous = DataIndex(_firstVisibleItemIndex.value - 1)
                    val alreadyComposedIndex = notUsedButComposedItems?.lastIndex ?: -1
                    val placeables = if (alreadyComposedIndex >= 0) {
                        notUsedButComposedItems!!.removeAt(alreadyComposedIndex)
                    } else {
                        subcompose(previous, itemContentFactory(previous.value)).fastMap {
                            it.measure(childConstraints)
                        }
                    }
                    visibleItemsPlaceables.addAll(0, placeables)
                    val size = placeables.fastSumBy { if (isVertical) it.height else it.width }
                    _firstVisibleItemScrollOffset += size
                    _firstVisibleItemIndex = previous
                }
                consumedScroll += toScrollBack
                if (_firstVisibleItemScrollOffset < 0) {
                    consumedScroll += _firstVisibleItemScrollOffset
                    mainAxisUsed += _firstVisibleItemScrollOffset
                    _firstVisibleItemScrollOffset = 0
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

            // Copy values to public MutableState
            firstVisibleItemIndex = _firstVisibleItemIndex.value
            firstVisibleItemScrollOffset = _firstVisibleItemScrollOffset

            return layout(layoutWidth, layoutHeight) {
                var currentMainAxis = -_firstVisibleItemScrollOffset
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

    companion object {
        /**
         * The default [Saver] implementation for [LazyListState].
         */
        fun Saver(
            flingConfig: FlingConfig,
            animationClock: AnimationClockObservable
        ): Saver<LazyListState, *> = listSaver(
            save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
            restore = {
                LazyListState(
                    firstVisibleItemIndex = it[0],
                    firstVisibleItemScrollOffset = it[1],
                    flingConfig = flingConfig,
                    animationClock = animationClock
                )
            }
        )
    }
}
