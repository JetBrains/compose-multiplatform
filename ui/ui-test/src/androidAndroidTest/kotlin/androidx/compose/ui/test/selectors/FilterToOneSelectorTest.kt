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
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class FilterToOneSelectorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun twoNodes_filterToOne() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onNodeWithTag("Parent")
            .onChildren()
            .filterToOne(hasTestTag("Child1"))
            .assert(hasTestTag("Child1"))
    }

    @Test
    fun twoNodes_filterToTwo_fail() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but found '2' nodes that satisfy: " +
                "(((TestTag = 'Parent').children).filterToOne((TestTag = 'Child1') || " +
                "(TestTag = 'Child2')))\n" +
                "Nodes found:\n" +
                "1) "
        ) {
            rule.onNodeWithTag("Parent")
                .onChildren()
                .filterToOne(hasTestTag("Child1") or hasTestTag("Child2"))
                .assertExists()
        }
    }

    @Test
    fun twoNodes_filterToNone() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onNodeWithTag("Parent")
            .onChildren()
            .filterToOne(hasTestTag("Child"))
            .assertDoesNotExist()
    }

    @Test
    fun twoNodes_filterToNone_fail() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith(
            "" +
                "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(((TestTag = 'Parent').children).filterToOne(TestTag = 'Child'))"
        ) {
            rule.onNodeWithTag("Parent")
                .onChildren()
                .filterToOne(hasTestTag("Child"))
                .assertExists()
        }
    }
}