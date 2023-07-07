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

package androidx.compose.ui.input.key

import org.jetbrains.skiko.*

actual typealias NativeKeyEvent = SkikoKeyboardEvent

/**
 * The key that was pressed.
 */
actual val KeyEvent.key: Key
    get() = skikoKeyToKey(nativeKeyEvent.key)

/**
 * The [type][KeyEventType] of key event.
 */
actual val KeyEvent.type: KeyEventType
    get() =
        when (nativeKeyEvent.kind) {
            SkikoKeyboardEventKind.UP -> KeyEventType.KeyUp
            SkikoKeyboardEventKind.DOWN -> KeyEventType.KeyDown
            SkikoKeyboardEventKind.TYPE -> KeyEventType.Unknown
            SkikoKeyboardEventKind.UNKNOWN -> KeyEventType.Unknown
        }

/**
 * Indicates whether the Alt key is pressed.
 */
actual val KeyEvent.isAltPressed: Boolean
    get() = this.nativeKeyEvent.modifiers.has(SkikoInputModifiers.ALT)

/**
 * Indicates whether the Ctrl key is pressed.
 */
actual val KeyEvent.isCtrlPressed: Boolean
    get() = this.nativeKeyEvent.modifiers.has(SkikoInputModifiers.CONTROL)

/**
 * Indicates whether the Meta key is pressed.
 */
actual val KeyEvent.isMetaPressed: Boolean
    get() = this.nativeKeyEvent.modifiers.has(SkikoInputModifiers.META)

/**
 * Indicates whether the Shift key is pressed.
 */
actual val KeyEvent.isShiftPressed: Boolean
    get() = this.nativeKeyEvent.modifiers.has(SkikoInputModifiers.SHIFT)
