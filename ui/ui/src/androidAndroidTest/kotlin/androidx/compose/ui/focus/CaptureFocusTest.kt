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
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.test.filters.SmallTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@OptIn(ExperimentalFocus::class)
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
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(FocusState.Active))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState).isEqualTo(FocusState.Captured)
        }
    }

    @Test
    fun activeParent_captureFocus_retainsStateAsActiveParent() {
        // Arrange.
        var focusState: FocusState = FocusState.ActiveParent
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState).isEqualTo(FocusState.ActiveParent)
        }
    }

    @Test
    fun captured_captureFocus_retainsStateAsCaptured() {
        // Arrange.
        var focusState = FocusState.Captured
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState).isEqualTo(FocusState.Captured)
        }
    }

    @Test
    fun disabled_captureFocus_retainsStateAsDisabled() {
        // Arrange.
        var focusState = FocusState.Disabled
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState).isEqualTo(FocusState.Disabled)
        }
    }

    @Test
    fun inactive_captureFocus_retainsStateAsInactive() {
        // Arrange.
        var focusState = FocusState.Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(FocusModifier(focusState))
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState).isEqualTo(FocusState.Inactive)
        }
    }
}
