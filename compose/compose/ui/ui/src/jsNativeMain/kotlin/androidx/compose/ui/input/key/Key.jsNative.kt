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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key.Companion.Number
import org.jetbrains.skiko.*

/**
 * Actual implementation of [Key] for JS and Native.
 *
 * @param keyCode an integer code representing the key pressed. Note: This keycode can be used to
 * uniquely identify a hardware key.
 */
actual value class Key(val keyCode: Long) {
    actual companion object {
        /** Unknown key. */
        @ExperimentalComposeUiApi
        actual val Unknown: Key = skikoKeyToKey(SkikoKey.KEY_UNKNOWN)

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        @ExperimentalComposeUiApi
        actual val Home: Key = skikoKeyToKey(SkikoKey.KEY_HOME)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionUp: Key = skikoKeyToKey(SkikoKey.KEY_UP)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionDown: Key = skikoKeyToKey(SkikoKey.KEY_DOWN)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionLeft: Key = skikoKeyToKey(SkikoKey.KEY_LEFT)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionRight: Key = skikoKeyToKey(SkikoKey.KEY_RIGHT)

        /** '0' key. */
        @ExperimentalComposeUiApi
        actual val Zero: Key = skikoKeyToKey(SkikoKey.KEY_0)

        /** '1' key. */
        @ExperimentalComposeUiApi
        actual val One: Key = skikoKeyToKey(SkikoKey.KEY_1)

        /** '2' key. */
        @ExperimentalComposeUiApi
        actual val Two: Key = skikoKeyToKey(SkikoKey.KEY_2)

        /** '3' key. */
        @ExperimentalComposeUiApi
        actual val Three: Key = skikoKeyToKey(SkikoKey.KEY_3)

        /** '4' key. */
        @ExperimentalComposeUiApi
        actual val Four: Key = skikoKeyToKey(SkikoKey.KEY_4)

        /** '5' key. */
        @ExperimentalComposeUiApi
        actual val Five: Key = skikoKeyToKey(SkikoKey.KEY_5)

        /** '6' key. */
        @ExperimentalComposeUiApi
        actual val Six: Key = skikoKeyToKey(SkikoKey.KEY_6)

        /** '7' key. */
        @ExperimentalComposeUiApi
        actual val Seven: Key = skikoKeyToKey(SkikoKey.KEY_7)

        /** '8' key. */
        @ExperimentalComposeUiApi
        actual val Eight: Key = skikoKeyToKey(SkikoKey.KEY_8)

        /** '9' key. */
        @ExperimentalComposeUiApi
        actual val Nine: Key = skikoKeyToKey(SkikoKey.KEY_9)

        /** '-' key. */
        @ExperimentalComposeUiApi
        actual val Minus: Key = skikoKeyToKey(SkikoKey.KEY_MINUS)

        /** '=' key. */
        @ExperimentalComposeUiApi
        actual val Equals: Key = skikoKeyToKey(SkikoKey.KEY_EQUALS)

        /** 'A' key. */
        @ExperimentalComposeUiApi
        actual val A: Key = skikoKeyToKey(SkikoKey.KEY_A)

        /** 'B' key. */
        @ExperimentalComposeUiApi
        actual val B: Key = skikoKeyToKey(SkikoKey.KEY_B)

        /** 'C' key. */
        @ExperimentalComposeUiApi
        actual val C: Key = skikoKeyToKey(SkikoKey.KEY_C)

        /** 'D' key. */
        @ExperimentalComposeUiApi
        actual val D: Key = skikoKeyToKey(SkikoKey.KEY_D)

        /** 'E' key. */
        @ExperimentalComposeUiApi
        actual val E: Key = skikoKeyToKey(SkikoKey.KEY_E)

        /** 'F' key. */
        @ExperimentalComposeUiApi
        actual val F: Key = skikoKeyToKey(SkikoKey.KEY_F)

        /** 'G' key. */
        @ExperimentalComposeUiApi
        actual val G: Key = skikoKeyToKey(SkikoKey.KEY_G)

        /** 'H' key. */
        @ExperimentalComposeUiApi
        actual val H: Key = skikoKeyToKey(SkikoKey.KEY_H)

        /** 'I' key. */
        @ExperimentalComposeUiApi
        actual val I: Key = skikoKeyToKey(SkikoKey.KEY_I)

        /** 'J' key. */
        @ExperimentalComposeUiApi
        actual val J: Key = skikoKeyToKey(SkikoKey.KEY_J)

        /** 'K' key. */
        @ExperimentalComposeUiApi
        actual val K: Key = skikoKeyToKey(SkikoKey.KEY_K)

        /** 'L' key. */
        @ExperimentalComposeUiApi
        actual val L: Key = skikoKeyToKey(SkikoKey.KEY_L)

        /** 'M' key. */
        @ExperimentalComposeUiApi
        actual val M: Key = skikoKeyToKey(SkikoKey.KEY_M)

        /** 'N' key. */
        @ExperimentalComposeUiApi
        actual val N: Key = skikoKeyToKey(SkikoKey.KEY_N)

        /** 'O' key. */
        @ExperimentalComposeUiApi
        actual val O: Key = skikoKeyToKey(SkikoKey.KEY_O)

        /** 'P' key. */
        @ExperimentalComposeUiApi
        actual val P: Key = skikoKeyToKey(SkikoKey.KEY_P)

        /** 'Q' key. */
        @ExperimentalComposeUiApi
        actual val Q: Key = skikoKeyToKey(SkikoKey.KEY_Q)

        /** 'R' key. */
        @ExperimentalComposeUiApi
        actual val R: Key = skikoKeyToKey(SkikoKey.KEY_R)

        /** 'S' key. */
        @ExperimentalComposeUiApi
        actual val S: Key = skikoKeyToKey(SkikoKey.KEY_S)

        /** 'T' key. */
        @ExperimentalComposeUiApi
        actual val T: Key = skikoKeyToKey(SkikoKey.KEY_T)

        /** 'U' key. */
        @ExperimentalComposeUiApi
        actual val U: Key = skikoKeyToKey(SkikoKey.KEY_U)

        /** 'V' key. */
        @ExperimentalComposeUiApi
        actual val V: Key = skikoKeyToKey(SkikoKey.KEY_V)

        /** 'W' key. */
        @ExperimentalComposeUiApi
        actual val W: Key = skikoKeyToKey(SkikoKey.KEY_W)

        /** 'X' key. */
        @ExperimentalComposeUiApi
        actual val X: Key = skikoKeyToKey(SkikoKey.KEY_X)

        /** 'Y' key. */
        @ExperimentalComposeUiApi
        actual val Y: Key = skikoKeyToKey(SkikoKey.KEY_Y)

        /** 'Z' key. */
        @ExperimentalComposeUiApi
        actual val Z: Key = skikoKeyToKey(SkikoKey.KEY_Z)

        /** ',' key. */
        @ExperimentalComposeUiApi
        actual val Comma: Key = skikoKeyToKey(SkikoKey.KEY_COMMA)

        /** '.' key. */
        @ExperimentalComposeUiApi
        actual val Period: Key = skikoKeyToKey(SkikoKey.KEY_PERIOD)

        /** Left Alt modifier key. */
        @ExperimentalComposeUiApi
        actual val AltLeft: Key = skikoKeyToKey(SkikoKey.KEY_LEFT_ALT)

        /** Right Alt modifier key. */
        @ExperimentalComposeUiApi
        actual val AltRight: Key = skikoKeyToKey(SkikoKey.KEY_RIGHT_ALT)

        /** Left Shift modifier key. */
        @ExperimentalComposeUiApi
        actual val ShiftLeft: Key = skikoKeyToKey(SkikoKey.KEY_LEFT_SHIFT)

        /** Right Shift modifier key. */
        @ExperimentalComposeUiApi
        actual val ShiftRight: Key = skikoKeyToKey(SkikoKey.KEY_RIGHT_SHIFT)

        /** Tab key. */
        @ExperimentalComposeUiApi
        actual val Tab: Key = skikoKeyToKey(SkikoKey.KEY_TAB)

        /** Space key. */
        @ExperimentalComposeUiApi
        actual val Spacebar: Key = skikoKeyToKey(SkikoKey.KEY_SPACE)

        /** Enter key. */
        @ExperimentalComposeUiApi
        actual val Enter: Key = skikoKeyToKey(SkikoKey.KEY_ENTER)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        @ExperimentalComposeUiApi
        actual val Backspace: Key = skikoKeyToKey(SkikoKey.KEY_BACKSPACE) // Key(KeyEvent.VK_BACK_SPACE)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        @ExperimentalComposeUiApi
        actual val Delete: Key = skikoKeyToKey(SkikoKey.KEY_DELETE)

        /** Escape key. */
        @ExperimentalComposeUiApi
        actual val Escape: Key = skikoKeyToKey(SkikoKey.KEY_ESCAPE)

        /** Left Control modifier key. */
        @ExperimentalComposeUiApi
        actual val CtrlLeft: Key = skikoKeyToKey(SkikoKey.KEY_LEFT_CONTROL)

        /** Right Control modifier key. */
        @ExperimentalComposeUiApi
        actual val CtrlRight: Key = skikoKeyToKey(SkikoKey.KEY_RIGHT_CONTROL)

        /** Caps Lock key. */
        @ExperimentalComposeUiApi
        actual val CapsLock: Key = skikoKeyToKey(SkikoKey.KEY_CAPSLOCK)

        /** Scroll Lock key. */
        @ExperimentalComposeUiApi
        actual val ScrollLock: Key = skikoKeyToKey(SkikoKey.KEY_SCROLL_LOCK)

        /** Left Meta modifier key. */
        @ExperimentalComposeUiApi
        actual val MetaLeft: Key = skikoKeyToKey(SkikoKey.KEY_LEFT_META)

        /** Right Meta modifier key. */
        @ExperimentalComposeUiApi
        actual val MetaRight: Key = skikoKeyToKey(SkikoKey.KEY_RIGHT_META)

        /** System Request / Print Screen key. */
        @ExperimentalComposeUiApi
        actual val PrintScreen: Key = skikoKeyToKey(SkikoKey.KEY_PRINTSCEEN)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        @ExperimentalComposeUiApi
        actual val Insert: Key = skikoKeyToKey(SkikoKey.KEY_INSERT)

        /** '`' (backtick) key. */
        @ExperimentalComposeUiApi
        actual val Grave: Key = skikoKeyToKey(SkikoKey.KEY_BACK_QUOTE)

        /** '[' key. */
        @ExperimentalComposeUiApi
        actual val LeftBracket: Key = skikoKeyToKey(SkikoKey.KEY_OPEN_BRACKET)

        /** ']' key. */
        @ExperimentalComposeUiApi
        actual val RightBracket: Key = skikoKeyToKey(SkikoKey.KEY_CLOSE_BRACKET)

        /** '/' key. */
        @ExperimentalComposeUiApi
        actual val Slash: Key = skikoKeyToKey(SkikoKey.KEY_SLASH)

        /** '\' key. */
        @ExperimentalComposeUiApi
        actual val Backslash: Key = skikoKeyToKey(SkikoKey.KEY_BACKSLASH)

        /** ';' key. */
        @ExperimentalComposeUiApi
        actual val Semicolon: Key = skikoKeyToKey(SkikoKey.KEY_SEMICOLON)

        /** Page Up key. */
        @ExperimentalComposeUiApi
        actual val PageUp: Key = skikoKeyToKey(SkikoKey.KEY_PGUP)

        /** Page Down key. */
        @ExperimentalComposeUiApi
        actual val PageDown: Key = skikoKeyToKey(SkikoKey.KEY_PGDOWN)

        /** F1 key. */
        @ExperimentalComposeUiApi
        actual val F1: Key = skikoKeyToKey(SkikoKey.KEY_F1)

        /** F2 key. */
        @ExperimentalComposeUiApi
        actual val F2: Key = skikoKeyToKey(SkikoKey.KEY_F2)

        /** F3 key. */
        @ExperimentalComposeUiApi
        actual val F3: Key = skikoKeyToKey(SkikoKey.KEY_F3)

        /** F4 key. */
        @ExperimentalComposeUiApi
        actual val F4: Key = skikoKeyToKey(SkikoKey.KEY_F4)

        /** F5 key. */
        @ExperimentalComposeUiApi
        actual val F5: Key = skikoKeyToKey(SkikoKey.KEY_F5)

        /** F6 key. */
        @ExperimentalComposeUiApi
        actual val F6: Key = skikoKeyToKey(SkikoKey.KEY_F6)

        /** F7 key. */
        @ExperimentalComposeUiApi
        actual val F7: Key = skikoKeyToKey(SkikoKey.KEY_F7)

        /** F8 key. */
        @ExperimentalComposeUiApi
        actual val F8: Key = skikoKeyToKey(SkikoKey.KEY_F8)

        /** F9 key. */
        @ExperimentalComposeUiApi
        actual val F9: Key = skikoKeyToKey(SkikoKey.KEY_F9)

        /** F10 key. */
        @ExperimentalComposeUiApi
        actual val F10: Key = skikoKeyToKey(SkikoKey.KEY_F10)

        /** F11 key. */
        @ExperimentalComposeUiApi
        actual val F11: Key = skikoKeyToKey(SkikoKey.KEY_F11)

        /** F12 key. */
        @ExperimentalComposeUiApi
        actual val F12: Key = skikoKeyToKey(SkikoKey.KEY_F12)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        @ExperimentalComposeUiApi
        actual val NumLock: Key = skikoKeyToKey(SkikoKey.KEY_NUM_LOCK)

        /** Numeric keypad '0' key. */
        @ExperimentalComposeUiApi
        actual val NumPad0: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_0)

        /** Numeric keypad '1' key. */
        @ExperimentalComposeUiApi
        actual val NumPad1: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_1)

        /** Numeric keypad '2' key. */
        @ExperimentalComposeUiApi
        actual val NumPad2: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_2)

        /** Numeric keypad '3' key. */
        @ExperimentalComposeUiApi
        actual val NumPad3: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_3)

        /** Numeric keypad '4' key. */
        @ExperimentalComposeUiApi
        actual val NumPad4: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_4)

        /** Numeric keypad '5' key. */
        @ExperimentalComposeUiApi
        actual val NumPad5: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_5)

        /** Numeric keypad '6' key. */
        @ExperimentalComposeUiApi
        actual val NumPad6: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_6)

        /** Numeric keypad '7' key. */
        @ExperimentalComposeUiApi
        actual val NumPad7: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_7)

        /** Numeric keypad '8' key. */
        @ExperimentalComposeUiApi
        actual val NumPad8: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_8)

        /** Numeric keypad '9' key. */
        @ExperimentalComposeUiApi
        actual val NumPad9: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_9)

        /** Numeric keypad '/' key (for division). */
        @ExperimentalComposeUiApi
        actual val NumPadDivide: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_DIVIDE)

        /** Numeric keypad '*' key (for multiplication). */
        @ExperimentalComposeUiApi
        actual val NumPadMultiply: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_MULTIPLY)

        /** Numeric keypad '-' key (for subtraction). */
        @ExperimentalComposeUiApi
        actual val NumPadSubtract: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_SUBTRACT)

        /** Numeric keypad '+' key (for addition). */
        @ExperimentalComposeUiApi
        actual val NumPadAdd: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_ADD)

        /** Numeric keypad Enter key. */
        @ExperimentalComposeUiApi
        actual val NumPadEnter: Key = skikoKeyToKey(SkikoKey.KEY_NUMPAD_ENTER)

        @ExperimentalComposeUiApi
        actual val MoveHome: Key = skikoKeyToKey(SkikoKey.KEY_HOME)

        @ExperimentalComposeUiApi
        actual val MoveEnd: Key = skikoKeyToKey(SkikoKey.KEY_END)

        // Unsupported Keys
        @ExperimentalComposeUiApi
        actual val SoftLeft = Key(-1000000001)

        @ExperimentalComposeUiApi
        actual val SoftRight = Key(-1000000002)

        @ExperimentalComposeUiApi
        actual val Back = Key(-1000000003)

        @ExperimentalComposeUiApi
        actual val NavigatePrevious = Key(-1000000004)

        @ExperimentalComposeUiApi
        actual val NavigateNext = Key(-1000000005)

        @ExperimentalComposeUiApi
        actual val NavigateIn = Key(-1000000006)

        @ExperimentalComposeUiApi
        actual val NavigateOut = Key(-1000000007)

        @ExperimentalComposeUiApi
        actual val SystemNavigationUp = Key(-1000000008)

        @ExperimentalComposeUiApi
        actual val SystemNavigationDown = Key(-1000000009)

        @ExperimentalComposeUiApi
        actual val SystemNavigationLeft = Key(-1000000010)

        @ExperimentalComposeUiApi
        actual val SystemNavigationRight = Key(-1000000011)

        @ExperimentalComposeUiApi
        actual val Call = Key(-1000000012)

        @ExperimentalComposeUiApi
        actual val EndCall = Key(-1000000013)

        @ExperimentalComposeUiApi
        actual val DirectionCenter = Key(-1000000014)

        @ExperimentalComposeUiApi
        actual val DirectionUpLeft = Key(-1000000015)

        @ExperimentalComposeUiApi
        actual val DirectionDownLeft = Key(-1000000016)

        @ExperimentalComposeUiApi
        actual val DirectionUpRight = Key(-1000000017)

        @ExperimentalComposeUiApi
        actual val DirectionDownRight = Key(-1000000018)

        @ExperimentalComposeUiApi
        actual val VolumeUp = Key(-1000000019)

        @ExperimentalComposeUiApi
        actual val VolumeDown = Key(-1000000020)

        @ExperimentalComposeUiApi
        actual val Power = Key(-1000000021)

        @ExperimentalComposeUiApi
        actual val Camera = Key(-1000000022)

        @ExperimentalComposeUiApi
        actual val Clear = Key(-1000000023)

        @ExperimentalComposeUiApi
        actual val Symbol = Key(-1000000024)

        @ExperimentalComposeUiApi
        actual val Browser = Key(-1000000025)

        @ExperimentalComposeUiApi
        actual val Envelope = Key(-1000000026)

        @ExperimentalComposeUiApi
        actual val Function = Key(-1000000027)

        @ExperimentalComposeUiApi
        actual val Break = Key(-1000000028)

        @ExperimentalComposeUiApi
        actual val Number = Key(-1000000031)

        @ExperimentalComposeUiApi
        actual val HeadsetHook = Key(-1000000032)

        @ExperimentalComposeUiApi
        actual val Focus = Key(-1000000033)

        @ExperimentalComposeUiApi
        actual val Menu = Key(-1000000034)

        @ExperimentalComposeUiApi
        actual val Notification = Key(-1000000035)

        @ExperimentalComposeUiApi
        actual val Search = Key(-1000000036)

        @ExperimentalComposeUiApi
        actual val PictureSymbols = Key(-1000000037)

        @ExperimentalComposeUiApi
        actual val SwitchCharset = Key(-1000000038)

        @ExperimentalComposeUiApi
        actual val ButtonA = Key(-1000000039)

        @ExperimentalComposeUiApi
        actual val ButtonB = Key(-1000000040)

        @ExperimentalComposeUiApi
        actual val ButtonC = Key(-1000000041)

        @ExperimentalComposeUiApi
        actual val ButtonX = Key(-1000000042)

        @ExperimentalComposeUiApi
        actual val ButtonY = Key(-1000000043)

        @ExperimentalComposeUiApi
        actual val ButtonZ = Key(-1000000044)

        @ExperimentalComposeUiApi
        actual val ButtonL1 = Key(-1000000045)

        @ExperimentalComposeUiApi
        actual val ButtonR1 = Key(-1000000046)

        @ExperimentalComposeUiApi
        actual val ButtonL2 = Key(-1000000047)

        @ExperimentalComposeUiApi
        actual val ButtonR2 = Key(-1000000048)

        @ExperimentalComposeUiApi
        actual val ButtonThumbLeft = Key(-1000000049)

        @ExperimentalComposeUiApi
        actual val ButtonThumbRight = Key(-1000000050)

        @ExperimentalComposeUiApi
        actual val ButtonStart = Key(-1000000051)

        @ExperimentalComposeUiApi
        actual val ButtonSelect = Key(-1000000052)

        @ExperimentalComposeUiApi
        actual val ButtonMode = Key(-1000000053)

        @ExperimentalComposeUiApi
        actual val Button1 = Key(-1000000054)

        @ExperimentalComposeUiApi
        actual val Button2 = Key(-1000000055)

        @ExperimentalComposeUiApi
        actual val Button3 = Key(-1000000056)

        @ExperimentalComposeUiApi
        actual val Button4 = Key(-1000000057)

        @ExperimentalComposeUiApi
        actual val Button5 = Key(-1000000058)

        @ExperimentalComposeUiApi
        actual val Button6 = Key(-1000000059)

        @ExperimentalComposeUiApi
        actual val Button7 = Key(-1000000060)

        @ExperimentalComposeUiApi
        actual val Button8 = Key(-1000000061)

        @ExperimentalComposeUiApi
        actual val Button9 = Key(-1000000062)

        @ExperimentalComposeUiApi
        actual val Button10 = Key(-1000000063)

        @ExperimentalComposeUiApi
        actual val Button11 = Key(-1000000064)

        @ExperimentalComposeUiApi
        actual val Button12 = Key(-1000000065)

        @ExperimentalComposeUiApi
        actual val Button13 = Key(-1000000066)

        @ExperimentalComposeUiApi
        actual val Button14 = Key(-1000000067)

        @ExperimentalComposeUiApi
        actual val Button15 = Key(-1000000068)

        @ExperimentalComposeUiApi
        actual val Button16 = Key(-1000000069)

        @ExperimentalComposeUiApi
        actual val Forward = Key(-1000000070)

        @ExperimentalComposeUiApi
        actual val MediaPlay = Key(-1000000071)

        @ExperimentalComposeUiApi
        actual val MediaPause = Key(-1000000072)

        @ExperimentalComposeUiApi
        actual val MediaPlayPause = Key(-1000000073)

        @ExperimentalComposeUiApi
        actual val MediaStop = Key(-1000000074)

        @ExperimentalComposeUiApi
        actual val MediaRecord = Key(-1000000075)

        @ExperimentalComposeUiApi
        actual val MediaNext = Key(-1000000076)

        @ExperimentalComposeUiApi
        actual val MediaPrevious = Key(-1000000077)

        @ExperimentalComposeUiApi
        actual val MediaRewind = Key(-1000000078)

        @ExperimentalComposeUiApi
        actual val MediaFastForward = Key(-1000000079)

        @ExperimentalComposeUiApi
        actual val MediaClose = Key(-1000000080)

        @ExperimentalComposeUiApi
        actual val MediaAudioTrack = Key(-1000000081)

        @ExperimentalComposeUiApi
        actual val MediaEject = Key(-1000000082)

        @ExperimentalComposeUiApi
        actual val MediaTopMenu = Key(-1000000083)

        @ExperimentalComposeUiApi
        actual val MediaSkipForward = Key(-1000000084)

        @ExperimentalComposeUiApi
        actual val MediaSkipBackward = Key(-1000000085)

        @ExperimentalComposeUiApi
        actual val MediaStepForward = Key(-1000000086)

        @ExperimentalComposeUiApi
        actual val MediaStepBackward = Key(-1000000087)

        @ExperimentalComposeUiApi
        actual val MicrophoneMute = Key(-1000000088)

        @ExperimentalComposeUiApi
        actual val VolumeMute = Key(-1000000089)

        @ExperimentalComposeUiApi
        actual val Info = Key(-1000000090)

        @ExperimentalComposeUiApi
        actual val ChannelUp = Key(-1000000091)

        @ExperimentalComposeUiApi
        actual val ChannelDown = Key(-1000000092)

        @ExperimentalComposeUiApi
        actual val ZoomIn = Key(-1000000093)

        @ExperimentalComposeUiApi
        actual val ZoomOut = Key(-1000000094)

        @ExperimentalComposeUiApi
        actual val Tv = Key(-1000000095)

        @ExperimentalComposeUiApi
        actual val Window = Key(-1000000096)

        @ExperimentalComposeUiApi
        actual val Guide = Key(-1000000097)

        @ExperimentalComposeUiApi
        actual val Dvr = Key(-1000000098)

        @ExperimentalComposeUiApi
        actual val Bookmark = Key(-1000000099)

        @ExperimentalComposeUiApi
        actual val Captions = Key(-1000000100)

        @ExperimentalComposeUiApi
        actual val Settings = Key(-1000000101)

        @ExperimentalComposeUiApi
        actual val TvPower = Key(-1000000102)

        @ExperimentalComposeUiApi
        actual val TvInput = Key(-1000000103)

        @ExperimentalComposeUiApi
        actual val SetTopBoxPower = Key(-1000000104)

        @ExperimentalComposeUiApi
        actual val SetTopBoxInput = Key(-1000000105)

        @ExperimentalComposeUiApi
        actual val AvReceiverPower = Key(-1000000106)

        @ExperimentalComposeUiApi
        actual val AvReceiverInput = Key(-1000000107)

        @ExperimentalComposeUiApi
        actual val ProgramRed = Key(-1000000108)

        @ExperimentalComposeUiApi
        actual val ProgramGreen = Key(-1000000109)

        @ExperimentalComposeUiApi
        actual val ProgramYellow = Key(-1000000110)

        @ExperimentalComposeUiApi
        actual val ProgramBlue = Key(-1000000111)

        @ExperimentalComposeUiApi
        actual val AppSwitch = Key(-1000000112)

        @ExperimentalComposeUiApi
        actual val LanguageSwitch = Key(-1000000113)

        @ExperimentalComposeUiApi
        actual val MannerMode = Key(-1000000114)

        @ExperimentalComposeUiApi
        actual val Toggle2D3D = Key(-1000000125)

        @ExperimentalComposeUiApi
        actual val Contacts = Key(-1000000126)

        @ExperimentalComposeUiApi
        actual val Calendar = Key(-1000000127)

        @ExperimentalComposeUiApi
        actual val Music = Key(-1000000128)

        @ExperimentalComposeUiApi
        actual val Calculator = Key(-1000000129)

        @ExperimentalComposeUiApi
        actual val ZenkakuHankaru = Key(-1000000130)

        @ExperimentalComposeUiApi
        actual val Eisu = Key(-1000000131)

        @ExperimentalComposeUiApi
        actual val Muhenkan = Key(-1000000132)

        @ExperimentalComposeUiApi
        actual val Henkan = Key(-1000000133)

        @ExperimentalComposeUiApi
        actual val KatakanaHiragana = Key(-1000000134)

        @ExperimentalComposeUiApi
        actual val Yen = Key(-1000000135)

        @ExperimentalComposeUiApi
        actual val Ro = Key(-1000000136)

        @ExperimentalComposeUiApi
        actual val Kana = Key(-1000000137)

        @ExperimentalComposeUiApi
        actual val Assist = Key(-1000000138)

        @ExperimentalComposeUiApi
        actual val BrightnessDown = Key(-1000000139)

        @ExperimentalComposeUiApi
        actual val BrightnessUp = Key(-1000000140)

        @ExperimentalComposeUiApi
        actual val Sleep = Key(-1000000141)

        @ExperimentalComposeUiApi
        actual val WakeUp = Key(-1000000142)

        @ExperimentalComposeUiApi
        actual val SoftSleep = Key(-1000000143)

        @ExperimentalComposeUiApi
        actual val Pairing = Key(-1000000144)

        @ExperimentalComposeUiApi
        actual val LastChannel = Key(-1000000145)

        @ExperimentalComposeUiApi
        actual val TvDataService = Key(-1000000146)

        @ExperimentalComposeUiApi
        actual val VoiceAssist = Key(-1000000147)

        @ExperimentalComposeUiApi
        actual val TvRadioService = Key(-1000000148)

        @ExperimentalComposeUiApi
        actual val TvTeletext = Key(-1000000149)

        @ExperimentalComposeUiApi
        actual val TvNumberEntry = Key(-1000000150)

        @ExperimentalComposeUiApi
        actual val TvTerrestrialAnalog = Key(-1000000151)

        @ExperimentalComposeUiApi
        actual val TvTerrestrialDigital = Key(-1000000152)

        @ExperimentalComposeUiApi
        actual val TvSatellite = Key(-1000000153)

        @ExperimentalComposeUiApi
        actual val TvSatelliteBs = Key(-1000000154)

        @ExperimentalComposeUiApi
        actual val TvSatelliteCs = Key(-1000000155)

        @ExperimentalComposeUiApi
        actual val TvSatelliteService = Key(-1000000156)

        @ExperimentalComposeUiApi
        actual val TvNetwork = Key(-1000000157)

        @ExperimentalComposeUiApi
        actual val TvAntennaCable = Key(-1000000158)

        @ExperimentalComposeUiApi
        actual val TvInputHdmi1 = Key(-1000000159)

        @ExperimentalComposeUiApi
        actual val TvInputHdmi2 = Key(-1000000160)

        @ExperimentalComposeUiApi
        actual val TvInputHdmi3 = Key(-1000000161)

        @ExperimentalComposeUiApi
        actual val TvInputHdmi4 = Key(-1000000162)

        @ExperimentalComposeUiApi
        actual val TvInputComposite1 = Key(-1000000163)

        @ExperimentalComposeUiApi
        actual val TvInputComposite2 = Key(-1000000164)

        @ExperimentalComposeUiApi
        actual val TvInputComponent1 = Key(-1000000165)

        @ExperimentalComposeUiApi
        actual val TvInputComponent2 = Key(-1000000166)

        @ExperimentalComposeUiApi
        actual val TvInputVga1 = Key(-1000000167)

        @ExperimentalComposeUiApi
        actual val TvAudioDescription = Key(-1000000168)

        @ExperimentalComposeUiApi
        actual val TvAudioDescriptionMixingVolumeUp = Key(-1000000169)

        @ExperimentalComposeUiApi
        actual val TvAudioDescriptionMixingVolumeDown = Key(-1000000170)

        @ExperimentalComposeUiApi
        actual val TvZoomMode = Key(-1000000171)

        @ExperimentalComposeUiApi
        actual val TvContentsMenu = Key(-1000000172)

        @ExperimentalComposeUiApi
        actual val TvMediaContextMenu = Key(-1000000173)

        @ExperimentalComposeUiApi
        actual val TvTimerProgramming = Key(-1000000174)

        @ExperimentalComposeUiApi
        actual val StemPrimary = Key(-1000000175)

        @ExperimentalComposeUiApi
        actual val Stem1 = Key(-1000000176)

        @ExperimentalComposeUiApi
        actual val Stem2 = Key(-1000000177)

        @ExperimentalComposeUiApi
        actual val Stem3 = Key(-1000000178)

        @ExperimentalComposeUiApi
        actual val AllApps = Key(-1000000179)

        @ExperimentalComposeUiApi
        actual val Refresh = Key(-1000000180)

        @ExperimentalComposeUiApi
        actual val ThumbsUp = Key(-1000000181)

        @ExperimentalComposeUiApi
        actual val ThumbsDown = Key(-1000000182)

        @ExperimentalComposeUiApi
        actual val ProfileSwitch = Key(-1000000183)

        @ExperimentalComposeUiApi
        actual val Help: Key = Key(-1000000184)

        @ExperimentalComposeUiApi
        actual val Plus: Key = Key(-1000000185)

        @ExperimentalComposeUiApi
        actual val Multiply: Key = Key(-1000000186)

        @ExperimentalComposeUiApi
        actual val Pound: Key = Key(-1000000187)

        @ExperimentalComposeUiApi
        actual val Cut: Key = Key(-1000000188)

        @ExperimentalComposeUiApi
        actual val Copy: Key = Key(-1000000189)

        @ExperimentalComposeUiApi
        actual val Paste: Key = Key(-1000000190)

        @ExperimentalComposeUiApi
        actual val Apostrophe: Key = Key(-1000000191)

        @ExperimentalComposeUiApi
        actual val At: Key = Key(-10000001902)

        @ExperimentalComposeUiApi
        actual val NumPadDot: Key = Key(-1000000193)

        @ExperimentalComposeUiApi
        actual val NumPadComma: Key = Key(-1000000194)

        @ExperimentalComposeUiApi
        actual val NumPadEquals: Key = Key(-1000000195)

        @ExperimentalComposeUiApi
        actual val NumPadLeftParenthesis: Key = Key(-1000000196)

        @ExperimentalComposeUiApi
        actual val NumPadRightParenthesis: Key = Key(-1000000197)
    }

    actual override fun toString() = "Key keyCode: $keyCode"
}

fun skikoKeyToKey(skikoKey: SkikoKey) = Key(skikoKey.platformKeyCode.toLong())

