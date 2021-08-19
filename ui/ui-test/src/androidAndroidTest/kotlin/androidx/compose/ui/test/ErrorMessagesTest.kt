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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.expectErrorMessage
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
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
            """
                Failed: assertExists.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'MyButton3')
            """.trimIndent()
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
            """
                Failed to perform a gesture.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'MyButton3')
            """.trimIndent()

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
            """
                Failed to perform a gesture.
                Reason: Expected exactly '1' node but could not find any node that satisfies: ((TestTag = 'MyButton3') && (OnClick is defined))
            """.trimIndent()
        ) {
            rule.onNode(hasTestTag("MyButton3") and hasClickAction())
                .performClick()
        }
    }

    @Test
    fun findByText_doClick_butMoreThanOneElementFound() {
        rule.setContentWithoutMinimumTouchTarget {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            """
                Failed to perform a gesture.
                Reason: Expected exactly '1' node but found '2' nodes that satisfy: (Text + EditableText contains 'Toggle' (ignoreCase: false))
                Nodes found:
                1) Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'MyButton'
            """.trimIndent()
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
            """
                Failed to perform OnClick action.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'MyButton3')
            """.trimIndent()
        ) {
            rule.onNodeWithTag("MyButton3")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    @Test
    fun findByTag_assertDoesNotExist_butElementFound() {
        rule.setContentWithoutMinimumTouchTarget {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            """
                Failed: assertDoesNotExist.
                Reason: Did not expect any node but found '1' node that satisfies: (TestTag = 'MyButton')
                Node found:
                Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'MyButton'
            """.trimIndent()
        ) {
            rule.onNodeWithTag("MyButton")
                .assertDoesNotExist()
        }
    }

    @Test
    fun findAll_assertMultiple_butIsDifferentAmount() {
        rule.setContentWithoutMinimumTouchTarget {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            """
                Failed to assert count of nodes.
                Reason: Expected '3' nodes but found '2' nodes that satisfy: (Text + EditableText contains 'Toggle' (ignoreCase: false))
                Nodes found:
                1) Node #X at (l=X, t=X, r=X, b=X)px
            """.trimIndent()
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
            """
                Failed to assert count of nodes.
                Reason: Expected '3' nodes but could not find any node that satisfies: (Text + EditableText contains 'Toggle2' (ignoreCase: false))
            """.trimIndent()
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

        expectErrorMessage(
            """
                Failed to perform a gesture.
                The node is no longer in the tree, last known semantics:
                Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[Hello]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
                Original selector: Text + EditableText contains 'Hello' (ignoreCase: false)
            """.trimIndent()
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

        expectErrorMessage(
            """
                Failed: assertExists.
                The node is no longer in the tree, last known semantics:
                Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[Hello]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
                Original selector: Text + EditableText contains 'Hello' (ignoreCase: false)
            """.trimIndent()
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

        expectErrorMessage(
            """
                Failed to assert the following: (OnClick is defined)
                The node is no longer in the tree, last known semantics:
                Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[Hello]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
                Original selector: Text + EditableText contains 'Hello' (ignoreCase: false)
            """.trimIndent()
        ) {
            node.assertHasClickAction()
        }
    }

    @Test
    fun findByTag_assertExists_noElementFoundButFoundInMerged() {
        rule.setContent {
            ComposeMerged()
        }

        expectErrorMessage(
            """
                Failed: assertExists.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + EditableText contains 'Banana' (ignoreCase: false))
                However, the unmerged tree contains '1' node that matches. Are you missing `useUnmergedNode = true` in your finder?
            """.trimIndent()
        ) {
            rule.onNodeWithText("Banana")
                .assertExists()
        }
    }
    @Test
    fun findByTag_assertExists_NoElementFoundButMultipleFoundInMerged() {
        rule.setContent {
            ComposeMerged(5)
        }

        expectErrorMessage(
            """
                Failed: assertExists.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + EditableText contains 'Banana' (ignoreCase: false))
                However, the unmerged tree contains '5' nodes that match. Are you missing `useUnmergedNode = true` in your finder?
            """.trimIndent()
        ) {
            rule.onNodeWithText("Banana")
                .assertExists()
        }
    }

    @Test
    fun findByTag_performAction_NoElementFoundButFoundInMerged() {
        rule.setContent {
            ComposeMerged()
        }

        expectErrorMessage(
            """
                Failed to perform a gesture.
                Reason: Expected exactly '1' node but could not find any node that satisfies: (Text + EditableText contains 'Banana' (ignoreCase: false))
                However, the unmerged tree contains '1' node that matches. Are you missing `useUnmergedNode = true` in your finder?
            """.trimIndent()
        ) {
            rule.onNodeWithText("Banana")
                .performClick()
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
    fun ComposeMerged(numberOfTexts: Int = 1) {
        Column {
            TestButton(
                modifier = Modifier
                    .testTag("MyButton")
                    .clearAndSetSemantics { text = AnnotatedString("Not Banana") }
            ) {
                repeat(numberOfTexts) {
                    Text("Banana")
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

fun ComposeContentTestRule.setContentWithoutMinimumTouchTarget(
    composable: @Composable () -> Unit
) {
    setContent {
        val oldViewConfiguration = LocalViewConfiguration.current
        val viewConfiguration = TestViewConfiguration(
            longPressTimeoutMillis = oldViewConfiguration.longPressTimeoutMillis,
            doubleTapTimeoutMillis = oldViewConfiguration.doubleTapTimeoutMillis,
            doubleTapMinTimeMillis = oldViewConfiguration.doubleTapMinTimeMillis,
            touchSlop = oldViewConfiguration.touchSlop,
            minimumTouchTargetSize = DpSize.Zero
        )
        CompositionLocalProvider(
            LocalViewConfiguration provides viewConfiguration,
            content = composable
        )
    }
}