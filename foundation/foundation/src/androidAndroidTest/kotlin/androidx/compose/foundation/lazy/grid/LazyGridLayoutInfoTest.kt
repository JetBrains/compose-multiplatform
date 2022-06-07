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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.LayoutInfoTestParam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyGridLayoutInfoTest(
    param: LayoutInfoTestParam
) : BaseLazyGridTestWithOrientation(param.orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            LayoutInfoTestParam(Orientation.Vertical, false),
            LayoutInfoTestParam(Orientation.Vertical, true),
            LayoutInfoTestParam(Orientation.Horizontal, false),
            LayoutInfoTestParam(Orientation.Horizontal, true),
        )
    }
    private val isVertical = param.orientation == Orientation.Vertical
    private val reverseLayout = param.reverseLayout

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
            LazyGrid(
                cells = 2,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.axisSize(gridWidthDp, itemSizeDp * 3.5f),
            ) {
                items((0..11).toList()) {
                    Box(Modifier.mainAxisSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 8, cells = 2)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScroll() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 2,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.axisSize(gridWidthDp, itemSizeDp * 3.5f),
            ) {
                items((0..11).toList()) {
                    Box(Modifier.mainAxisSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2, 10)
            }
            state.layoutInfo
                .assertVisibleItems(count = 8, startIndex = 2, startOffset = -10, cells = 2)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 1,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                mainAxisSpacedBy = itemSizeDp,
                modifier = Modifier.axisSize(itemSizeDp, itemSizeDp * 3.5f),
            ) {
                items((0..11).toList()) {
                    Box(Modifier.mainAxisSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 2, spacing = itemSizePx, cells = 1)
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
            LazyGrid(
                cells = 2,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.axisSize(itemSizeDp * 2f, itemSizeDp * 3.5f),
            ) {
                items((0..11).toList()) {
                    Box(Modifier.mainAxisSize(itemSizeDp))
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
                .assertVisibleItems(count = 8, startIndex = 2, cells = 2)
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
            LazyGrid(
                cells = 1,
                modifier = Modifier.crossAxisSize(itemSizeDp),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
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
                expectedSize = if (isVertical) {
                    IntSize(itemSizePx, itemSizePx * 2)
                } else {
                    IntSize(itemSizePx * 2, itemSizePx)
               },
                cells = 1
            )
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(
                count = 1,
                expectedSize = IntSize(itemSizePx, itemSizePx),
                cells = 1
            )
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 2,
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
            ) {
                items((0 until count).toList()) {
                    Box(Modifier.mainAxisSize(10.dp))
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
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(sizeDp * 2, sizeDp),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
            ) {
                items((0..7).toList()) {
                    Box(Modifier.mainAxisSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (isVertical) {
                    IntSize(sizePx * 2, sizePx)
                } else {
                    IntSize(sizePx, sizePx * 2)
                }
            )
        }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrectWithContentPadding() {
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(sizeDp * 2, sizeDp),
                contentPadding = PaddingValues(
                    beforeContent = beforeContentPaddingDp,
                    afterContent = afterContentPaddingDp,
                    beforeContentCrossAxis = 2.dp,
                    afterContentCrossAxis = 2.dp
                ),
                reverseLayout = reverseLayout,
                state = rememberLazyGridState().also { state = it },
            ) {
                items((0..7).toList()) {
                    Box(Modifier.mainAxisSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (isVertical) {
                    IntSize(sizePx * 2, sizePx)
                } else {
                    IntSize(sizePx, sizePx * 2)
                }
            )
        }
    }

    @Test
    fun emptyItemsInVisibleItemsInfo() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 2,
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
    fun emptyContent() {
        lateinit var state: LazyGridState
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        rule.setContent {
            LazyGrid(
                cells = 1,
                modifier = Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(
                    beforeContent = beforeContentPaddingDp,
                    afterContent = afterContentPaddingDp
                )
            ) {
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun viewportIsLargerThenTheContent() {
        lateinit var state: LazyGridState
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        rule.setContent {
            LazyGrid(
                cells = 1,
                modifier = Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                contentPadding = PaddingValues(
                    beforeContent = beforeContentPaddingDp,
                    afterContent = afterContentPaddingDp
                )
            ) {
                item {
                    Box(Modifier.size(sizeDp / 2))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun reverseLayoutIsCorrect() {
        lateinit var state: LazyGridState
        rule.setContent {
            LazyGrid(
                cells = 2,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.width(gridWidthDp).height(itemSizeDp * 3.5f),
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
            LazyGrid(
                cells = 2,
                state = rememberLazyGridState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.axisSize(gridWidthDp, itemSizeDp * 3.5f),
            ) {
                items((0..11).toList()) {
                    Box(Modifier.mainAxisSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.orientation == Orientation.Vertical).isEqualTo(isVertical)
        }
    }

    @Test
    fun viewportOffsetsSmallContentReverseArrangement() {
        val state = LazyGridState()
        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.mainAxisSize(itemSizeDp * 5).crossAxisSize(itemSizeDp * 2),
                state = state,
                reverseLayout = reverseLayout,
                reverseArrangement = true
            ) {
                items(8) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(itemSizePx * 5)
            state.layoutInfo.assertVisibleItems(count = 8, cells = 2, startOffset = itemSizePx)
        }
    }

    fun LazyGridLayoutInfo.assertVisibleItems(
        count: Int,
        cells: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        expectedSize: IntSize = IntSize(itemSizePx, itemSizePx),
        spacing: Int = 0
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        if (count == 0) return

        assertThat(startIndex % cells).isEqualTo(0)
        assertThat(visibleItemsInfo.size % cells).isEqualTo(0)

        var currentIndex = startIndex
        var currentOffset = startOffset
        var currentLine = startIndex / cells
        var currentCell = 0
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of item $currentIndex")
                .that(if (isVertical) it.offset.y else it.offset.x)
                .isEqualTo(currentOffset)
            assertThat(it.size).isEqualTo(expectedSize)
            assertThat(if (isVertical) it.row else it.column)
                .isEqualTo(currentLine)
            assertThat(if (isVertical) it.column else it.row)
                .isEqualTo(currentCell)
            currentIndex++
            currentCell++
            if (currentCell == cells) {
                currentCell = 0
                ++currentLine
                currentOffset += spacing + if (isVertical) it.size.height else it.size.width
            }
        }
    }
}

class LayoutInfoTestParam(
    val orientation: Orientation,
    val reverseLayout: Boolean
) {
    override fun toString(): String {
        return "orientation=$orientation;reverseLayout=$reverseLayout"
    }
}

@Stable
class StableRef<T>(var value: T)
