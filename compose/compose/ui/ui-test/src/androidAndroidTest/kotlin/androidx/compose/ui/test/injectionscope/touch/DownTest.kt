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
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
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
 * Tests if [TouchInjectionScope.down] works
 */
@MediumTest
class DownTest {
    companion object {
        private val position1 = Offset(5f, 5f)
        private val position2 = Offset(7f, 7f)
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
        // When we put a pointer down
        rule.performTouchInput { down(position1) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
                assertThat(events[0].pointerCount).isEqualTo(1)
                events[0].getPointer(0).verify(null, null, true, position1, Touch, Press)
            }
        }
    }

    @Test
    fun twoPointers() {
        // When we put two pointers down
        rule.performTouchInput { down(1, position1) }
        sleep(20) // (with some time in between)
        rule.performTouchInput { down(2, position2) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 2 down events with the same timestamp
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                assertThat(events[0].pointerCount).isEqualTo(1)
                events[0].getPointer(0).verify(null, null, true, position1, Touch, Press)

                val t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id

                assertThat(events[1].pointerCount).isEqualTo(2)
                events[1].getPointer(0).verify(t, pointerId1, true, position1, Touch, Press)
                events[1].getPointer(1).verify(t, null, true, position2, Touch, Press)

                val pointerId2 = events[1].getPointer(1).id
                assertThat(pointerId2).isNotEqualTo(pointerId1)
            }
        }
    }

    @Test
    fun duplicatePointers() {
        // When we inject two down events with the same pointer id
        rule.performTouchInput { down(1, position1) }
        // Then the second throws an exception
        expectError<IllegalArgumentException> {
            rule.performTouchInput { down(1, position1) }
        }
    }
}
