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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.filters.MediumTest
import androidx.compose.testutils.expectError
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.editableText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class FindersTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findAll_zeroOutOfOne_findsNone() {
        rule.setContent {
            BoundaryNode { testTag = "not_myTestTag" }
        }

        rule.onAllNodes(hasTestTag("myTestTag")).assertCountEquals(0)
    }

    @Test
    fun findAll_oneOutOfTwo_findsOne() {
        rule.setContent {
            BoundaryNode { testTag = "myTestTag" }
            BoundaryNode { testTag = "myTestTag2" }
        }

        rule.onAllNodes(hasTestTag("myTestTag"))
            .assertCountEquals(1)
            .onFirst()
            .assert(hasTestTag("myTestTag"))
    }

    @Test
    fun findAll_twoOutOfTwo_findsTwo() {
        rule.setContent {
            BoundaryNode { testTag = "myTestTag" }
            BoundaryNode { testTag = "myTestTag" }
        }

        rule.onAllNodes(hasTestTag("myTestTag"))
            .assertCountEquals(2)
            .apply {
                get(0).assert(hasTestTag("myTestTag"))
                get(1).assert(hasTestTag("myTestTag"))
            }
    }

    @Test
    fun findByText_matches() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        rule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun findByText_withEditableText_matches() {
        rule.setContent {
            BoundaryNode { editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun findByText_merged_matches() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Text("Hello")
                Text("World")
            }
        }

        rule.onNodeWithText("Hello").assertExists()
        rule.onNodeWithText("World").assertExists()
        rule.onAllNodesWithText("Hello").assertCountEquals(1)
        rule.onAllNodesWithText("World").assertCountEquals(1)
    }

    @Test(expected = AssertionError::class)
    fun findByText_fails() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        // Need to assert exists or it won't fail
        rule.onNodeWithText("World").assertExists()
    }

    @Test(expected = AssertionError::class)
    fun findByText_merged_fails() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Text("Hello")
                Text("World")
            }
        }

        rule.onNodeWithText("Hello, World").assertExists()
    }

    @Test
    fun findBySubstring_matches() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        rule.onNodeWithText("World", substring = true).assertExists()
    }

    @Test
    fun findBySubstring_merged_matches() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Text("Hello")
                Text("World")
            }
        }

        rule.onNodeWithText("Wo", substring = true).assertExists()
        rule.onNodeWithText("He", substring = true).assertExists()
    }

    @Test
    fun findBySubstring_ignoreCase_matches() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        rule.onNodeWithText("world", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun findBySubstring_wrongCase_fails() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        expectError<AssertionError> {
            // Need to assert exists or it won't fetch nodes
            rule.onNodeWithText("world", substring = true).assertExists()
        }
    }

    @Test
    fun findAllBySubstring() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
            BoundaryNode { text = AnnotatedString("Wello Horld") }
        }

        rule.onAllNodesWithText("Yellow World", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Hello", substring = true).assertCountEquals(1)
        rule.onAllNodesWithText("Wello", substring = true).assertCountEquals(1)
        rule.onAllNodesWithText("ello", substring = true).assertCountEquals(2)
    }

    @Test
    fun findByContentDescription_matches() {
        rule.setContent {
            BoundaryNode { contentDescription = "Hello World" }
        }

        rule.onNodeWithContentDescription("Hello World").assertExists()
    }

    @Test
    fun findByContentDescription_merged_matches() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Box(Modifier.semantics { contentDescription = "Hello" })
                Box(Modifier.semantics { contentDescription = "World" })
            }
        }

        rule.onNodeWithContentDescription("Hello").assertExists()
        rule.onNodeWithContentDescription("World").assertExists()
        rule.onAllNodesWithContentDescription("Hello").assertCountEquals(1)
        rule.onAllNodesWithContentDescription("World").assertCountEquals(1)
    }

    @Test(expected = AssertionError::class)
    fun findByContentDescription_fails() {
        rule.setContent {
            BoundaryNode { contentDescription = "Hello World" }
        }

        rule.onNodeWithContentDescription("Hello").assertExists()
    }

    @Test(expected = AssertionError::class)
    fun findByContentDescription_merged_fails() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Box(Modifier.semantics { contentDescription = "Hello" })
                Box(Modifier.semantics { contentDescription = "World" })
            }
        }

        rule.onNodeWithText("Hello, World").assertExists()
    }

    @Test
    fun findByContentDescription_substring_matches() {
        rule.setContent {
            BoundaryNode { contentDescription = "Hello World" }
        }

        rule.onNodeWithContentDescription("World", substring = true).assertExists()
    }

    @Test
    fun findByContentDescription_merged_substring_matches() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Box(Modifier.semantics { contentDescription = "Hello" })
                Box(Modifier.semantics { contentDescription = "World" })
            }
        }

        rule.onNodeWithContentDescription("Wo", substring = true).assertExists()
        rule.onNodeWithContentDescription("He", substring = true).assertExists()
    }

    fun findByContentDescription_substring_noResult() {
        rule.setContent {
            BoundaryNode { contentDescription = "Hello World" }
        }

        rule.onNodeWithContentDescription("world", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun findByContentDescription_substring_ignoreCase_matches() {
        rule.setContent {
            BoundaryNode { contentDescription = "Hello World" }
        }

        rule.onNodeWithContentDescription("world", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)) {
        Column(Modifier.semantics(properties = props)) {}
    }
}