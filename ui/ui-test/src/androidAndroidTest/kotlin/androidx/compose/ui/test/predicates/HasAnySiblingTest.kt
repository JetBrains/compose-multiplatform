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

package androidx.compose.ui.test.predicates

import androidx.test.filters.MediumTest
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class HasAnySiblingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findBySibling_oneSubtree_oneSibling_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        rule.onNode(hasAnySibling(hasTestTag("Sibling")))
            .assert(hasTestTag("Me"))
    }

    @Test
    fun findBySibling_oneSubtree_twoSiblings_matchesTwo() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
                BoundaryNode(testTag = "SomeoneElse")
            }
        }

        rule.onAllNodes(hasAnySibling(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_oneSubtree_oneSibling_matchesTwo() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Sibling")
                BoundaryNode(testTag = "Sibling")
            }
        }

        rule.onAllNodes(hasAnySibling(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_twoSubtrees_twoChildren_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        rule.onAllNodes(hasAnySibling(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_noSiblings_nothingFound() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
            }
        }

        rule.onNode(hasAnySibling(hasTestTag("Me")))
            .assertDoesNotExist()
    }

    @Test
    fun findBySibling_oneSiblings_differentTag_nothingFound() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        rule.onNode(hasAnySibling(hasTestTag("Sibling2")))
            .assertDoesNotExist()
    }

    @Test
    fun findBySibling_oneSiblings_notMySibling_nothingFound() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "SomeoneElse")
                BoundaryNode(testTag = "Sibling")
            }
        }

        rule.onNode(hasAnySibling(hasTestTag("Sibling")) and hasTestTag("Me"))
            .assertDoesNotExist()
    }

    @Test
    fun findByParentSibling_oneFound() {
        rule.setContent {
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Me")
                }
                BoundaryNode(testTag = "ParentSibling") {
                    BoundaryNode(testTag = "SomeoneElse")
                }
            }
        }

        rule.onNode(hasParent(hasAnySibling(hasTestTag("ParentSibling"))))
            .assert(hasTestTag("Me"))
    }
}