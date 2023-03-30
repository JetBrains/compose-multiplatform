/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@RunWith(Parameterized::class)
class LazyStaggeredGridReverseLayoutTest(
    val orientation: Orientation,
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initOrientation(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    private val StaggeredGridTag = "LazyStaggeredGrid"

    private var itemSize: Dp = 0.dp
    private val itemSizePx = 100

    @Before
    fun before() {
        with(rule.density) {
            itemSize = itemSizePx.toDp()
        }
    }

    @Test
    fun twoElements_inOneItem_placedToTheEnd() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(itemSize * 2, itemSize * 4),
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.size(itemSize * 2).testTag("0"))
                    Box(Modifier.size(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 2)
            .assertMainAxisSizeIsEqualTo(itemSize * 2)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun items_positioned_AtTheEnd() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(itemSize * 2, itemSize * 4),
                reverseLayout = true
            ) {
                items(4) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun initialScrollPosition_IsZero() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(itemSize * 2, itemSize * 4),
                reverseLayout = true,
                state = state
            ) {
                items(4) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun scrollForwards_doesNothing() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(itemSize * 2, itemSize * 4).testTag(StaggeredGridTag),
                reverseLayout = true,
                state = state
            ) {
                items(4) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        //  scroll down and as the scrolling is reversed it shouldn't affect anything
        rule.onNodeWithTag(StaggeredGridTag)
            .scrollMainAxisBy(itemSize)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun scrollBackwards_halfItem() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSize * 2, itemSize * 2)
                    .testTag(StaggeredGridTag),
                reverseLayout = true,
                state = state
            ) {
                items(6) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(StaggeredGridTag)
            .scrollMainAxisBy(-itemSize * 0.5f)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        }

        val scrolledByDp = with(rule.density) { state.firstVisibleItemScrollOffset.toDp() }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize + scrolledByDp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize + scrolledByDp)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(scrolledByDp)

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(scrolledByDp)
    }

    @Test
    fun scrollBackwards_toTheEnd() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSize * 2, itemSize * 2)
                    .testTag(StaggeredGridTag),
                reverseLayout = true,
                state = state
            ) {
                items(6) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(StaggeredGridTag)
            .scrollMainAxisBy(-itemSize * 10)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag("4")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("5")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun rtl_itemsAreReversed() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyStaggeredGrid(
                    lanes = 2,
                    modifier = Modifier
                        .axisSize(itemSize * 2, itemSize * 2)
                        .testTag(StaggeredGridTag),
                    reverseLayout = true,
                    state = state
                ) {
                    items(6) {
                        Box(Modifier.size(itemSize).testTag("$it"))
                    }
                }
            }
        }

        if (vertical) {
            rule.onNodeWithTag("0")
                .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
            rule.onNodeWithTag("1")
                .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        } else {
            // double reverse horizontally
            rule.onNodeWithTag("0")
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            rule.onNodeWithTag("1")
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
        }
    }

    @Test
    fun rtl_scrollToTheEnd() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyStaggeredGrid(
                    lanes = 2,
                    modifier = Modifier
                        .axisSize(itemSize * 2, itemSize * 2)
                        .testTag(StaggeredGridTag),
                    reverseLayout = true,
                    state = state
                ) {
                    items(6) {
                        Box(Modifier.size(itemSize).testTag("$it"))
                    }
                }
            }
        }

        val scrollBy = itemSize * 10
        rule.onNodeWithTag(StaggeredGridTag)
            .scrollMainAxisBy(if (vertical) -scrollBy else scrollBy)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        if (vertical) {
            rule.onNodeWithTag("4")
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

            rule.onNodeWithTag("5")
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        } else {
            // double reverse from RTL
            rule.onNodeWithTag("4")
                .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

            rule.onNodeWithTag("5")
                .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
        }
    }

    @Test
    fun remeasure_onReverseLayoutChange() {
        val state = LazyStaggeredGridState()
        var reverseLayout by mutableStateOf(false)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSize * 2, itemSize * 2)
                    .testTag(StaggeredGridTag),
                reverseLayout = reverseLayout,
                state = state
            ) {
                items(6) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        reverseLayout = true
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun contentPadding_isNotReversed() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSize * 2, itemSize * 5)
                    .testTag(StaggeredGridTag),
                contentPadding =
                    PaddingValues(beforeContent = itemSize, afterContent = itemSize * 2),
                reverseLayout = true,
                state = state
            ) {
                items(6) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        // bottom padding applies instead of the top
        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun initialPosition_isPreserved() {
        val state = LazyStaggeredGridState(initialFirstVisibleItemOffset = itemSizePx / 2)
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .axisSize(itemSize * 2, itemSize * 2)
                    .testTag(StaggeredGridTag),
                reverseLayout = true,
                state = state
            ) {
                items(6) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx / 2)
        }

        // bottom padding applies instead of the top
        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 1.5f)
        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 1.5f)
    }
    // initial position
}