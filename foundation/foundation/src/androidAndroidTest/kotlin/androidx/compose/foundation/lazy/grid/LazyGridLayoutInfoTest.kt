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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@OptIn(ExperimentalFoundationApi::class)
@RunWith(Parameterized::class)
class LazyGridLayoutInfoTest(
    private val reverseLayout: Boolean
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "reverseLayout={0}")
        fun initParameters(): Array<Any> = arrayOf(false, true)
    }

    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var gridWidthPx: Int = itemSizePx * 2
    private var gridWidthDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            gridWidthDp = gridWidthPx.toDp()
        }
    }

    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.width(gridWidthDp).height(itemSizeDp * 3.5f),
                columns = GridCells.Fixed(2)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 8, itemsPerRow = 2)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScroll() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.width(gridWidthDp).height(itemSizeDp * 3.5f),
                columns = GridCells.Fixed(2)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2, 10)
            }
            state.layoutInfo
                .assertVisibleItems(count = 8, startIndex = 2, startOffset = -10, itemsPerRow = 2)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                verticalArrangement = Arrangement.spacedBy(itemSizeDp),
                modifier = Modifier.width(itemSizeDp).height(itemSizeDp * 3.5f),
                columns = GridCells.Fixed(1)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 2, spacing = itemSizePx, itemsPerRow = 1)
        }
    }

    @Composable
    fun ObservingFun(state: LazyGridState, currentInfo: StableRef<LazyGridLayoutInfo?>) {
        currentInfo.value = state.layoutInfo
    }
    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: LazyGridState
        val currentInfo = StableRef<LazyGridLayoutInfo?>(null)
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.size(itemSizeDp * 2f, itemSizeDp * 3.5f),
                columns = GridCells.Fixed(2)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.scrollToItem(2, 0)
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!
                .assertVisibleItems(count = 8, startIndex = 2, itemsPerRow = 2)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: LazyGridState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: LazyGridLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            LazyVerticalGrid(
                modifier = Modifier.width(itemSizeDp),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
                columns = GridCells.Fixed(1)
            ) {
                item {
                    Box(Modifier.size(size))
                }
            }
            observingFun()
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(
                count = 1,
                expectedSize = IntSize(itemSizePx, itemSizePx * 2),
                itemsPerRow = 1
            )
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(
                count = 1,
                expectedSize = IntSize(itemSizePx, itemSizePx),
                itemsPerRow = 1
            )
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
                columns = GridCells.Fixed(2)
            ) {
                items((0 until count).toList()) {
                    Box(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(10)
            count = 20
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(20)
        }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                modifier = Modifier.height(sizeDp).width(sizeDp * 2),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
                columns = GridCells.Fixed(2)
            ) {
                items((0..7).toList()) {
                    Box(Modifier.size(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(IntSize(sizePx * 2, sizePx))
        }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrectWithContentPadding() {
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val topPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val bottomPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                modifier = Modifier.height(sizeDp).width(sizeDp * 2),
                contentPadding = PaddingValues(
                    top = topPaddingDp,
                    bottom = bottomPaddingDp,
                    start = 2.dp,
                    end = 2.dp
                ),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
                columns = GridCells.Fixed(2)
            ) {
                items((0..7).toList()) {
                    Box(Modifier.size(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(IntSize(sizePx * 2, sizePx))
        }
    }

    @Test
    fun emptyItemsInVisibleItemsInfo() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = rememberLazyGridState().also { state = it }
            ) {
                item { Box(Modifier) }
                item { }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItemsInfo.size).isEqualTo(2)
            assertThat(state.layoutInfo.visibleItemsInfo.first().index).isEqualTo(0)
            assertThat(state.layoutInfo.visibleItemsInfo.last().index).isEqualTo(1)
        }
    }

    @Test
    fun reverseLayoutIsCorrect() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.width(gridWidthDp).height(itemSizeDp * 3.5f),
                columns = GridCells.Fixed(2)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.reverseLayout).isEqualTo(reverseLayout)
        }
    }

    @Test
    fun orientationIsCorrect() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyVerticalGrid(
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.width(gridWidthDp).height(itemSizeDp * 3.5f),
                columns = GridCells.Fixed(2)
            ) {
                items((0..11).toList()) {
                    Box(Modifier.size(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.orientation).isEqualTo(Orientation.Vertical)
        }
    }

    fun LazyGridLayoutInfo.assertVisibleItems(
        count: Int,
        itemsPerRow: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        expectedSize: IntSize = IntSize(itemSizePx, itemSizePx),
        spacing: Int = 0
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        if (count == 0) return

        assertThat(startIndex % itemsPerRow).isEqualTo(0)
        assertThat(visibleItemsInfo.size % itemsPerRow).isEqualTo(0)

        var currentIndex = startIndex
        var currentOffset = startOffset
        var currentRow = startIndex / itemsPerRow
        var currentColumn = 0
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of item $currentIndex").that(it.offset.y)
                .isEqualTo(currentOffset)
            assertThat(it.size).isEqualTo(expectedSize)
            assertThat(it.row).isEqualTo(currentRow)
            assertThat(it.column).isEqualTo(currentColumn)
            currentIndex++
            currentColumn++
            if (currentColumn == itemsPerRow) {
                currentColumn = 0
                ++currentRow
                currentOffset += it.size.height + spacing
            }
        }
    }
}

@Stable
class StableRef<T>(var value: T)
