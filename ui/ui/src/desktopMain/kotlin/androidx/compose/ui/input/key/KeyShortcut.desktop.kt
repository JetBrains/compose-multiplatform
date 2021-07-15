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

package androidx.compose.ui.input.key

import java.awt.event.InputEvent
import javax.swing.KeyStroke

// TODO(https://github.com/JetBrains/compose-jb/issues/914): support arbitrary shortcuts
/**
 * Represents a key combination which should be pressed on a keyboard to trigger some action.
 */
class KeyShortcut(
    /**
     * Key that should be pressed to trigger an action
     */
    internal val key: Key,

    /**
     * true if Ctrl modifier key should be pressed to trigger an action
     */
    internal val ctrl: Boolean = false,

    /**
     * true if Meta modifier key should be pressed to trigger an action
     * (it is Command on macOs)
     */
    internal val meta: Boolean = false,

    /**
     * true if Alt modifier key should be pressed to trigger an action
     */
    internal val alt: Boolean = false,

    /**
     * true if Shift modifier key should be pressed to trigger an action
     */
    internal val shift: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyShortcut

        if (key != other.key) return false
        if (ctrl != other.ctrl) return false
        if (meta != other.meta) return false
        if (alt != other.alt) return false
        if (shift != other.shift) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + ctrl.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + alt.hashCode()
        result = 31 * result + shift.hashCode()
        return result
    }

    override fun toString() = buildString {
        if (ctrl) append("Ctrl+")
        if (meta) append("Meta+")
        if (alt) append("Alt+")
        if (shift) append("Shift+")
        append(key)
    }
}

internal fun KeyShortcut.toSwingKeyStroke(): KeyStroke = KeyStroke.getKeyStroke(
    key.nativeKeyCode,
    run {
        var value = 0
        if (ctrl) value = value or InputEvent.CTRL_DOWN_MASK
        if (meta) value = value or InputEvent.META_DOWN_MASK
        if (alt) value = value or InputEvent.ALT_DOWN_MASK
        if (shift) value = value or InputEvent.SHIFT_DOWN_MASK
        value
    }
)