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

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.geometry.Offset
import androidx.test.filters.SmallTest
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.util.Finger
import androidx.compose.ui.test.util.Touchscreen
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verify
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if [AndroidInputDispatcher.enqueueClick] works
 */
@SmallTest
@RunWith(Parameterized::class)
class SendClickTest(config: TestConfig) : InputDispatcherTest() {
    data class TestConfig(
        val x: Float,
        val y: Float
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(0f, 10f, -10f, 1000000f).flatMap { x ->
                listOf(0f, 10f, -10f, 1000000f).map { y ->
                    TestConfig(x, y)
                }
            }
        }
    }

    private val position = Offset(config.x, config.y)

    @Test
    fun testClick() {
        subject.enqueueClick(position)
        subject.sendAllSynchronous()
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            assertThat(size).isEqualTo(3)
            this[0].verify(position, ACTION_DOWN, 0, Touchscreen, Finger)
            this[1].verify(position, ACTION_MOVE, eventPeriodMillis, Touchscreen, Finger)
            this[2].verify(position, ACTION_UP, eventPeriodMillis, Touchscreen, Finger)
        }
    }
}
