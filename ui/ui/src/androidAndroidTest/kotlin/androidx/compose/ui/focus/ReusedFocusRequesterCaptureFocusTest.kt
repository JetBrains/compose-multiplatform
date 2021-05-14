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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ReusedFocusRequesterCaptureFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun oneActiveComponent_returnsTrue() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Active))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState.isCaptured).isTrue()
        }
    }

    @Test
    fun oneCapturedComponent_returnsTrue() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Captured))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState.isCaptured).isTrue()
        }
    }

    @Test
    fun oneInactiveComponent_returnsFalse() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Inactive))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun oneActiveOneInactiveComponent_returnsTrue() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Inactive))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Active))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState1.isFocused).isFalse()
            assertThat(focusState2.isCaptured).isTrue()
        }
    }

    @Test
    fun oneInactiveOneCapturedComponent_returnsTrue() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Inactive))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Captured))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState1.isFocused).isFalse()
            assertThat(focusState2.isCaptured).isTrue()
        }
    }

    @Test
    fun twoInactiveComponent_returnsFalse() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Inactive))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(Inactive))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.captureFocus()

            // Assert.
            assertThat(success).isFalse()
            assertThat(focusState1.isFocused).isFalse()
            assertThat(focusState2.isFocused).isFalse()
        }
    }
}
