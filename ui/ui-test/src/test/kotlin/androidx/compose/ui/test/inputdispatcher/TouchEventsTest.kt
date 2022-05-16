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

package androidx.compose.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.assertNoTouchGestureInProgress
import androidx.compose.ui.test.util.verifyTouchEvent
import androidx.compose.ui.test.util.verifyTouchPointer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests if [AndroidInputDispatcher.enqueueTouchDown] and friends work.
 */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class TouchEventsTest : InputDispatcherTest() {
    companion object {
        // Pointer ids
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33
        private const val pointer4 = 44

        // Positions, mostly used with corresponding pointerId:
        // pointerX with positionX or positionX_Y
        private val position1 = Offset(1f, 1f)
        private val position2 = Offset(2f, 2f)
        private val position3 = Offset(3f, 3f)
        private val position4 = Offset(4f, 4f)

        private val position1_1 = Offset(11f, 11f)
        private val position2_1 = Offset(21f, 21f)
        private val position3_1 = Offset(31f, 31f)

        private val position1_2 = Offset(12f, 12f)
        private val position2_2 = Offset(22f, 22f)

        private val position1_3 = Offset(13f, 13f)
    }

    @Test
    fun onePointer_down() {
        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.flush()

        val t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(1)
        recorder.events[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyTouchPointer(pointer1, position1)
    }

    @Test
    fun onePointer_downUp() {
        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.generateTouchUpAndCheck(pointer1)
        subject.assertNoTouchGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1)

            this[1].verifyTouchEvent(1, ACTION_UP, 0, t) // pointer1
            this[1].verifyTouchPointer(pointer1, position1)
        }
    }

    @Test
    fun onePointer_downDelayUp() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchUpAndCheck(pointer1, 2 * eventPeriodMillis)
        subject.assertNoTouchGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            t += 2 * eventPeriodMillis
            this[1].verifyTouchEvent(1, ACTION_UP, 0, t) // pointer1
            this[1].verifyTouchPointer(pointer1, position1_1)
        }
    }

    @Test
    fun onePointer_downUpdateMove() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.flush()

        var t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(2)
        recorder.events[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyTouchPointer(pointer1, position1_1)

        t += eventPeriodMillis
        recorder.events[1].verifyTouchEvent(1, ACTION_MOVE, 0, t) // pointer1
        recorder.events[1].verifyTouchPointer(pointer1, position1_2)
    }

    @Test
    fun onePointer_downCancel() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoTouchGestureInProgress()
        subject.flush()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)
            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            t += eventPeriodMillis
            this[1].verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            this[1].verifyTouchPointer(pointer1, position1_1)
        }
    }

    @Test
    fun twoPointers_downDownMoveMove() {
        // 2 fingers, both go down before they move
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.updateTouchPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyTouchPointer(pointer1, position1_2)
            this[2].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[3].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyTouchPointer(pointer1, position1_2)
            this[3].verifyTouchPointer(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_downMoveDownMove() {
        // 2 fingers, 1st finger moves before 2nd finger goes down and moves
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            t += eventPeriodMillis
            this[1].verifyTouchEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyTouchPointer(pointer1, position1_2)

            this[2].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[2].verifyTouchPointer(pointer1, position1_2)
            this[2].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[3].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyTouchPointer(pointer1, position1_2)
            this[3].verifyTouchPointer(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_moveSimultaneously() {
        // 2 fingers, use [updateTouchPointer] and [enqueueTouchMove]
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.updateTouchPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyTouchPointer(pointer1, position1_2)
            this[2].verifyTouchPointer(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_downUp_sameOrder() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.generateTouchUpAndCheck(pointer2)
        subject.generateTouchUpAndCheck(pointer1)
        subject.assertNoTouchGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            this[2].verifyTouchEvent(2, ACTION_POINTER_UP, 1, t) // pointer2
            this[2].verifyTouchPointer(pointer1, position1_1)
            this[2].verifyTouchPointer(pointer2, position2_1)

            this[3].verifyTouchEvent(1, ACTION_UP, 0, t) // pointer1
            this[3].verifyTouchPointer(pointer1, position1_1)
        }
    }

    @Test
    fun twoPointers_downUp_inverseOrder() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.generateTouchUpAndCheck(pointer1)
        subject.generateTouchUpAndCheck(pointer2)
        subject.assertNoTouchGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            this[2].verifyTouchEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[2].verifyTouchPointer(pointer1, position1_1)
            this[2].verifyTouchPointer(pointer2, position2_1)

            this[3].verifyTouchEvent(1, ACTION_UP, 0, t) // pointer2
            this[3].verifyTouchPointer(pointer2, position2_1)
        }
    }

    @Test
    fun twoPointers_downDownCancel() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoTouchGestureInProgress()
        subject.flush()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_CANCEL, 0, t)
            this[2].verifyTouchPointer(pointer1, position1_1)
            this[2].verifyTouchPointer(pointer2, position2_1)
        }
    }

    @Test
    fun threePointers_notSimultaneously() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down (no overlap)

        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.generateTouchDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchUpAndCheck(pointer1)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchDownAndCheck(pointer3, position3)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1)
            this[1].verifyTouchPointer(pointer2, position2)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyTouchPointer(pointer1, position1)
            this[2].verifyTouchPointer(pointer2, position2)

            this[3].verifyTouchEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyTouchPointer(pointer1, position1)
            this[3].verifyTouchPointer(pointer2, position2)

            t += eventPeriodMillis
            this[4].verifyTouchEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyTouchPointer(pointer2, position2)

            this[5].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer3
            this[5].verifyTouchPointer(pointer2, position2)
            this[5].verifyTouchPointer(pointer3, position3)
        }
    }

    @Test
    fun threePointers_pointerIdReuse() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down, and the 3rd
        // fingers reuses the pointerId of finger 1

        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.generateTouchDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchUpAndCheck(pointer1)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchDownAndCheck(pointer1, position1_2)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1)
            this[1].verifyTouchPointer(pointer2, position2)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyTouchPointer(pointer1, position1)
            this[2].verifyTouchPointer(pointer2, position2)

            this[3].verifyTouchEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyTouchPointer(pointer1, position1)
            this[3].verifyTouchPointer(pointer2, position2)

            t += eventPeriodMillis
            this[4].verifyTouchEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyTouchPointer(pointer2, position2)

            this[5].verifyTouchEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[5].verifyTouchPointer(pointer1, position1_2)
            this[5].verifyTouchPointer(pointer2, position2)
        }
    }

    @Test
    fun fourPointers_downOnly() {
        subject.generateTouchDownAndCheck(pointer3, position3)
        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.generateTouchDownAndCheck(pointer4, position4)
        subject.generateTouchDownAndCheck(pointer2, position2)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyTouchPointer(pointer3, position3)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[1].verifyTouchPointer(pointer1, position1)
            this[1].verifyTouchPointer(pointer3, position3)

            this[2].verifyTouchEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer4
            this[2].verifyTouchPointer(pointer1, position1)
            this[2].verifyTouchPointer(pointer3, position3)
            this[2].verifyTouchPointer(pointer4, position4)

            this[3].verifyTouchEvent(4, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyTouchPointer(pointer1, position1)
            this[3].verifyTouchPointer(pointer2, position2)
            this[3].verifyTouchPointer(pointer3, position3)
            this[3].verifyTouchPointer(pointer4, position4)
        }
    }

    @Test
    fun fourPointers_downWithMove() {
        // 4 fingers, going down at different times

        subject.generateTouchDownAndCheck(pointer3, position3)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchDownAndCheck(pointer1, position1)
        subject.generateTouchDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.advanceEventTime()
        subject.enqueueTouchMove()
        subject.generateTouchDownAndCheck(pointer4, position4)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(8)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyTouchPointer(pointer3, position3)

            t += eventPeriodMillis
            this[1].verifyTouchEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyTouchPointer(pointer3, position3)

            this[2].verifyTouchEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[2].verifyTouchPointer(pointer1, position1)
            this[2].verifyTouchPointer(pointer3, position3)

            this[3].verifyTouchEvent(3, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyTouchPointer(pointer1, position1)
            this[3].verifyTouchPointer(pointer2, position2)
            this[3].verifyTouchPointer(pointer3, position3)

            for (i in 4..6) {
                t += eventPeriodMillis
                this[i].verifyTouchEvent(3, ACTION_MOVE, 0, t)
                this[i].verifyTouchPointer(pointer1, position1)
                this[i].verifyTouchPointer(pointer2, position2)
                this[i].verifyTouchPointer(pointer3, position3)
            }

            this[7].verifyTouchEvent(4, ACTION_POINTER_DOWN, 3, t) // pointer4
            this[7].verifyTouchPointer(pointer1, position1)
            this[7].verifyTouchPointer(pointer2, position2)
            this[7].verifyTouchPointer(pointer3, position3)
            this[7].verifyTouchPointer(pointer4, position4)
        }
    }

    @Test
    fun enqueueTouchDown_flushesPointerMovement() {
        // Movement from [updateTouchPointer] that hasn't been sent will be sent when sending DOWN
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.updateTouchPointerAndCheck(pointer1, position1_3)
        subject.advanceEventTime()
        subject.generateTouchDownAndCheck(pointer3, position3_1)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyTouchPointer(pointer1, position1_3)
            this[2].verifyTouchPointer(pointer2, position2_1)

            this[3].verifyTouchEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer2
            this[3].verifyTouchPointer(pointer1, position1_3)
            this[3].verifyTouchPointer(pointer2, position2_1)
            this[3].verifyTouchPointer(pointer3, position3_1)
        }
    }

    @Test
    fun enqueueTouchUp_flushesPointerMovement() {
        // Movement from [updateTouchPointer] that hasn't been sent will be sent when sending UP
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.updateTouchPointerAndCheck(pointer1, position1_3)
        subject.advanceEventTime()
        subject.generateTouchUpAndCheck(pointer1)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[2].verifyTouchPointer(pointer1, position1_3)
            this[2].verifyTouchPointer(pointer2, position2_1)
        }
    }

    @Test
    fun enqueueTouchCancel_doesNotFlushPointerMovement() {
        // 2 fingers, both with pending movement.
        // CANCEL doesn't force a MOVE, but _does_ reflect the latest positions
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.updateTouchPointerAndCheck(pointer1, position1_2)
        subject.updateTouchPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyTouchEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyTouchPointer(pointer1, position1_1)

            this[1].verifyTouchEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyTouchPointer(pointer1, position1_1)
            this[1].verifyTouchPointer(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyTouchEvent(2, ACTION_CANCEL, 0, t)
            this[2].verifyTouchPointer(pointer1, position1_2)
            this[2].verifyTouchPointer(pointer2, position2_2)
        }
    }

    private fun AndroidInputDispatcher.generateCancelAndCheckPointers() {
        generateTouchCancelAndCheck()
        assertThat(getCurrentTouchPosition(pointer1)).isNull()
        assertThat(getCurrentTouchPosition(pointer2)).isNull()
        assertThat(getCurrentTouchPosition(pointer3)).isNull()
    }

    @Test
    fun enqueueTouchDown_afterDown() {
        subject.enqueueTouchDown(pointer1, position1)
        expectError<IllegalArgumentException> {
            subject.enqueueTouchDown(pointer1, position2)
        }
    }

    @Test
    fun updateTouchPointer_withoutDown() {
        expectError<IllegalStateException> {
            subject.updateTouchPointer(pointer1, position1_1)
        }
    }

    @Test
    fun updateTouchPointer_wrongPointerId() {
        subject.enqueueTouchDown(pointer1, position1_1)
        expectError<IllegalArgumentException> {
            subject.updateTouchPointer(pointer2, position1_2)
        }
    }

    @Test
    fun updateTouchPointer_afterUp() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchUp(pointer1)
        expectError<IllegalStateException> {
            subject.updateTouchPointer(pointer1, position1_2)
        }
    }

    @Test
    fun updateTouchPointer_afterCancel() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchCancel()
        expectError<IllegalStateException> {
            subject.updateTouchPointer(pointer1, position1_2)
        }
    }

    @Test
    fun enqueueTouchMove_withoutDown() {
        expectError<IllegalStateException> {
            subject.enqueueTouchMove()
        }
    }

    @Test
    fun enqueueTouchMove_afterUp() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchUp(pointer1)
        expectError<IllegalStateException> {
            subject.enqueueTouchMove()
        }
    }

    @Test
    fun enqueueTouchMove_afterCancel() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchCancel()
        expectError<IllegalStateException> {
            subject.enqueueTouchMove()
        }
    }

    @Test
    fun enqueueTouchUp_withoutDown() {
        expectError<IllegalStateException> {
            subject.enqueueTouchUp(pointer1)
        }
    }

    @Test
    fun enqueueTouchUp_wrongPointerId() {
        subject.enqueueTouchDown(pointer1, position1_1)
        expectError<IllegalArgumentException> {
            subject.enqueueTouchUp(pointer2)
        }
    }

    @Test
    fun enqueueTouchUp_afterUp() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchUp(pointer1)
        expectError<IllegalStateException> {
            subject.enqueueTouchUp(pointer1)
        }
    }

    @Test
    fun enqueueTouchUp_afterCancel() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchCancel()
        expectError<IllegalStateException> {
            subject.enqueueTouchUp(pointer1)
        }
    }

    @Test
    fun enqueueTouchCancel_withoutDown() {
        expectError<IllegalStateException> {
            subject.enqueueTouchCancel()
        }
    }

    @Test
    fun enqueueTouchCancel_afterUp() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchUp(pointer1)
        expectError<IllegalStateException> {
            subject.enqueueTouchCancel()
        }
    }

    @Test
    fun enqueueTouchCancel_afterCancel() {
        subject.enqueueTouchDown(pointer1, position1_1)
        subject.enqueueTouchCancel()
        expectError<IllegalStateException> {
            subject.enqueueTouchCancel()
        }
    }
}
