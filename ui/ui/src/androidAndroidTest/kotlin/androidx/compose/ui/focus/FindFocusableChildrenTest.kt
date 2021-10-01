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

package androidx.compose.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class FindFocusableChildrenTest(private val excludeDeactivated: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "excludeDeactivated = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun returnsFirstFocusNodeInModifierChain() {
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        val focusModifier3 = FocusModifier(Inactive)
        val focusModifier4 = FocusModifier(Inactive)
        // Arrange.
        // layoutNode--focusNode1--focusNode2--focusNode3--focusNode4
        rule.setContent {
            Box(
                modifier = Modifier
                .then(focusModifier1)
                .focusProperties { canFocus = false }
                .then(focusModifier2)
                .then(focusModifier3)
                .then(focusModifier4)
            )
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusNode.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).containsExactly(focusModifier3.focusNode)
            } else {
                assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
            }
        }
    }

    @Test
    fun skipsNonFocusNodesAndReturnsFirstFocusNodeInModifierChain() {
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        val focusModifier3 = FocusModifier(Inactive)
        // Arrange.
        // layoutNode--focusNode1--nonFocusNode--focusNode2--focusNode3
        rule.setContent {
            Box(
                modifier = Modifier
                    .then(focusModifier1)
                    .background(color = Red)
                    .focusProperties { canFocus = false }
                    .then(focusModifier2)
                    .then(focusModifier3)
            )
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusNode.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).containsExactly(focusModifier3.focusNode)
            } else {
                assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
            }
        }
    }

    @Test
    fun returnsFirstFocusChildOfEachChildLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode
        //       |___________________________________________
        //       |                                          |
        // childLayoutNode1--focusNode1--focusNode2    childLayoutNode2--focusNode3--focusNode4
        val parentFocusModifier = FocusModifier(Inactive)
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        val focusModifier3 = FocusModifier(Inactive)
        val focusModifier4 = FocusModifier(Inactive)
        rule.setContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = Modifier
                    .focusProperties { canFocus = false }
                    .then(focusModifier1)
                    .then(focusModifier2)
                )
                Box(modifier = Modifier
                    .then(focusModifier3)
                    .focusProperties { canFocus = false }
                    .then(focusModifier4)
                )
            }
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            parentFocusModifier.focusNode.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).containsExactly(
                    focusModifier2.focusNode, focusModifier3.focusNode
                )
            } else {
                assertThat(focusableChildren).containsExactly(
                    focusModifier1.focusNode, focusModifier3.focusNode
                )
            }
        }
    }
}
