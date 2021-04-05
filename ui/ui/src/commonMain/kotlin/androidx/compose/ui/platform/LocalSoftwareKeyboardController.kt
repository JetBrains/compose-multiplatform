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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.TextInputService

@ExperimentalComposeUiApi
public object LocalSoftwareKeyboardController {

    private val LocalSoftwareKeyboardController =
        compositionLocalOf<SoftwareKeyboardController?> { null }

    /**
     * Return a [SoftwareKeyboardController] that can control the current software keyboard.
     *
     * If it is not provided, the default implementation will delegate to [LocalTextInputService].
     *
     * Returns null if the software keyboard cannot be controlled.
     */
    @ExperimentalComposeUiApi
    public val current: SoftwareKeyboardController?
        @Composable get() {
            return LocalSoftwareKeyboardController.current ?: delegatingController()
        }

    @Composable
    private fun delegatingController(): SoftwareKeyboardController? {
        val textInputService = LocalTextInputService.current ?: return null
        return remember(textInputService) {
            DelegatingSoftwareKeyboardController(textInputService)
        }
    }

    /**
     * Set the key [LocalSoftwareKeyboardController] in [CompositionLocalProvider].
     */
    public infix fun provides(
        softwareKeyboardController: SoftwareKeyboardController
    ): ProvidedValue<SoftwareKeyboardController?> {
        return LocalSoftwareKeyboardController.provides(softwareKeyboardController)
    }
}

@ExperimentalComposeUiApi
private class DelegatingSoftwareKeyboardController(
    val textInputService: TextInputService
) : SoftwareKeyboardController {
    override fun show() {
        @Suppress("DEPRECATION")
        textInputService.showSoftwareKeyboard()
    }

    override fun hide() {
        @Suppress("DEPRECATION")
        textInputService.hideSoftwareKeyboard()
    }
}