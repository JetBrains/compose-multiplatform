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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InactiveTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.selection.AmbientSelectionRegistrar
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class InactiveTextFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val text = TextFieldValue("test")
    private val tag = "InactiveTextField"

    @Test
    fun inactiveTextField_disabled_noFocus() {
        val interactionState = InteractionState()
        val focusRequester = FocusRequester()
        rule.setContent {
            InactiveTextField(
                value = text,
                modifier = Modifier.testTag(tag).focusRequester(focusRequester),
                enabled = false,
                interactionState = interactionState
            )
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(interactionState.contains(Interaction.Focused)).isFalse()
        }

        rule.onNodeWithTag(tag)
            .assertIsNotEnabled()
    }

    @Test
    fun inactiveTextField_enabled_focusable() {
        val interactionState = InteractionState()
        val focusRequester = FocusRequester()
        rule.setContent {
            InactiveTextField(
                value = text,
                modifier = Modifier.testTag(tag).focusRequester(focusRequester),
                enabled = true,
                interactionState = interactionState
            )
        }
        rule.runOnIdle {
            assertThat(interactionState.contains(Interaction.Focused)).isFalse()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(interactionState.contains(Interaction.Focused)).isTrue()
        }
        rule.onNodeWithTag(tag)
            .assertIsFocused()
            .assertIsEnabled()
    }

    @Test
    fun inactiveTextField_disabled_noSelection() {
        rule.setContent {
            InactiveTextField(
                value = text,
                modifier = Modifier.testTag(tag).width(100.dp).composed {
                    assertThat(AmbientSelectionRegistrar.current).isNull()
                    Modifier
                },
                enabled = false
            )
        }
    }

    @Test
    fun inactiveTextField_enabled_selectable() {
        rule.setContent {
            InactiveTextField(
                value = text,
                modifier = Modifier.composed {
                    assertThat(AmbientSelectionRegistrar.current).isNotNull()
                    Modifier
                },
                enabled = true
            )
        }
    }
}