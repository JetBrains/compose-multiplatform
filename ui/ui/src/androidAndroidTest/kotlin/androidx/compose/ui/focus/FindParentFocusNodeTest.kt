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
class FindParentFocusNodeTest(private val deactivated: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isDeactivated = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun noParentReturnsNull() {
        // Arrange.
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        val rootFocusNode = rule.runOnIdle {
            focusModifier.focusNode.findParentFocusNode()!!.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(rootFocusNode).isNull()
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain() {
        // Arrange.
        // focusNode1--focusNode2--focusNode3--focusNode4--focusNode5
        val modifier1 = FocusModifier(Inactive)
        val modifier2 = FocusModifier(Inactive)
        val modifier3 = FocusModifier(Inactive)
        val modifier4 = FocusModifier(Inactive)
        val modifier5 = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = modifier1
                .focusProperties { canFocus = !deactivated }
                .then(modifier2)
                .then(modifier3)
                .then(modifier4)
                .then(modifier5)
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain_ignoresNonFocusModifiers() {
        // Arrange.
        // focusNode1--focusNode2--nonFocusNode--focusNode3
        val modifier1 = FocusModifier(Inactive)
        val modifier2 = FocusModifier(Inactive)
        val modifier3 = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(
                modifier = modifier1
                    .focusProperties { canFocus = !deactivated }
                    .then(modifier2)
                    .background(color = Red)
                    .then(modifier3)
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsLastFocusParentFromParentLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode1--parentFocusNode2
        //       |
        // layoutNode--focusNode
        val parentFocusModifier1 = FocusModifier(Inactive)
        val parentFocusModifier2 = FocusModifier(Inactive)
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier1
                .focusProperties { canFocus = !deactivated }
                .then(parentFocusModifier2)
            ) {
                Box(modifier = focusModifier)
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParent() {
        // Arrange.
        // greatGrandparentLayoutNode--greatGrandparentFocusNode
        //       |
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode--parentFocusNode
        //       |
        // layoutNode--focusNode
        val greatGrandparentFocusModifier = FocusModifier(Inactive)
        val grandparentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = greatGrandparentFocusModifier) {
                Box(modifier = grandparentFocusModifier) {
                    Box(modifier = Modifier
                        .focusProperties { canFocus = !deactivated }
                        .then(parentFocusModifier)
                    ) {
                        Box(modifier = focusModifier)
                    }
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier.focusNode)
        }
    }

    @Test
    fun ignoresIntermediateLayoutNodesThatDoNotHaveFocusNodes() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode
        //       |
        // layoutNode--focusNode
        val greatGrandparentFocusModifier = FocusModifier(Inactive)
        val grandparentFocusModifier = FocusModifier(Inactive)
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = greatGrandparentFocusModifier) {
                Box(modifier = Modifier
                    .focusProperties { canFocus = !deactivated }
                    .then(grandparentFocusModifier)
                ) {
                    Box {
                        Box(modifier = focusModifier)
                    }
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(grandparentFocusModifier.focusNode)
        }
    }
}
