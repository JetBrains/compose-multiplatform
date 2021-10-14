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

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectableTest {

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
    fun selectable_defaultSemantics() {
        rule.setContent {
            BasicText(
                "Text in item",
                modifier = Modifier.selectable(selected = true, onClick = {})
            )
        }

        rule.onAllNodes(isSelectable())
            .assertCountEquals(1)
            .onFirst()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsSelected()
    }

    @Test
    fun selectable_defaultClicks() {
        rule.setContent {
            val state = remember { mutableStateOf(false) }
            BasicText(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = state.value,
                    onClick = { state.value = !state.value }
                )
            )
        }

        rule.onNode(isSelectable())
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun selectable_noClicksNoChanges() {
        rule.setContent {
            val (selected, _) = remember { mutableStateOf(false) }
            BasicText(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = {}
                )
            )
        }

        rule.onNode(isSelectable())
            .assertIsNotSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun selectable_clicks_noPropagationWhenDisabled() {
        val enabled = mutableStateOf(false)
        rule.setContent {
            val state = remember { mutableStateOf(false) }
            val outerState = remember { mutableStateOf(false) }
            Box(
                Modifier
                    .testTag("outerBox")
                    .selectable(
                        selected = outerState.value,
                        onClick = { outerState.value = !outerState.value }
                    )
            ) {
                BasicText(
                    "Text in item",
                    modifier = Modifier.selectable(
                        selected = state.value,
                        onClick = { state.value = !state.value },
                        enabled = enabled.value
                    )
                )
            }
        }

        rule.onNodeWithText("Text in item")
            .assertIsNotSelected()
            .performClick()
            .assertIsNotSelected()

        rule.onNodeWithTag("outerBox")
            .assertIsNotSelected()
        rule.runOnIdle { enabled.value = true }

        rule.onNodeWithText("Text in item")
            .performClick()
            .assertIsSelected()

        rule.onNodeWithTag("outerBox")
            .assertIsNotSelected()
    }

    @Test
    fun selectableTest_interactionSource() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {}
                    )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithText("SelectableText")
            .performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithText("SelectableText")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun selectableTest_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitSelectableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitSelectableText) {
                    Box(
                        Modifier.selectable(
                            selected = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        BasicText("SelectableText")
                    }
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithText("SelectableText")
            .performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose selectable
        rule.runOnIdle {
            emitSelectableText = false
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectableTest_interactionSource_hover() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {}
                    )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithText("SelectableText")
            .performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithText("SelectableText")
            .performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1])
                .isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun selectableTest_interactionSource_focus_inTouchMode() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        val focusRequester = FocusRequester()

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .selectable(
                            selected = true,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {}
                        )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Touch mode by default, so we shouldn't be focused
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun selectableTest_interactionSource_focus_inKeyboardMode() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        val focusRequester = FocusRequester()

        lateinit var focusManager: FocusManager

        val keyboardInputModeManager = object : InputModeManager {
            override val inputMode = InputMode.Keyboard

            @OptIn(ExperimentalComposeUiApi::class)
            override fun requestInputMode(inputMode: InputMode) = true
        }

        rule.setContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            CompositionLocalProvider(LocalInputModeManager provides keyboardInputModeManager) {
                Box {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .selectable(
                                selected = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        BasicText("SelectableText")
                    }
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Keyboard mode, so we should now be focused and see an interaction
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle {
            focusManager.clearFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1])
                .isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    // TODO: b/202871171 - add test for changing between keyboard mode and touch mode, making sure
    // it resets existing focus

    @Test
    fun selectableTest_testInspectorValue_noIndication() {
        rule.setContent {
            val modifier = Modifier.selectable(false) {} as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("selectable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "selected",
                "enabled",
                "role",
                "onClick"
            )
        }
    }

    @Test
    fun selectableTest_testInspectorValue_fullParams() {
        rule.setContent {
            val modifier = Modifier.selectable(
                false,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}.first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("selectable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "selected",
                "enabled",
                "role",
                "interactionSource",
                "indication",
                "onClick"
            )
        }
    }
}