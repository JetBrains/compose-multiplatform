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

package androidx.compose.ui.input

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.FlakyTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalComposeUiApi::class)
class InputModeTest(private val param: Param) {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var inputModeManager: InputModeManager

    // Manually set global state to touch mode to prevent flakiness when another test leaves the
    // system in non-touch mode (b/267368621).
    @Before
    fun initializeInTouchMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(param.inputMode == Touch)
    }

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() = with(InstrumentationRegistry.getInstrumentation()) {
        if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
    }

    @Test
    fun switchToTouchModeProgrammatically() {
        // Arrange.
        rule.setContentWithInputManager {
            Box {}
        }
        val initialMode = rule.runOnIdle { inputModeManager.inputMode }

        // Act.
        val requestGranted = rule.runOnIdle {
            inputModeManager.requestInputMode(Touch)
        }

        // Assert
        rule.runOnIdle {
            when (initialMode) {
                Touch -> {
                    assertThat(requestGranted).isTrue()
                    assertThat(inputModeManager.inputMode).isEqualTo(Touch)
                }
                Keyboard -> {
                    assertThat(requestGranted).isFalse()
                    assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
                }
            }
        }
    }

    @FlakyTest(bugId = 202524920)
    @Test
    fun switchToKeyboardModeProgrammatically() {
        // Arrange.
        val testTag = "Box"
        rule.setContentWithInputManager {
            Box(Modifier.testTag(testTag))
        }

        // Act.
        val requestGranted = rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
        }

        // Assert
        rule.runOnIdle { assertThat(requestGranted).isTrue() }
        assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
    }

    private fun ComposeContentTestRule.setContentWithInputManager(
        composable: @Composable () -> Unit
    ) {
        setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            composable()
        }
        runOnIdle { inputModeManager.requestInputMode(param.inputMode) }
    }

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val inputMode: InputMode) {
        override fun toString() = inputMode.toString()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "initialInputMode = {0}")
        fun initParameters() = listOf(Param(Touch), Param(Keyboard))
    }
}
