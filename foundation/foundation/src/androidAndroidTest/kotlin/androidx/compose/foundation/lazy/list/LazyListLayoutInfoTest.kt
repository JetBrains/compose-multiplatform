/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
class LazyListLayoutInfoTest(
    param: LayoutInfoTestParam
) : BaseLazyListTestWithOrientation(param.orientation) {
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

    private val reverseLayout = param.reverseLayout

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScroll() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(1, 10)
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1, startOffset = -10)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                spacedBy = itemSizeDp,
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 2, spacing = itemSizePx)
        }
    }

    @Composable
    fun ObservingFun(state: LazyListState, currentInfo: StableRef<LazyListLayoutInfo?>) {
        currentInfo.value = state.layoutInfo
    }
    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: LazyListState
        val currentInfo = StableRef<LazyListLayoutInfo?>(null)
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.scrollToItem(1, 0)
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: LazyListState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: LazyListLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            LazyColumnOrRow(
                reverseLayout = reverseLayout,
                state = rememberLazyListState().also { state = it }
            ) {
                item {
                    Box(Modifier.requiredSize(size))
                }
            }
            observingFun()
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, expectedSize = itemSizePx * 2)
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, expectedSize = itemSizePx)
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                reverseLayout = reverseLayout,
                state = rememberLazyListState().also { state = it }
            ) {
                items((0 until count).toList()) {
                    Box(Modifier.requiredSize(10.dp))
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
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                reverseLayout = reverseLayout,
                state = rememberLazyListState().also { state = it }
            ) {
                items((0..3).toList()) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
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
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                contentPadding = PaddingValues(
                    beforeContent = beforeContentPaddingDp,
                    afterContent = afterContentPaddingDp,
                    beforeContentCrossAxis = 2.dp,
                    afterContentCrossAxis = 2.dp
                ),
                reverseLayout = reverseLayout,
                state = rememberLazyListState().also { state = it }
            ) {
                items((0..3).toList()) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun emptyItemsInVisibleItemsInfo() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it }
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
        lateinit var state: LazyListState
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
            LazyColumnOrRow(
                Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                state = rememberLazyListState().also { state = it },
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
        lateinit var state: LazyListState
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
            LazyColumnOrRow(
                Modifier.mainAxisSize(sizeDp).crossAxisSize(sizeDp * 2),
                state = rememberLazyListState().also { state = it },
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
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.reverseLayout).isEqualTo(reverseLayout)
        }
    }

    @Test
    fun orientationIsCorrect() {
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumnOrRow(
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f)
            ) {
                items((0..5).toList()) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.orientation)
                .isEqualTo(if (vertical) Orientation.Vertical else Orientation.Horizontal)
        }
    }

    @Test
    fun viewportOffsetsSmallContentReverseArrangement() {
        val state = LazyListState()
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(itemSizeDp * 5),
                reverseLayout = reverseLayout,
                reverseArrangement = true,
                state = state
            ) {
                items(4) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            println(state.layoutInfo.visibleItemsInfo.map { it.offset })
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(itemSizePx * 5)
            state.layoutInfo.assertVisibleItems(count = 4, startOffset = itemSizePx)
        }
    }

    fun LazyListLayoutInfo.assertVisibleItems(
        count: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        expectedSize: Int = itemSizePx,
        spacing: Int = 0
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        var currentIndex = startIndex
        var currentOffset = startOffset
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of item $currentIndex").that(it.offset)
                .isEqualTo(currentOffset)
            assertThat(it.size).isEqualTo(expectedSize)
            currentIndex++
            currentOffset += it.size + spacing
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
