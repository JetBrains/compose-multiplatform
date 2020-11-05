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

package androidx.compose.ui.test.selectors

import androidx.test.filters.MediumTest
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class AddIndexSelectorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun getFirst() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onNodeWithTag("Parent")
            .onChildren()
            .onFirst()
            .assert(hasTestTag("Child1"))
    }

    @Test
    fun getAtIndex() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onNodeWithTag("Parent")
            .onChildAt(1)
            .assert(hasTestTag("Child2"))
    }

    @Test
    fun getAtIndex_wrongIndex_fail() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed: assertExists.\n" +
                "Can't retrieve node at index '2' of '(TestTag = 'Parent').children'\n" +
                "There are '2' nodes only:"
        ) {
            rule.onNodeWithTag("Parent")
                .onChildAt(2)
                .assertExists()
        }
    }

    @Test
    fun getAtIndex_noItems() {
        rule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        rule.onNodeWithTag("Parent")
            .onChildAt(2)
            .assertDoesNotExist()
    }

    @Test
    fun getAtIndex_noItems_fail() {
        rule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed: assertExists.\n" +
                "Can't retrieve node at index '2' of '(TestTag = 'Parent').children'\n" +
                "There are no existing nodes for that selector."
        ) {
            rule.onNodeWithTag("Parent")
                .onChildAt(2)
                .assertExists()
        }
    }
}