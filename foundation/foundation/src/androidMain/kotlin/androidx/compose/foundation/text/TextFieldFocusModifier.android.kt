/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text

import android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * TextField consumes the D-pad keys, due to which we can't move focus once a TextField is focused.
 * To prevent this, this modifier can be used to intercept D-pad key events before they are sent to
 * the TextField. It intercepts and handles the directional (Up, Down, Left, Right & Center) D-pad
 * key presses, to move the focus between TextField and other focusable items on the screen.
 */
internal actual fun Modifier.interceptDPadAndMoveFocus(
    state: TextFieldState,
    focusManager: FocusManager
): Modifier {
    return this
        .onPreviewKeyEvent { keyEvent ->
            // If direction keys from virtual alphabetic keyboard are used, propagate the input
            val device = keyEvent.nativeKeyEvent.device ?: return@onPreviewKeyEvent false
            if (device.keyboardType == KEYBOARD_TYPE_ALPHABETIC && device.isVirtual) {
                return@onPreviewKeyEvent false
            }

            // Handle only the key press events, ignore key release events
            if (keyEvent.type != KeyDown) return@onPreviewKeyEvent false

            when (keyEvent.key.nativeKeyCode) {
                KEYCODE_DPAD_UP -> focusManager.moveFocus(Up)
                KEYCODE_DPAD_DOWN -> focusManager.moveFocus(Down)
                KEYCODE_DPAD_LEFT -> focusManager.moveFocus(Left)
                KEYCODE_DPAD_RIGHT -> focusManager.moveFocus(Right)
                KEYCODE_DPAD_CENTER -> {
                    // Enable keyboard on center key press
                    state.inputSession?.showSoftwareKeyboard()
                    true
                }
                else -> false
            }
        }
}