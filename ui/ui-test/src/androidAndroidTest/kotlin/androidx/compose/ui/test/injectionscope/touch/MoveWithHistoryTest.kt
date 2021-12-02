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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class MoveWithHistoryTest {
    companion object {
        private const val tag = "widget"
    }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = SinglePointerInputRecorder()

    @Composable
    fun Ui(alignment: Alignment) {
        Box(Modifier.fillMaxSize().wrapContentSize(alignment)) {
            ClickableTestBox(modifier = recorder, tag = tag)
        }
    }

    @Test
    fun flingScrollableWithHistorical() {
        val scrollState = ScrollState(initial = 0)

        flingScrollableImpl(scrollState, useHistoricalEvents = true)

        // Scrolled a large amount after the fling animation (caused by historical events) ends.
        Truth.assertThat(scrollState.value).isGreaterThan(500)
    }

    @Test
    fun flingScrollableWithoutHistorical() {
        val scrollState = ScrollState(initial = 0)

        flingScrollableImpl(scrollState, useHistoricalEvents = false)

        // We expect only finger-distance scrolling for this sequence without historical events.
        // This test is intended as a verification that the flingScrollableWithHistorical
        // is actually testing historical events plumbing instead of accidentally passing
        // due to fling velocity tracker implementation details.
        Truth.assertThat(scrollState.value).isLessThan(101)
    }

    fun flingScrollableImpl(scrollState: ScrollState, useHistoricalEvents: Boolean) {
        val touchSlop = 18f
        rule.setContent {
            WithTouchSlop(touchSlop) {
                with(LocalDensity.current) {
                    // Scrollable with a viewport the size of 10 boxes
                    Column(
                        Modifier
                            .testTag("scrollable")
                            .requiredSize(100.toDp(), 1000.toDp())
                            .verticalScroll(scrollState)
                    ) {
                        repeat(100) {
                            ClickableTestBox()
                        }
                    }
                }
            }
        }

        Truth.assertThat(scrollState.value).isEqualTo(0)
        // numBoxes * boxHeight - viewportHeight = 100 * 100 - 1000
        Truth.assertThat(scrollState.maxValue).isEqualTo(9000)

        val swipeDistance = 20f - touchSlop
        rule.onNodeWithTag("scrollable").performTouchInput {
            // Simulate a rapid back and forth gesture which only moves 20px in 100ms if you
            // only compare beginning and end, but much faster (70px in 16ms) when the intermediate
            // historical events are included.
            val from = topCenter + Offset(0f, 120f)
            val to = topCenter + Offset(0f, 100f)

            val historicalTimes = listOf(-16L, -8L)
            val historicalCoordinates = listOf(to + Offset(0f, 70f), to + Offset(0f, 35f))
            val delayMillis = 100L

            down(from)
            updatePointerTo(0, to)
            if (useHistoricalEvents) {
                @OptIn(ExperimentalTestApi::class)
                moveWithHistory(historicalTimes, historicalCoordinates, delayMillis)
            } else {
                move(delayMillis)
            }
            up()
        }
        // Equal to swipe distance before fling animation starts
        Truth.assertThat(scrollState.value).isEqualTo(swipeDistance.roundToInt())

        rule.waitForIdle()
    }
}