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

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import androidx.compose.ui.geometry.Offset
import androidx.test.filters.SmallTest
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.verifyEvent
import androidx.compose.ui.test.util.verifyPointer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests if [AndroidInputDispatcher.enqueueDown] works
 */
@SmallTest
class SendDownTest : InputDispatcherTest() {
    companion object {
        // Pointer ids
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33
        private const val pointer4 = 44

        // Positions (mostly used with corresponding pointerId: pointerX with positionX)
        private val position1 = Offset(1f, 1f)
        private val position2 = Offset(2f, 2f)
        private val position3 = Offset(3f, 3f)
        private val position4 = Offset(4f, 4f)

        // Single alternative for pointer1
        private val position1_2 = Offset(12f, 12f)
    }

    @Test
    fun onePointer() {
        subject.generateDownAndCheck(pointer1, position1)
        subject.sendAllSynchronous()

        val t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(1)
        recorder.events[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
        recorder.events[0].verifyPointer(pointer1, position1, Finger)
    }

    @Test
    fun twoPointers_ascending() {
        // 2 fingers, sent in ascending order of pointerId (matters for actionIndex)
        subject.generateDownAndCheck(pointer1, position1)
        subject.generateDownAndCheck(pointer2, position2)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1, Finger)
            this[1].verifyPointer(pointer2, position2, Finger)
        }
    }

    @Test
    fun twoPointers_descending() {
        // 2 fingers, sent in descending order of pointerId (matters for actionIndex)
        subject.generateDownAndCheck(pointer2, position2)
        subject.generateDownAndCheck(pointer1, position1)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer2
            this[0].verifyPointer(pointer2, position2, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 0, t, Touchscreen) // pointer1
            this[1].verifyPointer(pointer1, position1, Finger)
            this[1].verifyPointer(pointer2, position2, Finger)
        }
    }

    @Test
    fun fourPointers() {
        // 4 fingers, sent in non-trivial order of pointerId (matters for actionIndex)

        subject.generateDownAndCheck(pointer3, position3)
        subject.generateDownAndCheck(pointer1, position1)
        subject.generateDownAndCheck(pointer4, position4)
        subject.generateDownAndCheck(pointer2, position2)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer3
            this[0].verifyPointer(pointer3, position3, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 0, t, Touchscreen) // pointer1
            this[1].verifyPointer(pointer1, position1, Finger)
            this[1].verifyPointer(pointer3, position3, Finger)

            this[2].verifyEvent(3, ACTION_POINTER_DOWN, 2, t, Touchscreen) // pointer4
            this[2].verifyPointer(pointer1, position1, Finger)
            this[2].verifyPointer(pointer3, position3, Finger)
            this[2].verifyPointer(pointer4, position4, Finger)

            this[3].verifyEvent(4, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[3].verifyPointer(pointer1, position1, Finger)
            this[3].verifyPointer(pointer2, position2, Finger)
            this[3].verifyPointer(pointer3, position3, Finger)
            this[3].verifyPointer(pointer4, position4, Finger)
        }
    }

    @Test
    fun staggeredDown() {
        // 4 fingers, going down at different times
        // Each [sendMove] increases the time by 10 milliseconds

        subject.generateDownAndCheck(pointer3, position3)
        subject.enqueueMove()
        subject.generateDownAndCheck(pointer1, position1)
        subject.generateDownAndCheck(pointer2, position2)
        subject.enqueueMove()
        subject.enqueueMove()
        subject.enqueueMove()
        subject.generateDownAndCheck(pointer4, position4)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(8)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer3
            this[0].verifyPointer(pointer3, position3, Finger)

            t += eventPeriodMillis
            this[1].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen)
            this[1].verifyPointer(pointer3, position3, Finger)

            this[2].verifyEvent(2, ACTION_POINTER_DOWN, 0, t, Touchscreen) // pointer1
            this[2].verifyPointer(pointer1, position1, Finger)
            this[2].verifyPointer(pointer3, position3, Finger)

            this[3].verifyEvent(3, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[3].verifyPointer(pointer1, position1, Finger)
            this[3].verifyPointer(pointer2, position2, Finger)
            this[3].verifyPointer(pointer3, position3, Finger)

            for (i in 4..6) {
                t += eventPeriodMillis
                this[i].verifyEvent(3, ACTION_MOVE, 0, t, Touchscreen)
                this[i].verifyPointer(pointer1, position1, Finger)
                this[i].verifyPointer(pointer2, position2, Finger)
                this[i].verifyPointer(pointer3, position3, Finger)
            }

            this[7].verifyEvent(4, ACTION_POINTER_DOWN, 3, t, Touchscreen) // pointer4
            this[7].verifyPointer(pointer1, position1, Finger)
            this[7].verifyPointer(pointer2, position2, Finger)
            this[7].verifyPointer(pointer3, position3, Finger)
            this[7].verifyPointer(pointer4, position4, Finger)
        }
    }

    @Test
    fun nonOverlappingPointers() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down (no overlap)
        // Each [sendMove] increases the time by 10 milliseconds

        subject.generateDownAndCheck(pointer1, position1)
        subject.generateDownAndCheck(pointer2, position2)
        subject.enqueueMove()
        subject.generateUpAndCheck(pointer1)
        subject.enqueueMove()
        subject.generateDownAndCheck(pointer3, position3)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1, Finger)
            this[1].verifyPointer(pointer2, position2, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1, Finger)
            this[2].verifyPointer(pointer2, position2, Finger)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t, Touchscreen) // pointer1
            this[3].verifyPointer(pointer1, position1, Finger)
            this[3].verifyPointer(pointer2, position2, Finger)

            t += eventPeriodMillis
            this[4].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen)
            this[4].verifyPointer(pointer2, position2, Finger)

            this[5].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer3
            this[5].verifyPointer(pointer2, position2, Finger)
            this[5].verifyPointer(pointer3, position3, Finger)
        }
    }

    @Test
    fun pointerIdReuse() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down, and the 3rd
        // fingers reuses the pointerId of finger 1
        // Each [sendMove] increases the time by 10 milliseconds

        subject.generateDownAndCheck(pointer1, position1)
        subject.generateDownAndCheck(pointer2, position2)
        subject.enqueueMove()
        subject.generateUpAndCheck(pointer1)
        subject.enqueueMove()
        subject.generateDownAndCheck(pointer1, position1_2)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1, Finger)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t, Touchscreen) // pointer2
            this[1].verifyPointer(pointer1, position1, Finger)
            this[1].verifyPointer(pointer2, position2, Finger)

            t += eventPeriodMillis
            this[2].verifyEvent(2, ACTION_MOVE, 0, t, Touchscreen)
            this[2].verifyPointer(pointer1, position1, Finger)
            this[2].verifyPointer(pointer2, position2, Finger)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t, Touchscreen) // pointer1
            this[3].verifyPointer(pointer1, position1, Finger)
            this[3].verifyPointer(pointer2, position2, Finger)

            t += eventPeriodMillis
            this[4].verifyEvent(1, ACTION_MOVE, 0, t, Touchscreen)
            this[4].verifyPointer(pointer2, position2, Finger)

            this[5].verifyEvent(2, ACTION_POINTER_DOWN, 0, t, Touchscreen) // pointer1
            this[5].verifyPointer(pointer1, position1_2, Finger)
            this[5].verifyPointer(pointer2, position2, Finger)
        }
    }

    @Test
    fun downAfterDown() {
        subject.enqueueDown(pointer1, position1)
        expectError<IllegalArgumentException> {
            subject.enqueueDown(pointer1, position2)
        }
    }
}
