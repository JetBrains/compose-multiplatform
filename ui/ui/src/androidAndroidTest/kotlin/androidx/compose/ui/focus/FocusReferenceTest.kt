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

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.platform.AmbientView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusReferenceTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun requestFocus_noFocusModifierInLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
            )
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_focusModifierInLayoutNode_butBeforeFocusReference() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusModifier()
                    .focusReference(focusReference)
            )
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_focusModifierInLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
                    .focusModifier()
            )
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusReference(focusReference)
                    .onFocusChanged { focusState = it }
            ) {
                Box(modifier = Modifier.focusModifier())
            }
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierAndReferenceInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusReference(focusReference)
                        .focusModifier()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierAndObserverInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.focusReference(focusReference)
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState = it }
                        .focusModifier()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierInDistantDescendantLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
            ) {
                Box {
                    Box {
                        Box {
                            Box {
                                Box {
                                    Box(
                                        modifier = Modifier.focusModifier()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_firstFocusableChildIsFocused() {
        // Arrange.
        var focusState1 = Inactive
        var focusState2 = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Column(
                modifier = Modifier.focusReference(focusReference)
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState1 = it }
                        .focusModifier()
                )
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState2 = it }
                        .focusModifier()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusReference.requestFocus()

            // Assert.
            assertThat(focusState1).isEqualTo(Active)
            assertThat(focusState2).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocusForAnyChild_triggersonFocusChangedInParent() {
        // Arrange.
        lateinit var hostView: View
        var focusState = Inactive
        val (focusReference1, focusReference2) = FocusReference.createRefs()
        rule.setFocusableContent {
            hostView = AmbientView.current
            Column(
                modifier = Modifier.onFocusChanged { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusReference(focusReference1)
                        .focusModifier()
                )
                Box(
                    modifier = Modifier
                        .focusReference(focusReference2)
                        .focusModifier()
                )
            }
        }

        // Request focus for first child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState).isEqualTo(Inactive)

            // Act.
            focusReference1.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }

        // Request focus for second child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState).isEqualTo(Inactive)

            // Act.
            focusReference2.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }
}
