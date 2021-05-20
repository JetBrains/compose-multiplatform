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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class AssertContentDescription {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun equals() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionEquals("Hello", "World")
        rule.onNodeWithTag("test")
            .assertContentDescriptionEquals("World", "Hello")
    }

    @Test
    fun equals_empty() {
        rule.setContent {
            Box(Modifier.semantics { testTag = "test" })
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionEquals()
    }

    @Test
    fun contains() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionContains("Hello")
        rule.onNodeWithTag("test")
            .assertContentDescriptionContains("World")
    }

    @Test
    fun contains_substring() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionContains("He", substring = true)
        rule.onNodeWithTag("test")
            .assertContentDescriptionContains("Wo", substring = true)
    }

    @Test(expected = AssertionError::class)
    fun equals_fails_notEnoughElements() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionEquals("Hello")
    }

    @Test(expected = AssertionError::class)
    fun equals_fails_tooManyElements() {
        rule.setContent {
            TestContent()
        }
        rule.onNodeWithTag("test")
            .assertContentDescriptionEquals("Hello", "World", "More")
    }

    @Composable
    fun TestContent() {
        Box(Modifier.semantics(mergeDescendants = true) { testTag = "test" }) {
            Box(Modifier.semantics { contentDescription = "Hello" })
            Box(Modifier.semantics { contentDescription = "World" })
        }
    }
}
