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

package androidx.compose.ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextActionsTest {

    private val fieldTag = "Field"

    @get:Rule
    val rule = createComposeRule()

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    fun TextFieldUi(
        imeAction: ImeAction = ImeAction.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        textCallback: (String) -> Unit = {}
    ) {
        val state = remember { mutableStateOf("") }
        BasicTextField(
            modifier = Modifier.testTag(fieldTag),
            value = state.value,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = keyboardActions,
            onValueChange = {
                state.value = it
                textCallback(it)
            }
        )
    }

    @Test
    fun sendText_clearText() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello!")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello!")
        }

        rule.onNodeWithTag(fieldTag)
            .performTextClearance()

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("")
        }
    }

    @FlakyTest(bugId = 215584831)
    @Test
    fun sendTextTwice_shouldAppend() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello ")

        rule.onNodeWithTag(fieldTag)
            .performTextInput("world!")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    // @Test - not always appends, seems to be flaky
    fun sendTextTwice_shouldAppend_ver2() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        // This helps. So there must be some timing issue.
        // Thread.sleep(3000)

        rule.onNodeWithTag(fieldTag)
            .performTextInput(" world!")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    @Test
    fun replaceText() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello")
        }

        rule.onNodeWithTag(fieldTag)
            .performTextReplacement("world")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("world")
        }
    }

    @Test
    fun sendImeAction_search() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Search,
                keyboardActions = KeyboardActions(onSearch = { actionPerformed = true })
            )
        }
        assertThat(actionPerformed).isFalse()

        rule.onNodeWithTag(fieldTag)
            .performImeAction()

        rule.runOnIdle {
            assertThat(actionPerformed).isTrue()
        }
    }

    @Test
    fun sendImeAction_actionNotDefined_shouldFail() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Default,
                keyboardActions = KeyboardActions { actionPerformed = true }
            )
        }
        assertThat(actionPerformed).isFalse()

        expectErrorMessageStartsWith(
            "" +
                "Failed to perform IME action as current node does not specify any.\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performImeAction()
        }
    }

    @Test
    fun sendImeAction_inputNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(testTag = "node")
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed to perform IME action.\n" +
                "Failed to assert the following: (SetText is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag("node")
                .performImeAction()
        }
    }
}