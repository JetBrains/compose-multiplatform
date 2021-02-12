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
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.TextInputService

@ExperimentalComposeUiApi
public object LocalSoftwareKeyboardController {

    /**
     * Return a [SoftwareKeyboardController] that delegates to the current [LocalTextInputService].
     *
     * Returns null if there is no [LocalTextInputService] and the software keyboard cannot be
     * controlled.
     */
    @ExperimentalComposeUiApi
    public val current: SoftwareKeyboardController?
        @Composable get() {
            val textInputService = LocalTextInputService.current ?: return null
            return remember(textInputService) {
                DelegatingSotwareKeyboardController(textInputService)
            }
        }
}

@ExperimentalComposeUiApi
private class DelegatingSotwareKeyboardController(
    val textInputService: TextInputService?
) : SoftwareKeyboardController {
    override fun showSoftwareKeyboard() {
        textInputService?.showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        textInputService?.hideSoftwareKeyboard()
    }
}