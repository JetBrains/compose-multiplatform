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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun showsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
            ) {
                item {
                    Spacer(
                        Modifier.size(itemSizeDp).testTag(itemTestTag)
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
                        Modifier.size(itemSizeDp).testTag("$it")
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp * (it + 1)
                        ).testTag("$it")
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
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp * (it + 1)
                        ).testTag("$it")
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
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp * (it + 1)
                        ).testTag("$it")
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
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp * (it + 1)
                        ).testTag("$it")
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
        val state = LazyStaggeredGridState()
        var expanded by mutableStateOf(false)
        rule.setContent {
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp
                        ).testTag("$it")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp
                        ).testTag("${it + 1}")
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
        val state = LazyStaggeredGridState()
        var expanded by mutableStateOf(true)
        rule.setContent {
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp
                        ).testTag("$it")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp
                        ).testTag("${it + 1}")
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
        val state = LazyStaggeredGridState(
            // intentionally wrong values, normally items should be [0, 1][2, 3][4, 5]
            initialFirstVisibleItems = intArrayOf(3, 4),
            initialFirstVisibleOffsets = intArrayOf(itemSizePx / 2, itemSizePx / 2)
        )
        rule.setContent {
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp
                        ).testTag("$it")
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp
                        ).testTag("$it")
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
        val state = LazyStaggeredGridState()
        var itemsCount by mutableStateOf(20)
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
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp
                        ).testTag("$it")
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
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp * 5
                ).testTag("lazy").border(1.dp, Color.Red),
            ) {
                items(20) {
                    Box(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizes[it]
                        ).testTag("$it").border(1.dp, Color.Black)
                    ) {
                        BasicText("$it")
                    }
                }
            }
        }

        rule.onNodeWithTag("lazy")
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

        rule.onNodeWithTag("lazy")
            .scrollMainAxisBy(-itemSizeDp * 20)

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun removingItems_maintainsCorrectOffsets() {
        val state = LazyStaggeredGridState(
            initialFirstVisibleItems = intArrayOf(10, 11),
            initialFirstVisibleOffsets = intArrayOf(0, 0)
        )
        var itemCount by mutableStateOf(20)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(
                    crossAxis = itemSizeDp * 2,
                    mainAxis = itemSizeDp * 5
                ).testTag("lazy").border(1.dp, Color.Red),
            ) {
                items(itemCount) {
                    Box(
                        Modifier.axisSize(
                            crossAxis = itemSizeDp,
                            mainAxis = itemSizeDp * (it % 3 + 1)
                        ).testTag("$it").border(1.dp, Color.Black)
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
    fun scrollingALot_layoutIsNotRecomposed() {
        val state = LazyStaggeredGridState()
        var recomposed = 0
        rule.setContent {
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
                        Modifier.mainAxisSize(itemSizeDp).testTag("$it")
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
        val state = LazyStaggeredGridState()
        rule.setContent {
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
                        Modifier.mainAxisSize(itemSizeDp).testTag("$it")
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(state.measurePassCount).isEqualTo(1)
    }
}