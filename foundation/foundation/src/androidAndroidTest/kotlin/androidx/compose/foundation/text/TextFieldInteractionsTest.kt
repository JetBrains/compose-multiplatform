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

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldInteractionsTest {

    @get:Rule
    val rule = createComposeRule()

    val testTag = "textField"

    @Test
    fun coreTextField_interaction_pressed() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<PressInteraction.Press>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                up()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<PressInteraction.Press>()).hasSize(1)
            assertThat(interactions.filterIsInstance<PressInteraction.Release>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_pressed_removedWhenCancelled() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<PressInteraction.Press>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                cancel()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<PressInteraction.Press>()).hasSize(1)
            assertThat(interactions.filterIsInstance<PressInteraction.Cancel>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_focused() {
        val state = mutableStateOf(TextFieldValue(""))
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
            Box(
                modifier = Modifier.requiredSize(10.dp).focusRequester(focusRequester).focusable(),
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performClick()
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
        }
        rule.runOnIdle {
            // request focus on the box so TextField will lose it
            focusRequester.requestFocus()
        }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
            assertThat(interactions.filterIsInstance<FocusInteraction.Unfocus>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_horizontally_dragged() {
        val state = mutableStateOf(TextFieldValue("test ".repeat(100)))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                singleLine = true,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 100f, y = 0f))
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                up()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
            assertThat(interactions.filterIsInstance<DragInteraction.Stop>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_dragged_horizontally_cancelled() {
        val state = mutableStateOf(TextFieldValue("test ".repeat(100)))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.testTag(testTag),
                value = state.value,
                singleLine = true,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 100f, y = 0f))
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                cancel()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
            assertThat(interactions.filterIsInstance<DragInteraction.Cancel>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_vertically_dragged() {
        val state = mutableStateOf(TextFieldValue("test\n".repeat(10)))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.requiredSize(50.dp).testTag(testTag),
                value = state.value,
                maxLines = 3,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 0f, y = 150f))
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                up()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
            assertThat(interactions.filterIsInstance<DragInteraction.Stop>()).hasSize(1)
        }
    }

    @Test
    fun coreTextField_interaction_dragged_vertically_cancelled() {
        val state = mutableStateOf(TextFieldValue("test\n".repeat(10)))
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                modifier = Modifier.requiredSize(50.dp).testTag(testTag),
                value = state.value,
                maxLines = 3,
                onValueChange = { state.value = it },
                interactionSource = interactionSource
            )
        }
        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 0f, y = 150f))
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
        }
        rule.onNodeWithTag(testTag)
            .performTouchInput {
                cancel()
            }
        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<DragInteraction.Start>()).hasSize(1)
            assertThat(interactions.filterIsInstance<DragInteraction.Cancel>()).hasSize(1)
        }
    }
}
