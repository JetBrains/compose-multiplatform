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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
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
class FocusableTest {

    @get:Rule
    val rule = createComposeRule()

    val focusTag = "myFocusable"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun focusableTest_defaultSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusable()
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assertIsEnabled()
            .assert(isFocusable())
    }

    @Test
    fun focusableTest_disabledSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusable(enabled = false)
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assert(isNotFocusable())
    }

    @ExperimentalComposeUiApi
    @Test
    fun focusableTest_focusAcquire() {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(focusRequester)
                        .focusable()
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherFocusRequester)
                        .focusable()
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsFocused()

        rule.runOnIdle {
            otherFocusRequester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()
    }

    @ExperimentalComposeUiApi
    @Test
    fun focusableTest_interactionSource() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherFocusRequester)
                        .focusable()
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle {
            otherFocusRequester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            Truth.assertThat(interactions[1])
                .isInstanceOf(FocusInteraction.Unfocus::class.java)
            Truth.assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusableTest_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var emitFocusableText by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitFocusableText) {
                    BasicText(
                        "focusableText",
                        modifier = Modifier
                            .testTag(focusTag)
                            .focusRequester(focusRequester)
                            .focusable(interactionSource = interactionSource)
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Dispose focusable, Interaction should be gone
        rule.runOnIdle {
            emitFocusableText = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            Truth.assertThat(interactions[1])
                .isInstanceOf(FocusInteraction.Unfocus::class.java)
            Truth.assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusableText_testInspectorValue() {
        rule.setContent {
            val modifier = Modifier.focusable() as InspectableValue
            Truth.assertThat(modifier.nameFallback).isEqualTo("focusable")
            Truth.assertThat(modifier.valueOverride).isNull()
            Truth.assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "interactionSource"
                )
        }
    }
}
