/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.TestCounter
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@SmallTest
class PhaseOrderingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun singlePass() {
        val counter = TestCounter()
        rule.setContent {
            // This should never recompose.
            counter.expect(1)

            LaunchedEffect(Unit) {
                counter.expect(2)
                withFrameNanos {
                    counter.expect(6)
                    launch {
                        // No continuations resumed during a frame should be dispatched until after
                        // the frame callbacks finish running.
                        counter.expect(8)
                    }
                    counter.expect(7)
                }
                counter.expect(9)
            }

            Layout(
                content = {},
                modifier = Modifier.drawBehind {
                    counter.expect(5)
                }
            ) { _, _ ->
                counter.expect(3)
                layout(1, 1) {
                    counter.expect(4)
                }
            }
        }
    }

    @Test
    fun frameCallbackRestartsLayout() {
        val counter = TestCounter()
        var firstPass by mutableStateOf(true)
        var layoutCount = 0

        rule.setContent {
            LaunchedEffect(Unit) {
                counter.expect(1)
                withFrameNanos {
                    // This won't run until the 2nd frame, so the first layout happens before it.
                    counter.expect(4)
                    firstPass = false
                }
                counter.expect(7)
            }

            Layout(content = {}) { _, _ ->
                counter.expect(if (firstPass) 2 else 5)
                layoutCount++
                layout(1, 1) {
                    counter.expect(if (firstPass) 3 else 6)
                }
            }
        }

        rule.runOnIdle {
            assertThat(layoutCount).isEqualTo(2)
        }
    }
}
