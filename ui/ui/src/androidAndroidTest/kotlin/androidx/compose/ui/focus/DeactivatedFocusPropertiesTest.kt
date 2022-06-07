/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class DeactivatedFocusPropertiesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun notDeactivatedByDefault() {
        // Arrange.
        var isDeactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier
                .onFocusChanged { isDeactivated = it.isDeactivated }
                .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(isDeactivated).isFalse() }
    }

    @Test
    fun initializedAsNotDeactivated() {
        // Arrange.
        var deactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = true }
                .onFocusChanged { deactivated = it.isDeactivated }
                .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(deactivated).isFalse() }
    }

    @Test
    fun initializedAsDeactivated() {
        // Arrange.
        var isDeactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = false }
                .onFocusChanged { isDeactivated = it.isDeactivated }
                .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(isDeactivated).isTrue() }
    }

    @Test
    fun leftMostDeactivatedPropertyTakesPrecedence() {
        // Arrange.
        var deactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = false }
                .focusProperties { canFocus = true }
                .onFocusChanged { deactivated = it.isDeactivated }
                .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(deactivated).isTrue() }
    }

    @Test
    fun leftMostNonDeactivatedPropertyTakesPrecedence() {
        // Arrange.
        var deactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = true }
                .focusProperties { canFocus = false }
                .onFocusChanged { deactivated = it.isDeactivated }
                .focusTarget()
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(deactivated).isFalse() }
    }

    @Test
    fun ParentsDeactivatedPropertyTakesPrecedence() {
        // Arrange.
        var deactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier.focusProperties { canFocus = false }) {
                Box(modifier = Modifier
                    .focusProperties { canFocus = true }
                    .onFocusChanged { deactivated = it.isDeactivated }
                    .focusTarget()
                )
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(deactivated).isTrue() }
    }

    @Test
    fun ParentsNotDeactivatedPropertyTakesPrecedence() {
        // Arrange.
        var deactivated: Boolean? = null
        rule.setFocusableContent {
            Box(modifier = Modifier.focusProperties { canFocus = true }) {
                Box(modifier = Modifier
                    .focusProperties { canFocus = false }
                    .onFocusChanged { deactivated = it.isDeactivated }
                    .focusTarget()
                )
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(deactivated).isFalse() }
    }

    @Test
    fun deactivatedItemDoesNotGainFocus() {
        // Arrange.
        var isFocused: Boolean? = null
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = false }
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(isFocused).isFalse() }
    }

    @Test
    fun deactivatedFocusPropertiesOnNonFocusableParentAppliesToAllChildren() {
        // Arrange.
        var isParentDeactivated: Boolean? = null
        var isChild1Deactivated: Boolean? = null
        var isChild2Deactivated: Boolean? = null
        var isGrandChildDeactivated: Boolean? = null
        rule.setFocusableContent {
            Column(modifier = Modifier
                .focusProperties { canFocus = false }
                .onFocusChanged { isParentDeactivated = it.isDeactivated }
            ) {
                Box(modifier = Modifier
                    .onFocusChanged { isChild1Deactivated = it.isDeactivated }
                    .focusTarget()
                )
                Box(modifier = Modifier
                        .onFocusChanged { isChild2Deactivated = it.isDeactivated }
                        .focusTarget()
                ) {
                    Box(modifier = Modifier
                        .onFocusChanged { isGrandChildDeactivated = it.isDeactivated }
                        .focusTarget()
                    )
                }
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(isParentDeactivated).isTrue() }
        rule.runOnIdle { assertThat(isChild1Deactivated).isTrue() }
        rule.runOnIdle { assertThat(isChild2Deactivated).isTrue() }
        rule.runOnIdle { assertThat(isGrandChildDeactivated).isFalse() }
    }

    @Test
    fun focusedItemLosesFocusWhenDeactivated() {
        // Arrange.
        var isFocused: Boolean? = null
        val focusRequester = FocusRequester()
        var deactivated by mutableStateOf(false)
        rule.setFocusableContent {
            Box(modifier = Modifier
                .focusProperties { canFocus = !deactivated }
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { deactivated = true }

        // Assert.
        rule.runOnIdle { assertThat(isFocused).isFalse() }
    }
}

private val FocusState.isDeactivated: Boolean
    get() = (this as FocusStateImpl).isDeactivated
