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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.input.InputSessionToken
import androidx.compose.ui.text.input.TextInputService

/**
 * Provide software keyboard control.
 */
class SoftwareKeyboardController(
    private val textInputService: TextInputService,
    private val token: InputSessionToken
) {
    /**
     * Show software keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     * Do nothing if bound text field loses input session.
     */
    fun showSoftwareKeyboard() = textInputService.showSoftwareKeyboard(token)

    /**
     * Hide software keyboard
     *
     * Do nothing if bound text field loses input session.
     */
    fun hideSoftwareKeyboard() = textInputService.hideSoftwareKeyboard(token)

    /**
     * Notify to IME about the currently focused rectangle.
     *
     * Do nothing if bound text field loses input session.
     * @param rect focused rectangle in the root view coordinate.
     */
    fun notifyFocusedRect(rect: Rect) = textInputService.notifyFocusedRect(token, rect)
}