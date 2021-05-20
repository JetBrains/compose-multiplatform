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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusTargetAttachDetachTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun reorderedFocusRequesterModifiers_onFocusChangedInSameModifierChain() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var observingFocusTarget1: MutableState<Boolean>
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusTarget1 = Modifier.focusTarget()
            val focusTarget2 = Modifier.focusTarget()
            Box {
                observingFocusTarget1 = remember { mutableStateOf(true) }
                Box(
                    modifier = if (observingFocusTarget1.value) {
                        onFocusChanged
                            .then(focusRequesterModifier)
                            .then(focusTarget1)
                            .then(focusTarget2)
                    } else {
                        focusTarget1
                            .then(onFocusChanged)
                            .then(focusRequesterModifier)
                            .then(focusTarget2)
                    }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { observingFocusTarget1.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedModifier_onFocusChangedDoesNotHaveAFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var onFocusChangedHasFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusTarget = Modifier.focusTarget()
            Box {
                onFocusChangedHasFocusTarget = remember { mutableStateOf(true) }
                Box(
                    modifier = if (onFocusChangedHasFocusTarget.value) {
                        onFocusChanged
                            .then(focusRequesterModifier)
                            .then(focusTarget)
                    } else {
                        focusTarget
                            .then(onFocusChanged)
                            .then(focusRequesterModifier)
                    }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { onFocusChangedHasFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusTarget.value) Modifier.focusTarget() else Modifier)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusTarget.value) Modifier.focusTarget() else Modifier)
            ) {
                Box(modifier = Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedCapturedFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusTarget.value) Modifier.focusTarget() else Modifier)
            ) {
                Box(modifier = Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState.isCaptured).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(if (optionalFocusTarget.value) Modifier.focusTarget() else Modifier)
                    .focusRequester(focusRequester)
            ) {
                Box(modifier = Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun removedActiveParentFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(
                        if (optionalFocusTarget.value) {
                            Modifier
                                .focusTarget()
                                .focusRequester(focusRequester)
                                .focusTarget()
                        } else {
                            Modifier
                        }
                    )
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusTargetAndFocusedChild_clearsFocusFromAllParents() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTargets: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTargets = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier.onFocusChanged { focusState = it }.then(
                        if (optionalFocusTargets.value) {
                            Modifier.focusTarget()
                                .focusRequester(focusRequester)
                                .focusTarget()
                        } else {
                            Modifier
                        }
                    )
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(parentFocusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTargets.value = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(parentFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun removedInactiveFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusTarget = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(if (optionalFocusTarget.value) Modifier.focusTarget() else Modifier)
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusTarget_pointsToTheFocusTargetJustAdded() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var addFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusTarget = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (addFocusTarget.value) Modifier.focusTarget() else Modifier)
            ) {
                Box(modifier = Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { addFocusTarget.value = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var addFocusTarget: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusTarget = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (addFocusTarget.value) Modifier.focusTarget() else Modifier)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }

        // Act.
        rule.runOnIdle { addFocusTarget.value = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }
}
