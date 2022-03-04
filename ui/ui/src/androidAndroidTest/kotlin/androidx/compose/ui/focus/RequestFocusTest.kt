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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RequestFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun active_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
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
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun deactivated_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier
                .focusProperties { canFocus = false }
                .focusTarget(focusModifier)
            )
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Deactivated)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        val childFocusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(focusModifier)) {
                Box(Modifier.focusTarget(childFocusModifier))
            }
        }
        rule.runOnIdle {
            focusModifier.focusedChild = childFocusModifier
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Active)
            assertThat(focusModifier.focusedChild).isNull()
            assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun deactivatedParent_withNoFocusedChild_throwsException() {
        // Arrange.
        val focusModifier = FocusModifier(DeactivatedParent)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }
    }

    @Test
    fun deactivatedParent_propagateFocus() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        val childFocusModifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier
                .focusProperties { canFocus = false }
                .focusTarget(focusModifier)
            ) {
                Box(Modifier.focusTarget(childFocusModifier))
            }
        }
        rule.runOnIdle {
            focusModifier.focusedChild = childFocusModifier
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            // Unchanged.
            assertThat(focusModifier.focusState).isEqualTo(DeactivatedParent)
            assertThat(childFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun deactivatedParent_activeChild_propagateFocus() {
        // Arrange.
        val focusModifier = FocusModifier(ActiveParent)
        val childFocusModifier = FocusModifier(Active)
        val grandchildFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier
                .focusProperties { canFocus = false }
                .focusTarget(focusModifier)
            ) {
                Box(Modifier.focusTarget(childFocusModifier)) {
                    Box(Modifier.focusTarget(grandchildFocusModifier))
                }
            }
        }
        rule.runOnIdle {
            focusModifier.focusedChild = childFocusModifier
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
                    assertThat(focusModifier.focusState).isEqualTo(DeactivatedParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusedChild).isNull()
                    assertThat(grandchildFocusModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(rootFocusModifier))
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.requestFocus()
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
            Box(Modifier.focusTarget(rootFocusModifier)) {
                Box(Modifier.focusTarget(childFocusModifier))
            }
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
                    assertThat(rootFocusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveNonRootWithChildren() {
        // Arrange.
        val parentFocusModifier = FocusModifier(Active)
        val focusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier)) {
                    Box(Modifier.focusTarget(childFocusModifier))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        val rootFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(rootFocusModifier))
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.requestFocus()
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
            Box(Modifier.focusTarget(rootFocusModifier)) {
                Box(Modifier.focusTarget(childFocusModifier))
            }
        }

        // Act.
        rule.runOnIdle {
            rootFocusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(rootFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParentFocusModifier)) {
                Box(Modifier.focusTarget(parentFocusModifier)) {
                    Box(Modifier.focusTarget(childFocusModifier))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier(Inactive)
        val parentFocusModifier = FocusModifier(Inactive)
        val childFocusModifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParentFocusModifier)) {
                Box(Modifier.focusTarget(parentFocusModifier)) {
                    Box(Modifier.focusTarget(childFocusModifier))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            childFocusModifier.requestFocus()
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
            Box(Modifier.focusTarget(grandParentFocusModifier)) {
                Box(Modifier.focusTarget(parentFocusModifier)) {
                    Box(Modifier.focusTarget(childFocusModifier))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            childFocusModifier.requestFocus()
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
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier))
            }
        }

        // After executing requestFocus, siblingNode will be 'Active'.
        rule.runOnIdle {
            focusModifier.requestFocus()
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
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier))
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = focusModifier
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(Active)
                    assertThat(focusModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val parentFocusModifier = FocusModifier(ActiveParent)
        val focusModifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier))
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = focusModifier
        }

        // Act.
        rule.runOnIdle {
            parentFocusModifier.requestFocus()
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
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier))
                Box(Modifier.focusTarget(siblingModifier))
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
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
            Box(Modifier.focusTarget(parentFocusModifier)) {
                Box(Modifier.focusTarget(focusModifier))
                Box(Modifier.focusTarget(siblingModifier))
            }
        }
        rule.runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
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
            Box(Modifier.focusTarget(grandParentModifier)) {
                Box(Modifier.focusTarget(parentModifier)) {
                    Box(Modifier.focusTarget(focusModifier))
                }
                Box(Modifier.focusTarget(auntModifier)) {
                    Box(Modifier.focusTarget(cousinModifier))
                }
            }
        }
        rule.runOnIdle {
            grandParentModifier.focusedChild = auntModifier
            auntModifier.focusedChild = cousinModifier
        }

        // Verify Setup.
        rule.runOnIdle {
            assertThat(cousinModifier.focusState).isEqualTo(Active)
            assertThat(focusModifier.focusState).isEqualTo(Inactive)
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
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
            Box(Modifier.focusTarget(grandParentModifier)) {
                Box(Modifier.focusTarget(parentModifier)) {
                    Box(Modifier.focusTarget(focusModifier))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusModifier.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(parentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }
}