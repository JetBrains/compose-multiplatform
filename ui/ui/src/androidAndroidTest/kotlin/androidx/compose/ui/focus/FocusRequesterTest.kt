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
import androidx.compose.ui.focus
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.compose.ui.platform.ViewAmbient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@OptIn(ExperimentalFocus::class)
@RunWith(AndroidJUnit4::class)
class FocusRequesterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun requestFocus_noFocusModifierInLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_focusModifierInLayoutNode_butBeforeFocusRequester() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focus()
                    .focusRequester(focusRequester)
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_focusModifierInLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .focus()
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusObserver { focusState = it }
            ) {
                Box(modifier = Modifier.focus())
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierAndRequesterInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.focusObserver { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focus()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierAndObserverInChildLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Box(
                    modifier = Modifier
                        .focusObserver { focusState = it }
                        .focus()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_focusModifierInDistantDescendantLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusObserver { focusState = it }
                    .focusRequester(focusRequester)
            ) {
                Box {
                    Box {
                        Box {
                            Box {
                                Box {
                                    Box(
                                        modifier = Modifier.focus()
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
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_firstFocusableChildIsFocused() {
        // Arrange.
        var focusState1 = Inactive
        var focusState2 = Inactive
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Box(
                    modifier = Modifier
                        .focusObserver { focusState1 = it }
                        .focus()
                )
                Box(
                    modifier = Modifier
                        .focusObserver { focusState2 = it }
                        .focus()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1).isEqualTo(Active)
            assertThat(focusState2).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocusForAnyChild_triggersFocusObserverInParent() {
        // Arrange.
        lateinit var hostView: View
        var focusState = Inactive
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()
        rule.setFocusableContent {
            hostView = ViewAmbient.current
            Column(
                modifier = Modifier.focusObserver { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester1)
                        .focus()
                )
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester2)
                        .focus()
                )
            }
        }

        // Request focus for first child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState).isEqualTo(Inactive)

            // Act.
            focusRequester1.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }

        // Request focus for second child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState).isEqualTo(Inactive)

            // Act.
            focusRequester2.requestFocus()

            // Assert.
            assertThat(focusState).isEqualTo(Active)
        }
    }
}