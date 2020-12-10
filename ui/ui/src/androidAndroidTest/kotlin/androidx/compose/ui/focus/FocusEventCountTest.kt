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
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.platform.AmbientFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusEventCountTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun initially_onFocusEventIsCalledThrice() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusReferece = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusReference(focusReferece)
                    .focusModifier()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
                Inactive, // triggered by focus node's onModifierChanged().
                Inactive, // triggered by focus node's attach().
            )
        }
    }

    @Test
    fun initiallyNoFocusModifier_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        rule.setFocusableContent {
            Box(modifier = Modifier.onFocusEvent { focusStates.add(it) })
        }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Inactive) }
    }

    @Test
    fun whenFocusIsGained_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusReference(focusReference)
                    .focusModifier()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { focusReference.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Active) }
    }

    @Test
    fun requestingFocusWhenAlreadyFocused_onFocusEventIsCalledAgain() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusReference(focusReference)
                    .focusModifier()
            )
        }
        rule.runOnIdle {
            focusReference.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusReference.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Active) }
    }

    @Test
    fun whenFocusIsLost_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusReference = FocusReference()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = AmbientFocusManager.current
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusReference(focusReference)
                    .focusModifier()
            )
        }
        rule.runOnIdle {
            focusReference.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Inactive) }
    }

    @Test
    fun removingActiveFocusNode_onFocusEventIsCalledTwice() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusReference = FocusReference()
        lateinit var addFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusReference(focusReference)
                    .then(if (addFocusModifier.value) Modifier.focusModifier() else Modifier)
            )
        }
        rule.runOnIdle {
            focusReference.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { addFocusModifier.value = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by focus node's state change.
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
            )
        }
    }

    @Test
    fun removingInactiveFocusNode_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        lateinit var addFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusModifier = remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusModifier.value) Modifier.focusModifier() else Modifier)
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusModifier.value = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Inactive) }
    }

    @Test
    fun addingFocusModifier_onFocusEventIsCalledThrice() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        lateinit var addFocusModifier: MutableState<Boolean>
        rule.setFocusableContent {
            addFocusModifier = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusModifier.value) Modifier.focusModifier() else Modifier)
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusModifier.value = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by focus node's attach().
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
                Inactive, // triggered by focus node's onModifierChanged().
            )
        }
    }
}
