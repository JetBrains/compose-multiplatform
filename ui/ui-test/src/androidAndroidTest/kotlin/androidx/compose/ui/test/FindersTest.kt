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

import androidx.compose.foundation.layout.Column
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

    @Test(expected = AssertionError::class)
    fun findByText_fails() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        // Need to assert exists or it won't fail
        rule.onNodeWithText("World").assertExists()
    }

    @Test
    fun findBySubstring_matches() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        rule.onNodeWithSubstring("World").assertExists()
    }

    @Test
    fun findBySubstring_ignoreCase_matches() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        rule.onNodeWithSubstring("world", ignoreCase = true).assertExists()
    }

    @Test
    fun findBySubstring_wrongCase_fails() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
        }

        expectError<AssertionError> {
            // Need to assert exists or it won't fetch nodes
            rule.onNodeWithSubstring("world").assertExists()
        }
    }

    @Test
    fun findAllBySubstring() {
        rule.setContent {
            BoundaryNode { text = AnnotatedString("Hello World") }
            BoundaryNode { text = AnnotatedString("Wello Horld") }
        }

        rule.onAllNodesWithSubstring("Yellow World").assertCountEquals(0)
        rule.onAllNodesWithSubstring("Hello").assertCountEquals(1)
        rule.onAllNodesWithSubstring("Wello").assertCountEquals(1)
        rule.onAllNodesWithSubstring("ello").assertCountEquals(2)
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)) {
        Column(Modifier.semantics(properties = props)) {}
    }
}