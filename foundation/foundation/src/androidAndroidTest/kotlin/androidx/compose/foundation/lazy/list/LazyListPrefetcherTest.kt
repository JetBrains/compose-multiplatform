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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListPrefetcherTest(
    val config: Config
) : BaseLazyListTestWithOrientation(config.orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Config(Orientation.Vertical, 0),
            Config(Orientation.Vertical, 1),
            Config(Orientation.Horizontal, 0),
            Config(Orientation.Horizontal, 1)
        )

        class Config(
            val orientation: Orientation,
            val beyondBoundsItemCount: Int
        ) {
            override fun toString() = "orientation=$orientation with" +
                " $beyondBoundsItemCount non-visible items"
        }
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyListState

    @Test
    fun notPrefetchingForwardInitially() {
        composeList()

        rule.onNodeWithTag("${config.beyondBoundsItemCount + 2}")
            .assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeList(firstItem = 2)

        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        composeList()
        val preFetchIndex = 2
        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(preFetchIndex)

        rule.onNodeWithTag("$preFetchIndex")
            .assertExists()
        rule.onNodeWithTag("${config.beyondBoundsItemCount + preFetchIndex + 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        composeList(firstItem = 2, itemOffset = 10)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(1)

        rule.onNodeWithTag("1")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        val initialIndex = 5
        composeList(firstItem = initialIndex)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }
        var prefetchIndex = initialIndex + 2
        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex - config.beyondBoundsItemCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        prefetchIndex -= 3
        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex + config.beyondBoundsItemCount + 3}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composeList()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(2)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemsSizePx / 2f)
                state.scrollBy(itemsSizePx / 2f)
            }
        }

        val prefetchIndex = 3

        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("${prefetchIndex - 1}")
            .assertIsDisplayed()
        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex + config.beyondBoundsItemCount + 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composeList(firstItem = 4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(2)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemsSizePx / 2f)
                state.scrollBy(-itemsSizePx / 2f)
            }
        }

        waitForPrefetch(1)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardReverseLayout() {
        val initialIndex = 5
        composeList(firstItem = initialIndex, reverseLayout = true)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        var prefetchIndex = initialIndex + 2

        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex - config.beyondBoundsItemCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        prefetchIndex -= 3
        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex + config.beyondBoundsItemCount + 3}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardWithContentPadding() {
        val halfItemSize = itemsSizeDp / 2f
        val initialIndex = 5
        composeList(
            firstItem = initialIndex,
            itemOffset = 5,
            contentPadding = PaddingValues(mainAxis = halfItemSize)
        )

        rule.onNodeWithTag("${initialIndex - 1}")
            .assertIsDisplayed()
        rule.onNodeWithTag("$initialIndex")
            .assertIsDisplayed()
        rule.onNodeWithTag("${initialIndex + 1}")
            .assertIsDisplayed()
        rule.onNodeWithTag("${initialIndex - config.beyondBoundsItemCount - 2}")
            .assertDoesNotExist()
        rule.onNodeWithTag("${initialIndex + config.beyondBoundsItemCount + 2}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        var prefetchIndex = initialIndex + 1
        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("${prefetchIndex + 1}")
            .assertExists()
        rule.onNodeWithTag("${prefetchIndex - config.beyondBoundsItemCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
            }
        }

        prefetchIndex -= 3
        waitForPrefetch(prefetchIndex)

        rule.onNodeWithTag("$prefetchIndex")
            .assertExists()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun prefetchingStickyHeaderItem() {
        rule.setContent {
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = 1,
                initialFirstVisibleItemScrollOffset = itemsSizePx / 2
            )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
            ) {
                stickyHeader {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemsSizeDp)
                            .then(fillParentMaxCrossAxis())
                            .testTag("header")
                    )
                }
                items(100) {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemsSizeDp)
                            .then(fillParentMaxCrossAxis())
                            .testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        rule.onNodeWithTag("header")
            .assertIsDisplayed()
        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    @Test
    fun disposingWhilePrefetchingScheduled() {
        var emit = true
        lateinit var remeasure: Remeasurement
        rule.setContent {
            SubcomposeLayout(
                modifier = object : RemeasurementModifier {
                    override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                        remeasure = remeasurement
                    }
                }
            ) { constraints ->
                val placeable = if (emit) {
                    subcompose(Unit) {
                        state = rememberLazyListState()
                        LazyColumnOrRow(
                            Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                            state,
                        ) {
                            items(1000) {
                                Spacer(
                                    Modifier
                                        .mainAxisSize(itemsSizeDp)
                                        .then(fillParentMaxCrossAxis())
                                )
                            }
                        }
                    }.first().measure(constraints)
                } else {
                    null
                }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            // this will schedule the prefetching
            runBlocking(AutoTestFrameClock()) {
                state.scrollBy(itemsSizePx.toFloat())
            }
            // then we synchronously dispose LazyColumn
            emit = false
            remeasure.forceRemeasure()
        }

        rule.runOnIdle { }
    }

    @Test
    fun snappingToOtherPositionWhilePrefetchIsScheduled() {
        val composedItems = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
            ) {
                items(1000) {
                    composedItems.add(it)
                    Spacer(
                        Modifier
                            .mainAxisSize(itemsSizeDp)
                            .then(fillParentMaxCrossAxis())
                    )
                }
            }
        }

        rule.runOnIdle {
            // now we have items 0 and 1 visible
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 1 and 2 are visible
                // and schedule a prefetching for 3
                state.scrollBy(itemsSizePx.toFloat())
                // then we move so that items 100 and 101 are visible.
                // this should cancel the prefetch for 3
                state.scrollToItem(100)
            }
        }

        // wait a few frames to make sure prefetch happens if was scheduled
        rule.waitForIdle()
        rule.waitForIdle()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(composedItems).doesNotContain(3)
        }
    }

    @Test
    fun scrollingByListSizeCancelsPreviousPrefetch() {
        composeList()

        // now we have items 0-1 visible
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 1-2 are visible
                // and schedule a prefetching for 3
                state.scrollBy(itemsSizePx.toFloat())

                // move viewport by screen size to items 4-5, so item 3 is just behind
                // the first visible item
                state.scrollBy(itemsSizePx * 3f)

                // move scroll further to items 5-6, so item 3 is reused
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch(7)

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure item 3 was dropped
                state.scrollBy(itemsSizePx * 100f)
            }
        }

        rule.runOnIdle {
            assertThat(activeNodes).doesNotContain(3)
        }
    }

    private fun waitForPrefetch(index: Int) {
        rule.waitUntil {
            activeNodes.contains(index) && activeMeasuredNodes.contains(index)
        }
    }

    private val activeNodes = mutableSetOf<Int>()
    private val activeMeasuredNodes = mutableSetOf<Int>()

    private fun composeList(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        rule.setContent {
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = firstItem,
                initialFirstVisibleItemScrollOffset = itemOffset
            )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                beyondBoundsItemCount = config.beyondBoundsItemCount
            ) {
                items(100) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose {
                            activeNodes.remove(it)
                            activeMeasuredNodes.remove(it)
                        }
                    }
                    Spacer(
                        Modifier
                            .mainAxisSize(itemsSizeDp)
                            .fillMaxCrossAxis()
                            .testTag("$it")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                activeMeasuredNodes.add(it)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
    }
}
