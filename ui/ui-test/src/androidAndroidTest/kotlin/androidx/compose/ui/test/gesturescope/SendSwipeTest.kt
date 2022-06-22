/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.test.gesturescope

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.expectError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.bottomCenter
import androidx.compose.ui.test.bottomRight
import androidx.compose.ui.test.centerX
import androidx.compose.ui.test.centerY
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.topCenter
import androidx.compose.ui.test.topLeft
import androidx.compose.ui.test.up
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.assertDecreasing
import androidx.compose.ui.test.util.assertIncreasing
import androidx.compose.ui.test.util.assertOnlyLastEventIsUp
import androidx.compose.ui.test.util.assertSame
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class SendSwipeTest {
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
    fun swipeUp() {
        rule.setContent { Ui(Alignment.TopStart) }
        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture { swipeUp() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsUp()
            }
        }
    }

    @Test
    fun swipeDown() {
        rule.setContent { Ui(Alignment.TopEnd) }
        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture { swipeDown() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsDown()
            }
        }
    }

    @Test
    fun swipeLeft() {
        rule.setContent { Ui(Alignment.BottomEnd) }
        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture { swipeLeft() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsLeft()
            }
        }
    }

    @Test
    fun swipeRight() {
        rule.setContent { Ui(Alignment.BottomStart) }
        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture { swipeRight() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsRight()
            }
        }
    }

    @Test
    fun swipeUp_withParameters() {
        rule.setContent { Ui(Alignment.TopStart) }
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performGesture { swipeUp(endY = centerY) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsUp()
            }
        }
    }

    @Test
    fun swipeDown_withParameters() {
        rule.setContent { Ui(Alignment.TopEnd) }
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performGesture { swipeDown(endY = centerY) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsDown()
            }
        }
    }

    @Test
    fun swipeLeft_withParameters() {
        rule.setContent { Ui(Alignment.BottomEnd) }
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performGesture { swipeLeft(endX = centerX) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsLeft()
            }
        }
    }

    @Test
    fun swipeRight_withParameters() {
        rule.setContent { Ui(Alignment.BottomStart) }
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performGesture { swipeRight(endX = centerX) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertSwipeIsRight()
            }
        }
    }

    @Test
    fun swipeUp_wrongParameters() {
        rule.setContent { Ui(Alignment.TopStart) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startY=0.0 needs to be greater than or equal to endY=1.0"
        ) {
            @Suppress("DEPRECATION")
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performGesture { swipeUp(startY = 0f, endY = 1f) }
        }
    }

    @Test
    fun swipeDown_wrongParameters() {
        rule.setContent { Ui(Alignment.TopEnd) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startY=1.0 needs to be less than or equal to endY=0.0"
        ) {
            @Suppress("DEPRECATION")
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performGesture { swipeDown(startY = 1f, endY = 0f) }
        }
    }

    @Test
    fun swipeLeft_wrongParameters() {
        rule.setContent { Ui(Alignment.BottomEnd) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startX=0.0 needs to be greater than or equal to endX=1.0"
        ) {
            @Suppress("DEPRECATION")
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performGesture { swipeLeft(startX = 0f, endX = 1f) }
        }
    }

    @Test
    fun swipeRight_wrongParameters() {
        rule.setContent { Ui(Alignment.BottomStart) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startX=1.0 needs to be less than or equal to endX=0.0"
        ) {
            @Suppress("DEPRECATION")
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performGesture { swipeRight(startX = 1f, endX = 0f) }
        }
    }

    @Test
    fun swipeShort() {
        rule.setContent { Ui(Alignment.Center) }
        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture { swipe(topLeft, bottomRight, 1) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()

                // DOWN, MOVE, UP
                assertThat(events.size).isEqualTo(3)

                // DOWN is in top left corner (0, 0)
                events[0].verify(null, null, true, Offset(0f, 0f), Touch, Press)

                val t = events[0].timestamp + 1
                val pointerId = events[0].id

                // MOVE is in bottom right corner (box is 100x100, so corner is (99, 99))
                events[1].verify(t, pointerId, true, Offset(99f, 99f), Touch, Move)
                // UP is also in bottom right corner
                events[2].verify(t, pointerId, false, Offset(99f, 99f), Touch, Release)
            }
        }
    }

    @Test
    fun swipeScrollable() {
        val touchSlop = 18f
        val scrollState = ScrollState(initial = 0)
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

        assertThat(scrollState.value).isEqualTo(0)
        // numBoxes * boxHeight - viewportHeight = 100 * 100 - 1000
        assertThat(scrollState.maxValue).isEqualTo(9000)

        val swipeDistance = 800f - touchSlop
        @Suppress("DEPRECATION")
        rule.onNodeWithTag("scrollable").performGesture {
            val from = bottomCenter - Offset(0f, 99f)
            val touchSlopThreshold = from - Offset(0f, touchSlop)
            val to = topCenter + Offset(0f, 100f)

            down(from)
            moveTo(touchSlopThreshold)
            moveTo(to)
            up()
        }

        assertThat(scrollState.value).isEqualTo(swipeDistance.roundToInt())
        assertThat(scrollState.maxValue).isEqualTo(9000)
    }

    private fun SinglePointerInputRecorder.assertSwipeIsUp() {
        // Must have at least two events to have a direction
        assertThat(events.size).isAtLeast(2)
        // Last event must be above first event
        assertThat(events.last().position.y).isLessThan(events.first().position.y)
        // All events in between only move up
        events.map { it.position.x }.assertSame(tolerance = 0.001f)
        events.map { it.position.y }.assertDecreasing()
    }

    private fun SinglePointerInputRecorder.assertSwipeIsDown() {
        // Must have at least two events to have a direction
        assertThat(events.size).isAtLeast(2)
        // Last event must be below first event
        assertThat(events.last().position.y).isGreaterThan(events.first().position.y)
        // All events in between only move down
        events.map { it.position.x }.assertSame(tolerance = 0.001f)
        events.map { it.position.y }.assertIncreasing()
    }

    private fun SinglePointerInputRecorder.assertSwipeIsLeft() {
        // Must have at least two events to have a direction
        assertThat(events.size).isAtLeast(2)
        // Last event must be to the left of first event
        assertThat(events.last().position.x).isLessThan(events.first().position.x)
        // All events in between only move to the left
        events.map { it.position.x }.assertDecreasing()
        events.map { it.position.y }.assertSame(tolerance = 0.001f)
    }

    private fun SinglePointerInputRecorder.assertSwipeIsRight() {
        // Must have at least two events to have a direction
        assertThat(events.size).isAtLeast(2)
        // Last event must be to the right of first event
        assertThat(events.last().position.x).isGreaterThan(events.first().position.x)
        // All events in between only move to the right
        events.map { it.position.x }.assertIncreasing()
        events.map { it.position.y }.assertSame(tolerance = 0.001f)
    }
}
