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

import androidx.compose.foundation.Box
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.ActiveParent
import androidx.compose.ui.focus.FocusState.Captured
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.test.filters.SmallTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@OptIn(ExperimentalFocus::class)
@RunWith(JUnit4::class)
class FocusModifierAttachDetachTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reorderedModifiers_focusObserverInParentLayoutNode() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var oneBeforeTwo: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val focusModifier1 = Modifier.focus()
            val focusModifier2 = Modifier.focus()
            Box(modifier = Modifier.focusObserver { focusState = it }) {
                oneBeforeTwo = remember { mutableStateOf(true) }
                Box(
                    modifier = if (oneBeforeTwo.value) {
                        focusRequesterModifier
                            .then(focusModifier1)
                            .then(focusModifier2)
                    } else {
                        focusRequesterModifier
                            .then(focusModifier2)
                            .then(focusModifier1)
                    }
                )
            }
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { oneBeforeTwo.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun reorderedModifiers_focusObserverInParentLayoutNode2() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var oneBeforeTwo: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val focusModifier1 = Modifier.focus()
            val focusModifier2 = Modifier.focus()

            Box(modifier = Modifier.focusObserver { focusState = it }) {
                oneBeforeTwo = remember { mutableStateOf(true) }
                Box(
                    modifier = if (oneBeforeTwo.value) {
                        focusModifier1
                            .then(focusRequesterModifier)
                            .then(focusModifier2)
                    } else {
                        focusModifier2
                            .then(focusRequesterModifier)
                            .then(focusModifier1)
                    }
                )
            }
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(ActiveParent)
        }

        // Act.
        runOnIdle { oneBeforeTwo.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun reorderedModifiers_focusObserverInSameModifierChain() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var observingFocusModifier1: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val focusObserver = Modifier.focusObserver { focusState = it }
            val focusModifier1 = Modifier.focus()
            val focusModifier2 = Modifier.focus()
            Box {
                observingFocusModifier1 = remember { mutableStateOf(true) }
                Box(
                    modifier = if (observingFocusModifier1.value) {
                        focusObserver
                            .then(focusRequesterModifier)
                            .then(focusModifier1)
                            .then(focusModifier2)
                    } else {
                        focusModifier1
                            .then(focusObserver)
                            .then(focusRequesterModifier)
                            .then(focusModifier2)
                    }
                )
            }
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { observingFocusModifier1.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun reorderedModifiers_focusObserverDoesNotHaveAFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var focusObserverHasFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val focusObserver = Modifier.focusObserver { focusState = it }
            val focusModifier = Modifier.focus()
            Box {
                focusObserverHasFocusModifier = remember { mutableStateOf(true) }
                Box(
                    modifier = if (focusObserverHasFocusModifier.value) {
                        focusObserver
                            .then(focusRequesterModifier)
                            .then(focusModifier)
                    } else {
                        focusModifier
                            .then(focusObserver)
                            .then(focusRequesterModifier)
                    }
                )
            }
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { focusObserverHasFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun removedFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
                optionalFocusModifier = remember { mutableStateOf(true) }
                Box(
                    modifier = Modifier.focusObserver { focusState = it }
                        .focusRequester(focusRequester)
                        .then(if (optionalFocusModifier.value) Modifier.focus() else Modifier)
                )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun removedActiveFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusModifier.value) Modifier.focus() else Modifier)
                    .focus()
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun removedCapturedFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.focusObserver { focusState = it }
                    .focusRequester(focusRequester)
                    .then(if (optionalFocusModifier.value) Modifier.focus() else Modifier)
                    .focus()
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState).isEqualTo(Captured)
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun removedActiveParentFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.focusObserver { focusState = it }
                    .then(if (optionalFocusModifier.value) Modifier.focus() else Modifier)
                    .focusRequester(focusRequester)
                    .focus()
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(ActiveParent)
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun removedActiveParentFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.focusObserver { focusState = it }
                    .then(
                        if (optionalFocusModifier.value) {
                            Modifier
                                .focus()
                                .focusRequester(focusRequester)
                                .focus()
                    } else {
                            Modifier
                        }
                    )
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(ActiveParent)
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun removedInactiveFocusModifier_pointsToNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var optionalFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
            optionalFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.focusObserver { focusState = it }
                    .then(if (optionalFocusModifier.value) Modifier.focus() else Modifier)
                    .focusRequester(focusRequester)
                    .focus()
            )
        }

        // Act.
        runOnIdle { optionalFocusModifier.value = false }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun addedFocusModifier_pointsToTheFocusModifierJustAdded() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var addFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
                addFocusModifier = remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.focusObserver { focusState = it }
                        .focusRequester(focusRequester)
                        .then(if (addFocusModifier.value) Modifier.focus() else Modifier)
                        .focus()
                )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        runOnIdle { addFocusModifier.value = true }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun addedFocusModifier_withNoNextFocusModifier() {
        // Arrange.
        var focusState = Inactive
        val focusRequester = FocusRequester()
        lateinit var addFocusModifier: MutableState<Boolean>
        composeTestRule.setFocusableContent {
                addFocusModifier = remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.focusObserver { focusState = it }
                        .focusRequester(focusRequester)
                        .then(if (addFocusModifier.value) Modifier.focus() else Modifier)
                )
        }
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState).isEqualTo(Inactive)
        }

        // Act.
        runOnIdle { addFocusModifier.value = true }

        // Assert.
        runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }
}
