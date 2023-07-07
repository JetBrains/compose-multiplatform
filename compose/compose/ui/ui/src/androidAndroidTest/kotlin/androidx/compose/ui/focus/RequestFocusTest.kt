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
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun captured_isUnchanged() {

        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun deactivated_isUnchanged() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier
                .focusRequester(focusRequester)
                .focusProperties { canFocus = false }
                .onFocusChanged { focusState = it }
                .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var childFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget())
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun deactivatedParent_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            // Unchanged.
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Active)
        }
    }

    @Test
    fun deactivatedParent_activeChild_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()

        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        lateinit var grandChildFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .onFocusChanged { grandChildFocusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Active)
            assertThat(grandChildFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .onFocusChanged { childFocusState = it }
                        .focusTarget())
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveNonRootWithChildren() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        lateinit var parentFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(initialFocus)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .onFocusChanged { childFocusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget())
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(Modifier.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                ) {
                    Box(Modifier.focusTarget())
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier
                        .onFocusChanged { focusState = it }
                        .focusTarget()) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget())
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget())
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(initialFocus)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // After executing requestFocus, siblingNode will be 'Active'.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { focusState = it }
                        .focusTarget())
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(Active)
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
            initialFocus.captureFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        lateinit var siblingFocusState: FocusState

        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { siblingFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
            assertThat(siblingFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        lateinit var siblingFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget())
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .onFocusChanged { siblingFocusState = it }
                        .focusTarget())
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
            initialFocus.captureFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Inactive)
            assertThat(siblingFocusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier
                            .focusRequester(initialFocus)
                            .focusTarget())
                }
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var grandParentFocusState: FocusState
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(initialFocus)
                    .onFocusChanged { grandParentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .onFocusChanged { parentFocusState = it }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentFocusState).isEqualTo(ActiveParent)
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
        }
    }
}
