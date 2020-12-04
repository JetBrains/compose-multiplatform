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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ToggleableTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun toggleableTest_defaultSemantics() {
        rule.setContent {
            Column {
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.On, onClick = {})
                        .testTag("checkedToggleable"),
                    content = {
                        BasicText("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.Off, onClick = {})
                        .testTag("unCheckedToggleable"),
                    content = {
                        BasicText("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .triStateToggleable(state = ToggleableState.Indeterminate, onClick = {})
                        .testTag("indeterminateToggleable"),
                    content = {
                        BasicText("ToggleableText")
                    }
                )
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher = SemanticsMatcher.expectValue(
            SemanticsProperties.ToggleableState, ToggleableState.Indeterminate
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
                    content = {
                        BasicText("ToggleableText")
                    }
                )
                Box(
                    Modifier
                        .toggleable(value = false, onValueChange = {})
                        .testTag("unCheckedToggleable"),
                    content = {
                        BasicText("ToggleableText")
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
                    content = {
                        BasicText("ToggleableText")
                    }
                )
            }
        }

        rule.onNode(isToggleable())
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        rule.setContent {
            Box {
                Box(
                    Modifier.toggleable(value = checked, onValueChange = onCheckedChange),
                    content = {
                        BasicText("ToggleableText")
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
                    BasicText("ToggleableText")
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
                        BasicText("ToggleableText")
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

    @Test
    fun testInspectorValue() {
        rule.setContent {
            val modifier = Modifier.toggleable(value = true, onValueChange = {}) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("toggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "value",
                "enabled",
                "indication",
                "interactionState",
                "onValueChange",
            )
        }
    }

    @Test
    fun testInspectorValueTriState() {
        rule.setContent {
            val modifier = Modifier.triStateToggleable(state = ToggleableState.On, onClick = {})
                as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("triStateToggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "state",
                "enabled",
                "indication",
                "interactionState",
                "onClick",
            )
        }
    }
}
