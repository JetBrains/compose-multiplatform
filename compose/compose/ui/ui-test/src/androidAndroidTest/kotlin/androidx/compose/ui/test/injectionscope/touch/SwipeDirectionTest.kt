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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.assertDecreasing
import androidx.compose.ui.test.util.assertIncreasing
import androidx.compose.ui.test.util.assertOnlyLastEventIsUp
import androidx.compose.ui.test.util.assertSame
import androidx.compose.ui.test.util.assertSinglePointer
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.assertUpSameAsLastMove
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [TouchInjectionScope.swipeUp], [TouchInjectionScope.swipeLeft],
 * [TouchInjectionScope.swipeDown] and [TouchInjectionScope.swipeRight]
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SwipeDirectionTest {
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
        rule.onNodeWithTag(tag).performTouchInput { swipeUp() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsUp()
            }
        }
    }

    @Test
    fun swipeDown() {
        rule.setContent { Ui(Alignment.TopEnd) }
        rule.onNodeWithTag(tag).performTouchInput { swipeDown() }
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
        rule.onNodeWithTag(tag).performTouchInput { swipeLeft() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsLeft()
            }
        }
    }

    @Test
    fun swipeRight() {
        rule.setContent { Ui(Alignment.BottomStart) }
        rule.onNodeWithTag(tag).performTouchInput { swipeRight() }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsRight()
            }
        }
    }

    @Test
    fun swipeUp_withParameters() {
        rule.setContent { Ui(Alignment.TopStart) }
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performTouchInput { swipeUp(endY = centerY) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsUp()
            }
        }
    }

    @Test
    fun swipeDown_withParameters() {
        rule.setContent { Ui(Alignment.TopEnd) }
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performTouchInput { swipeDown(endY = centerY) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsDown()
            }
        }
    }

    @Test
    fun swipeLeft_withParameters() {
        rule.setContent { Ui(Alignment.BottomEnd) }
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performTouchInput { swipeLeft(endX = centerX) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
                assertSwipeIsLeft()
            }
        }
    }

    @Test
    fun swipeRight_withParameters() {
        rule.setContent { Ui(Alignment.BottomStart) }
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag).performTouchInput { swipeRight(endX = centerX) }
        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()
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
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performTouchInput { swipeUp(startY = 0f, endY = 1f) }
        }
    }

    @Test
    fun swipeDown_wrongParameters() {
        rule.setContent { Ui(Alignment.TopEnd) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startY=1.0 needs to be less than or equal to endY=0.0"
        ) {
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performTouchInput { swipeDown(startY = 1f, endY = 0f) }
        }
    }

    @Test
    fun swipeLeft_wrongParameters() {
        rule.setContent { Ui(Alignment.BottomEnd) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startX=0.0 needs to be greater than or equal to endX=1.0"
        ) {
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performTouchInput { swipeLeft(startX = 0f, endX = 1f) }
        }
    }

    @Test
    fun swipeRight_wrongParameters() {
        rule.setContent { Ui(Alignment.BottomStart) }
        expectError<IllegalArgumentException>(
            expectedMessage = "startX=1.0 needs to be less than or equal to endX=0.0"
        ) {
            @OptIn(ExperimentalTestApi::class)
            rule.onNodeWithTag(tag).performTouchInput { swipeRight(startX = 1f, endX = 0f) }
        }
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
