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
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        val rootFocusNode = rule.runOnIdle {
            focusModifier.parent!!.parent
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
            Box(
                Modifier
                    .focusTarget(modifier1)
                    .focusProperties { canFocus = !deactivated }
                    .focusTarget(modifier2)
                    .focusTarget(modifier3)
                    .focusTarget(modifier4)
                    .focusTarget(modifier5)
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            modifier3.parent
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(modifier2)
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
                Modifier
                    .focusTarget(modifier1)
                    .focusProperties { canFocus = !deactivated }
                    .focusTarget(modifier2)
                    .background(color = Red)
                    .focusTarget(modifier3)
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            modifier3.parent
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(modifier2)
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
            Box(
                Modifier
                    .focusTarget(parentFocusModifier1)
                    .focusProperties { canFocus = !deactivated }
                    .focusTarget(parentFocusModifier2)
            ) {
                Box(Modifier.focusTarget(focusModifier))
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.parent
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier2)
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
            Box(Modifier.focusTarget(greatGrandparentFocusModifier)) {
                Box(Modifier.focusTarget(grandparentFocusModifier)) {
                    Box(Modifier
                        .focusProperties { canFocus = !deactivated }
                        .focusTarget(parentFocusModifier)
                    ) {
                        Box(Modifier.focusTarget(focusModifier))
                    }
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.parent
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(parentFocusModifier)
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
            Box(Modifier.focusTarget(greatGrandparentFocusModifier)) {
                Box(Modifier
                    .focusProperties { canFocus = !deactivated }
                    .focusTarget(grandparentFocusModifier)
                ) {
                    Box {
                        Box(Modifier.focusTarget(focusModifier))
                    }
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            focusModifier.parent
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent).isEqualTo(grandparentFocusModifier)
        }
    }
}
