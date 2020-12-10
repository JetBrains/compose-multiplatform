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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.ActiveParent
import androidx.compose.ui.focus.FocusState.Inactive
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
class FocusManagerAmbientTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clearFocus_singleLayout() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusReference: FocusReference
        var focusState = Inactive
        rule.setFocusableContent {
            focusManager = AmbientFocusManager.current
            focusReference = FocusReference()
            Box(
                modifier = Modifier
                    .focusReference(focusReference)
                    .onFocusChanged { focusState = it }
                    .focusModifier()
            )
        }
        rule.runOnIdle {
            focusReference.requestFocus()
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun clearFocus_entireHierarchyIsCleared() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusReference: FocusReference
        var focusState = Inactive
        var parentFocusState = Inactive
        var grandparentFocusState = Inactive
        rule.setFocusableContent {
            focusManager = AmbientFocusManager.current
            focusReference = FocusReference()
            Box(
                modifier = Modifier
                    .onFocusChanged { grandparentFocusState = it }
                    .focusModifier()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { parentFocusState = it }
                        .focusModifier()
                ) {
                    Box(
                        modifier = Modifier
                            .focusReference(focusReference)
                            .onFocusChanged { focusState = it }
                            .focusModifier()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusReference.requestFocus()
            assertThat(grandparentFocusState).isEqualTo(ActiveParent)
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(grandparentFocusState).isEqualTo(Inactive)
            assertThat(parentFocusState).isEqualTo(Inactive)
            assertThat(focusState).isEqualTo(Inactive)
        }
    }
}
