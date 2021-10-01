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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.platform.LocalFocusManager
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
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
                Inactive, // triggered by focus node's attach().
            )
        }
    }

    @Test
    fun initiallyNoFocusTarget_onFocusEventIsCalledOnce() {
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
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Active) }
    }

    @Test
    fun requestingFocusWhenAlreadyFocused_onFocusEventIsCalledAgain() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Active) }
    }

    @Test
    fun whenFocusIsLost_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
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
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusRequester(focusRequester)
                    .then(if (addFocusTarget) Modifier.focusTarget() else Modifier)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = false }

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
        var addFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusTarget) Modifier.focusTarget() else Modifier)
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Inactive) }
    }

    @Test
    fun addingFocusTarget_onFocusEventIsCalledThrice() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusTarget) Modifier.focusTarget() else Modifier)
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by focus node's attach().
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
                Inactive, // triggered by focus node's onModifierChanged().
            )
        }
    }

    @Test
    fun addingEmptyFocusProperties_onFocusEventIsCalledTwice() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var addFocusProperties by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusProperties) Modifier.focusProperties {} else Modifier)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusProperties = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusStates).containsExactly(
                Inactive, // triggered by onFocusEvent node's onModifierChanged().
                Inactive, // triggered by focus node's onModifierChanged().
            )
        }
    }

    @Test
    fun deactivatingFocusNode_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var deactiated by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusProperties { canFocus = !deactiated }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { deactiated = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Deactivated) }
    }

    @Test
    fun activatingFocusNode_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var deactiated by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusProperties { canFocus = !deactiated }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { deactiated = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).containsExactly(Inactive) }
    }
}
