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

import java.awt.Component
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import java.awt.event.KeyEvent.KEY_TYPED
import java.awt.event.KeyEvent.VK_UNDEFINED
import java.awt.event.KeyEvent as KeyEventAwt

private object DummyComponent : Component()
/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
fun keyEvent(key: Key, keyEventType: KeyEventType): KeyEvent {
    val action = when (keyEventType) {
        KeyEventType.KeyDown -> KEY_PRESSED
        KeyEventType.KeyUp -> KEY_RELEASED
        KeyEventType.Unknown -> error("Unknown key event type")
    }
    return KeyEventDesktop(
        KeyEventAwt(
            DummyComponent, action, 0L, 0, key.keyCode,
            KeyEventAwt.getKeyText(key.keyCode)[0]
        )
    )
}

/**
 * Creates [KeyEvent] of Unknown type. It wraps KEY_TYPED AWTs KeyEvent
 */
fun keyTypedEvent(key: Key): KeyEvent {
    return KeyEventDesktop(
        KeyEventAwt(
            DummyComponent, KEY_TYPED, 0L, 0, VK_UNDEFINED,
            KeyEventAwt.getKeyText(key.keyCode)[0]
        )
    )
}
