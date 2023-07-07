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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class FocusEventCountTest(focusEventType: String) {
    private val onFocusEvent = if (focusEventType == UseOnFocusEvent) {
        OnFocusEventCall
    } else {
        FocusEventModifierCall
    }

    @get:Rule
    val rule = createComposeRule()

    companion object {
        val OnFocusEventCall: Modifier.((FocusState) -> Unit) -> Modifier = {
            onFocusEvent(it)
        }
        val FocusEventModifierCall: Modifier.((FocusState) -> Unit) -> Modifier = {
            focusEventModifier(it)
        }
        private const val UseOnFocusEvent = "onFocusEvent"
        private const val UseFocusEventModifier = "FocusEventModifier"

        @JvmStatic
        @Parameterized.Parameters(name = "onFocusEvent = {0}")
        fun initParameters() = listOf(UseOnFocusEvent, UseFocusEventModifier)

        private fun Modifier.focusEventModifier(event: (FocusState) -> Unit) = this.then(
            @Suppress("DEPRECATION")
            object : FocusEventModifier {
                override fun onFocusEvent(focusState: FocusState) = event(focusState)
            }
        )
    }

    @Test
    fun initially_onFocusEventIsCalledOnce() {
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun initiallyNoFocusTarget_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        rule.setFocusableContent {
            Box(modifier = Modifier.onFocusEvent { focusStates.add(it) })
        }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Active) }
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Active) }
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun removingActiveComposable_onFocusEventIsCalledWithDefaultValue() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusRequester = FocusRequester()
        var showBox by mutableStateOf(true)
        rule.setFocusableContent {
            if (showBox) {
                Box(
                    modifier = Modifier
                        .onFocusEvent { focusStates.add(it) }
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { showBox = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
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
            assertThat(focusStates).isExactly(
                Inactive, // triggered by clearFocus() of the active node.
                Inactive, // triggered by onFocusEvent node's attach().
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun removingInactiveFocusNode_withActiveChild_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (addFocusTarget) Modifier.focusTarget() else Modifier)
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Active) }
    }

    @Test
    fun removingInactiveFocusNode_withActiveChildLayout_onFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(Modifier.onFocusEvent { focusStates.add(it) }) {
                Box(if (addFocusTarget) Modifier.focusTarget() else Modifier) {
                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Active) }
    }

    @Test
    fun addingFocusTarget_onFocusEventIsCalledOnce() {
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun addingEmptyFocusProperties_onFocusEventIsTriggered() {
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
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun addingCanFocusProperty_onFocusEventIsTriggered() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var addFocusProperties by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(
                        if (addFocusProperties) {
                            Modifier.focusProperties { canFocus = true }
                        } else {
                            Modifier
                        }
                    )
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { addFocusProperties = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun addingCantFocusProperty_noFocusEventIsTriggered() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var add by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (add) Modifier.focusProperties { canFocus = false } else Modifier)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { add = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun removingCanFocusProperty_onFocusEventIsTriggered() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var remove by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (remove) Modifier else Modifier.focusProperties { canFocus = true })
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { remove = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun removingCantFocusProperty_onFocusEventIsTriggered() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var remove by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .then(if (remove) Modifier else Modifier.focusProperties { canFocus = false })
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { remove = true }

        // Assert.
         rule.runOnIdle { assertThat(focusStates).isExactly(Inactive) }
    }

    @Test
    fun deactivatingFocusNode_noFocusEventIsCalledOnce() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var deactivated by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusProperties { canFocus = !deactivated }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { deactivated = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isEmpty() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun changingFocusProperty_onFocusEventIsNotCalled() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        val (item1, item2) = FocusRequester.createRefs()
        var nextItem by mutableStateOf(item1)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusProperties { next = nextItem }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { nextItem = item2 }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isEmpty() }
    }

    @Test
    fun activatingFocusNode_doesNotTriggerFocusEvent() {
        // Arrange.
        val focusStates = mutableListOf<FocusState>()
        var canFocus by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusEvent { focusStates.add(it) }
                    .focusProperties { this.canFocus = canFocus }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { canFocus = true }

        // Assert.
        rule.runOnIdle { assertThat(focusStates).isEmpty() }
    }
}
