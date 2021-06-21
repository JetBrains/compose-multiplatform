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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
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
@RunWith(Parameterized::class)
class LazyListLayoutInfoTest(
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
            LazyColumn(
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
            LazyColumn(
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
            LazyColumn(
                state = rememberLazyListState().also { state = it },
                reverseLayout = reverseLayout,
                verticalArrangement = Arrangement.spacedBy(itemSizeDp),
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
            LazyColumn(
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
            LazyColumn(
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
            LazyColumn(
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
    fun viewportOffsetsAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumn(
                Modifier.requiredSize(sizeDp),
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
        }
    }

    @Test
    fun viewportOffsetsAreCorrectWithContentPadding() {
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
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumn(
                Modifier.requiredSize(sizeDp),
                contentPadding = PaddingValues(top = topPaddingDp, bottom = bottomPaddingDp),
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

@Stable
class StableRef<T>(var value: T)
