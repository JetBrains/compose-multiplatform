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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.scrollBy
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridTest(
    private val orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    private val LazyStaggeredGridTag = "LazyStaggeredGridTag"

    internal lateinit var state: LazyStaggeredGridState

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 50

    @Before
    fun setUp() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @After
    fun tearDown() {
        if (::state.isInitialized) {
            var isSorted = true
            var previousIndex = Int.MIN_VALUE
            for (item in state.layoutInfo.visibleItemsInfo) {
                if (previousIndex > item.index) {
                    isSorted = false
                    break
                }
                previousIndex = item.index
            }
            assertTrue(
                "Visible items MUST BE sorted: ${state.layoutInfo.visibleItemsInfo}",
                isSorted
            )
        }
    }

    @Test
    fun showsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
            ) {
                item {
                    Spacer(
                        Modifier
                            .size(itemSizeDp)
                            .testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun distributesSingleLine() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
                modifier = Modifier.crossAxisSize(itemSizeDp * 3),
            ) {
                items(3) {
                    Spacer(
                        Modifier
                            .size(itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
    }

    @Test
    fun distributesTwoLines() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
                modifier = Modifier.crossAxisSize(itemSizeDp * 3),
            ) {
                items(6) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it + 1)
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item, 0, 0]
        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item, item x 2, 0]
        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item, item x 2, item x 3]
        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item x 4, item x 2, item x 3]
        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item x 4, item x 7, item x 3]
        rule.onNodeWithTag("5")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 3)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item x 4, item x 7, item x 9]
    }

    @Test
    fun moreItemsDisplayedOnScroll() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it + 1)
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        state.scrollBy(itemSizeDp * 3)

        // [item, item x 2, item x 3]
        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item x 4, item x 2, item x 3]
        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item x 4, item x 7, item x 3]
        rule.onNodeWithTag("5")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item x 4, item x 7, item x 9]
    }

    @Test
    fun itemsAreHiddenOnScroll() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it + 1)
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()

        state.scrollBy(itemSizeDp * 3)

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()
    }

    @Test
    fun itemsArePresentedWhenScrollingBack() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it + 1)
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()

        state.scrollBy(itemSizeDp * 3)
        state.scrollBy(-itemSizeDp * 3)

        for (i in 0..2) {
            rule.onNodeWithTag("$i")
                .assertIsDisplayed()
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun itemsAreCorrectedWhenSizeIncreased() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp * 2
                ),
            ) {
                item {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp
                            )
                            .testTag("${it + 1}")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)

        state.scrollBy(itemSizeDp * 3)

        expanded = true

        state.scrollBy(-itemSizeDp * 3)

        rule.onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
    }

    @Test
    fun itemsAreCorrectedWhenSizeDecreased() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp * 2
                ),
            ) {
                item {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp
                            )
                            .testTag("${it + 1}")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)

        state.scrollBy(itemSizeDp * 3)

        expanded = false

        state.scrollBy(-itemSizeDp * 3)

        rule.onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
    }

    @Test
    fun itemsAreCorrectedWithWrongColumns() {
        rule.setContent {
            // intentionally wrong values, normally items should be [0, 1][2, 3][4, 5]
            state = rememberLazyStaggeredGridState(
                firstVisibleItemIndex = 3,
                firstVisibleItemOffset = itemSizePx / 2
            )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp
                ),
            ) {
                items(6) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp / 2)

        rule.onNodeWithTag("4")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp / 2)

        state.scrollBy(-itemSizeDp * 3)

        rule.onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun addItems() {
        val state = LazyStaggeredGridState()
        var itemsCount by mutableStateOf(1)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp
                ),
            ) {
                items(itemsCount) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        itemsCount = 10

        rule.waitForIdle()

        state.scrollBy(itemSizeDp * 10)

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()

        itemsCount = 20

        rule.waitForIdle()

        state.scrollBy(itemSizeDp * 10)

        rule.onNodeWithTag("18")
            .assertIsDisplayed()

        rule.onNodeWithTag("19")
            .assertIsDisplayed()
    }

    @Test
    fun removeItems() {
        var itemsCount by mutableStateOf(20)
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp
                ),
            ) {
                items(itemsCount) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        state.scrollBy(itemSizeDp * 20)

        rule.onNodeWithTag("18")
            .assertIsDisplayed()

        rule.onNodeWithTag("19")
            .assertIsDisplayed()

        itemsCount = 10

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()

        itemsCount = 1

        rule.onNodeWithTag("0")
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            // seems like reuse keeps the node around?
            .assertIsNotDisplayed()
    }

    @Test
    fun resizingItems_maintainsScrollingRange() {
        val state = LazyStaggeredGridState()
        var itemSizes by mutableStateOf(
            List(20) {
                itemSizeDp * (it % 4 + 1)
            }
        )
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier
                    .axisSize(
                        crossAxis = itemSizeDp * 2,
                        mainAxis = itemSizeDp * 5
                    )
                    .testTag(LazyStaggeredGridTag)
                    .border(1.dp, Color.Red),
            ) {
                items(20) {
                    Box(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizes[it]
                            )
                            .testTag("$it")
                            .border(1.dp, Color.Black)
                    ) {
                        BasicText("$it")
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag)
            .scrollMainAxisBy(itemSizeDp * 20)

        rule.onNodeWithTag("18")
            .assertMainAxisSizeIsEqualTo(itemSizes[18])

        rule.onNodeWithTag("19")
            .assertMainAxisSizeIsEqualTo(itemSizes[19])

        itemSizes = itemSizes.reversed()

        rule.onNodeWithTag("18")
            .assertIsDisplayed()

        rule.onNodeWithTag("19")
            .assertIsDisplayed()

        rule.onNodeWithTag(LazyStaggeredGridTag)
            .scrollMainAxisBy(-itemSizeDp * 20)

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun removingItems_maintainsCorrectOffsets() {
        var itemCount by mutableStateOf(20)
        rule.setContent {
            state = rememberLazyStaggeredGridState(
                firstVisibleItemIndex = 10,
                firstVisibleItemOffset = 0
            )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier
                    .axisSize(
                        crossAxis = itemSizeDp * 2,
                        mainAxis = itemSizeDp * 5
                    )
                    .testTag(LazyStaggeredGridTag)
                    .border(1.dp, Color.Red),
            ) {
                items(itemCount) {
                    Box(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it % 3 + 1)
                            )
                            .testTag("$it")
                            .border(1.dp, Color.Black)
                    ) {
                        BasicText("$it")
                    }
                }
            }
        }

        itemCount = 3

        rule.waitForIdle()

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun staggeredGrid_supportsLargeIndices() {
        rule.setContent {
            state = rememberLazyStaggeredGridState(
                firstVisibleItemIndex = Int.MAX_VALUE / 2,
                firstVisibleItemOffset = 0
            )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier
                    .axisSize(
                        crossAxis = itemSizeDp * 2,
                        mainAxis = itemSizeDp * 5
                    )
                    .testTag(LazyStaggeredGridTag)
                    .border(1.dp, Color.Red),
            ) {
                items(Int.MAX_VALUE) {
                    Spacer(
                        Modifier
                            .axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it % 3 + 1)
                            )
                            .testTag("$it")
                            .border(1.dp, Color.Black)
                    )
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE / 2}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE / 2 + 1}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        // check that scrolling back and forth doesn't crash
        rule.onNodeWithTag(LazyStaggeredGridTag)
            .scrollMainAxisBy(10000.dp)

        rule.onNodeWithTag(LazyStaggeredGridTag)
            .scrollMainAxisBy(-10000.dp)
    }

    @Test
    fun scrollPositionIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        var state: LazyStaggeredGridState?

        restorationTester.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state!!,
                modifier = Modifier
                    .mainAxisSize(itemSizeDp * 10)
                    .testTag(LazyStaggeredGridTag)
            ) {
                items(1000) {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag)
            .scrollMainAxisBy(itemSizeDp * 10f)

        rule.onNodeWithTag("30")
            .assertIsDisplayed()

        state = null
        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNodeWithTag("30")
            .assertIsDisplayed()
    }

    @Test
    fun restoredScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        val restorationTester = StateRestorationTester(rule)

        var itemsCount = 100
        val recomposeCounter = mutableStateOf(0)

        restorationTester.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier
                    .mainAxisSize(itemSizeDp * 10)
                    .testTag(LazyStaggeredGridTag)
            ) {
                recomposeCounter.value // read state to force recomposition

                items(itemsCount) {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(9, 10)
            }
            itemsCount = 0
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            itemsCount = 100
            recomposeCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun scrollingALot_layoutIsNotRecomposed() {
        var recomposed = 0
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier
                    .mainAxisSize(itemSizeDp * 10)
                    .composed {
                        recomposed++
                        Modifier
                    }
            ) {
                items(1000) {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(recomposed).isEqualTo(1)

        state.scrollBy(1000.dp)

        rule.waitForIdle()
        assertThat(recomposed).isEqualTo(1)
    }

    @Test
    fun onlyOneInitialMeasurePass() {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier
                    .mainAxisSize(itemSizeDp * 10)
                    .composed {
                        Modifier
                    }
            ) {
                items(1000) {
                    Spacer(
                        Modifier
                            .mainAxisSize(itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(state.measurePassCount).isEqualTo(1)
    }
}