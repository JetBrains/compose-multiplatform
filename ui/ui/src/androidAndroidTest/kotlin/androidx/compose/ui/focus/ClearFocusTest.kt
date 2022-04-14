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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
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
class ClearFocusTest(private val forced: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "forcedClear = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isCleared() {
        // Arrange.
        val modifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier))
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parent)) {
                Box(Modifier.focusTarget(modifier))
            }
            SideEffect {
                parent.focusedChild = modifier
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_noFocusedChild_throwsException() {
        // Arrange.
        val modifier = FocusModifier(ActiveParent)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier))
        }

        // Act.
        rule.runOnIdle {
            modifier.clearFocus(forced)
        }
    }

    @Test
    fun activeParent_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parent)) {
                Box(Modifier.focusTarget(modifier)) {
                    Box(Modifier.focusTarget(child))
                }
            }
            SideEffect {
                parent.focusedChild = modifier
                modifier.focusedChild = child
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun activeParent_clearsEntireHierarchy() {
        // Arrange.
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(ActiveParent)
        val grandchild = FocusModifier(ActiveParent)
        val greatGrandchild = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier)) {
                Box(Modifier.focusTarget(child)) {
                    Box(Modifier.focusTarget(grandchild)) {
                        Box(Modifier.focusTarget(greatGrandchild))
                    }
                }
            }
            SideEffect {
                modifier.focusedChild = child
                child.focusedChild = grandchild
                grandchild.focusedChild = greatGrandchild
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(child.focusedChild).isNull()
            assertThat(grandchild.focusedChild).isNull()
            assertThat(modifier.focusState).isEqualTo(Inactive)
            assertThat(child.focusState).isEqualTo(Inactive)
            assertThat(grandchild.focusState).isEqualTo(Inactive)
            assertThat(greatGrandchild.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun captured_isCleared_whenForced() {
        // Arrange.
        val modifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier))
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            when (forced) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(modifier.focusState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(modifier.focusState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild_whenForced() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(Captured)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parent)) {
                Box(Modifier.focusTarget(modifier))
            }
            SideEffect {
                parent.focusedChild = modifier
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            when (forced) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(modifier.focusState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(modifier.focusState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun Inactive_isUnchanged() {
        // Arrange.
        val modifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier))
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun Deactivated_isUnchanged() {
        // Arrange.
        val modifier = FocusModifier(Inactive)
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties { canFocus = false }
                    .focusTarget(modifier)
            )
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusState.isDeactivated).isTrue()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun deactivatedParent_noFocusedChild_throwsException() {
        // Arrange.
        val modifier = FocusModifier(DeactivatedParent)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(modifier))
        }

        // Act.
        rule.runOnIdle {
            modifier.clearFocus(forced)
        }
    }

    @Test
    fun deactivatedParent_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier.focusTarget(parent)) {
                Box(
                    Modifier
                        .focusProperties { canFocus = false }
                        .focusTarget(modifier)
                ) {
                    Box(Modifier.focusTarget(child))
                }
            }
            SideEffect {
                parent.focusedChild = modifier
                modifier.focusedChild = child
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(modifier.focusState.isDeactivated).isTrue()
        }
    }

    @Test
    fun deactivatedParent_withDeactivatedGrandParent_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifier(ActiveParent)
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier
                .focusProperties { canFocus = false }
                .focusTarget(parent)
            ) {
                Box(Modifier
                    .focusProperties { canFocus = false }
                    .focusTarget(modifier)
                ) {
                    Box(Modifier.focusTarget(child))
                }
            }
            SideEffect {
                parent.focusedChild = modifier
                modifier.focusedChild = child
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(modifier.focusState.isDeactivated).isTrue()
        }
    }

    @Test
    fun deactivatedParent_clearsEntireHierarchy() {
        // Arrange.
        val modifier = FocusModifier(ActiveParent)
        val child = FocusModifier(ActiveParent)
        val grandchild = FocusModifier(ActiveParent)
        val greatGrandchild = FocusModifier(ActiveParent)
        val greatGreatGrandchild = FocusModifier(Active)
        rule.setFocusableContent {
            Box(Modifier
                .focusProperties { canFocus = false }
                .focusTarget(modifier)
            ) {
                Box(modifier = Modifier.focusTarget(child)) {
                    Box(Modifier
                        .focusProperties { canFocus = false }
                        .focusTarget(grandchild)
                    ) {
                        Box(Modifier.focusTarget(greatGrandchild)) {
                            Box(Modifier.focusTarget(greatGreatGrandchild))
                        }
                    }
                }
            }
            SideEffect {
                modifier.focusedChild = child
                child.focusedChild = grandchild
                grandchild.focusedChild = greatGrandchild
                greatGrandchild.focusedChild = greatGreatGrandchild
            }
        }

        // Act.
        val cleared = rule.runOnIdle {
            modifier.clearFocus(forced)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(child.focusedChild).isNull()
            assertThat(grandchild.focusedChild).isNull()
            assertThat(modifier.focusState).isEqualTo(Deactivated)
            assertThat(child.focusState).isEqualTo(Inactive)
            assertThat(grandchild.focusState).isEqualTo(Deactivated)
            assertThat(greatGrandchild.focusState).isEqualTo(Inactive)
            assertThat(greatGreatGrandchild.focusState).isEqualTo(Inactive)
        }
    }
}
