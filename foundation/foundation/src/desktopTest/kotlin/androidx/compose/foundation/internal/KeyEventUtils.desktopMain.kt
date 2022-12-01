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

package androidx.compose.foundation.internal

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.nativeKeyLocation
import java.awt.Component
import java.awt.event.KeyEvent

private object DummyComponent : Component()
/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal actual fun keyEvent(
    key: Key,
    keyEventType: KeyEventType,
    modifiers: Int
): androidx.compose.ui.input.key.KeyEvent {
    val action = when (keyEventType) {
        KeyEventType.KeyDown -> java.awt.event.KeyEvent.KEY_PRESSED
        KeyEventType.KeyUp -> java.awt.event.KeyEvent.KEY_RELEASED
        else -> error("Unknown key event type")
    }
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(
            DummyComponent,
            action,
            0L,
            modifiers,
            key.nativeKeyCode,
            KeyEvent.getKeyText(key.nativeKeyCode)[0],
            key.nativeKeyLocation
        )
    )
}
