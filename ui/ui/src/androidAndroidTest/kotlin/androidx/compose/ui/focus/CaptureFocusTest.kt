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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
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
class CaptureFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun active_captureFocus_changesStateToCaptured() {
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

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState.isCaptured).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun activeParent_captureFocus_retainsStateAsActiveParent() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(ActiveParent))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.isCaptured).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }
    }

    @Test
    fun captured_captureFocus_retainsStateAsCaptured() {
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

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState.isCaptured).isTrue()
        }
    }

    @Test
    fun deactivated_captureFocus_retainsStateAsDeactivated() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusable()
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.isCaptured).isFalse()
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.isDeactivated).isTrue()
        }
    }

    @Test
    fun deactivatedParent_captureFocus_retainsStateAsDeactivatedParent() {
        // Arrange.
        lateinit var focusState: FocusState
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusable()
            ) {
                Box(modifier = Modifier
                    .focusRequester(initialFocus)
                    .focusable()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.isCaptured).isFalse()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isDeactivated).isTrue()
        }
    }

    @Test
    fun inactive_captureFocus_retainsStateAsInactive() {
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

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.isCaptured).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }
}

private val FocusState.isDeactivated: Boolean
    get() = (this as FocusStateImpl).isDeactivated
