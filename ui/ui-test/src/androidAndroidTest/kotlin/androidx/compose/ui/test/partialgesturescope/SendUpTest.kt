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

package androidx.compose.ui.test.partialgesturescope

import android.os.SystemClock.sleep
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.partialgesturescope.Common.partialGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertNoTouchGestureInProgress
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [up] works
 */
@Suppress("DEPRECATION")
@MediumTest
class SendUpTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
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
        // When we inject a down event followed by an up event
        rule.partialGesture { down(downPosition1) }
        sleep(20) // (with some time in between)
        rule.partialGesture { up() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 up event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                val t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1].getPointer(0).verify(t, pointerId, false, downPosition1, Touch, Release)
            }
        }

        // And no gesture is in progress
        rule.onNodeWithTag(defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by two up events
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { down(2, downPosition2) }
        rule.partialGesture { up(1) }
        rule.partialGesture { up(2) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and two up events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(4)

                val t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, false, downPosition1, Touch, Release)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2, Touch, Release)

                assertThat(events[3].pointerCount).isEqualTo(1)
                events[3].getPointer(0).verify(t, pointerId2, false, downPosition2, Touch, Release)
            }
        }

        // And no gesture is in progress
        rule.onNodeWithTag(defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun upWithoutDown() {
        expectError<IllegalStateException> {
            rule.partialGesture { up() }
        }
    }

    @Test
    fun upWrongPointerId() {
        rule.partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            rule.partialGesture { up(2) }
        }
    }

    @Test
    fun upAfterUp() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { up() }
        expectError<IllegalStateException> {
            rule.partialGesture { up() }
        }
    }

    @Test
    fun upAfterCancel() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { cancel() }
        expectError<IllegalStateException> {
            rule.partialGesture { up() }
        }
    }
}
