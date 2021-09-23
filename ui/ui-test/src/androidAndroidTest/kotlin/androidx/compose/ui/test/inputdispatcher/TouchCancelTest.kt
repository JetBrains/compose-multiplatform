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
import android.view.MotionEvent.ACTION_POINTER_DOWN
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verifyEvent
import androidx.compose.ui.test.util.verifyPointer
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests if [AndroidInputDispatcher.enqueueTouchCancel] works
 */
@SmallTest
class TouchCancelTest : InputDispatcherTest() {
    companion object {
        // pointerIds
        private const val pointer1 = 11
        private const val pointer2 = 22

        // positions (used with corresponding pointerId: pointerX with positionX_Y)
        private val position1_1 = Offset(11f, 11f)
        private val position2_1 = Offset(21f, 21f)
    }

    private fun AndroidInputDispatcher.generateCancelAndCheckPointers(delay: Long? = null) {
        generateTouchCancelAndCheck(delay)
        assertThat(getCurrentTouchPosition(pointer1)).isNull()
        assertThat(getCurrentTouchPosition(pointer2)).isNull()
    }

    @Test
    fun onePointer() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoTouchGestureInProgress()
        subject.sendAllSynchronous()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)
            this[0].verifyEvent(1, ACTION_DOWN, 0, t, Touchscreen) // pointer1
            this[0].verifyPointer(pointer1, position1_1, Finger)

            t += eventPeriodMillis
            this[1].verifyEvent(1, ACTION_CANCEL, 0, t, Touchscreen)
            this[1].verifyPointer(pointer1, position1_1, Finger)
        }
    }

    @Test
    fun multiplePointers() {
        subject.generateTouchDownAndCheck(pointer1, position1_1)
        subject.generateTouchDownAndCheck(pointer2, position2_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoTouchGestureInProgress()
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
            this[2].verifyPointer(pointer1, position1_1, Finger)
            this[2].verifyPointer(pointer2, position2_1, Finger)
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
