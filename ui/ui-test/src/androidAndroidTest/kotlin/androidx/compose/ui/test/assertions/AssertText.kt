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

package androidx.compose.ui.test.assertions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class AssertText {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun equals() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertTextEquals("Hello", "World")
        rule.onNodeWithTag("test")
            .assertTextEquals("World", "Hello")
    }

    @Test
    fun equals_empty() {
        rule.setContent {
            Box(Modifier.semantics { testTag = "test" })
        }
        rule.onNodeWithTag("test")
            .assertTextEquals()
        rule.onNodeWithTag("test")
            .assertTextEquals(includeEditableText = false)
    }

    @Test
    fun contains() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertTextContains("Hello")
        rule.onNodeWithTag("test")
            .assertTextContains("World")
    }

    @Test
    fun contains_substring() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertTextContains("He", substring = true)
        rule.onNodeWithTag("test")
            .assertTextContains("Wo", substring = true)
    }

    @Test(expected = AssertionError::class)
    fun equals_fails_notEnoughElements() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertTextEquals("Hello")
    }

    @Test(expected = AssertionError::class)
    fun equals_fails_tooManyElements() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertTextEquals("Hello", "World", "More")
    }

    @Test(expected = AssertionError::class)
    fun textAndEditText() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { testTag = "test" }) {
                Text("Hello")
                Text("World")
                TextField("TextField", onValueChange = {})
            }
        }
        rule.onNodeWithTag("test")
            .assertTextEquals("Hello", "World", "TextField")
        rule.onNodeWithTag("test")
            .assertTextEquals("Hello", "World", includeEditableText = false)
        rule.onNodeWithTag("test")
            .assertTextContains("TextField")
    }

    @Test
    fun assertTextFieldText_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithTag("test")
            .assertTextEquals("Hello World")
    }

    @Test(expected = AssertionError::class)
    fun assertTextFieldText_fails() {
        rule.setContent {
            BoundaryNode { testTag = "test"; editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithTag("test")
            .assertTextEquals("Hello")
    }

    @Test
    fun assertTextFieldText_substring_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithTag("test")
            .assertTextContains("Hello", substring = true)
    }

    @Test(expected = AssertionError::class)
    fun assertTextFieldText_substring_fails() {
        rule.setContent {
            BoundaryNode { testTag = "test"; editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithTag("test")
            .assertTextContains("hello")
    }

    @Test
    fun assertTextFieldText_substring_ignoreCase_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; editableText = AnnotatedString("Hello World") }
        }

        rule.onNodeWithTag("test")
            .assertTextContains("hello", ignoreCase = true, substring = true)
    }

    @Composable
    fun TestContent() {
        Box(Modifier.semantics(mergeDescendants = true) { testTag = "test" }) {
            Text("Hello")
            Text("World")
        }
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)) {
        Column(Modifier.semantics(properties = props)) {}
    }
}
