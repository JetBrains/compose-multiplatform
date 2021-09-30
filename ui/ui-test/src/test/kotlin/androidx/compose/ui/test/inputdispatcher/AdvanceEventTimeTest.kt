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

package androidx.compose.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests if [AndroidInputDispatcher.advanceEventTime] works by sending three events with a
 * delay in between them.
 */
@SmallTest
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(minSdk = RobolectricMinSdk)
class AdvanceEventTimeTest(private val config: TestConfig) : InputDispatcherTest() {
    data class TestConfig(
        val firstDelayMillis: Long,
        val secondDelayMillis: Long,
    )

    companion object {
        private val anyPosition = Offset.Zero

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (delay1 in listOf(0L, 23L)) {
                    for (delay2 in listOf(0L, 47L)) {
                        add(
                            TestConfig(
                                firstDelayMillis = delay1,
                                secondDelayMillis = delay2,
                            )
                        )
                    }
                }
            }
        }
    }

    @Test
    fun advanceEventTime() {
        // Send three events with a delay in between
        subject.enqueueTouchDown(0, anyPosition)
        subject.advanceEventTime(config.firstDelayMillis)
        subject.enqueueTouchMove()
        subject.advanceEventTime(config.secondDelayMillis)
        subject.enqueueTouchUp(0)
        subject.flush()

        // Check if the time between the events was exactly the delay
        val expectedFirstDelay = config.firstDelayMillis
        val expectedSecondDelay = config.secondDelayMillis
        recorder.events.apply {
            assertThat(this).hasSize(3)
        }.zipWithNext { a, b -> b.eventTime - a.eventTime }.apply {
            assertThat(this).isEqualTo(listOf(expectedFirstDelay, expectedSecondDelay))
        }
    }
}
