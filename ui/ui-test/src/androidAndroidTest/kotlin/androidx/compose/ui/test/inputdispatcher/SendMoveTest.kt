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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.verifyEvent
import androidx.compose.ui.test.util.verifyPointer
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests if [AndroidInputDispatcher.movePointer] and [AndroidInputDispatcher.enqueueMove] work
 */
@SmallTest
class SendMoveTest : InputDispatcherTest() {
    companion object {
        // pointerIds
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33

        // positions (used with corresponding pointerId: pointerX with positionX_Y)
        private val position1_1 = Offset(11f, 11f)
        private val position2_1 = Offset(21f, 21f)
        private val position3_1 = Offset(31f, 31f)

        private val position1_2 = Offset(12f, 12f)
        private val position2_2 = Offset(22f, 22f)

        private val position1_3 = Offset(13f, 13f)
    }

    private fun AndroidInputDispatcher.generateCancelAndCheckPointers() {
        generateCancelAndCheck()
        assertThat(getCurrentPosition(pointer1)).isNull()
        assertThat(getCurrentPosition(pointer2)).isNull()
        assertThat(getCurrentPosition(pointer3)).isNull()
    }

    @Test
    fun onePointer() {
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        var t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(2)
        recorder.events[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
        recorder.events[0].verifyPointer(pointer1, position1_1, Finger)

        t += eventPeriodMillis
        recorder.events[1].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen) // pointer1
        recorder.events[1].verifyPointer(pointer1, position1_2, Finger)
    }

    @Test
    fun onePointerWithDelay() {
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.enqueueMove(2 * eventPeriodMillis)
        subject.sendAllSynchronous()

        var t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(2)
        recorder.events[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
        recorder.events[0].verifyPointer(pointer1, position1_1, Finger)

        t += 2 * eventPeriodMillis
        recorder.events[1].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen) // pointer1
        recorder.events[1].verifyPointer(pointer1, position1_2, Finger)
    }

    @Test
    fun twoPointers_downDownMoveMove() {
        // 2 fingers, both go down before they move
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.enqueueMove()
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_2, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[3].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[3].verifyPointer(pointer1, position1_2, Finger)
            this[3].verifyPointer(pointer2, position2_2, Finger)
        }
    }

    @Test
    fun twoPointers_downMoveDownMove() {
        // 2 fingers, 1st finger moves before 2nd finger goes down and moves
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.enqueueMove()
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            t += eventPeriodMillis
            this[1].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen)
            this[1].verifyPointer(pointer1, position1_2, Finger)

            this[2].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[2].verifyPointer(pointer1, position1_2, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[3].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[3].verifyPointer(pointer1, position1_2, Finger)
            this[3].verifyPointer(pointer2, position2_2, Finger)
        }
    }

    @Test
    fun movePointer_oneMovePerPointer() {
        // 2 fingers, use [movePointer] and [sendMove]
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_2, Finger)
            this[2].verifyPointer(pointer2, position2_2, Finger)
        }
    }

    @Test
    fun movePointer_multipleMovesPerPointer() {
        // 2 fingers, do several [movePointer]s and then [sendMove]
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_3, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)
        }
    }

    @Test
    fun sendMoveWithoutMovePointer() {
        // 2 fingers, do [sendMove] without [movePointer]
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.enqueueMove()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_1, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)
        }
    }

    @Test
    fun downFlushesPointerMovement() {
        // Movement from [movePointer] that hasn't been sent will be sent when sending DOWN
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.generateDownAndCheck(pointer3, position3_1)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_3, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)

            this[3].verifyEvent(3, ACTION_POINTER_DOWN, 2, t, Touchscreen) // pointer2
            this[3].verifyPointer(pointer1, position1_3, Finger)
            this[3].verifyPointer(pointer2, position2_1, Finger)
            this[3].verifyPointer(pointer3, position3_1, Finger)
        }
    }

    @Test
    fun upFlushesPointerMovement() {
        // Movement from [movePointer] that hasn't been sent will be sent when sending UP
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.generateUpAndCheck(pointer1)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_3, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t, Touchscreen) // pointer1
            this[3].verifyPointer(pointer1, position1_3, Finger)
            this[3].verifyPointer(pointer2, position2_1, Finger)
        }
    }

    @Test
    fun cancelDoesNotFlushPointerMovement() {
        // 2 fingers, both with pending movement.
        // CANCEL doesn't force a MOVE, but _does_ reflect the latest positions
        subject.generateDownAndCheck(pointer1, position1_1)
        subject.generateDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.generateCancelAndCheckPointers()
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1_1, Finger)
            this[1].verifyPointer(pointer2, position2_1, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_CANCEL, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1_2, Finger)
            this[2].verifyPointer(pointer2, position2_2, Finger)
        }
    }

    @Test
    fun movePointerWithoutDown() {
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_1)
        }
    }

    @Test
    fun movePointerWrongPointerId() {
        subject.enqueueDown(pointer1, position1_1)
        expectError<IllegalArgumentException> {
            subject.movePointer(pointer2, position1_2)
        }
    }

    @Test
    fun movePointerAfterUp() {
        subject.enqueueDown(pointer1, position1_1)
        subject.enqueueUp(pointer1)
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_2)
        }
    }

    @Test
    fun movePointerAfterCancel() {
        subject.enqueueDown(pointer1, position1_1)
        subject.enqueueCancel()
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_2)
        }
    }

    @Test
    fun sendMoveWithoutDown() {
        expectError<IllegalStateException> {
            subject.enqueueMove()
        }
    }

    @Test
    fun sendMoveAfterUp() {
        subject.enqueueDown(pointer1, position1_1)
        subject.enqueueUp(pointer1)
        expectError<IllegalStateException> {
            subject.enqueueMove()
        }
    }

    @Test
    fun sendMoveAfterCancel() {
        subject.enqueueDown(pointer1, position1_1)
        subject.enqueueCancel()
        expectError<IllegalStateException> {
            subject.enqueueMove()
        }
    }
}
