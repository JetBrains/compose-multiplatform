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

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.test.filters.MediumTest
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.down
import androidx.compose.ui.test.inputdispatcher.verifyNoGestureInProgress
import androidx.compose.ui.test.partialgesturescope.Common.partialGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.InputDispatcherTestRule
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [cancel] works
 */
@MediumTest
class SendCancelTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
    }

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule()

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
        // When we inject a down event followed by a cancel event
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
        }

        // And no gesture is in progress
        rule.partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by a cancel event
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { down(2, downPosition2) }
        rule.partialGesture { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 2 down events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)
            }
        }

        // And no gesture is in progress
        rule.partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun cancelWithoutDown() {
        expectError<IllegalStateException> {
            rule.partialGesture { cancel() }
        }
    }

    @Test
    fun cancelAfterUp() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { up() }
        expectError<IllegalStateException> {
            rule.partialGesture { cancel() }
        }
    }

    @Test
    fun cancelAfterCancel() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { cancel() }
        expectError<IllegalStateException> {
            rule.partialGesture { cancel() }
        }
    }
}
