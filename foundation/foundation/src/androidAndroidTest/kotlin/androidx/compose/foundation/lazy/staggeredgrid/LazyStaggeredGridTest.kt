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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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