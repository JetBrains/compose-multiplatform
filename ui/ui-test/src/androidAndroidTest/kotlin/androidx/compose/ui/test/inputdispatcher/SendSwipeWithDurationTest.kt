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
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verify
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if the [AndroidInputDispatcher.enqueueSwipe] gesture works when specifying the gesture as a
 * function between two positions. Verifies if the generated MotionEvents for a gesture with a
 * given duration have the expected timestamps. The timestamps should divide the duration as
 * equally as possible with as close to [InputDispatcher.eventPeriodMillis] between each
 * successive event as possible.
 */
@SmallTest
@RunWith(Parameterized::class)
class SendSwipeWithDurationTest(private val config: TestConfig) : InputDispatcherTest() {
    data class TestConfig(
        val durationMillis: Long,
        val expectedTimestamps: List<Long>
    )

    companion object {
        private val curve = { t: Long ->
            Offset(t.toFloat(), (-t).toFloat())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // With eventPeriod of 10.ms, 0 events is 0.ms and 1 event is 10.ms
                // Even though 1.ms is closer to 0.ms than to 10.ms, split into 1 event as we
                // must have at least 1 move event to have movement.
                TestConfig(1, listOf(1)),
                // With eventPeriod of 10.ms, 0 events is 0.ms and 1 event is 10.ms
                // Split 7.ms in 1 event as 7.ms is closer to 10.ms than to 0.ms
                TestConfig(7, listOf(7)),
                // With eventPeriod of 10.ms, a duration of 10.ms is exactly 1 event
                TestConfig(10, listOf(10)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // Split 14.ms in 1 event as 14.ms is closer to 10.ms than to 20.ms
                TestConfig(14, listOf(14)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // 15.ms is as close to 10.ms as it is to 20.ms, in which case the larger number
                // of events is preferred -> 2 events
                TestConfig(15, listOf(8, 15)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // Split 19.ms in 2 events as 19.ms is closer to 20.ms than to 10.ms
                TestConfig(19, listOf(10, 19)),
                // With eventPeriod of 10.ms, 2 events is 20.ms and 3 events is 30.ms
                // Split 24.ms in 2 events as 24.ms is closer to 20.ms than to 30.ms
                TestConfig(24, listOf(12, 24)),
                // With eventPeriod of 10.ms, 2 events is 20.ms and 3 events is 30.ms
                // 25.ms is as close to 20.ms as it is to 30.ms, in which case the larger number
                // of events is preferred -> 3 events
                TestConfig(25, listOf(8, 17, 25)),
                // With eventPeriod of 10.ms, 9 event is 90.ms and 10 events is 100.ms
                // Split 97.ms in 10 events as 97.ms is closer to 100.ms than to 90.ms
                TestConfig(97, listOf(10, 19, 29, 39, 49, 58, 68, 78, 87, 97))
            )
        }
    }

    @Test
    fun swipeWithDuration() {
        // Given a swipe with a given duration
        subject.enqueueSwipe(curve = curve, durationMillis = config.durationMillis)
        subject.sendAllSynchronous()

        // then
        val expectedNumberOfMoveEvents = config.expectedTimestamps.size
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            // down + up + #move
            assertThat(size).isEqualTo(2 + expectedNumberOfMoveEvents)

            val durationMs = config.durationMillis
            // First is down, last is up
            first().verify(curve, ACTION_DOWN, 0, Touchscreen, Finger)
            last().verify(curve, ACTION_UP, durationMs, Touchscreen, Finger)
            // In between are all move events with the expected timestamps
            drop(1).zip(config.expectedTimestamps).forEach { (event, expectedTimestamp) ->
                event.verify(curve, ACTION_MOVE, expectedTimestamp, Touchscreen, Finger)
            }
        }
    }
}
