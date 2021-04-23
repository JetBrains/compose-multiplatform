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

package androidx.compose.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

internal class UpdateEffectTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `call update when any mutableStateOf is changed`() {
        var state1 by mutableStateOf(0)
        var state2 by mutableStateOf(0)
        var updatedState1 = -1
        var updatedState2 = -1

        rule.setContent {
            UpdateEffect {
                updatedState1 = state1
                updatedState2 = state2
            }
        }

        rule.waitForIdle()
        assertThat(updatedState1).isEqualTo(state1)
        assertThat(updatedState2).isEqualTo(state2)

        state1 = 1
        rule.waitForIdle()
        assertThat(updatedState1).isEqualTo(state1)
        assertThat(updatedState2).isEqualTo(state2)

        state2 = 1
        rule.waitForIdle()
        assertThat(updatedState1).isEqualTo(state1)
        assertThat(updatedState2).isEqualTo(state2)

        state1 = 1
        state2 = 1
        rule.waitForIdle()
        assertThat(updatedState1).isEqualTo(state1)
        assertThat(updatedState2).isEqualTo(state2)
    }

    @Test
    fun `don't call update if there no any changed state`() {
        var state = 0
        var updatedState = -1

        rule.setContent {
            UpdateEffect {
                updatedState = state
            }
        }

        rule.waitForIdle()
        assertThat(updatedState).isEqualTo(state)

        state = 1
        rule.waitForIdle()
        assertThat(updatedState).isNotEqualTo(state)
    }
}