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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
class LazyStaggeredGridContentPaddingTest(
    orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )

        private const val LazyStaggeredGrid = "Lazy"
    }

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 100

    private lateinit var state: LazyStaggeredGridState

    @Before
    fun setUp() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @Test
    fun contentPadding_isApplied() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSizeDp * 4, itemSizeDp * 5)
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = itemSizeDp * 2,
                    crossAxis = itemSizeDp
                )
            ) {
                items(4) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp)

        rule.onNodeWithTag(LazyStaggeredGrid)
            .scrollMainAxisBy(itemSizeDp * 100)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp)
    }

    @Test
    fun contentPadding_scrollPosition_setAndReportedCorrectly() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 10)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSizeDp * 4, itemSizeDp * 5)
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = itemSizeDp * 2,
                    crossAxis = itemSizeDp
                ),
                state = state
            ) {
                items(100) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        // offset by content padding
        rule.onNodeWithTag("10")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("11")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        state.scrollBy(itemSizeDp * 2)

        rule.onNodeWithTag("10")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        rule.onNodeWithTag("11")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // offset (reverse) by content padding
        assertThat(state.firstVisibleItemIndex).isEqualTo(14)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun contentPadding_reportedInLayoutInfo() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 10)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSizeDp * 4, itemSizeDp * 5)
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = itemSizeDp * 2,
                    crossAxis = itemSizeDp
                ),
                state = state
            ) {
                items(100) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(itemSizePx * 2)
        assertThat(state.layoutInfo.afterContentPadding).isEqualTo(itemSizePx * 2)
        // -beforeContentPadding
        assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-itemSizePx * 2)
        // layoutSize - beforeContentPadding
        assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(itemSizePx * 3)
    }

    @Test
    fun contentPadding_itemsAreDisplayedInPaddingArea() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSizeDp * 4, itemSizeDp * 5)
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = itemSizeDp * 2,
                    crossAxis = itemSizeDp
                ),
                state = state
            ) {
                items(10) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        state.scrollBy(itemSizeDp * 2.5f)

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * -0.5f)

        state.scrollBy(itemSizeDp * 10)

        rule.onNodeWithTag("9")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        state.scrollBy(itemSizeDp * (-2.5f))

        rule.onNodeWithTag("9")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 4.5f)
    }

    @Test
    fun contentPadding_largePadding_itemsAreDisplayedCorrectly() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSizeDp * 2, itemSizeDp * 5)
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = itemSizeDp * 6
                ),
                state = state
            ) {
                items(10) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 6)

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        state.scrollBy(itemSizeDp * 20)

        rule.onNodeWithTag("8")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * -2f)

        assertThat(state.firstVisibleItemIndex).isEqualTo(8)
        // normally this item is invisible, but being the last item in the lane, it is forced to stay
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx * 8)
    }

    @Test
    fun contentPadding_zeroItems() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGrid),
                contentPadding = PaddingValues(
                    mainAxis = 10.dp,
                    crossAxis = 2.dp
                ),
                state = state
            ) {
                items(0) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGrid)
            .assertMainAxisSizeIsEqualTo(20.dp)
            .assertCrossAxisSizeIsEqualTo(4.dp)
    }

    @Test
    fun contentPadding_zeroHeightItem() {
        state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
        rule.setContent {
            Box(Modifier.crossAxisSize(itemSizeDp * 2)) {
                LazyStaggeredGrid(
                    lanes = 2,
                    modifier = Modifier
                        .testTag(LazyStaggeredGrid),
                    contentPadding = PaddingValues(
                        mainAxis = 10.dp,
                        crossAxis = 2.dp
                    ),
                    state = state
                ) {
                    items(2) {
                        Spacer(Modifier.mainAxisSize(0.dp).testTag("$it"))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGrid)
            .assertMainAxisSizeIsEqualTo(20.dp)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
    }
}