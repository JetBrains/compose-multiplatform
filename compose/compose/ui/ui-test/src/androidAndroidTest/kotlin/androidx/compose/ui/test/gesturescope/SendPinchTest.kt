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

package androidx.compose.ui.test.gesturescope

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.isMonotonicBetween
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SendPinchTest {
    companion object {
        private const val TAG = "PINCH"
    }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = MultiPointerInputRecorder()

    @Test
    fun pinch() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, tag = TAG)
            }
        }

        val start0 = Offset(40f, 50f)
        val end0 = Offset(8f, 50f)
        val start1 = Offset(60f, 50f)
        val end1 = Offset(92f, 50f)
        val duration = 400L

        @Suppress("DEPRECATION")
        rule.onNodeWithTag(TAG).performGesture {
            pinch(start0, end0, start1, end1, duration)
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                val expectedMoveEvents = duration / eventPeriodMillis
                // expect up and down events for each pointer as well as the move events
                assertThat(events.size).isEqualTo(4 + expectedMoveEvents)

                val pointerChanges = events.flatMap { it.pointers }

                val pointerIds = pointerChanges.map { it.id }.distinct()
                val pointerUpChanges = pointerChanges.filter { !it.down }

                assertThat(pointerIds).hasSize(2)

                // Assert each pointer went back up
                assertThat(pointerUpChanges.map { it.id }).containsExactlyElementsIn(pointerIds)

                // Assert the up events are at the end
                @Suppress("NestedLambdaShadowedImplicitParameter")
                assertThat(events.takeLastWhile { it.pointers.any { !it.down } }).hasSize(2)

                pointerChanges.filter { it.id.value == 0L }.isMonotonicBetween(start0, end0)
                pointerChanges.filter { it.id.value == 1L }.isMonotonicBetween(start1, end1)
            }
        }
    }
}
