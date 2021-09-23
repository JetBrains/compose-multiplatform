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

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.injectionscope.touch.Common.performTouchInput
import androidx.compose.ui.test.inputdispatcher.assertNoTouchGestureInProgress
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [TouchInjectionScope.cancel] works
 */
@MediumTest
class CancelTest {
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
        // When we inject a down event followed by a cancel event
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by a cancel event
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { down(2, downPosition2) }
        rule.performTouchInput { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 2 down events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)
            }
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun cancel_withoutDown() {
        expectError<IllegalStateException> {
            rule.performTouchInput { cancel() }
        }
    }

    @Test
    fun cancel_afterUp() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { up() }
        expectError<IllegalStateException> {
            rule.performTouchInput { cancel() }
        }
    }

    @Test
    fun cancel_afterCancel() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { cancel() }
        expectError<IllegalStateException> {
            rule.performTouchInput { cancel() }
        }
    }
}
