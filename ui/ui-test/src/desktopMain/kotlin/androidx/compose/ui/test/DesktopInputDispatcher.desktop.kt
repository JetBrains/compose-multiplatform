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

@file:OptIn(InternalComposeUiApi::class)

package androidx.compose.ui.test

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.nativeKeyLocation
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.SkiaRootForTest
import java.awt.Component
import java.awt.event.InputEvent

private object DummyComponent : Component()
/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal actual fun keyEvent(
    key: Key,
    keyEventType: KeyEventType,
    modifiers: Int
): KeyEvent {
    val action = when (keyEventType) {
        KeyEventType.KeyDown -> java.awt.event.KeyEvent.KEY_PRESSED
        KeyEventType.KeyUp -> java.awt.event.KeyEvent.KEY_RELEASED
        else -> error("Unknown key event type")
    }
    return KeyEvent(
        java.awt.event.KeyEvent(
            DummyComponent,
            action,
            0L,
            modifiers,
            key.nativeKeyCode,
            java.awt.event.KeyEvent.getKeyText(key.nativeKeyCode)[0],
            key.nativeKeyLocation
        )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Int.updatedKeyboardModifiers(key: Key, down: Boolean): Int {
    val mask = when (key) {
        Key.ShiftLeft, Key.ShiftRight -> InputEvent.SHIFT_DOWN_MASK
        Key.CtrlLeft, Key.CtrlRight -> InputEvent.CTRL_DOWN_MASK
        Key.AltLeft, Key.AltRight -> InputEvent.ALT_DOWN_MASK
        Key.MetaLeft, Key.MetaRight -> InputEvent.META_DOWN_MASK
        else -> null
    }
     return if (mask != null) {
         if (down) this or mask else this xor mask
     } else {
         this
     }
}
