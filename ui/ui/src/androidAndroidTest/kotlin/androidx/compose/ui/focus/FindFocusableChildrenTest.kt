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
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FindFocusableChildrenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun returnsFirstFocusNodeInModifierChain() {
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        val focusModifier3 = FocusModifier(Inactive)
        // Arrange.
        // layoutNode--focusNode1--focusNode2--focusNode3
        rule.setContent {
            Box(modifier = focusModifier1.then(focusModifier2).then(focusModifier3))
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusNode.focusableChildren()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun skipsNonFocusNodesAndReturnsFirstFocusNodeInModifierChain() {
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        // Arrange.
        // layoutNode--focusNode1--nonFocusNode--focusNode2
        rule.setContent {
            Box(focusModifier1.background(color = Red).then(focusModifier2))
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            focusModifier1.focusNode.focusableChildren()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun returnsFirstFocusChildOfEachChildLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode
        //       |___________________________________
        //       |                                   |
        // childLayoutNode1--focusNode1          childLayoutNode2--focusNode2--focusNode3
        val parentFocusModifier = FocusModifier(Inactive)
        val focusModifier1 = FocusModifier(Inactive)
        val focusModifier2 = FocusModifier(Inactive)
        val focusModifier3 = FocusModifier(Inactive)
        rule.setContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier1)
                Box(modifier = focusModifier2.then(focusModifier3))
            }
        }

        // Act.
        val focusableChildren = rule.runOnIdle {
            parentFocusModifier.focusNode.focusableChildren()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusableChildren).containsExactly(
                focusModifier1.focusNode, focusModifier2.focusNode
            )
        }
    }
}
