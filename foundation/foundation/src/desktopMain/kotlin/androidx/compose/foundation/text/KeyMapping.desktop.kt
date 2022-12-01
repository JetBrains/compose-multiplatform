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

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import java.awt.event.KeyEvent as AwtKeyEvent

internal actual val platformDefaultKeyMapping: KeyMapping =
    createPlatformDefaultKeyMapping(DesktopPlatform.Current)

internal fun createPlatformDefaultKeyMapping(platform: DesktopPlatform): KeyMapping {
    return when (platform) {
        DesktopPlatform.MacOS -> createMacosDefaultKeyMapping()
        else -> defaultKeyMapping
    }
}

internal actual object MappedKeys {
    actual val A: Key = Key(AwtKeyEvent.VK_A)
    actual val C: Key = Key(AwtKeyEvent.VK_C)
    actual val H: Key = Key(AwtKeyEvent.VK_H)
    actual val V: Key = Key(AwtKeyEvent.VK_V)
    actual val X: Key = Key(AwtKeyEvent.VK_X)
    actual val Z: Key = Key(AwtKeyEvent.VK_Z)
    actual val Backslash: Key = Key(AwtKeyEvent.VK_BACK_SLASH)
    actual val DirectionLeft: Key = Key(AwtKeyEvent.VK_LEFT)
    actual val DirectionRight: Key = Key(AwtKeyEvent.VK_RIGHT)
    actual val DirectionUp: Key = Key(AwtKeyEvent.VK_UP)
    actual val DirectionDown: Key = Key(AwtKeyEvent.VK_DOWN)
    actual val PageUp: Key = Key(AwtKeyEvent.VK_PAGE_UP)
    actual val PageDown: Key = Key(AwtKeyEvent.VK_PAGE_DOWN)
    actual val MoveHome: Key = Key(AwtKeyEvent.VK_HOME)
    actual val MoveEnd: Key = Key(AwtKeyEvent.VK_END)
    actual val Insert: Key = Key(AwtKeyEvent.VK_INSERT)
    actual val Enter: Key = Key(AwtKeyEvent.VK_ENTER)
    actual val Backspace: Key = Key(AwtKeyEvent.VK_BACK_SPACE)
    actual val Delete: Key = Key(AwtKeyEvent.VK_DELETE)
    actual val Paste: Key = Key(AwtKeyEvent.VK_PASTE)
    actual val Cut: Key = Key(AwtKeyEvent.VK_CUT)
    val Copy: Key = Key(AwtKeyEvent.VK_COPY)
    actual val Tab: Key = Key(AwtKeyEvent.VK_TAB)
}

internal object ExtendedMappedKeys {
    val Space: Key = Key(AwtKeyEvent.VK_SPACE)
    val F: Key = Key(AwtKeyEvent.VK_F)
    val B: Key = Key(AwtKeyEvent.VK_B)
    val P: Key = Key(AwtKeyEvent.VK_P)
    val N: Key = Key(AwtKeyEvent.VK_N)
    val E: Key = Key(AwtKeyEvent.VK_E)
    val D: Key = Key(AwtKeyEvent.VK_D)
    val K: Key = Key(AwtKeyEvent.VK_K)
    val O: Key = Key(AwtKeyEvent.VK_O)
}

internal actual val MappedKeys.Space: Key
    get() = ExtendedMappedKeys.Space

internal actual val MappedKeys.F: Key
    get() = ExtendedMappedKeys.F

internal actual val MappedKeys.B: Key
    get() = ExtendedMappedKeys.B

internal actual val MappedKeys.P: Key
    get() = ExtendedMappedKeys.P

internal actual val MappedKeys.N: Key
    get() = ExtendedMappedKeys.N

internal actual val MappedKeys.E: Key
    get() = ExtendedMappedKeys.E

internal actual val MappedKeys.D: Key
    get() = ExtendedMappedKeys.D

internal actual val MappedKeys.K: Key
    get() = ExtendedMappedKeys.K

internal actual val MappedKeys.O: Key
    get() = ExtendedMappedKeys.O
