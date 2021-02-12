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

package androidx.compose.ui.platform

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalComposeUiApi
@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalSoftwareKeyboardControllerTest {

    @get:Rule
    val rule = createComposeRule()

    @ExperimentalComposeUiApi
    @Test
    fun localSoftwareKeybardController_delegatesTo_textInputService() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)

        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                val controller = LocalSoftwareKeyboardController.current
                SideEffect {
                    controller?.hideSoftwareKeyboard()
                }
            }
        }

        rule.runOnIdle {
            verify(platformTextInputService, times(1))
                .hideSoftwareKeyboard()
        }
    }

    @Test
    fun localSoftwareKeybardController_whenFocused_delegatesToPlatformService() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)
        var controller: SoftwareKeyboardController? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                controller = LocalSoftwareKeyboardController.current
                BasicTextField("string", {})
            }
        }

        rule.onNodeWithText("string").performClick()

        rule.runOnIdle {
            controller?.hideSoftwareKeyboard()
            controller?.showSoftwareKeyboard()
            inOrder(platformTextInputService) {
                verify(platformTextInputService).showSoftwareKeyboard() // focus
                verify(platformTextInputService).hideSoftwareKeyboard() // explicit call
                verify(platformTextInputService).showSoftwareKeyboard() // explicit call
            }
        }
    }

    @Test
    fun showHideSoftKeyboard_dontCrash_beforeSession() {
        var keyboardController: SoftwareKeyboardController? = null
        rule.setContent {
            keyboardController = LocalSoftwareKeyboardController.current
        }
        keyboardController!!.showSoftwareKeyboard()
        keyboardController!!.hideSoftwareKeyboard()
    }
}