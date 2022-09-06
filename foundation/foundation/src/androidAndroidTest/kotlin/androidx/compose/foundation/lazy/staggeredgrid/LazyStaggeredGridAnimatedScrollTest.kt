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

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.isEqualTo
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridAnimatedScrollTest(
    orientation: Orientation
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
    internal lateinit var scope: CoroutineScope

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    @Before
    fun setUp() {
        itemSizeDp = with(rule.density) {
            itemSizePx.toDp()
        }
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyStaggeredGridState()
            TestContent()
        }
        rule.waitForIdle()
    }

    @Test
    fun animateScrollBy() = runBlocking {
        val scrollDistance = 320

        val expectedIndex = scrollDistance * 2 / itemSizePx // resolves to 6
        val expectedOffset = scrollDistance % itemSizePx // resolves to 20px

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun animateScrollToItem_positiveOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItem_positiveOffset_largerThanItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 150)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(12)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(50)
    }

    @Test
    fun animateScrollToItem_negativeOffset() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(8)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx - 10)
    }

    @Test
    fun animateScrollToItem_beforeFirstItem() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(10)
            state.animateScrollToItem(0, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_afterLastItem() {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(100)
        }
        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(90)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_inBounds() {
        assertSpringAnimation(2)
    }

    @Test
    fun animateScrollToItem_inBounds_withOffset() {
        assertSpringAnimation(2, itemSizePx / 2)
    }

    @Test
    fun animateScrollToItem_outOfBounds() {
        assertSpringAnimation(10)
    }

    @Test
    fun animateScrollToItem_firstItem() {
        assertSpringAnimation(fromIndex = 10, fromOffset = 10, toIndex = 0)
    }

    @Test
    fun animateScrollToItem_firstItem_toOffset() {
        assertSpringAnimation(fromIndex = 10, fromOffset = 10, toIndex = 0, toOffset = 10)
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

        val startOffset = (fromIndex / 2 * itemSizePx + fromOffset).toFloat()
        val endOffset = (toIndex / 2 * itemSizePx + toOffset).toFloat()
        val spec = FloatSpringSpec()

        val duration =
            TimeUnit.NANOSECONDS.toMillis(spec.getDurationNanos(startOffset, endOffset, 0f))
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        val frameDuration = 16L
        for (i in 0..duration step frameDuration) {
            val nanosTime = TimeUnit.MILLISECONDS.toNanos(i)
            val expectedValue =
                spec.getValueFromNanos(nanosTime, startOffset, endOffset, 0f)
            val actualValue =
                (state.firstVisibleItemIndex / 2 * itemSizePx + state.firstVisibleItemScrollOffset)
            assertWithMessage(
                "On animation frame at $i index=${state.firstVisibleItemIndex} " +
                    "offset=${state.firstVisibleItemScrollOffset} expectedValue=$expectedValue"
            ).that(actualValue).isEqualTo(expectedValue.roundToInt(), tolerance = 1)

            rule.mainClock.advanceTimeBy(frameDuration)
            expectedTime += frameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            rule.waitForIdle()
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(toIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(toOffset)
    }

    @Composable
    private fun TestContent() {
        LazyStaggeredGrid(
            lanes = 2,
            state = state,
            modifier = Modifier.axisSize(itemSizeDp * 2, itemSizeDp * 5)
        ) {
            items(100) {
                BasicText(
                    "$it",
                    Modifier
                        .mainAxisSize(itemSizeDp)
                        .testTag("$it")
                        .border(1.dp, Color.Black)
                )
            }
        }
    }
}