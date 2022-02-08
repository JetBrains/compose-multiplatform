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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
// @RunWith(Parameterized::class)
@OptIn(ExperimentalFoundationApi::class)
class LazyScrollTest { // (private val orientation: Orientation)
    @get:Rule
    val rule = createComposeRule()

    private val vertical: Boolean
        get() = true // orientation == Orientation.Vertical

    private val itemsCount = 20
    private lateinit var state: LazyGridState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified
    private var containerSizeDp = Dp.Unspecified

    @Before
    fun setup() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            containerSizeDp = itemSizeDp * 3
        }
        rule.setContent {
            state = rememberLazyGridState()
            TestContent()
        }
    }

    @Test
    fun setupWorks() {
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
    }

    @Test
    fun scrollToItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(0)
            state.scrollToItem(3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItemWithOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithNegativeOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(6, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        val item6Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 6 }.offset.y
        assertThat(item6Offset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithPositiveOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount - 6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not 10
    }

    @Test
    fun scrollToItemWithNegativeOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(1, -(itemSizePx + 10))
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not -10
    }

    @Test
    fun scrollToItemWithIndexLargerThanItemsCount() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount + 4)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
    }

    @Test
    fun animateScrollBy() = runBlocking {
        val scrollDistance = 320

        val expectedLine = scrollDistance / itemSizePx // resolves to 3
        val expectedItem = expectedLine * 2 // resolves to 6
        val expectedOffset = scrollDistance % itemSizePx // resolves to 20px

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedItem)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun animateScrollToItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithNegativeOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(6, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        val item6Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 6 }.offset.y
        assertThat(item6Offset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithPositiveOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount - 6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not 10
    }

    @Test
    fun animateScrollToItemWithNegativeOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(2, -(itemSizePx + 10))
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not -10
    }

    @Test
    fun animateScrollToItemWithIndexLargerThanItemsCount() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount + 2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
    }

    @Composable
    private fun TestContent() {
        if (vertical) {
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.height(containerSizeDp), state) {
                items(itemsCount) {
                    ItemContent()
                }
            }
        } else {
            // LazyRow(Modifier.width(300.dp), state) {
            //     items(items) {
            //         ItemContent()
            //     }
            // }
        }
    }

    @Composable
    private fun ItemContent() {
        val modifier = if (vertical) {
            Modifier.height(itemSizeDp)
        } else {
            Modifier.width(itemSizeDp)
        }
        Spacer(modifier)
    }

    // companion object {
    //     @JvmStatic
    //     @Parameterized.Parameters(name = "{0}")
    //     fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    // }
}
