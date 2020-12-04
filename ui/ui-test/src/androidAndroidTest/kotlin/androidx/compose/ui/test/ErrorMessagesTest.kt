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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.expectErrorMessage
import androidx.compose.ui.test.util.expectErrorMessageMatches
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ErrorMessagesTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findByTag_assertHasClickAction() {
        rule.setContent {
            ComposeSimpleCase()
        }

        rule.onNodeWithTag("MyButton")
            .assertHasClickAction()
    }

    @Test
    fun findByTag_assertExists_butNoElementFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage(
            "" +
                "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            rule.onNodeWithTag("MyButton3")
                .assertExists()
        }
    }

    @Test
    fun findByTag_doClick_butNoElementFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage(
            "" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            rule.onNodeWithTag("MyButton3")
                .performClick()
        }
    }

    @Test
    fun findByPredicate_doClick_butNoElementFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage(
            "" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "((TestTag = 'MyButton3') && (OnClick is defined))"
        ) {
            rule.onNode(hasTestTag("MyButton3") and hasClickAction())
                .performClick()
        }
    }

    @Test
    fun findByText_doClick_butMoreThanOneElementFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but found '2' nodes that satisfy: " +
                "(Text = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Node #X at (X, X, X, X)px, Tag: 'MyButton'"
        ) {
            rule.onNodeWithText("Toggle")
                .performClick()
        }
    }

    @Test
    fun findByTag_callSemanticsAction_butElementDoesNotExist() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed to perform OnClick action.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            rule.onNodeWithTag("MyButton3")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    @Test
    fun findByTag_assertDoesNotExist_butElementFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed: assertDoesNotExist.\n" +
                "Reason: Did not expect any node but found '1' node that satisfies: " +
                "(TestTag = 'MyButton')\n" +
                "Node found:\n" +
                "Node #X at (X, X, X, X)px, Tag: 'MyButton'"
        ) {
            rule.onNodeWithTag("MyButton")
                .assertDoesNotExist()
        }
    }

    @Test
    fun findAll_assertMultiple_butIsDifferentAmount() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed to assert count of nodes.\n" +
                "Reason: Expected '3' nodes but found '2' nodes that satisfy: " +
                "(Text = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Node #X at (X, X, X, X)px"
        ) {
            rule.onAllNodesWithText("Toggle")
                .assertCountEquals(3)
        }
    }

    @Test
    fun findAll_assertMultiple_butIsZero() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage(
            "" +
                "Failed to assert count of nodes.\n" +
                "Reason: Expected '3' nodes but could not find any node that satisfies: " +
                "(Text = 'Toggle2' (ignoreCase: false))"
        ) {
            rule.onAllNodesWithText("Toggle2")
                .assertCountEquals(3)
        }
    }

    @Test
    fun findOne_hideIt_tryToClickIt_butDoesNotExist() {
        rule.setContent {
            ComposeTextToHideCase()
        }

        val node = rule.onNodeWithText("Hello")
            .assertExists()

        rule.onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessageMatches(
            "" +
                "Failed to perform a gesture.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at \\(X, X, X, X\\)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction\\(label=null, action=.*\\)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' \\(ignoreCase: false\\)"
        ) {
            node.performClick()
        }
    }

    @Test
    fun findOne_removeIt_assertExists_butDoesNotExist() {
        rule.setContent {
            ComposeTextToHideCase()
        }

        val node = rule.onNodeWithText("Hello")
            .assertExists()

        // Hide text
        rule.onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessageMatches(
            "" +
                "Failed: assertExists.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at \\(X, X, X, X\\)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction\\(label=null, action=.*\\)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' \\(ignoreCase: false\\)"
        ) {
            node.assertExists()
        }
    }

    @Test
    fun findOne_removeIt_assertHasClickAction_butDoesNotExist() {
        rule.setContent {
            ComposeTextToHideCase()
        }

        val node = rule.onNodeWithText("Hello")
            .assertExists()

        // Hide text
        rule.onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessageMatches(
            "" +
                "Failed to assert the following: \\(OnClick is defined\\)\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at \\(X, X, X, X\\)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction\\(label=null, action=.*\\)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' \\(ignoreCase: false\\)"
        ) {
            node.assertHasClickAction()
        }
    }

    @Composable
    fun ComposeSimpleCase() {
        MaterialTheme {
            Column {
                TestButton(Modifier.testTag("MyButton")) {
                    Text("Toggle")
                }
                TestButton(Modifier.testTag("MyButton2")) {
                    Text("Toggle")
                }
            }
        }
    }

    @Composable
    fun ComposeTextToHideCase() {
        MaterialTheme {
            val (showText, toggle) = remember { mutableStateOf(true) }
            Column {
                TestButton(
                    modifier = Modifier.testTag("MyButton"),
                    onClick = { toggle(!showText) }
                ) {
                    Text("Toggle")
                }
                if (showText) {
                    Text("Hello")
                }
            }
        }
    }

    @Composable
    fun TestButton(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        Surface {
            Box(modifier.clickable(onClick = onClick ?: {}, enabled = onClick != null)) {
                Box { content() }
            }
        }
    }
}