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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.plus
import androidx.compose.ui.input.key.shortcuts
import androidx.compose.ui.platform.DesktopPlatform

private val modifier by lazy {
    when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> Key.MetaLeft
        else -> Key.CtrlLeft
    }
}

private val copyToClipboardKeySet by lazy { modifier + Key.C }

private val pasteFromClipboardKeySet by lazy { modifier + Key.V }

private val cutToClipboardKeySet by lazy { modifier + Key.X }

private val selectAllKeySet by lazy { modifier + Key.A }

internal actual fun Modifier.textFieldKeyboardModifier(
    manager: TextFieldSelectionManager
): Modifier = composed {
    shortcuts {
        on(copyToClipboardKeySet) {
            manager.copy(false)
        }
        on(pasteFromClipboardKeySet) {
            manager.paste()
        }
        on(cutToClipboardKeySet) {
            manager.cut()
        }
        on(selectAllKeySet) {
            manager.selectAll()
        }
    }
}