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

package androidx.compose.ui.test.inputdispatcher

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.isMonotonicBetween
import androidx.compose.ui.test.util.moveEvents
import androidx.compose.ui.test.util.splitsDurationEquallyInto
import androidx.compose.ui.test.util.verify
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max

/**
 * Tests if the [AndroidInputDispatcher.enqueueSwipe] gesture works when specifying the gesture as a
 * line between two positions
 */
@SmallTest
@RunWith(Parameterized::class)
class SendSwipeLineTest(private val config: TestConfig) : InputDispatcherTest(config.eventPeriod) {
    data class TestConfig(
        val duration: Long,
        val eventPeriod: Long
    )

    companion object {
        private val start = Offset(5f, 7f)
        private val end = Offset(23f, 29f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(10L, 9L, 11L).flatMap { period ->
                (1L..100L step 11).map { durationMs ->
                    TestConfig(durationMs, period)
                }
            }
        }
    }

    private val duration get() = config.duration
    private val eventPeriod = config.eventPeriod

    @Test
    fun swipeByLine() {
        subject.enqueueSwipe(start, end, duration)
        subject.sendAllSynchronous()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val expectedMoveEvents = max(1, duration / eventPeriod).toInt()
            assertThat(size).isAtLeast(2 + expectedMoveEvents) // down move+ up

            // Check down and up events
            val durationMs = duration
            first().verify(start, MotionEvent.ACTION_DOWN, 0, Touchscreen, Finger)
            last().verify(end, MotionEvent.ACTION_UP, durationMs, Touchscreen, Finger)

            // Check coordinates and timestamps of move events
            moveEvents.isMonotonicBetween(start, end)
            moveEvents.splitsDurationEquallyInto(0L, durationMs, eventPeriod)
        }
    }
}
