/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.ToggleableState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.semantics.FoundationSemanticsProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.test.filters.MediumTest
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.isToggleable
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performClick
import androidx.ui.test.performGesture
import androidx.ui.test.up
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class ToggleableTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toggleableTest_defaultSemantics() {
        rule.setContent {
            Column {
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.On, onClick = {})
                        .testTag("checkedToggleable"),
                    children = {
                        Text("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.Off, onClick = {})
                        .testTag("unCheckedToggleable"),
                    children = {
                        Text("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.Indeterminate, onClick = {})
                        .testTag("indeterminateToggleable"),
                    children = {
                        Text("ToggleableText")
                    }
                )
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher = SemanticsMatcher.expectValue(
            FoundationSemanticsProperties.ToggleableState, ToggleableState.Indeterminate
        )

        rule.onNodeWithTag("checkedToggleable")
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        rule.onNodeWithTag("unCheckedToggleable")
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
        rule.onNodeWithTag("indeterminateToggleable")
            .assertIsEnabled()
            .assert(hasIndeterminateState())
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_booleanOverload_defaultSemantics() {
        rule.setContent {
            Column {
                Box(
                    Modifier
                        .toggleable(value = true, onValueChange = {})
                        .testTag("checkedToggleable"),
                    children = {
                        Text("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .toggleable(value = false, onValueChange = {})
                        .testTag("unCheckedToggleable"),
                    children = {
                        Text("ToggleableText")
                    }
                )
            }
        }

        rule.onNodeWithTag("checkedToggleable")
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        rule.onNodeWithTag("unCheckedToggleable")
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_disabledSemantics() {
        rule.setContent {
            Box {
                Box(
                    Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        onClick = {},
                        enabled = false
                    ),
                    children = {
                        Text("ToggleableText")
                    }
                )
            }
        }

        rule.onNode(isToggleable())
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        rule.setContent {
            Box {
                Box(
                    Modifier.toggleable(value = checked, onValueChange = onCheckedChange),
                    children = {
                        Text("ToggleableText")
                    }
                )
            }
        }

        rule.onNode(isToggleable())
            .performClick()

        rule.runOnIdle {
            assertThat(checked).isEqualTo(false)
        }
    }

    @Test
    fun toggleableTest_interactionState() {
        val interactionState = InteractionState()

        rule.setContent {
            Box {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionState = interactionState,
                        onValueChange = {}
                    )
                ) {
                    Text("ToggleableText")
                }
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithText("ToggleableText")
            .performGesture { down(center) }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        rule.onNodeWithText("ToggleableText")
            .performGesture { up() }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun toggleableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitToggleableText by mutableStateOf(true)

        rule.setContent {
            Box {
                if (emitToggleableText) {
                    Box(
                        Modifier.toggleable(
                            value = true,
                            interactionState = interactionState,
                            onValueChange = {}
                        )
                    ) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithText("ToggleableText")
            .performGesture { down(center) }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose toggleable
        rule.runOnIdle {
            emitToggleableText = false
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }
}
