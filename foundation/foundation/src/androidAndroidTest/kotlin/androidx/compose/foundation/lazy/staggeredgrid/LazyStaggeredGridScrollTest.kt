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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridScrollTest(
    private val orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    internal lateinit var state: LazyStaggeredGridState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    @Before
    fun setUp() {
        itemSizeDp = with(rule.density) {
            itemSizePx.toDp()
        }
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            TestContent()
        }
        rule.waitForIdle()
    }

    @Test
    fun setupWorks() {
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
    }

    @Test
    fun scrollToItem_byIndexAndOffset_outsideBounds() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(10, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItem_byIndexAndOffset_inBounds() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(2, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(110)
    }

    @Test
    fun scrollToItem_byIndexAndOffset_inBounds_secondLane() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(4, 10)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItem_byIndexAndNegativeOffset() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(4, -10)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx * 2 - 10)
    }

    @Test
    fun scrollToItem_offsetLargerThanItem() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(10, itemSizePx * 2)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(13)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItem_beyondFirstItem() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(10)
            state.scrollToItem(0, -10)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItem_beyondLastItem() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(99, itemSizePx * 3)
        }

        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        assertThat(lastItem.index).isEqualTo(99)
        val mainAxisOffset = if (orientation == Orientation.Vertical) {
            lastItem.offset.y
        } else {
            lastItem.offset.x
        }
        assertThat(mainAxisOffset).isEqualTo(itemSizePx * 3) // x5 (grid) - x2 (item)
    }

    @Test
    fun scrollToItem_beyondItemCount() {
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(420)
        }

        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        assertThat(lastItem.index).isEqualTo(99)
        val mainAxisOffset = if (orientation == Orientation.Vertical) {
            lastItem.offset.y
        } else {
            lastItem.offset.x
        }
        assertThat(mainAxisOffset).isEqualTo(itemSizePx * 3) // x5 (grid) - x2 (item)
    }

    @Composable
    private fun TestContent() {
        // |-|-|
        // |0|1|
        // |-| |
        // |2| |
        // |-|-|
        // |3|4|
        // | |-|
        // | |5|
        // |-| |
        LazyStaggeredGrid(
            lanes = 2,
            state = state,
            modifier = Modifier.axisSize(itemSizeDp * 2, itemSizeDp * 5)
        ) {
            items(100) {
                BasicText(
                    "$it",
                    Modifier
                        .mainAxisSize(itemSizeDp * ((it % 2) + 1))
                        .testTag("$it")
                        .border(1.dp, Color.Black)
                )
            }
        }
    }
}