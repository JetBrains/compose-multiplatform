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

import androidx.compose.ui.input.key.Key.Companion.Number
/*
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.KEY_LOCATION_LEFT
import java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD
import java.awt.event.KeyEvent.KEY_LOCATION_RIGHT
import java.awt.event.KeyEvent.KEY_LOCATION_STANDARD
*/
import androidx.compose.ui.util.unpackInt1

/**
 * Actual implementation of [Key] for Desktop.
 *
 * @param keyCode an integer code representing the key pressed. Note: This keycode can be used to
 * uniquely identify a hardware key. It is different from the native keycode.
 */
actual inline class Key(val keyCode: Long) {
    actual companion object {
        /** Unknown key. */
        actual val Unknown: Key = TODO("implement native Key") // Key(KeyEvent.VK_UNDEFINED)

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        actual val Home: Key = TODO("implement native Key") // Key(KeyEvent.VK_HOME)

        /** Help key. */
        actual val Help: Key = TODO("implement native Key") // Key(KeyEvent.VK_HELP)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionUp: Key = TODO("implement native Key") // Key(KeyEvent.VK_UP)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionDown: Key = TODO("implement native Key") // Key(KeyEvent.VK_DOWN)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionLeft: Key = TODO("implement native Key") // Key(KeyEvent.VK_LEFT)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionRight: Key = TODO("implement native Key") // Key(KeyEvent.VK_RIGHT)

        /** '0' key. */
        actual val Zero: Key = TODO("implement native Key") // Key(KeyEvent.VK_0)

        /** '1' key. */
        actual val One: Key = TODO("implement native Key") // Key(KeyEvent.VK_1)

        /** '2' key. */
        actual val Two: Key = TODO("implement native Key") // Key(KeyEvent.VK_2)

        /** '3' key. */
        actual val Three: Key = TODO("implement native Key") // Key(KeyEvent.VK_3)

        /** '4' key. */
        actual val Four: Key = TODO("implement native Key") // Key(KeyEvent.VK_4)

        /** '5' key. */
        actual val Five: Key = TODO("implement native Key") // Key(KeyEvent.VK_5)

        /** '6' key. */
        actual val Six: Key = TODO("implement native Key") // Key(KeyEvent.VK_6)

        /** '7' key. */
        actual val Seven: Key = TODO("implement native Key") // Key(KeyEvent.VK_7)

        /** '8' key. */
        actual val Eight: Key = TODO("implement native Key") // Key(KeyEvent.VK_8)

        /** '9' key. */
        actual val Nine: Key = TODO("implement native Key") // Key(KeyEvent.VK_9)

        /** '+' key. */
        actual val Plus: Key = TODO("implement native Key") // Key(KeyEvent.VK_PLUS)

        /** '-' key. */
        actual val Minus: Key = TODO("implement native Key") // Key(KeyEvent.VK_MINUS)

        /** '*' key. */
        actual val Multiply: Key = TODO("implement native Key") // Key(KeyEvent.VK_MULTIPLY)

        /** '=' key. */
        actual val Equals: Key = TODO("implement native Key") // Key(KeyEvent.VK_EQUALS)

        /** '#' key. */
        actual val Pound: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMBER_SIGN)

        /** 'A' key. */
        actual val A: Key = TODO("implement native Key") // Key(KeyEvent.VK_A)

        /** 'B' key. */
        actual val B: Key = TODO("implement native Key") // Key(KeyEvent.VK_B)

        /** 'C' key. */
        actual val C: Key = TODO("implement native Key") // Key(KeyEvent.VK_C)

        /** 'D' key. */
        actual val D: Key = TODO("implement native Key") // Key(KeyEvent.VK_D)

        /** 'E' key. */
        actual val E: Key = TODO("implement native Key") // Key(KeyEvent.VK_E)

        /** 'F' key. */
        actual val F: Key = TODO("implement native Key") // Key(KeyEvent.VK_F)

        /** 'G' key. */
        actual val G: Key = TODO("implement native Key") // Key(KeyEvent.VK_G)

        /** 'H' key. */
        actual val H: Key = TODO("implement native Key") // Key(KeyEvent.VK_H)

        /** 'I' key. */
        actual val I: Key = TODO("implement native Key") // Key(KeyEvent.VK_I)

        /** 'J' key. */
        actual val J: Key = TODO("implement native Key") // Key(KeyEvent.VK_J)

        /** 'K' key. */
        actual val K: Key = TODO("implement native Key") // Key(KeyEvent.VK_K)

        /** 'L' key. */
        actual val L: Key = TODO("implement native Key") // Key(KeyEvent.VK_L)

        /** 'M' key. */
        actual val M: Key = TODO("implement native Key") // Key(KeyEvent.VK_M)

        /** 'N' key. */
        actual val N: Key = TODO("implement native Key") // Key(KeyEvent.VK_N)

        /** 'O' key. */
        actual val O: Key = TODO("implement native Key") // Key(KeyEvent.VK_O)

        /** 'P' key. */
        actual val P: Key = TODO("implement native Key") // Key(KeyEvent.VK_P)

        /** 'Q' key. */
        actual val Q: Key = TODO("implement native Key") // Key(KeyEvent.VK_Q)

        /** 'R' key. */
        actual val R: Key = TODO("implement native Key") // Key(KeyEvent.VK_R)

        /** 'S' key. */
        actual val S: Key = TODO("implement native Key") // Key(KeyEvent.VK_S)

        /** 'T' key. */
        actual val T: Key = TODO("implement native Key") // Key(KeyEvent.VK_T)

        /** 'U' key. */
        actual val U: Key = TODO("implement native Key") // Key(KeyEvent.VK_U)

        /** 'V' key. */
        actual val V: Key = TODO("implement native Key") // Key(KeyEvent.VK_V)

        /** 'W' key. */
        actual val W: Key = TODO("implement native Key") // Key(KeyEvent.VK_W)

        /** 'X' key. */
        actual val X: Key = TODO("implement native Key") // Key(KeyEvent.VK_X)

        /** 'Y' key. */
        actual val Y: Key = TODO("implement native Key") // Key(KeyEvent.VK_Y)

        /** 'Z' key. */
        actual val Z: Key = TODO("implement native Key") // Key(KeyEvent.VK_Z)

        /** ',' key. */
        actual val Comma: Key = TODO("implement native Key") // Key(KeyEvent.VK_COMMA)

        /** '.' key. */
        actual val Period: Key = TODO("implement native Key") // Key(KeyEvent.VK_PERIOD)

        /** Left Alt modifier key. */
        actual val AltLeft: Key = TODO("implement native Key") // Key(KeyEvent.VK_ALT, KEY_LOCATION_LEFT)

        /** Right Alt modifier key. */
        actual val AltRight: Key = TODO("implement native Key") // Key(KeyEvent.VK_ALT, KEY_LOCATION_RIGHT)

        /** Left Shift modifier key. */
        actual val ShiftLeft: Key = TODO("implement native Key") // Key(KeyEvent.VK_SHIFT, KEY_LOCATION_LEFT)

        /** Right Shift modifier key. */
        actual val ShiftRight: Key = TODO("implement native Key") // Key(KeyEvent.VK_SHIFT, KEY_LOCATION_RIGHT)

        /** Tab key. */
        actual val Tab: Key = TODO("implement native Key") // Key(KeyEvent.VK_TAB)

        /** Space key. */
        actual val Spacebar: Key = TODO("implement native Key") // Key(KeyEvent.VK_SPACE)

        /** Enter key. */
        actual val Enter: Key = TODO("implement native Key") // Key(KeyEvent.VK_ENTER)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        actual val Backspace: Key = TODO("implement native Key") // Key(KeyEvent.VK_BACK_SPACE)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        actual val Delete: Key = TODO("implement native Key") // Key(KeyEvent.VK_DELETE)

        /** Escape key. */
        actual val Escape: Key = TODO("implement native Key") // Key(KeyEvent.VK_ESCAPE)

        /** Left Control modifier key. */
        actual val CtrlLeft: Key = TODO("implement native Key") // Key(KeyEvent.VK_CONTROL, KEY_LOCATION_LEFT)

        /** Right Control modifier key. */
        actual val CtrlRight: Key = TODO("implement native Key") // Key(KeyEvent.VK_CONTROL, KEY_LOCATION_RIGHT)

        /** Caps Lock key. */
        actual val CapsLock: Key = TODO("implement native Key") // Key(KeyEvent.VK_CAPS_LOCK)

        /** Scroll Lock key. */
        actual val ScrollLock: Key = TODO("implement native Key") // Key(KeyEvent.VK_SCROLL_LOCK)

        /** Left Meta modifier key. */
        actual val MetaLeft: Key = TODO("implement native Key") // Key(KeyEvent.VK_META, KEY_LOCATION_LEFT)

        /** Right Meta modifier key. */
        actual val MetaRight: Key = TODO("implement native Key") // Key(KeyEvent.VK_META, KEY_LOCATION_RIGHT)

        /** System Request / Print Screen key. */
        actual val PrintScreen: Key = TODO("implement native Key") // Key(KeyEvent.VK_PRINTSCREEN)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        actual val Insert: Key = TODO("implement native Key") // Key(KeyEvent.VK_INSERT)

        /** Cut key. */
        actual val Cut: Key = TODO("implement native Key") // Key(KeyEvent.VK_CUT)

        /** Copy key. */
        actual val Copy: Key = TODO("implement native Key") // Key(KeyEvent.VK_COPY)

        /** Paste key. */
        actual val Paste: Key = TODO("implement native Key") // Key(KeyEvent.VK_PASTE)

        /** '`' (backtick) key. */
        actual val Grave: Key = TODO("implement native Key") // Key(KeyEvent.VK_BACK_QUOTE)

        /** '[' key. */
        actual val LeftBracket: Key = TODO("implement native Key") // Key(KeyEvent.VK_OPEN_BRACKET)

        /** ']' key. */
        actual val RightBracket: Key = TODO("implement native Key") // Key(KeyEvent.VK_CLOSE_BRACKET)

        /** '/' key. */
        actual val Slash: Key = TODO("implement native Key") // Key(KeyEvent.VK_SLASH)

        /** '\' key. */
        actual val Backslash: Key = TODO("implement native Key") // Key(KeyEvent.VK_BACK_SLASH)

        /** ';' key. */
        actual val Semicolon: Key = TODO("implement native Key") // Key(KeyEvent.VK_SEMICOLON)

        /** ''' (apostrophe) key. */
        actual val Apostrophe: Key = TODO("implement native Key") // Key(KeyEvent.VK_QUOTE)

        /** '@' key. */
        actual val At: Key = TODO("implement native Key") // Key(KeyEvent.VK_AT)

        /** Page Up key. */
        actual val PageUp: Key = TODO("implement native Key") // Key(KeyEvent.VK_PAGE_UP)

        /** Page Down key. */
        actual val PageDown: Key = TODO("implement native Key") // Key(KeyEvent.VK_PAGE_DOWN)

        /** F1 key. */
        actual val F1: Key = TODO("implement native Key") // Key(KeyEvent.VK_F1)

        /** F2 key. */
        actual val F2: Key = TODO("implement native Key") // Key(KeyEvent.VK_F2)

        /** F3 key. */
        actual val F3: Key = TODO("implement native Key") // Key(KeyEvent.VK_F3)

        /** F4 key. */
        actual val F4: Key = TODO("implement native Key") // Key(KeyEvent.VK_F4)

        /** F5 key. */
        actual val F5: Key = TODO("implement native Key") // Key(KeyEvent.VK_F5)

        /** F6 key. */
        actual val F6: Key = TODO("implement native Key") // Key(KeyEvent.VK_F6)

        /** F7 key. */
        actual val F7: Key = TODO("implement native Key") // Key(KeyEvent.VK_F7)

        /** F8 key. */
        actual val F8: Key = TODO("implement native Key") // Key(KeyEvent.VK_F8)

        /** F9 key. */
        actual val F9: Key = TODO("implement native Key") // Key(KeyEvent.VK_F9)

        /** F10 key. */
        actual val F10: Key = TODO("implement native Key") // Key(KeyEvent.VK_F10)

        /** F11 key. */
        actual val F11: Key = TODO("implement native Key") // Key(KeyEvent.VK_F11)

        /** F12 key. */
        actual val F12: Key = TODO("implement native Key") // Key(KeyEvent.VK_F12)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        actual val NumLock: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUM_LOCK, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '0' key. */
        actual val NumPad0: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD0, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '1' key. */
        actual val NumPad1: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD1, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '2' key. */
        actual val NumPad2: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD2, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '3' key. */
        actual val NumPad3: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD3, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '4' key. */
        actual val NumPad4: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD4, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '5' key. */
        actual val NumPad5: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD5, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '6' key. */
        actual val NumPad6: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD6, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '7' key. */
        actual val NumPad7: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD7, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '8' key. */
        actual val NumPad8: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD8, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '9' key. */
        actual val NumPad9: Key = TODO("implement native Key") // Key(KeyEvent.VK_NUMPAD9, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '/' key (for division). */
        actual val NumPadDivide: Key = TODO("implement native Key") // Key(KeyEvent.VK_DIVIDE, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '*' key (for multiplication). */
        actual val NumPadMultiply: Key = TODO("implement native Key") // Key(KeyEvent.VK_MULTIPLY, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '-' key (for subtraction). */
        actual val NumPadSubtract: Key = TODO("implement native Key") // Key(KeyEvent.VK_SUBTRACT, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '+' key (for addition). */
        actual val NumPadAdd: Key = TODO("implement native Key") // Key(KeyEvent.VK_ADD, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        actual val NumPadDot: Key = TODO("implement native Key") // Key(KeyEvent.VK_PERIOD, KEY_LOCATION_NUMPAD)

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        actual val NumPadComma: Key = TODO("implement native Key") // Key(KeyEvent.VK_COMMA, KEY_LOCATION_NUMPAD)

        /** Numeric keypad Enter key. */
        actual val NumPadEnter: Key = TODO("implement native Key") // Key(KeyEvent.VK_ENTER, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '=' key. */
        actual val NumPadEquals: Key = TODO("implement native Key") // Key(KeyEvent.VK_EQUALS, KEY_LOCATION_NUMPAD)

        /** Numeric keypad '(' key. */
        actual val NumPadLeftParenthesis: Key = TODO("implement native Key") // Key(KeyEvent.VK_LEFT_PARENTHESIS, KEY_LOCATION_NUMPAD)

        /** Numeric keypad ')' key. */
        actual val NumPadRightParenthesis: Key = TODO("implement native Key") // Key(KeyEvent.VK_RIGHT_PARENTHESIS, KEY_LOCATION_NUMPAD)

        actual val MoveHome: Key = TODO("implement native Key") // Key(KeyEvent.VK_HOME)
        actual val MoveEnd: Key = TODO("implement native Key") // Key(KeyEvent.VK_END)

        // Unsupported Keys. These keys will never be sent by the desktop. However we need unique
        // keycodes so that these constants can be used in a when statement without a warning.
        actual val SoftLeft: Key = TODO("implement native Key") // Key(-1000000001)
        actual val SoftRight: Key = TODO("implement native Key") // Key(-1000000002)
        actual val Back: Key = TODO("implement native Key") // Key(-1000000003)
        actual val NavigatePrevious: Key = TODO("implement native Key") // Key(-1000000004)
        actual val NavigateNext: Key = TODO("implement native Key") // Key(-1000000005)
        actual val NavigateIn: Key = TODO("implement native Key") // Key(-1000000006)
        actual val NavigateOut: Key = TODO("implement native Key") // Key(-1000000007)
        actual val SystemNavigationUp: Key = TODO("implement native Key") // Key(-1000000008)
        actual val SystemNavigationDown: Key = TODO("implement native Key") // Key(-1000000009)
        actual val SystemNavigationLeft: Key = TODO("implement native Key") // Key(-1000000010)
        actual val SystemNavigationRight: Key = TODO("implement native Key") // Key(-1000000011)
        actual val Call: Key = TODO("implement native Key") // Key(-1000000012)
        actual val EndCall: Key = TODO("implement native Key") // Key(-1000000013)
        actual val DirectionCenter: Key = TODO("implement native Key") // Key(-1000000014)
        actual val DirectionUpLeft: Key = TODO("implement native Key") // Key(-1000000015)
        actual val DirectionDownLeft: Key = TODO("implement native Key") // Key(-1000000016)
        actual val DirectionUpRight: Key = TODO("implement native Key") // Key(-1000000017)
        actual val DirectionDownRight: Key = TODO("implement native Key") // Key(-1000000018)
        actual val VolumeUp: Key = TODO("implement native Key") // Key(-1000000019)
        actual val VolumeDown: Key = TODO("implement native Key") // Key(-1000000020)
        actual val Power: Key = TODO("implement native Key") // Key(-1000000021)
        actual val Camera: Key = TODO("implement native Key") // Key(-1000000022)
        actual val Clear: Key = TODO("implement native Key") // Key(-1000000023)
        actual val Symbol: Key = TODO("implement native Key") // Key(-1000000024)
        actual val Browser: Key = TODO("implement native Key") // Key(-1000000025)
        actual val Envelope: Key = TODO("implement native Key") // Key(-1000000026)
        actual val Function: Key = TODO("implement native Key") // Key(-1000000027)
        actual val Break: Key = TODO("implement native Key") // Key(-1000000028)
        actual val Number: Key = TODO("implement native Key") // Key(-1000000031)
        actual val HeadsetHook: Key = TODO("implement native Key") // Key(-1000000032)
        actual val Focus: Key = TODO("implement native Key") // Key(-1000000033)
        actual val Menu: Key = TODO("implement native Key") // Key(-1000000034)
        actual val Notification: Key = TODO("implement native Key") // Key(-1000000035)
        actual val Search: Key = TODO("implement native Key") // Key(-1000000036)
        actual val PictureSymbols: Key = TODO("implement native Key") // Key(-1000000037)
        actual val SwitchCharset: Key = TODO("implement native Key") // Key(-1000000038)
        actual val ButtonA: Key = TODO("implement native Key") // Key(-1000000039)
        actual val ButtonB: Key = TODO("implement native Key") // Key(-1000000040)
        actual val ButtonC: Key = TODO("implement native Key") // Key(-1000000041)
        actual val ButtonX: Key = TODO("implement native Key") // Key(-1000000042)
        actual val ButtonY: Key = TODO("implement native Key") // Key(-1000000043)
        actual val ButtonZ: Key = TODO("implement native Key") // Key(-1000000044)
        actual val ButtonL1: Key = TODO("implement native Key") // Key(-1000000045)
        actual val ButtonR1: Key = TODO("implement native Key") // Key(-1000000046)
        actual val ButtonL2: Key = TODO("implement native Key") // Key(-1000000047)
        actual val ButtonR2: Key = TODO("implement native Key") // Key(-1000000048)
        actual val ButtonThumbLeft: Key = TODO("implement native Key") // Key(-1000000049)
        actual val ButtonThumbRight: Key = TODO("implement native Key") // Key(-1000000050)
        actual val ButtonStart: Key = TODO("implement native Key") // Key(-1000000051)
        actual val ButtonSelect: Key = TODO("implement native Key") // Key(-1000000052)
        actual val ButtonMode: Key = TODO("implement native Key") // Key(-1000000053)
        actual val Button1: Key = TODO("implement native Key") // Key(-1000000054)
        actual val Button2: Key = TODO("implement native Key") // Key(-1000000055)
        actual val Button3: Key = TODO("implement native Key") // Key(-1000000056)
        actual val Button4: Key = TODO("implement native Key") // Key(-1000000057)
        actual val Button5: Key = TODO("implement native Key") // Key(-1000000058)
        actual val Button6: Key = TODO("implement native Key") // Key(-1000000059)
        actual val Button7: Key = TODO("implement native Key") // Key(-1000000060)
        actual val Button8: Key = TODO("implement native Key") // Key(-1000000061)
        actual val Button9: Key = TODO("implement native Key") // Key(-1000000062)
        actual val Button10: Key = TODO("implement native Key") // Key(-1000000063)
        actual val Button11: Key = TODO("implement native Key") // Key(-1000000064)
        actual val Button12: Key = TODO("implement native Key") // Key(-1000000065)
        actual val Button13: Key = TODO("implement native Key") // Key(-1000000066)
        actual val Button14: Key = TODO("implement native Key") // Key(-1000000067)
        actual val Button15: Key = TODO("implement native Key") // Key(-1000000068)
        actual val Button16: Key = TODO("implement native Key") // Key(-1000000069)
        actual val Forward: Key = TODO("implement native Key") // Key(-1000000070)
        actual val MediaPlay: Key = TODO("implement native Key") // Key(-1000000071)
        actual val MediaPause: Key = TODO("implement native Key") // Key(-1000000072)
        actual val MediaPlayPause: Key = TODO("implement native Key") // Key(-1000000073)
        actual val MediaStop: Key = TODO("implement native Key") // Key(-1000000074)
        actual val MediaRecord: Key = TODO("implement native Key") // Key(-1000000075)
        actual val MediaNext: Key = TODO("implement native Key") // Key(-1000000076)
        actual val MediaPrevious: Key = TODO("implement native Key") // Key(-1000000077)
        actual val MediaRewind: Key = TODO("implement native Key") // Key(-1000000078)
        actual val MediaFastForward: Key = TODO("implement native Key") // Key(-1000000079)
        actual val MediaClose: Key = TODO("implement native Key") // Key(-1000000080)
        actual val MediaAudioTrack: Key = TODO("implement native Key") // Key(-1000000081)
        actual val MediaEject: Key = TODO("implement native Key") // Key(-1000000082)
        actual val MediaTopMenu: Key = TODO("implement native Key") // Key(-1000000083)
        actual val MediaSkipForward: Key = TODO("implement native Key") // Key(-1000000084)
        actual val MediaSkipBackward: Key = TODO("implement native Key") // Key(-1000000085)
        actual val MediaStepForward: Key = TODO("implement native Key") // Key(-1000000086)
        actual val MediaStepBackward: Key = TODO("implement native Key") // Key(-1000000087)
        actual val MicrophoneMute: Key = TODO("implement native Key") // Key(-1000000088)
        actual val VolumeMute: Key = TODO("implement native Key") // Key(-1000000089)
        actual val Info: Key = TODO("implement native Key") // Key(-1000000090)
        actual val ChannelUp: Key = TODO("implement native Key") // Key(-1000000091)
        actual val ChannelDown: Key = TODO("implement native Key") // Key(-1000000092)
        actual val ZoomIn: Key = TODO("implement native Key") // Key(-1000000093)
        actual val ZoomOut: Key = TODO("implement native Key") // Key(-1000000094)
        actual val Tv: Key = TODO("implement native Key") // Key(-1000000095)
        actual val Window: Key = TODO("implement native Key") // Key(-1000000096)
        actual val Guide: Key = TODO("implement native Key") // Key(-1000000097)
        actual val Dvr: Key = TODO("implement native Key") // Key(-1000000098)
        actual val Bookmark: Key = TODO("implement native Key") // Key(-1000000099)
        actual val Captions: Key = TODO("implement native Key") // Key(-1000000100)
        actual val Settings: Key = TODO("implement native Key") // Key(-1000000101)
        actual val TvPower: Key = TODO("implement native Key") // Key(-1000000102)
        actual val TvInput: Key = TODO("implement native Key") // Key(-1000000103)
        actual val SetTopBoxPower: Key = TODO("implement native Key") // Key(-1000000104)
        actual val SetTopBoxInput: Key = TODO("implement native Key") // Key(-1000000105)
        actual val AvReceiverPower: Key = TODO("implement native Key") // Key(-1000000106)
        actual val AvReceiverInput: Key = TODO("implement native Key") // Key(-1000000107)
        actual val ProgramRed: Key = TODO("implement native Key") // Key(-1000000108)
        actual val ProgramGreen: Key = TODO("implement native Key") // Key(-1000000109)
        actual val ProgramYellow: Key = TODO("implement native Key") // Key(-1000000110)
        actual val ProgramBlue: Key = TODO("implement native Key") // Key(-1000000111)
        actual val AppSwitch: Key = TODO("implement native Key") // Key(-1000000112)
        actual val LanguageSwitch: Key = TODO("implement native Key") // Key(-1000000113)
        actual val MannerMode: Key = TODO("implement native Key") // Key(-1000000114)
        actual val Toggle2D3D: Key = TODO("implement native Key") // Key(-1000000125)
        actual val Contacts: Key = TODO("implement native Key") // Key(-1000000126)
        actual val Calendar: Key = TODO("implement native Key") // Key(-1000000127)
        actual val Music: Key = TODO("implement native Key") // Key(-1000000128)
        actual val Calculator: Key = TODO("implement native Key") // Key(-1000000129)
        actual val ZenkakuHankaru: Key = TODO("implement native Key") // Key(-1000000130)
        actual val Eisu: Key = TODO("implement native Key") // Key(-1000000131)
        actual val Muhenkan: Key = TODO("implement native Key") // Key(-1000000132)
        actual val Henkan: Key = TODO("implement native Key") // Key(-1000000133)
        actual val KatakanaHiragana: Key = TODO("implement native Key") // Key(-1000000134)
        actual val Yen: Key = TODO("implement native Key") // Key(-1000000135)
        actual val Ro: Key = TODO("implement native Key") // Key(-1000000136)
        actual val Kana: Key = TODO("implement native Key") // Key(-1000000137)
        actual val Assist: Key = TODO("implement native Key") // Key(-1000000138)
        actual val BrightnessDown: Key = TODO("implement native Key") // Key(-1000000139)
        actual val BrightnessUp: Key = TODO("implement native Key") // Key(-1000000140)
        actual val Sleep: Key = TODO("implement native Key") // Key(-1000000141)
        actual val WakeUp: Key = TODO("implement native Key") // Key(-1000000142)
        actual val SoftSleep: Key = TODO("implement native Key") // Key(-1000000143)
        actual val Pairing: Key = TODO("implement native Key") // Key(-1000000144)
        actual val LastChannel: Key = TODO("implement native Key") // Key(-1000000145)
        actual val TvDataService: Key = TODO("implement native Key") // Key(-1000000146)
        actual val VoiceAssist: Key = TODO("implement native Key") // Key(-1000000147)
        actual val TvRadioService: Key = TODO("implement native Key") // Key(-1000000148)
        actual val TvTeletext: Key = TODO("implement native Key") // Key(-1000000149)
        actual val TvNumberEntry: Key = TODO("implement native Key") // Key(-1000000150)
        actual val TvTerrestrialAnalog: Key = TODO("implement native Key") // Key(-1000000151)
        actual val TvTerrestrialDigital: Key = TODO("implement native Key") // Key(-1000000152)
        actual val TvSatellite: Key = TODO("implement native Key") // Key(-1000000153)
        actual val TvSatelliteBs: Key = TODO("implement native Key") // Key(-1000000154)
        actual val TvSatelliteCs: Key = TODO("implement native Key") // Key(-1000000155)
        actual val TvSatelliteService: Key = TODO("implement native Key") // Key(-1000000156)
        actual val TvNetwork: Key = TODO("implement native Key") // Key(-1000000157)
        actual val TvAntennaCable: Key = TODO("implement native Key") // Key(-1000000158)
        actual val TvInputHdmi1: Key = TODO("implement native Key") // Key(-1000000159)
        actual val TvInputHdmi2: Key = TODO("implement native Key") // Key(-1000000160)
        actual val TvInputHdmi3: Key = TODO("implement native Key") // Key(-1000000161)
        actual val TvInputHdmi4: Key = TODO("implement native Key") // Key(-1000000162)
        actual val TvInputComposite1: Key = TODO("implement native Key") // Key(-1000000163)
        actual val TvInputComposite2: Key = TODO("implement native Key") // Key(-1000000164)
        actual val TvInputComponent1: Key = TODO("implement native Key") // Key(-1000000165)
        actual val TvInputComponent2: Key = TODO("implement native Key") // Key(-1000000166)
        actual val TvInputVga1: Key = TODO("implement native Key") // Key(-1000000167)
        actual val TvAudioDescription: Key = TODO("implement native Key") // Key(-1000000168)
        actual val TvAudioDescriptionMixingVolumeUp: Key = TODO("implement native Key") // Key(-1000000169)
        actual val TvAudioDescriptionMixingVolumeDown: Key = TODO("implement native Key") // Key(-1000000170)
        actual val TvZoomMode: Key = TODO("implement native Key") // Key(-1000000171)
        actual val TvContentsMenu: Key = TODO("implement native Key") // Key(-1000000172)
        actual val TvMediaContextMenu: Key = TODO("implement native Key") // Key(-1000000173)
        actual val TvTimerProgramming: Key = TODO("implement native Key") // Key(-1000000174)
        actual val StemPrimary: Key = TODO("implement native Key") // Key(-1000000175)
        actual val Stem1: Key = TODO("implement native Key") // Key(-1000000176)
        actual val Stem2: Key = TODO("implement native Key") // Key(-1000000177)
        actual val Stem3: Key = TODO("implement native Key") // Key(-1000000178)
        actual val AllApps: Key = TODO("implement native Key") // Key(-1000000179)
        actual val Refresh: Key = TODO("implement native Key") // Key(-1000000180)
        actual val ThumbsUp: Key = TODO("implement native Key") // Key(-1000000181)
        actual val ThumbsDown: Key = TODO("implement native Key") // Key(-1000000182)
        actual val ProfileSwitch: Key = TODO("implement native Key") // Key(-1000000183)
    }

    actual override fun toString(): String {
        TODO("implement Key toString")
        //return "Key: ${KeyEvent.getKeyText(nativeKeyCode)}"
    }
}

/**
 * Creates instance of [Key].
 *
 * @param nativeKeyCode represents this key as defined in [java.awt.event.KeyEvent]
 * @param nativeKeyLocation represents the location of key as defined in [java.awt.event.KeyEvent]
 */
fun Key(nativeKeyCode: Int, nativeKeyLocation: Int /*= KEY_LOCATION_STANDARD*/): Key {
    // First 32 bits are for keycode.
    val keyCode = nativeKeyCode.toLong().shl(32)

    // Next 3 bits are for location.
    val location = (nativeKeyLocation.toLong() and 0x7).shl(29)

    return Key(keyCode or location)
}

/**
 * The native keycode corresponding to this [Key].
 */
val Key.nativeKeyCode: Int
    get() = unpackInt1(keyCode)

/**
 * The native location corresponding to this [Key].
 */
val Key.nativeKeyLocation: Int
    get() = (keyCode and 0xFFFFFFFF).shr(29).toInt()
