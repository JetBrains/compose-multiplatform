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
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class HasAnyDescendantTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findByDescendant_directDescendant_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        rule.onNode(hasAnyDescendant(hasTestTag("Child")) and hasTestTag("Parent"))
            .assert(hasTestTag("Parent"))
    }

    @Test
    fun findByDescendant_indirectDescendant_matches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        rule.onNode(
            hasAnyDescendant(hasTestTag("Child")) and !hasTestTag("Parent")
                and hasTestTag("Grandparent")
        )
            .assert(hasTestTag("Grandparent"))
    }

    @Test
    fun findByDescendant_justSelf_oneMatch() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
        }

        rule.onNode(hasAnyDescendant(hasTestTag("Node")))
            .assertExists() // Root node
    }

    @Test
    fun findByDescendant_twoSubtrees_threeMatches() {
        rule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
            BoundaryNode(testTag = "Parent2") {
                BoundaryNode(testTag = "Child")
            }
        }

        rule.onAllNodes(hasAnyDescendant(hasTestTag("Child")))
            .assertCountEquals(3) // Parent, Parent2 and root
    }
}