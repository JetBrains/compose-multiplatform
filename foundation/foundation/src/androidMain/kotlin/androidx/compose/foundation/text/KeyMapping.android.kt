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

package androidx.compose.foundation.text

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key

internal actual val platformDefaultKeyMapping = defaultKeyMapping

internal actual object MappedKeys {
    actual val A: Key = Key(KeyEvent.KEYCODE_A)
    actual val C: Key = Key(KeyEvent.KEYCODE_C)
    actual val H: Key = Key(KeyEvent.KEYCODE_H)
    actual val V: Key = Key(KeyEvent.KEYCODE_V)
    actual val X: Key = Key(KeyEvent.KEYCODE_X)
    actual val Z: Key = Key(KeyEvent.KEYCODE_Z)
    actual val Backslash: Key = Key(KeyEvent.KEYCODE_BACKSLASH)
    actual val DirectionLeft: Key = Key(KeyEvent.KEYCODE_DPAD_LEFT)
    actual val DirectionRight: Key = Key(KeyEvent.KEYCODE_DPAD_RIGHT)
    actual val DirectionUp: Key = Key(KeyEvent.KEYCODE_DPAD_UP)
    actual val DirectionDown: Key = Key(KeyEvent.KEYCODE_DPAD_DOWN)
    actual val PageUp: Key = Key(KeyEvent.KEYCODE_PAGE_UP)
    actual val PageDown: Key = Key(KeyEvent.KEYCODE_PAGE_DOWN)
    actual val MoveHome: Key = Key(KeyEvent.KEYCODE_MOVE_HOME)
    actual val MoveEnd: Key = Key(KeyEvent.KEYCODE_MOVE_END)
    actual val Insert: Key = Key(KeyEvent.KEYCODE_INSERT)
    actual val Enter: Key = Key(KeyEvent.KEYCODE_ENTER)
    actual val Backspace: Key = Key(KeyEvent.KEYCODE_DEL)
    actual val Delete: Key = Key(KeyEvent.KEYCODE_FORWARD_DEL)
    actual val Paste: Key = Key(KeyEvent.KEYCODE_PASTE)
    actual val Cut: Key = Key(KeyEvent.KEYCODE_CUT)
    actual val Tab: Key = Key(KeyEvent.KEYCODE_TAB)
}
