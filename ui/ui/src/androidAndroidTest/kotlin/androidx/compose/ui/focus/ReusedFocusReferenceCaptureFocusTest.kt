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
import androidx.compose.ui.focus.FocusState.Captured
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ReusedFocusReferenceCaptureFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun oneActiveComponent_returnsTrue() {
        // Arrange.
        var focusState = Active
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun oneCapturedComponent_returnsTrue() {
        // Arrange.
        var focusState = Captured
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun oneInactiveComponent_returnsFalse() {
        // Arrange.
        var focusState = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isFalse()
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun oneActiveOneInactiveComponent_returnsTrue() {
        // Arrange.
        var focusState1 = Inactive
        var focusState2 = Active
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState1))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState2))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState1).isEqualTo(Inactive)
            assertThat(focusState2).isEqualTo(Captured)
        }
    }

    @Test
    fun oneInactiveOneCapturedComponent_returnsTrue() {
        // Arrange.
        var focusState1 = Inactive
        var focusState2 = Captured
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState1))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState2))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isTrue()
            assertThat(focusState1).isEqualTo(Inactive)
            assertThat(focusState2).isEqualTo(Captured)
        }
    }

    @Test
    fun twoInactiveComponent_returnsFalse() {
        // Arrange.
        var focusState1 = Inactive
        var focusState2 = Inactive
        val focusReference = FocusReference()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState1))
            )
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState2 = it }
                    .focusReference(focusReference)
                    .then(FocusModifier(focusState2))
            )
        }

        rule.runOnIdle {
            // Act.
            val success = focusReference.captureFocus()

            // Assert.
            assertThat(success).isFalse()
            assertThat(focusState1).isEqualTo(Inactive)
            assertThat(focusState2).isEqualTo(Inactive)
        }
    }
}
