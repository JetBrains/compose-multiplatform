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

import android.os.SystemClock.sleep
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.injectionscope.touch.Common.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [TouchInjectionScope.moveTo] and [TouchInjectionScope.updatePointerTo] work
 */
@MediumTest
class MoveToTest() {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
        private val moveToPosition1 = Offset(11f, 11f)
        private val moveToPosition2 = Offset(21f, 21f)
    }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Test
    fun onePointer() {
        // When we inject a down event followed by a move event
        rule.performTouchInput { down(downPosition1) }
        sleep(20) // (with some time in between)
        rule.performTouchInput { moveTo(moveToPosition1) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 move event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                t += eventPeriodMillis
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1].getPointer(0).verify(t, pointerId, true, moveToPosition1, Touch, Move)
            }
        }
    }

    @Test
    fun twoPointers_separateMoveEvents() {
        // When we inject two down events followed by two move events
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { down(2, downPosition2) }
        rule.performTouchInput { moveTo(1, moveToPosition1) }
        rule.performTouchInput { moveTo(2, moveToPosition2) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and two move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(4)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += eventPeriodMillis
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, moveToPosition1, Touch, Move)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2, Touch, Move)

                t += eventPeriodMillis
                assertThat(events[3].pointerCount).isEqualTo(2)
                events[3].getPointer(0).verify(t, pointerId1, true, moveToPosition1, Touch, Move)
                events[3].getPointer(1).verify(t, pointerId2, true, moveToPosition2, Touch, Move)
            }
        }
    }

    @Test
    fun twoPointers_oneMoveEvent() {
        // When we inject two down events followed by one move events
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { down(2, downPosition2) }
        sleep(20) // (with some time in between)
        rule.performTouchInput { updatePointerTo(1, moveToPosition1) }
        rule.performTouchInput { updatePointerTo(2, moveToPosition2) }
        rule.performTouchInput { move() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and one move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(3)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += eventPeriodMillis
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, moveToPosition1, Touch, Move)
                events[2].getPointer(1).verify(t, pointerId2, true, moveToPosition2, Touch, Move)
            }
        }
    }

    @Test
    fun moveTo_withoutDown() {
        expectError<IllegalStateException> {
            rule.performTouchInput { moveTo(moveToPosition1) }
        }
    }

    @Test
    fun moveTo_wrongPointerId() {
        rule.performTouchInput { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            rule.performTouchInput { moveTo(2, moveToPosition1) }
        }
    }

    @Test
    fun moveTo_afterUp() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { up() }
        expectError<IllegalStateException> {
            rule.performTouchInput { moveTo(moveToPosition1) }
        }
    }

    @Test
    fun moveTo_afterCancel() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { cancel() }
        expectError<IllegalStateException> {
            rule.performTouchInput { moveTo(moveToPosition1) }
        }
    }

    @Test
    fun updatePointerTo_withoutDown() {
        expectError<IllegalStateException> {
            rule.performTouchInput { updatePointerTo(1, moveToPosition1) }
        }
    }

    @Test
    fun updatePointerTo_wrongPointerId() {
        rule.performTouchInput { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            rule.performTouchInput { updatePointerTo(2, moveToPosition1) }
        }
    }

    @Test
    fun updatePointerTo_afterUp() {
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { up(1) }
        expectError<IllegalStateException> {
            rule.performTouchInput { updatePointerTo(1, moveToPosition1) }
        }
    }

    @Test
    fun updatePointerTo_afterCancel() {
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { cancel() }
        expectError<IllegalStateException> {
            rule.performTouchInput { updatePointerTo(1, moveToPosition1) }
        }
    }
}
