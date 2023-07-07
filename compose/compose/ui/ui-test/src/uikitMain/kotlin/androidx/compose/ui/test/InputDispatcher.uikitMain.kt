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
@file:OptIn(ExperimentalTestApi::class)

package androidx.compose.ui.test

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind

/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal actual fun keyEvent(
    key: Key, keyEventType: KeyEventType, modifiers: Int
): KeyEvent {
    return return KeyEvent(
        SkikoKeyboardEvent(
// TODO replace  `enum class SkikoKey` by `class SkikoKey` in skiko, change code here to `SkikoKey(it.platformKeyCode)`
            key = SkikoKey.values().firstOrNull {
                it.platformKeyCode.toLong() == key.keyCode
            } ?: error("SkikoKey not found for key=$key"),
            modifiers = SkikoInputModifiers(modifiers),
            kind = when (keyEventType) {
                KeyEventType.KeyUp -> SkikoKeyboardEventKind.UP
                KeyEventType.KeyDown -> SkikoKeyboardEventKind.DOWN
                else -> error("Unknown key event type: $keyEventType")

            },
            timestamp = 0L,
            platform = null
        )
    )
}

internal actual fun Int.updatedKeyboardModifiers(key: Key, down: Boolean): Int = this // TODO: implement updatedKeyboardModifiers
