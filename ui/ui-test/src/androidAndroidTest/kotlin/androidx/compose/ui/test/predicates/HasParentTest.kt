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
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class HasParentTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findByParent_oneSubtree_oneChild_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        rule.onNode(hasParent(hasTestTag("Parent")))
            .assert(hasTestTag("Child"))
    }

    @Test
    fun findByParent_oneSubtree_twoChildren_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onAllNodes(hasParent(hasTestTag("Parent")))
            .assertCountEquals(2)
    }

    @Test
    fun findByParent_twoSubtrees_twoChildren_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child3")
                BoundaryNode(testTag = "Child4")
            }
        }

        rule.onAllNodes(hasParent(hasTestTag("Parent")))
            .assertCountEquals(4)
    }

    @Test
    fun findByParent_nothingFound() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "ExtraNode") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        rule.onNode(
            hasParent(hasTestTag("Parent"))
                and hasTestTag("Child")
        )
            .assertDoesNotExist()
    }

    @Test
    fun findByGrandParent_oneFound() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "ExtraNode") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        rule.onNode(hasParent(hasParent(hasTestTag("Parent"))))
            .assert(hasTestTag("Child"))
    }
}