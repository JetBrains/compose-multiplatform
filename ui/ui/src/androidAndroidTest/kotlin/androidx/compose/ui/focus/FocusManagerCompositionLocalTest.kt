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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
class FocusManagerCompositionLocalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clearFocus_singleLayout() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusRequester: FocusRequester
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            focusRequester = remember { FocusRequester() }
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
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun clearFocus_entireHierarchyIsCleared() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusRequester: FocusRequester
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        lateinit var grandparentFocusState: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            focusRequester = remember { FocusRequester() }
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
}
