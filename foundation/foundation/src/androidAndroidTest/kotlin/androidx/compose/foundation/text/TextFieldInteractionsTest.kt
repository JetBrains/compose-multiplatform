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

package androidx.compose.foundation.text

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusReference
import androidx.compose.ui.focus.focusReference
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(InternalTextApi::class)
class TextFieldInteractionsTest {

    @get:Rule
    val rule = createComposeRule()

    val testTag = "textField"

    @Test
    fun coreTextField_interaction_pressed() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
            }
        assertThat(interactionState.value).contains(Interaction.Pressed)
        rule.onNodeWithTag(testTag)
            .performGesture {
                up()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
    }

    @Test
    fun coreTextField_interaction_pressed_removedWhenCancelled() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
            }
        assertThat(interactionState.value).contains(Interaction.Pressed)
        rule.onNodeWithTag(testTag)
            .performGesture {
                cancel()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
    }

    @Test
    fun coreTextField_interaction_focused() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionState = InteractionState()
        val focusReference = FocusReference()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
            Box(
                modifier = Modifier.size(10.dp).focusReference(focusReference).focusable(),
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Focused)
        rule.onNodeWithTag(testTag)
            .performClick()
        assertThat(interactionState.value).contains(Interaction.Focused)
        rule.runOnIdle {
            // request focus on the box so TextField will lose it
            focusReference.requestFocus()
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Focused)
    }

    @Test
    fun coreTextField_interaction_horizontally_dragged() {
        val state = mutableStateOf(TextFieldValue("test ".repeat(100)))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                singleLine = true,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
                moveBy(Offset(x = 100f, y = 0f))
            }
        assertThat(interactionState.value).contains(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                up()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
    }

    @Test
    fun coreTextField_interaction_dragged_horizontally_cancelled() {
        val state = mutableStateOf(TextFieldValue("test ".repeat(100)))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                singleLine = true,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
                moveBy(Offset(x = 100f, y = 0f))
            }
        assertThat(interactionState.value).contains(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                cancel()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
    }

    @Test
    fun coreTextField_interaction_vertically_dragged() {
        val state = mutableStateOf(TextFieldValue("test\n".repeat(10)))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.size(50.dp).testTag(testTag),
                value = state.value,
                maxLines = 3,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
                moveBy(Offset(x = 0f, y = 150f))
            }
        assertThat(interactionState.value).contains(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                up()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
    }

    @Test
    fun coreTextField_interaction_dragged_vertically_cancelled() {
        val state = mutableStateOf(TextFieldValue("test\n".repeat(10)))
        val interactionState = InteractionState()
        rule.setContent {
            BasicTextField(
                modifier = Modifier.size(50.dp).testTag(testTag),
                value = state.value,
                maxLines = 3,
                onValueChange = { state.value = it },
                interactionState = interactionState
            )
        }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                down(center)
                moveBy(Offset(x = 0f, y = 150f))
            }
        assertThat(interactionState.value).contains(Interaction.Dragged)
        rule.onNodeWithTag(testTag)
            .performGesture {
                cancel()
            }
        assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
    }
}
