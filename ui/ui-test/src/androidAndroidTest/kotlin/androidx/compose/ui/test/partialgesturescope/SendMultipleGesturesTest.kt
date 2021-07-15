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

package androidx.compose.ui.test.partialgesturescope

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.partialgesturescope.Common.partialGesture
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendMultipleGesturesTest {
    @get:Rule
    val rule = createComposeRule()

    private val recorder = SinglePointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Test
    fun test_noTimeInBetween() {
        testWithTwoGestures(
            expectedDifference = 0,
            betweenGesturesBlock = {}
        )
    }

    @Test
    fun test_someTimeInBetween() {
        testWithTwoGestures(
            expectedDifference = 1273,
            betweenGesturesBlock = {
                rule.mainClock.advanceTimeBy(1273, ignoreFrameDuration = true)
            }
        )
    }

    private fun testWithTwoGestures(
        expectedDifference: Long,
        betweenGesturesBlock: () -> Unit
    ) {
        rule.partialGesture { click() }
        betweenGesturesBlock.invoke()
        rule.partialGesture { click() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded [down, move, up*, down**, move, up] and the difference
                // between *) and **) is zero
                assertThat(events).hasSize(6)
                val t1 = events[2].timestamp
                val t2 = events[3].timestamp
                assertThat(t2 - t1).isEqualTo(expectedDifference)
            }
        }
    }
}
