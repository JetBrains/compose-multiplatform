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

/**
 * The native platform-specific keyboard key event.
 */
expect class NativeKeyEvent

/**
 * When a user presses a key on a hardware keyboard, a [KeyEvent] is sent to the
 * [KeyInputModifier] that is currently active.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class KeyEvent(val nativeKeyEvent: NativeKeyEvent)

/**
 * The key that was pressed.
 */
expect val KeyEvent.key: Key

/**
 * The UTF16 value corresponding to the key event that was pressed. The unicode character
 * takes into account any meta keys that are pressed (eg. Pressing shift results in capital
 * alphabets). The UTF16 value uses the
 * [U+n notation][http://www.unicode.org/reports/tr27/#notation] of the Unicode Standard.
 *
 * An [Int] is used instead of a [Char] so that we can support supplementary characters. The
 * Unicode Standard allows for characters whose representation requires more than 16 bits.
 * The range of legal code points is U+0000 to U+10FFFF, known as Unicode scalar value.
 *
 * The set of characters from U+0000 to U+FFFF is sometimes referred to as the Basic
 * Multilingual Plane (BMP). Characters whose code points are greater than U+FFFF are called
 * supplementary characters. In this representation, supplementary characters are represented
 * as a pair of char values, the first from the high-surrogates range, (\uD800-\uDBFF), the
 * second from the low-surrogates range (\uDC00-\uDFFF).
 */
expect val KeyEvent.utf16CodePoint: Int

/**
 * The [type][KeyEventType] of key event.
 */
expect val KeyEvent.type: KeyEventType

/**
 * Indicates whether the Alt key is pressed.
 */
expect val KeyEvent.isAltPressed: Boolean

/**
 * Indicates whether the Ctrl key is pressed.
 */
expect val KeyEvent.isCtrlPressed: Boolean

/**
 * Indicates whether the Meta key is pressed.
 */
expect val KeyEvent.isMetaPressed: Boolean

/**
 * Indicates whether the Shift key is pressed.
 */
expect val KeyEvent.isShiftPressed: Boolean

/**
 * The type of Key Event.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class KeyEventType(val value: Int) {

    override fun toString(): String {
        return when (this) {
            KeyUp -> "KeyUp"
            KeyDown -> "KeyDown"
            Unknown -> "Unknown"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Unknown key event.
         */
        val Unknown: KeyEventType = KeyEventType(0)

        /**
         * Type of KeyEvent sent when the user lifts their finger off a key on the keyboard.
         */
        val KeyUp: KeyEventType = KeyEventType(1)

        /**
         * Type of KeyEvent sent when the user presses down their finger on a key on the keyboard.
         */
        val KeyDown: KeyEventType = KeyEventType(2)
    }
}
