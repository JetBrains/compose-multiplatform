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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusManagerCompositionLocalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clearFocus_singleLayout() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState.isFocused).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_entireHierarchyIsCleared() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        lateinit var grandparentFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .onFocusChanged { grandparentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { parentFocusState = it }
                        .focusTarget()
                ) {
                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(grandparentFocusState.hasFocus).isTrue()
            assertThat(parentFocusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(grandparentFocusState.hasFocus).isFalse()
            assertThat(parentFocusState.hasFocus).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun takeFocus_whenRootIsInactive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { view.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Active)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    fun takeFocus_whenRootIsActive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusManager.setRootFocusState(Active) }

        // Act.
        rule.runOnIdle { view.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Active)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun takeFocus_whenRootIsActiveParent() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { view.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun releaseFocus_whenRootIsInactive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { view.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    fun releaseFocus_whenRootIsActive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusManager.setRootFocusState(Active) }

        // Act.
        rule.runOnIdle { view.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Ignore("b/257499180")
    @Test
    fun releaseFocus_whenRootIsActiveParent() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            view.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_whenRootIsInactive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Ignore("b/257499180")
    @Test
    fun clearFocus_whenRootIsActive() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusManager.setRootFocusState(Active) }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_whenRootIsActiveParent() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            // TODO(b/257499180): Compose should not hold focus state when clear focus is requested.
            assertThat(focusManager.rootFocusState).isEqualTo(Active)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_whenHierarchyHasCapturedFocus() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun clearFocus_forced_whenHierarchyHasCapturedFocus() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus(force = true) }

        // Assert.
        rule.runOnIdle {
            // TODO(b/257499180): Compose should clear focus and send focus to the root view.
            assertThat(focusManager.rootFocusState).isEqualTo(Active)
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private val FocusManager.rootFocusState: FocusState
        get() = (this as FocusOwnerImpl).rootFocusNode.focusState

    @OptIn(ExperimentalComposeUiApi::class)
    private fun FocusManager.setRootFocusState(focusState: FocusStateImpl) {
        (this as FocusOwnerImpl).rootFocusNode.focusStateImpl = focusState
    }
}
