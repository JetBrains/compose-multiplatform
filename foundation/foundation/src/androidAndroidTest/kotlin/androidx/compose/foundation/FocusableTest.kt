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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focusRequester
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFocus::class)
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

    @Test
    fun focusableTest_focusAcquire() {
        val (requester, otherRequester) = FocusRequester.createRefs()
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(requester)
                        .focusable()
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherRequester)
                        .focusable()
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()

        rule.runOnIdle {
            requester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsFocused()

        rule.runOnIdle {
            otherRequester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()
    }

    @Test
    fun focusableTest_interactionState() {
        val interactionState = InteractionState()
        val (requester, otherRequester) = FocusRequester.createRefs()
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(requester)
                        .focusable(interactionState = interactionState)
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherRequester)
                        .focusable()
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Focused)
        }

        rule.runOnIdle {
            requester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Focused)
        }

        rule.runOnIdle {
            otherRequester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Focused)
        }
    }

    @Test
    fun focusableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        val requester = FocusRequester()
        var emitFocusableText by mutableStateOf(true)

        rule.setContent {
            Box {
                if (emitFocusableText) {
                    BasicText(
                        "focusableText",
                        modifier = Modifier
                            .testTag(focusTag)
                            .focusRequester(requester)
                            .focusable(interactionState = interactionState)
                    )
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Focused)
        }

        rule.runOnIdle {
            requester.requestFocus()
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Focused)
        }

        // Dispose focusable, Interaction should be gone
        rule.runOnIdle {
            emitFocusableText = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Focused)
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
                    "interactionState"
                )
        }
    }
}