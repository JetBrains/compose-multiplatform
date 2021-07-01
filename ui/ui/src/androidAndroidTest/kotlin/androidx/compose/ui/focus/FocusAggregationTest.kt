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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
class FocusAggregationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nonFocusableItem() {
        // Arrange.
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { focusState = it })
        }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isFalse()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun focusableItem_notFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusable()
            )
        }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isFalse()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun focusableItem_focused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusable()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentOfNonFocusableItem() {
        // Arrange.
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { focusState = it }) {
                Box {}
            }
        }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isFalse()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentWithSingleFocusableChild_childNotFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusable())
            }
        }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isFalse()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentWithSingleFocusableChild_childFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentWithMultipleFocusableChildren_firstChildFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusRequester(focusRequester).focusable())
                Box(Modifier.focusable())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentWithMultipleFocusableChildren_secondChildFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusable())
                Box(Modifier.focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun parentWithMultipleFocusableChildren_firstChildCapturesFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusRequester(focusRequester).focusable())
                Box(Modifier.focusable())
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isTrue()
    }

    @Test
    fun parentWithMultipleFocusableChildren_secondChildCapturesFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Box(Modifier.focusable())
                Box(Modifier.focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Assert.
        assertThat(focusState.isFocused).isTrue()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isTrue()
    }

    @Test
    fun focusableParentWithMultipleFocusableChildren_childFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }.focusable()) {
                Box(Modifier.focusable())
                Box(Modifier.focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun nonFocusableGrandParent_grandChildFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Row(Modifier.focusable()) {
                    Box(Modifier.focusRequester(focusRequester).focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun nonFocusableGrandParentWithMultipleGrandChildren_grandChildFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Row(Modifier.focusable()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusRequester(focusRequester).focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }

    @Test
    fun nonFocusableGrandParentWithMultipleChildrenAndGrandChildren_grandChildFocused() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Column(Modifier.onFocusChanged { focusState = it }) {
                Row(Modifier.focusable()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusable())
                }
                Row(Modifier.focusable()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusRequester(focusRequester).focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        assertThat(focusState.isFocused).isFalse()
        assertThat(focusState.hasFocus).isTrue()
        assertThat(focusState.isCaptured).isFalse()
    }
}
