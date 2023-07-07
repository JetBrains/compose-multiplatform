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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.compose.ui.test.util.obfuscateNodesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PrintToStringTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun printToString_nothingFound() {
        rule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies:"
        ) {
            rule.onNodeWithText("Oops").printToString()
        }
    }

    @Test
    fun printToString_one() {
        rule.setContent {
            ComposeSimpleCase()
        }

        val result = rule.onNodeWithText("Hello")
            .printToString(maxDepth = 0)

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[Hello]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
            """.trimIndent()
        )
    }

    @Test
    fun printToString_many() {
        rule.setContent {
            ComposeSimpleCase()
        }

        val result = rule.onRoot()
            .onChildren()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                1) Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[Hello]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
                2) Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[World]'
                Actions = [GetTextLayoutResult]
                Has 1 sibling
            """.trimIndent()
        )
    }

    @Test
    fun printHierarchy() {
        rule.setContentWithoutMinimumTouchTarget {
            Column(Modifier.semantics { this.disabled(); this.testTag = "column" }) {
                Box(Modifier.semantics { this.disabled(); this.testTag = "box" }) {
                    Button(onClick = {}) {
                        Text("Button")
                    }
                }
                Text("Hello")
            }
        }

        val result = rule.onRoot()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                Node #X at (l=X, t=X, r=X, b=X)px
                 |-Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'column'
                   [Disabled]
                    |-Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'box'
                    | [Disabled]
                    |  |-Node #X at (l=X, t=X, r=X, b=X)px
                    |    Role = 'Button'
                    |    Focused = 'false'
                    |    Text = '[Button]'
                    |    Actions = [OnClick, RequestFocus, GetTextLayoutResult]
                    |    MergeDescendants = 'true'
                    |-Node #X at (l=X, t=X, r=X, b=X)px
                      Text = '[Hello]'
                      Actions = [GetTextLayoutResult]
            """.trimIndent()
        )
    }

    @Test
    fun printMultiple_withDepth() {
        rule.setContent {
            BoundaryNode("tag1") {
                BoundaryNode("tag11") {
                    BoundaryNode("tag111")
                }
            }
            BoundaryNode("tag2") {
                BoundaryNode("tag22") {
                    BoundaryNode("tag222")
                }
            }
        }

        val result = rule.onRoot()
            .onChildren()
            .printToString(maxDepth = 1)

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                1) Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'tag1'
                 |-Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'tag11'
                   Has 1 child
                2) Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'tag2'
                 |-Node #X at (l=X, t=X, r=X, b=X)px, Tag: 'tag22'
                   Has 1 child
            """.trimIndent()
        )
    }

    @Test
    fun printMergedContentDescriptions() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Box(Modifier.semantics { contentDescription = "first" })
                Box(Modifier.semantics { contentDescription = "second" })
            }
        }

        val result = rule.onRoot()
            .onChild()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                Node #X at (l=X, t=X, r=X, b=X)px
                ContentDescription = '[first, second]'
                MergeDescendants = 'true'
            """.trimIndent()
        )
    }

    @Test
    fun printUnmergedContentDescriptions() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Box(Modifier.semantics { contentDescription = "first" })
                Box(Modifier.semantics { contentDescription = "second" })
            }
        }

        val result = rule.onRoot(useUnmergedTree = true)
            .onChild()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'true'
                Node #X at (l=X, t=X, r=X, b=X)px
                MergeDescendants = 'true'
                 |-Node #X at (l=X, t=X, r=X, b=X)px
                 | ContentDescription = '[first]'
                 |-Node #X at (l=X, t=X, r=X, b=X)px
                   ContentDescription = '[second]'
            """.trimIndent()
        )
    }

    @Test
    fun printMergedText() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Text("first")
                Text("second")
            }
        }

        val result = rule.onRoot()
            .onChild()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'false'
                Node #X at (l=X, t=X, r=X, b=X)px
                Text = '[first, second]'
                Actions = [GetTextLayoutResult]
                MergeDescendants = 'true'
            """.trimIndent()
        )
    }

    @Test
    fun printUnmergedText() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) { }) {
                Text("first")
                Text("second")
            }
        }

        val result = rule.onRoot(useUnmergedTree = true)
            .onChild()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo(
            """
                Printing with useUnmergedTree = 'true'
                Node #X at (l=X, t=X, r=X, b=X)px
                MergeDescendants = 'true'
                 |-Node #X at (l=X, t=X, r=X, b=X)px
                 | Text = '[first]'
                 | Actions = [GetTextLayoutResult]
                 |-Node #X at (l=X, t=X, r=X, b=X)px
                   Text = '[second]'
                   Actions = [GetTextLayoutResult]
            """.trimIndent()
        )
    }

    @Composable
    fun ComposeSimpleCase() {
        MaterialTheme {
            Column {
                Text("Hello")
                Text("World")
            }
        }
    }
}