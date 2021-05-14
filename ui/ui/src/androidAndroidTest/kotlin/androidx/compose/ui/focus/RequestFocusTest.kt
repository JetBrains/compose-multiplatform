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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class RequestFocusTest(val propagateFocus: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "propagateFocus = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun captured_isUnchanged() {

        // Arrange.
        val focusModifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun disabled_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier(Disabled)
        rule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Disabled)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        rule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        val childFocusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = focusModifier) {
                Box(modifier = childFocusModifier)
            }
        }
        rule.runOnIdle {
            focusModifier.focusedChild = childFocusModifier.focusNode
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                    assertThat(focusModifier.focusedChild).isNull()
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = rootFocusModifier)
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(rootFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(rootFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(rootFocusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveNonRootWithChilcren() {
        // Arrange.
        val parentFocusModifier = FocusModifier(Active)
        val focusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = rootFocusModifier)
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(rootFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> assertThat(rootFocusModifier.focusState).isEqualTo(ActiveParent)
                false -> assertThat(rootFocusModifier.focusState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                false -> assertThat(parentFocusModifier.focusState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        rule.runOnIdle {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }
        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        rule.runOnIdle {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(childFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier(Active)
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }

        // After executing requestFocus, siblingNode will be 'Active'.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier(ActiveParent)
        val focusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(Active)
                    assertThat(focusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val parentFocusModifier = FocusModifier(ActiveParent)
        val focusModifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier(ActiveParent)
        val focusModifier = FocusModifier(Inactive)
        val siblingModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
            assertThat(siblingModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier(ActiveParent)
        val focusModifier = FocusModifier(Inactive)
        val siblingModifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Inactive)
            assertThat(siblingModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        val grandParentModifier = FocusModifier(ActiveParent)
        val parentModifier = FocusModifier(Inactive)
        val focusModifier = FocusModifier(Inactive)
        val auntModifier = FocusModifier(ActiveParent)
        val cousinModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
                Box(modifier = auntModifier) {
                    Box(modifier = cousinModifier)
                }
            }
        }
        rule.runOnIdle {
            grandParentModifier.focusedChild = auntModifier.focusNode
            auntModifier.focusedChild = cousinModifier.focusNode
        }

        // Verify Setup.
        rule.runOnIdle {
            assertThat(cousinModifier.focusState).isEqualTo(Active)
            assertThat(focusModifier.focusState).isEqualTo(Inactive)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cousinModifier.focusState).isEqualTo(Inactive)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        val grandParentModifier = FocusModifier(Active)
        val parentModifier = FocusModifier(Inactive)
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(parentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }
}