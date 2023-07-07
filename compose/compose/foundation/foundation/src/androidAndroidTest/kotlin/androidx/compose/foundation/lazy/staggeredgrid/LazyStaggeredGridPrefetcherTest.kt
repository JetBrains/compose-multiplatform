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

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyStaggeredGridPrefetcherTest(
    orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    internal lateinit var state: LazyStaggeredGridState

    @Test
    fun notPrefetchingForwardInitially() {
        composeStaggeredGrid()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeStaggeredGrid(firstItem = 4)

        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        composeStaggeredGrid()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(5)

        rule.onNodeWithTag("4")
            .assertExists()
        rule.onNodeWithTag("5")
            .assertExists()
        rule.onNodeWithTag("6")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        composeStaggeredGrid(firstItem = 4, itemOffset = 10)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(2)

        rule.onNodeWithTag("2")
            .assertExists()
        rule.onNodeWithTag("3")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        composeStaggeredGrid(firstItem = 4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(9)

        rule.onNodeWithTag("8")
            .assertExists()
        rule.onNodeWithTag("9")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(2)

        rule.onNodeWithTag("2")
            .assertExists()
        rule.onNodeWithTag("3")
            .assertExists()
        rule.onNodeWithTag("8")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composeStaggeredGrid()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemsSizePx / 2f)
                state.scrollBy(itemsSizePx / 2f)
            }
        }

        waitForPrefetch(6)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
        rule.onNodeWithTag("6")
            .assertExists()
        rule.onNodeWithTag("8")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composeStaggeredGrid(firstItem = 8)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
            }
        }

        waitForPrefetch(4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemsSizePx / 2f)
                state.scrollBy(-itemsSizePx / 2f)
            }
        }

        waitForPrefetch(2)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
        rule.onNodeWithTag("6")
            .assertIsDisplayed()
        rule.onNodeWithTag("2")
            .assertExists()
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingCancelledIfScrollDirectionChanged() {
        composeStaggeredGrid(firstItem = 8)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-5f)
                state.scrollBy(5f)
            }
        }

        waitForPrefetch(13)

        rule.onNodeWithTag("12")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("13")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("4")
            .assertDoesNotExist()
        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

//    @Test
//    fun prefetchingForwardAndBackwardReverseLayout() {
//        composeStaggeredGrid(firstItem = 2, reverseLayout = true)
//
//        rule.runOnIdle {
//            runBlocking {
//                state.scrollBy(5f)
//            }
//        }
//
//        waitForPrefetch(6)
//
//        rule.onNodeWithTag("6")
//            .assertExists()
//        rule.onNodeWithTag("7")
//            .assertExists()
//        rule.onNodeWithTag("0")
//            .assertDoesNotExist()
//        rule.onNodeWithTag("1")
//            .assertDoesNotExist()
//
//        rule.runOnIdle {
//            runBlocking {
//                state.scrollBy(-2f)
//                state.scrollBy(-1f)
//            }
//        }
//
//        waitForPrefetch(0)
//
//        rule.onNodeWithTag("0")
//            .assertExists()
//        rule.onNodeWithTag("1")
//            .assertExists()
//        rule.onNodeWithTag("6")
//            .assertDoesNotExist()
//        rule.onNodeWithTag("7")
//            .assertDoesNotExist()
//    }

