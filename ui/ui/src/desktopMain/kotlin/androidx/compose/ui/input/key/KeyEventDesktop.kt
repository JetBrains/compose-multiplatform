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

package androidx.compose.ui.input.key

import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import java.awt.event.KeyEvent as KeyEventAwt

@OptIn(ExperimentalKeyInput::class)
internal inline class KeyEventDesktop(val keyEvent: KeyEventAwt) : KeyEvent {

    override val key: Key
        get() = Key(keyEvent.keyCode)

    override val utf16CodePoint: Int
        get() = keyEvent.keyChar.toInt()

    override val type: KeyEventType
        get() = when (keyEvent.id) {
            KEY_PRESSED -> KeyEventType.KeyDown
            KEY_RELEASED -> KeyEventType.KeyUp
            else -> KeyEventType.Unknown
        }

    override val isAltPressed: Boolean
        get() = keyEvent.isAltDown || keyEvent.isAltGraphDown

    override val isCtrlPressed: Boolean
        get() = keyEvent.isControlDown

    override val isMetaPressed: Boolean
        get() = keyEvent.isMetaDown

    override val isShiftPressed: Boolean
        get() = keyEvent.isShiftDown

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override val alt: Alt
        get() = AltDesktop(keyEvent)
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalKeyInput::class)
internal inline class AltDesktop(val keyEvent: KeyEventAwt) : Alt {

    override val isLeftAltPressed
        get() = keyEvent.isAltDown

    override val isRightAltPressed
        get() = keyEvent.isAltGraphDown
}
