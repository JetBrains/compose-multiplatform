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

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(Parameterized::class)
class LazyScrollTest(private val orientation: Orientation) {
    @get:Rule
    val rule = createComposeRule()

    private val vertical: Boolean
        get() = orientation == Orientation.Vertical

    private val itemsCount = 20
    private lateinit var state: LazyListState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified
    private var containerSizeDp = Dp.Unspecified

    lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            containerSizeDp = itemSizeDp * 3
        }
        rule.setContent {
            state = rememberLazyListState()
            scope = rememberCoroutineScope()
            TestContent()
        }
    }

    @Test
    fun setupWorks() {
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItemWithOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(3, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithNegativeOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(3, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        val item3Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 3 }.offset
        assertThat(item3Offset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithPositiveOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount - 3, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 3)
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
            state.scrollToItem(itemsCount + 2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 3)
    }

    @Test
    fun animateScrollBy() = runBlocking {
        val scrollDistance = 320

        val expectedIndex = scrollDistance / itemSizePx // resolves to 3
        val expectedOffset = scrollDistance % itemSizePx // resolves to 20px

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun animateScrollToItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(5, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(5)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(3, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithNegativeOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(3, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        val item3Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 3 }.offset
        assertThat(item3Offset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithPositiveOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount - 3, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not 10
    }

    @Test
    fun animateScrollToItemWithNegativeOffsetLargerThanAvailableSize() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(1, -(itemSizePx + 10))
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not -10
    }

    @Test
    fun animateScrollToItemWithIndexLargerThanItemsCount() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount + 2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 3)
    }

    @Test
    fun animatePerFrameForwardToVisibleItem() {
        assertSpringAnimation(toIndex = 2)
    }

    @Test
    fun animatePerFrameForwardToVisibleItemWithOffset() {
        assertSpringAnimation(toIndex = 2, toOffset = 35)
    }

    @Test
    fun animatePerFrameForwardToNotVisibleItem() {
        assertSpringAnimation(toIndex = 8)
    }

    @Test
    fun animatePerFrameForwardToNotVisibleItemWithOffset() {
        assertSpringAnimation(toIndex = 10, toOffset = 35)
    }

    @Test
    fun animatePerFrameBackward() {
        assertSpringAnimation(toIndex = 1, fromIndex = 6)
    }

    @Test
    fun animatePerFrameBackwardWithOffset() {
        assertSpringAnimation(toIndex = 1, fromIndex = 5, fromOffset = 58)
    }

    @Test
    fun animatePerFrameBackwardWithInitialOffset() {
        assertSpringAnimation(toIndex = 0, toOffset = 20, fromIndex = 8)
    }

    private fun assertSpringAnimation(
        toIndex: Int,
        toOffset: Int = 0,
        fromIndex: Int = 0,
        fromOffset: Int = 0
    ) {
        if (fromIndex != 0 || fromOffset != 0) {
            rule.runOnIdle {
                runBlocking {
                    state.scrollToItem(fromIndex, fromOffset)
                }
            }
        }
        rule.waitForIdle()

        assertThat(state.firstVisibleItemIndex).isEqualTo(fromIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(fromOffset)

        rule.mainClock.autoAdvance = false

        scope.launch {
            state.animateScrollToItem(toIndex, toOffset)
        }

        while (!state.isScrollInProgress) {
            Thread.sleep(5)
        }

        val startOffset = (fromIndex * itemSizePx + fromOffset).toFloat()
        val endOffset = (toIndex * itemSizePx + toOffset).toFloat()
        val spec = FloatSpringSpec()

        val duration =
            TimeUnit.NANOSECONDS.toMillis(spec.getDurationNanos(startOffset, endOffset, 0f))
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step FrameDuration) {
            val nanosTime = TimeUnit.MILLISECONDS.toNanos(i)
            val expectedValue =
                spec.getValueFromNanos(nanosTime, startOffset, endOffset, 0f)
            val actualValue =
                (state.firstVisibleItemIndex * itemSizePx + state.firstVisibleItemScrollOffset)
            assertWithMessage(
                "On animation frame at $i index=${state.firstVisibleItemIndex} " +
                    "offset=${state.firstVisibleItemScrollOffset} expectedValue=$expectedValue"
            ).that(actualValue).isEqualTo(expectedValue.roundToInt(), tolerance = 1)

            rule.mainClock.advanceTimeBy(FrameDuration)
            expectedTime += FrameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            rule.waitForIdle()
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(toIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(toOffset)
    }

    @Composable
    private fun TestContent() {
        if (vertical) {
            LazyColumn(Modifier.height(containerSizeDp), state) {
                items(itemsCount) {
                    ItemContent()
                }
            }
        } else {
            LazyRow(Modifier.width(containerSizeDp), state) {
                items(itemsCount) {
                    ItemContent()
                }
            }
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

private val FrameDuration = 16L
