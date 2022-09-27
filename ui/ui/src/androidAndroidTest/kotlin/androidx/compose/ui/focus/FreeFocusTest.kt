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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FreeFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun active_freeFocus_retainFocusAsActive() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        val success = rule.runOnIdle { focusRequester.freeFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun activeParent_freeFocus_retainFocusAsActiveParent() {
        // Arrange.
        lateinit var focusState: FocusState
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .focusTarget())
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        val success = rule.runOnIdle { focusRequester.freeFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }
    }

    @Test
    fun captured_freeFocus_changesStateToActive() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState.isFocused).isTrue()
            assertThat(focusState.isCaptured).isTrue()
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.freeFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState.isFocused).isTrue()
            assertThat(focusState.isCaptured).isFalse()
        }
    }

    @Test
    fun inactive_freeFocus_retainFocusAsInactive() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }

        // Act.
        val success = rule.runOnIdle {
            focusRequester.freeFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }
}
