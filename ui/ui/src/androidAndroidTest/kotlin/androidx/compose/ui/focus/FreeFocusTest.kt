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
import androidx.compose.ui.FocusModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.ActiveParent
import androidx.compose.ui.focus.FocusState.Captured
import androidx.compose.ui.focus.FocusState.Disabled
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@OptIn(ExperimentalFocus::class)
@RunWith(AndroidJUnit4::class)
class FreeFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun active_freeFocus_retainFocusAsActive() {
        // Arrange.
        var focusState: FocusState = Active
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.freeFocus()

            // Assert.
            Truth.assertThat(success).isTrue()
            Truth.assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun activeParent_freeFocus_retainFocusAsActiveParent() {
        // Arrange.
        var focusState: FocusState = ActiveParent
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun captured_freeFocus_changesStateToActive() {
        // Arrange.
        var focusState: FocusState = Captured
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.freeFocus()

            // Assert.
            Truth.assertThat(success).isTrue()
            Truth.assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun disabled_freeFocus_retainFocusAsDisabled() {
        // Arrange.
        var focusState: FocusState = Disabled
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusState).isEqualTo(Disabled)
        }
    }

    @Test
    fun inactive_freeFocus_retainFocusAsInactive() {
        // Arrange.
        var focusState: FocusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusRequester.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusState).isEqualTo(Inactive)
        }
    }
}