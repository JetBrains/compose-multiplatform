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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester as modifierNodeFocusRequester
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class FocusRequesterTest(private val modifierNodeVersion: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun requestFocus_noFocusTargetInLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun requestFocus_focusTargetInLayoutNode_butBeforeFocusRequester() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
                    .focusRequester(focusRequester)
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun requestFocus_focusTargetInLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_focusTargetInChildLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
            ) {
                Box(modifier = Modifier.focusTarget())
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_focusTargetAndReferenceInChildLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_focusTargetAndObserverInChildLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_focusTargetInDistantDescendantLayoutNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
            ) {
                Box {
                    Box {
                        Box {
                            Box {
                                Box {
                                    Box(
                                        modifier = Modifier.focusTarget()
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
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_firstFocusableChildIsFocused() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState1 = it }
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState2 = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1.isFocused).isTrue()
            assertThat(focusState2.isFocused).isFalse()
        }
    }

    @Test
    fun requestFocus_onDeactivatedNode_focusesOnChild() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_onDeactivatedParent_focusesOnChild() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .focusRequester(initialFocus)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_intermediateDisabledParents_focusesOnDistantChild() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .focusTarget()
                ) {
                    Box(
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    ) {
                        Box(
                            modifier = Modifier
                                .onFocusChanged { childFocusState = it }
                                .focusTarget()
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_DisabledParent_doesNotPerformImplicitEnterIfCanFocusIsFalse() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties {
                        canFocus = false
                        @OptIn(ExperimentalComposeUiApi::class)
                        enter = { FocusRequester.Cancel }
                    }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun requestFocus_DisabledParents_selectsSiblingFromPathWhereCanFocusIsNotFalse() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .focusProperties {
                            canFocus = false
                            @OptIn(ExperimentalComposeUiApi::class)
                            enter = { FocusRequester.Cancel }
                        }
                        .focusTarget()
                ) {
                    Box(modifier = Modifier.focusTarget())
                }
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_intermediateDisabledParents_focusesOnLeftMostChild() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Row(
                modifier = Modifier
                    .size(100.dp)
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .focusProperties { canFocus = false }
                        .focusTarget()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .focusTarget()
                    )
                }
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocus_intermediateDisabledParents_focusesOnLeftMostChild_regardlessOfDepth() {
        // Arrange.
        lateinit var childFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Row(
                modifier = Modifier
                    .size(100.dp)
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .focusProperties { canFocus = false }
                        .focusTarget()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .onFocusChanged { childFocusState = it }
                            .focusTarget()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun requestFocus_onDeactivatedNode_performsFocusEnter() {
        // Arrange.
        lateinit var child1FocusState: FocusState
        lateinit var child2FocusState: FocusState
        val focusRequester = FocusRequester()
        val child2 = FocusRequester()
        rule.setFocusableContent {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { enter = { child2 } }
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { child1FocusState = it }
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .focusRequester(child2)
                        .onFocusChanged { child2FocusState = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(child1FocusState.isFocused).isFalse()
            assertThat(child2FocusState.isFocused).isTrue()
        }
    }

    // The order in which the children are added to the hierarchy should not change the order
    // in which focus should be resolved.
    @Test
    fun requestFocus_firstFocusableChildIsFocused_afterChange() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        var showBox1 by mutableStateOf(false)
        rule.setFocusableContent {
            Column(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                if (showBox1) {
                    Box(
                        modifier = Modifier
                            .onFocusChanged { focusState1 = it }
                            .focusTarget()
                    )
                }
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState2 = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            showBox1 = true
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1.isFocused).isTrue()
            assertThat(focusState2.isFocused).isFalse()
        }
    }

    @Test
    fun requestFocus_firstFocusableChildIsFocused_differentDepths() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .onFocusChanged { focusState1 = it }
                            .focusTarget()
                    )
                }
                Box(
                    modifier = Modifier
                        .onFocusChanged { focusState2 = it }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1.isFocused).isTrue()
            assertThat(focusState2.isFocused).isFalse()
        }
    }

    @ExperimentalComposeUiApi
    @Test
    fun requestFocusForAnyChild_triggersOnFocusChangedInParent() {
        // Arrange.
        lateinit var hostView: View
        lateinit var focusState: FocusState
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        rule.setFocusableContent {
            hostView = LocalView.current
            Column(
                modifier = Modifier.onFocusChanged { focusState = it }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester1)
                        .focusTarget()
                )
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester2)
                        .focusTarget()
                )
            }
        }

        // Request focus for first child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState.isFocused).isFalse()

            // Act.
            focusRequester1.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }

        // Request focus for second child.
        rule.runOnIdle {
            // Arrange.
            hostView.clearFocus()
            assertThat(focusState.isFocused).isFalse()

            // Act.
            focusRequester2.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    private fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier {
        return if (modifierNodeVersion) {
            this.modifierNodeFocusRequester(focusRequester)
        } else {
            composed(debugInspectorInfo {
                name = "focusRequester"
                properties["focusRequester"] = focusRequester
            }) {
                remember(focusRequester) {
                    object : @Suppress("DEPRECATION") FocusRequesterModifier {
                        override val focusRequester: FocusRequester
                            get() = focusRequester
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "modifierNodeVersion = {0}")
        fun initParameters() = listOf(true, false)
    }
}