//    @Test
//    fun prefetchingForwardAndBackwardWithContentPadding() {
//        val halfItemSize = itemsSizeDp / 2f
//        composeStaggeredGrid(
//            firstItem = 4,
//            itemOffset = 5,
//            contentPadding = PaddingValues(mainAxis = halfItemSize)
//        )
//
//        rule.onNodeWithTag("2")
//            .assertIsDisplayed()
//        rule.onNodeWithTag("4")
//            .assertIsDisplayed()
//        rule.onNodeWithTag("6")
//            .assertIsDisplayed()
//        rule.onNodeWithTag("0")
//            .assertDoesNotExist()
//        rule.onNodeWithTag("8")
//            .assertDoesNotExist()
//
//        rule.runOnIdle {
//            runBlocking {
//                state.scrollBy(5f)
//            }
//        }
//
//        waitForPrefetch(6)
//
//        rule.onNodeWithTag("8")
//            .assertExists()
//        rule.onNodeWithTag("0")
//            .assertDoesNotExist()
//
//        rule.runOnIdle {
//            runBlocking {
//                state.scrollBy(-2f)
//            }
//        }
//
//        waitForPrefetch(0)
//
//        rule.onNodeWithTag("0")
//            .assertExists()
//    }

    @Test
    fun disposingWhilePrefetchingScheduled() {
        var emit = true
        lateinit var remeasure: Remeasurement
        state = LazyStaggeredGridState()
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
                        LazyStaggeredGrid(
                            2,
                            Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                            state,
                        ) {
                            items(1000) {
                                Spacer(
                                    Modifier.mainAxisSize(itemsSizeDp)
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
            // then we synchronously dispose
            emit = false
            remeasure.forceRemeasure()
        }

        rule.runOnIdle { }
    }

    @Test
    fun snappingToOtherPositionWhilePrefetchIsScheduled() {
        val composedItems = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                1,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
            ) {
                items(1000) {
                    composedItems.add(it)
                    Spacer(Modifier.mainAxisSize(itemsSizeDp))
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
        composeStaggeredGrid()

        // now we have items 0-3 visible
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 2-5 are visible
                // and schedule a prefetching for 6-7
                state.scrollBy(itemsSizePx.toFloat())

                // move viewport by screen size to items 8-11, so item 6 is just behind
                // the first visible item
                state.scrollBy(itemsSizePx * 3f)

                // move scroll further to items 10-13, so item 6 is reused
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch(13)

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure item 6 was dropped
                state.scrollBy(itemsSizePx * 100f)
            }
        }

        rule.runOnIdle {
            assertThat(activeNodes).doesNotContain(6)
        }
    }

    @Test
    fun scrollingWithStaggeredItemsPrefetchesCorrectly() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 5f),
                state,
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
                            .mainAxisSize(if (it == 0) itemsSizeDp else itemsSizeDp * 2)
                            .border(Dp.Hairline, Color.Black)
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

        // ┌─┬─┐
        // ├─┤1│
        // │2├─┤
        // ├─┤3│
        // │4├─┤
        // └─┴─┘

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so item 6 is visible
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch(7)
        waitForPrefetch(8)

        // ┌─┬─┐
        // │2├─┤
        // ├─┤3│
        // │4├─┤
        // ├─┤5│
        // └─┴─┘

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so item 7 is visible
                state.scrollBy(itemsSizePx.toFloat())
            }
            assertThat(activeNodes).contains(8)
        }

        // ┌─┬─┐
        // ├─┤3│
        // │4├─┤
        // ├─┤5│
        // │6├─┤
        // └─┴─┘

        waitForPrefetch(9)
    }

    @Test
    fun fullSpanIsPrefetchedCorrectly() {
        val nodeConstraints = mutableMapOf<Int, Constraints>()
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 5f).crossAxisSize(itemsSizeDp * 2f),
                state,
            ) {
                items(
                    count = 100,
                    span = {
                        if (it % 10 == 0)
                            StaggeredGridItemSpan.FullLine
                        else
                            StaggeredGridItemSpan.SingleLane
                    }
                ) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose {
                            activeNodes.remove(it)
                            activeMeasuredNodes.remove(it)
                        }
                    }
                    Spacer(
                        Modifier
                            .border(Dp.Hairline, Color.Black)
                            .testTag("$it")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                activeMeasuredNodes.add(it)
                                nodeConstraints.put(it, constraints)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                            .mainAxisSize(if (it == 0) itemsSizeDp else itemsSizeDp * 2)
                    )
                }
            }
        }

        // ┌─┬─┐
        // │5├─┤
        // ├─┤6│
        // │7├─┤
        // ├─┤8│
        // └─┴─┘

        state.scrollBy(itemsSizeDp * 5f)
        assertThat(activeNodes).contains(9)

        waitForPrefetch(10)
        val expectedConstraints = if (vertical) {
            Constraints.fixedWidth(itemsSizePx * 2)
        } else {
            Constraints.fixedHeight(itemsSizePx * 2)
        }
        assertThat(nodeConstraints[10]).isEqualTo(expectedConstraints)
    }

    @Test
    fun fullSpanIsPrefetchedCorrectly_scrollingBack() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 5f),
                state,
            ) {
                items(
                    count = 100,
                    span = {
                        if (it % 10 == 0)
                            StaggeredGridItemSpan.FullLine
                        else
                            StaggeredGridItemSpan.SingleLane
                    }
                ) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose {
                            activeNodes.remove(it)
                            activeMeasuredNodes.remove(it)
                        }
                    }
                    Spacer(
                        Modifier
                            .mainAxisSize(if (it == 0) itemsSizeDp else itemsSizeDp * 2)
                            .border(Dp.Hairline, Color.Black)
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

        state.scrollBy(itemsSizeDp * 13f + 2.dp)
        rule.waitForIdle()

        // second scroll to make sure subcompose marks elements as /not/ active
        state.scrollBy(2.dp)
        rule.waitForIdle()

        // ┌──┬──┐
        // │11│12│
        // ├──┼──┤
        // │13│14│
        // ├──┼──┤
        // └──┴──┘

        assertThat(activeNodes).contains(11)
        assertThat(activeNodes).doesNotContain(10)

        state.scrollBy(-1.dp)

        waitForPrefetch(10)
    }

    private fun waitForPrefetch(index: Int) {
        rule.waitUntil {
            activeNodes.contains(index) && activeMeasuredNodes.contains(index)
        }
    }

    private val activeNodes = mutableSetOf<Int>()
    private val activeMeasuredNodes = mutableSetOf<Int>()

    private fun composeStaggeredGrid(
        firstItem: Int = 0,
        itemOffset: Int = 0,
    ) {
        state = LazyStaggeredGridState(
            initialFirstVisibleItemIndex = firstItem,
            initialFirstVisibleItemOffset = itemOffset
        )
        rule.setContent {
            LazyStaggeredGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
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
                            .border(Dp.Hairline, Color.Black)
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
