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

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.MediumTest
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class AssertAllTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun twoNodes_twoSatisfied() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        rule.onNodeWithTag("Parent")
            .onChildren()
            .assertAll(hasTestTag("Child1") or hasTestTag("Child2"))
    }

    @Test
    fun twoNodes_oneSatisfied() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith(
            "Failed to assertAll(TestTag = 'Child1')\n" +
                "Found '1' node not matching:\n" +
                "Node #X"
        ) {
            rule.onNodeWithTag("Parent")
                .onChildren()
                .assertAll(hasTestTag("Child1"))
        }
    }

    @Test
    fun threeNodes_oneSatisfied() {
        rule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
                BoundaryNode(testTag = "Child3")
            }
        }

        expectErrorMessageStartsWith(
            "Failed to assertAll(TestTag = 'Child1')\n" +
                "Found '2' nodes not matching:\n" +
                "1) "
        ) {
            rule.onNodeWithTag("Parent")
                .onChildren()
                .assertAll(hasTestTag("Child1"))
        }
    }

    @Test
    fun zeroNodes() {
        rule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        rule.onNodeWithTag("Parent")
            .onChildren()
            .assertCountEquals(0)
            .assertAll(hasTestTag("Child"))
    }
}
