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
class FocusModifierAttachDetachTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun reorderedFocusRequesterModifiers_onFocusChangedInSameModifierChain() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var observingFocusModifier1: MutableState<Boolean>
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusModifier1 = Modifier.focusModifier()
            val focusModifier2 = Modifier.focusModifier()
            Box {
                observingFocusModifier1 = remember { mutableStateOf(true) }
                Box(
                    modifier = if (observingFocusModifier1.value) {
                        onFocusChanged
                            .then(focusRequesterModifier)
                            .then(focusModifier1)
                            .then(focusModifier2)
                    } else {
                        focusModifier1
                            .then(onFocusChanged)
                            .then(focusRequesterModifier)
                            .then(focusModifier2)
                    }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { observingFocusModifier1.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedModifier_onFocusChangedDoesNotHaveAFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var onFocusChangedHasFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusModifier = Modifier.focusModifier()
            Box {
                onFocusChangedHasFocusModifier = remember { mutableStateOf(true) }
                Box(
                    modifier = if (onFocusChangedHasFocusModifier.value) {
                        onFocusChanged
                            .then(focusRequesterModifier)
                            .then(focusModifier)
                    } else {
                        focusModifier
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
        rule.runOnIdle { onFocusChangedHasFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusModifier.value) Modifier.focusModifier() else Modifier)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusModifier.value) Modifier.focusModifier() else Modifier)
            ) {
                Box(modifier = Modifier.focusModifier())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedCapturedFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusModifier.value) Modifier.focusModifier() else Modifier)
            ) {
                Box(modifier = Modifier.focusModifier())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState.isCaptured).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(if (optionalFocusModifier.value) Modifier.focusModifier() else Modifier)
                    .focusRequester(focusRequester)
            ) {
                Box(modifier = Modifier.focusModifier())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun removedActiveParentFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(
                        if (optionalFocusModifier.value) {
                            Modifier
                                .focusModifier()
                                .focusRequester(focusRequester)
                                .focusModifier()
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
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusModifierAndFocusedChild_clearsFocusFromAllParents() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifiers: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifiers = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusModifier()
            ) {
                Box(
                    modifier = Modifier.onFocusChanged { focusState = it }.then(
                        if (optionalFocusModifiers.value) {
                            Modifier.focusModifier()
                                .focusRequester(focusRequester)
                                .focusModifier()
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
        rule.runOnIdle { optionalFocusModifiers.value = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(parentFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun removedInactiveFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .then(if (optionalFocusModifier.value) Modifier.focusModifier() else Modifier)
                    .focusRequester(focusRequester)
                    .focusModifier()
            )
        }

        // Act.
        rule.runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusModifier_pointsToTheFocusModifierJustAdded() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var addFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusModifier = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (addFocusModifier.value) Modifier.focusModifier() else Modifier)
            ) {
                Box(modifier = Modifier.focusModifier())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { addFocusModifier.value = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        lateinit var addFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusModifier = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (addFocusModifier.value) Modifier.focusModifier() else Modifier)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }

        // Act.
        rule.runOnIdle { addFocusModifier.value = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }
}
