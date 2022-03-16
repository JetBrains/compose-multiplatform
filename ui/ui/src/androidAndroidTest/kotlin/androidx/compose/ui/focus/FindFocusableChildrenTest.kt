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
import androidx.compose.ui.ExperimentalComposeUiApi
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
                Modifier
                    .focusTarget(focusModifier1)
                    .focusProperties { canFocus = false }
                    .focusTarget(focusModifier2)
                    .focusTarget(focusModifier3)
                    .focusTarget(focusModifier4)
            )
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).isExactly(focusModifier3)
            } else {
                assertThat(focusableChildren).isExactly(focusModifier2)
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
                Modifier
                    .focusTarget(focusModifier1)
                    .background(color = Red)
                    .focusProperties { canFocus = false }
                    .focusTarget(focusModifier2)
                    .focusTarget(focusModifier3)
            )
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).isExactly(focusModifier3)
            } else {
                assertThat(focusableChildren).isExactly(focusModifier2)
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
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(
                    Modifier
                        .focusProperties { canFocus = false }
                        .focusTarget(focusModifier1)
                        .focusTarget(focusModifier2)
                )
                Box(
                    Modifier
                        .focusTarget(focusModifier3)
                        .focusProperties { canFocus = false }
                        .focusTarget(focusModifier4)
                )
            }
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            parentFocusModifier.focusableChildren(excludeDeactivated)
        }

        // Assert.
        rule.runOnIdle {
            if (excludeDeactivated) {
                assertThat(focusableChildren).isExactly(
                    focusModifier2, focusModifier3
                )
            } else {
                assertThat(focusableChildren).isExactly(
                    focusModifier1, focusModifier3
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun focusedChildIsAvailableFromOnFocusEvent() {
        // Arrange.
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        val focusRequester = FocusRequester()
        var focusedChildAtTimeOfEvent: FocusModifier? = null
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(
                    Modifier
                        .onFocusEvent {
                            if (it.isFocused) {
                                focusedChildAtTimeOfEvent = parentFocusModifier.focusedChild
                            }
                        }
                        .focusRequester(focusRequester)
                        .focusTarget(childFocusModifier)
                )
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusedChildAtTimeOfEvent)
            .isEqualTo(childFocusModifier)
    }

    private fun FocusModifier.focusableChildren(excludeDeactivated: Boolean): List<FocusModifier> =
        (if (excludeDeactivated) activatedChildren() else children).asMutableList()
}
