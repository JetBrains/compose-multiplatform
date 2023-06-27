/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Rule
import org.junit.Test


/**
 * Tests the node-finding (e.g. [onNodeWithTag]) functionality of the testing framework.
 */
class MatchersTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testNodeWithTag() {
        rule.setContent {
            Box(
                Modifier.testTag("tag")
            ) {
                Button(onClick = {}) {
                    Text("Hello", Modifier.testTag("text1"))
                    Text("Compose", Modifier.testTag("text2"))
                }
            }
        }

        rule.onNodeWithTag("tag").assertExists()
        rule.onNodeWithTag("mark").assertDoesNotExist()
        rule.onNodeWithTag("text1", useUnmergedTree = false).assertDoesNotExist()
        rule.onNodeWithTag("text1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun testAllNodesWithTag() {
        rule.setContent {
            Box(Modifier.testTag("tag"))
            Text("text", Modifier.testTag("tag"))
        }

        rule.onAllNodesWithTag("tag").assertCountEquals(2)
    }

    @Test
    fun testNodeWithText() {
        rule.setContent {
            Text("text")
            Button(onClick = {}) {
                Text("Hello", Modifier.testTag("text1"))
                Text("Compose", Modifier.testTag("text2"))
            }
        }

        rule.onNodeWithText("text").assertExists()
        rule.onNodeWithText("Text").assertDoesNotExist()
        rule.onNodeWithText("Text", ignoreCase = true).assertExists()
        rule.onNodeWithText("Hello").assertHasClickAction()  // Button
        rule.onNodeWithText("Hello", useUnmergedTree = true).assertHasNoClickAction() // Text
        rule.onNodeWithText("tex").assertDoesNotExist()
        rule.onNodeWithText("tex", substring = true).assertExists()
    }

    @Test
    fun testAllNodesWithText() {
        rule.setContent {
            Text("text")
            Text("text")
            Text("long text")
            Text("Text")
        }

        rule.onAllNodesWithText("text").assertCountEquals(2)
        rule.onAllNodesWithText("text", substring = true).assertCountEquals(3)
        rule.onAllNodesWithText("text", ignoreCase = true).assertCountEquals(3)
        rule.onAllNodesWithText("text", ignoreCase = true, substring = true).assertCountEquals(4)
    }

    @Test
    fun testNodeWithContentDescription() {
        rule.setContent {
            Box(Modifier.semantics { contentDescription = "desc" })
            Button(onClick = {}) {
                Box(Modifier.semantics { contentDescription = "Hello" })
                Box(Modifier.semantics { contentDescription = "Compose" })
            }
        }

        rule.onNodeWithContentDescription("desc").assertExists()
        rule.onNodeWithContentDescription("Desc").assertDoesNotExist()
        rule.onNodeWithContentDescription("Desc", ignoreCase = true).assertExists()
        rule.onNodeWithContentDescription("Hello").assertHasClickAction()  // Button
        rule.onNodeWithContentDescription("Hello", useUnmergedTree = true)  // Text
            .assertHasNoClickAction()
        rule.onNodeWithContentDescription("des").assertDoesNotExist()
        rule.onNodeWithContentDescription("desc", substring = true).assertExists()
    }

    @Test
    fun testAllNodesWithContentDescription() {
        rule.setContent {
            Box(Modifier.semantics { contentDescription = "desc" })
            Box(Modifier.semantics { contentDescription = "desc" })
            Box(Modifier.semantics { contentDescription = "long desc" })
            Box(Modifier.semantics { contentDescription = "Desc" })
        }

        rule.onAllNodesWithContentDescription("desc").assertCountEquals(2)
        rule.onAllNodesWithContentDescription("desc", substring = true).assertCountEquals(3)
        rule.onAllNodesWithContentDescription("desc", ignoreCase = true).assertCountEquals(3)
        rule.onAllNodesWithContentDescription("desc", substring = true, ignoreCase = true)
            .assertCountEquals(4)
    }

    @Test
    fun testOnNode() {
        rule.setContent {
            Text("Hello", Modifier.semantics { contentDescription = "text" })
            Text("Compose")
        }

        fun expectText(text: String) = SemanticsMatcher.expectValue(
            key = SemanticsProperties.Text,
            expectedValue = listOf(AnnotatedString(text))
        )

        fun expectContentDescription(desc: String) = SemanticsMatcher.expectValue(
            key = SemanticsProperties.ContentDescription,
            expectedValue = listOf(desc)
        )

        rule.onNode(
            matcher = expectText("Hello").and(expectContentDescription("text"))
        ).assertExists()
        rule.onNode(
            matcher = expectText("Compose").and(expectContentDescription("text"))
        ).assertDoesNotExist()
    }
}
