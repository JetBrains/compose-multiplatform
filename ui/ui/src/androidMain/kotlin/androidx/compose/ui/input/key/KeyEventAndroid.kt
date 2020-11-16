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

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.META_ALT_LEFT_ON
import android.view.KeyEvent.META_ALT_MASK
import android.view.KeyEvent.META_ALT_RIGHT_ON
import androidx.compose.ui.input.key.KeyEventType.KeyDown
import androidx.compose.ui.input.key.KeyEventType.KeyUp
import androidx.compose.ui.input.key.KeyEventType.Unknown
import android.view.KeyEvent as AndroidKeyEvent

@OptIn(ExperimentalKeyInput::class)
internal inline class KeyEventAndroid(val keyEvent: AndroidKeyEvent) : KeyEvent {

    override val key: Key
        get() = Key(keyEvent.keyCode)

    override val utf16CodePoint: Int
        get() = keyEvent.unicodeChar

    override val type: KeyEventType
        get() = when (keyEvent.action) {
            ACTION_DOWN -> KeyDown
            ACTION_UP -> KeyUp
            else -> Unknown
        }
    override val isAltPressed: Boolean
        get() = keyEvent.isAltPressed

    override val isCtrlPressed: Boolean
        get() = keyEvent.isCtrlPressed

    override val isMetaPressed: Boolean
        get() = keyEvent.isMetaPressed

    override val isShiftPressed: Boolean
        get() = keyEvent.isShiftPressed

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override val alt: Alt
        get() = AltAndroid(keyEvent)
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalKeyInput::class)
internal inline class AltAndroid(val keyEvent: AndroidKeyEvent) : Alt {
    override val isLeftAltPressed
        get() = (keyEvent.metaState and META_ALT_LEFT_ON) != 0

    override val isRightAltPressed
        get() = (keyEvent.metaState and META_ALT_RIGHT_ON) != 0

    /**
     * We override [isPressed] because Android has some synthetic meta states (eg. META_ALT_LOCKED)
     * and provides a META_ALT_MASK that can be used to check if the Alt key is pressed.
     */
    override val isPressed
        get() = (keyEvent.metaState and META_ALT_MASK) != 0
}
