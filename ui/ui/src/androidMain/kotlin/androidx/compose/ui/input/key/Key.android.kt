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

import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key.Companion.Number
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1

/**
 * Actual implementation of [Key] for Android.
 *
 * @param keyCode an integer code representing the key pressed.
 *
 * @sample androidx.compose.ui.samples.KeyEventIsAltPressedSample
 */
@JvmInline
actual value class Key(val keyCode: Long) {
    actual companion object {
        /** Unknown key. */
        @ExperimentalComposeUiApi
        actual val Unknown = Key(KeyEvent.KEYCODE_UNKNOWN)

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom left
         * of the display.
         */
        @ExperimentalComposeUiApi
        actual val SoftLeft = Key(KeyEvent.KEYCODE_SOFT_LEFT)

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom right
         * of the display.
         */
        @ExperimentalComposeUiApi
        actual val SoftRight = Key(KeyEvent.KEYCODE_SOFT_RIGHT)

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        @ExperimentalComposeUiApi
        actual val Home = Key(KeyEvent.KEYCODE_HOME)

        /** Back key. */
        @ExperimentalComposeUiApi
        actual val Back = Key(KeyEvent.KEYCODE_BACK)

        /** Help key. */
        @ExperimentalComposeUiApi
        actual val Help = Key(KeyEvent.KEYCODE_HELP)

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        @ExperimentalComposeUiApi
        actual val NavigatePrevious = Key(KeyEvent.KEYCODE_NAVIGATE_PREVIOUS)

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        @ExperimentalComposeUiApi
        actual val NavigateNext = Key(KeyEvent.KEYCODE_NAVIGATE_NEXT)

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        @ExperimentalComposeUiApi
        actual val NavigateIn = Key(KeyEvent.KEYCODE_NAVIGATE_IN)

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        @ExperimentalComposeUiApi
        actual val NavigateOut = Key(KeyEvent.KEYCODE_NAVIGATE_OUT)

        /** Consumed by the system for navigation up. */
        @ExperimentalComposeUiApi
        actual val SystemNavigationUp = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP)

        /** Consumed by the system for navigation down. */
        @ExperimentalComposeUiApi
        actual val SystemNavigationDown = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN)

        /** Consumed by the system for navigation left. */
        @ExperimentalComposeUiApi
        actual val SystemNavigationLeft = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT)

        /** Consumed by the system for navigation right. */
        @ExperimentalComposeUiApi
        actual val SystemNavigationRight = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT)

        /** Call key. */
        @ExperimentalComposeUiApi
        actual val Call = Key(KeyEvent.KEYCODE_CALL)

        /** End Call key. */
        @ExperimentalComposeUiApi
        actual val EndCall = Key(KeyEvent.KEYCODE_ENDCALL)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionUp = Key(KeyEvent.KEYCODE_DPAD_UP)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionDown = Key(KeyEvent.KEYCODE_DPAD_DOWN)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionLeft = Key(KeyEvent.KEYCODE_DPAD_LEFT)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionRight = Key(KeyEvent.KEYCODE_DPAD_RIGHT)

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        actual val DirectionCenter = Key(KeyEvent.KEYCODE_DPAD_CENTER)

        /** Directional Pad Up-Left. */
        @ExperimentalComposeUiApi
        actual val DirectionUpLeft = Key(KeyEvent.KEYCODE_DPAD_UP_LEFT)

        /** Directional Pad Down-Left. */
        @ExperimentalComposeUiApi
        actual val DirectionDownLeft = Key(KeyEvent.KEYCODE_DPAD_DOWN_LEFT)

        /** Directional Pad Up-Right. */
        @ExperimentalComposeUiApi
        actual val DirectionUpRight = Key(KeyEvent.KEYCODE_DPAD_UP_RIGHT)

        /** Directional Pad Down-Right. */
        @ExperimentalComposeUiApi
        actual val DirectionDownRight = Key(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT)

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        @ExperimentalComposeUiApi
        actual val VolumeUp = Key(KeyEvent.KEYCODE_VOLUME_UP)

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        @ExperimentalComposeUiApi
        actual val VolumeDown = Key(KeyEvent.KEYCODE_VOLUME_DOWN)

        /** Power key.  */
        @ExperimentalComposeUiApi
        actual val Power = Key(KeyEvent.KEYCODE_POWER)

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        @ExperimentalComposeUiApi
        actual val Camera = Key(KeyEvent.KEYCODE_CAMERA)

        /** Clear key. */
        @ExperimentalComposeUiApi
        actual val Clear = Key(KeyEvent.KEYCODE_CLEAR)

        /** '0' key. */
        @ExperimentalComposeUiApi
        actual val Zero = Key(KeyEvent.KEYCODE_0)

        /** '1' key. */
        @ExperimentalComposeUiApi
        actual val One = Key(KeyEvent.KEYCODE_1)

        /** '2' key. */
        @ExperimentalComposeUiApi
        actual val Two = Key(KeyEvent.KEYCODE_2)

        /** '3' key. */
        @ExperimentalComposeUiApi
        actual val Three = Key(KeyEvent.KEYCODE_3)

        /** '4' key. */
        @ExperimentalComposeUiApi
        actual val Four = Key(KeyEvent.KEYCODE_4)

        /** '5' key. */
        @ExperimentalComposeUiApi
        actual val Five = Key(KeyEvent.KEYCODE_5)

        /** '6' key. */
        @ExperimentalComposeUiApi
        actual val Six = Key(KeyEvent.KEYCODE_6)

        /** '7' key. */
        @ExperimentalComposeUiApi
        actual val Seven = Key(KeyEvent.KEYCODE_7)

        /** '8' key. */
        @ExperimentalComposeUiApi
        actual val Eight = Key(KeyEvent.KEYCODE_8)

        /** '9' key. */
        @ExperimentalComposeUiApi
        actual val Nine = Key(KeyEvent.KEYCODE_9)

        /** '+' key. */
        @ExperimentalComposeUiApi
        actual val Plus = Key(KeyEvent.KEYCODE_PLUS)

        /** '-' key. */
        @ExperimentalComposeUiApi
        actual val Minus = Key(KeyEvent.KEYCODE_MINUS)

        /** '*' key. */
        @ExperimentalComposeUiApi
        actual val Multiply = Key(KeyEvent.KEYCODE_STAR)

        /** '=' key. */
        @ExperimentalComposeUiApi
        actual val Equals = Key(KeyEvent.KEYCODE_EQUALS)

        /** '#' key. */
        @ExperimentalComposeUiApi
        actual val Pound = Key(KeyEvent.KEYCODE_POUND)

        /** 'A' key. */
        @ExperimentalComposeUiApi
        actual val A = Key(KeyEvent.KEYCODE_A)

        /** 'B' key. */
        @ExperimentalComposeUiApi
        actual val B = Key(KeyEvent.KEYCODE_B)

        /** 'C' key. */
        @ExperimentalComposeUiApi
        actual val C = Key(KeyEvent.KEYCODE_C)

        /** 'D' key. */
        @ExperimentalComposeUiApi
        actual val D = Key(KeyEvent.KEYCODE_D)

        /** 'E' key. */
        @ExperimentalComposeUiApi
        actual val E = Key(KeyEvent.KEYCODE_E)

        /** 'F' key. */
        @ExperimentalComposeUiApi
        actual val F = Key(KeyEvent.KEYCODE_F)

        /** 'G' key. */
        @ExperimentalComposeUiApi
        actual val G = Key(KeyEvent.KEYCODE_G)

        /** 'H' key. */
        @ExperimentalComposeUiApi
        actual val H = Key(KeyEvent.KEYCODE_H)

        /** 'I' key. */
        @ExperimentalComposeUiApi
        actual val I = Key(KeyEvent.KEYCODE_I)

        /** 'J' key. */
        @ExperimentalComposeUiApi
        actual val J = Key(KeyEvent.KEYCODE_J)

        /** 'K' key. */
        @ExperimentalComposeUiApi
        actual val K = Key(KeyEvent.KEYCODE_K)

        /** 'L' key. */
        @ExperimentalComposeUiApi
        actual val L = Key(KeyEvent.KEYCODE_L)

        /** 'M' key. */
        @ExperimentalComposeUiApi
        actual val M = Key(KeyEvent.KEYCODE_M)

        /** 'N' key. */
        @ExperimentalComposeUiApi
        actual val N = Key(KeyEvent.KEYCODE_N)

        /** 'O' key. */
        @ExperimentalComposeUiApi
        actual val O = Key(KeyEvent.KEYCODE_O)

        /** 'P' key. */
        @ExperimentalComposeUiApi
        actual val P = Key(KeyEvent.KEYCODE_P)

        /** 'Q' key. */
        @ExperimentalComposeUiApi
        actual val Q = Key(KeyEvent.KEYCODE_Q)

        /** 'R' key. */
        @ExperimentalComposeUiApi
        actual val R = Key(KeyEvent.KEYCODE_R)

        /** 'S' key. */
        @ExperimentalComposeUiApi
        actual val S = Key(KeyEvent.KEYCODE_S)

        /** 'T' key. */
        @ExperimentalComposeUiApi
        actual val T = Key(KeyEvent.KEYCODE_T)

        /** 'U' key. */
        @ExperimentalComposeUiApi
        actual val U = Key(KeyEvent.KEYCODE_U)

        /** 'V' key. */
        @ExperimentalComposeUiApi
        actual val V = Key(KeyEvent.KEYCODE_V)

        /** 'W' key. */
        @ExperimentalComposeUiApi
        actual val W = Key(KeyEvent.KEYCODE_W)

        /** 'X' key. */
        @ExperimentalComposeUiApi
        actual val X = Key(KeyEvent.KEYCODE_X)

        /** 'Y' key. */
        @ExperimentalComposeUiApi
        actual val Y = Key(KeyEvent.KEYCODE_Y)

        /** 'Z' key. */
        @ExperimentalComposeUiApi
        actual val Z = Key(KeyEvent.KEYCODE_Z)

        /** ',' key. */
        @ExperimentalComposeUiApi
        actual val Comma = Key(KeyEvent.KEYCODE_COMMA)

        /** '.' key. */
        @ExperimentalComposeUiApi
        actual val Period = Key(KeyEvent.KEYCODE_PERIOD)

        /** Left Alt modifier key. */
        @ExperimentalComposeUiApi
        actual val AltLeft = Key(KeyEvent.KEYCODE_ALT_LEFT)

        /** Right Alt modifier key. */
        @ExperimentalComposeUiApi
        actual val AltRight = Key(KeyEvent.KEYCODE_ALT_RIGHT)

        /** Left Shift modifier key. */
        @ExperimentalComposeUiApi
        actual val ShiftLeft = Key(KeyEvent.KEYCODE_SHIFT_LEFT)

        /** Right Shift modifier key. */
        @ExperimentalComposeUiApi
        actual val ShiftRight = Key(KeyEvent.KEYCODE_SHIFT_RIGHT)

        /** Tab key. */
        @ExperimentalComposeUiApi
        actual val Tab = Key(KeyEvent.KEYCODE_TAB)

        /** Space key. */
        @ExperimentalComposeUiApi
        actual val Spacebar = Key(KeyEvent.KEYCODE_SPACE)

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        @ExperimentalComposeUiApi
        actual val Symbol = Key(KeyEvent.KEYCODE_SYM)

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        @ExperimentalComposeUiApi
        actual val Browser = Key(KeyEvent.KEYCODE_EXPLORER)

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        @ExperimentalComposeUiApi
        actual val Envelope = Key(KeyEvent.KEYCODE_ENVELOPE)

        /** Enter key. */
        @ExperimentalComposeUiApi
        actual val Enter = Key(KeyEvent.KEYCODE_ENTER)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        @ExperimentalComposeUiApi
        actual val Backspace = Key(KeyEvent.KEYCODE_DEL)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        @ExperimentalComposeUiApi
        actual val Delete = Key(KeyEvent.KEYCODE_FORWARD_DEL)

        /** Escape key. */
        @ExperimentalComposeUiApi
        actual val Escape = Key(KeyEvent.KEYCODE_ESCAPE)

        /** Left Control modifier key. */
        @ExperimentalComposeUiApi
        actual val CtrlLeft = Key(KeyEvent.KEYCODE_CTRL_LEFT)

        /** Right Control modifier key. */
        @ExperimentalComposeUiApi
        actual val CtrlRight = Key(KeyEvent.KEYCODE_CTRL_RIGHT)

        /** Caps Lock key. */
        @ExperimentalComposeUiApi
        actual val CapsLock = Key(KeyEvent.KEYCODE_CAPS_LOCK)

        /** Scroll Lock key. */
        @ExperimentalComposeUiApi
        actual val ScrollLock = Key(KeyEvent.KEYCODE_SCROLL_LOCK)

        /** Left Meta modifier key. */
        @ExperimentalComposeUiApi
        actual val MetaLeft = Key(KeyEvent.KEYCODE_META_LEFT)

        /** Right Meta modifier key. */
        @ExperimentalComposeUiApi
        actual val MetaRight = Key(KeyEvent.KEYCODE_META_RIGHT)

        /** Function modifier key. */
        @ExperimentalComposeUiApi
        actual val Function = Key(KeyEvent.KEYCODE_FUNCTION)

        /** System Request / Print Screen key. */
        @ExperimentalComposeUiApi
        actual val PrintScreen = Key(KeyEvent.KEYCODE_SYSRQ)

        /** Break / Pause key. */
        @ExperimentalComposeUiApi
        actual val Break = Key(KeyEvent.KEYCODE_BREAK)

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line
         * or to the top of a list.
         */
        @ExperimentalComposeUiApi
        actual val MoveHome = Key(KeyEvent.KEYCODE_MOVE_HOME)

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line
         * or to the bottom of a list.
         */
        @ExperimentalComposeUiApi
        actual val MoveEnd = Key(KeyEvent.KEYCODE_MOVE_END)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        @ExperimentalComposeUiApi
        actual val Insert = Key(KeyEvent.KEYCODE_INSERT)

        /** Cut key. */
        @ExperimentalComposeUiApi
        actual val Cut = Key(KeyEvent.KEYCODE_CUT)

        /** Copy key. */
        @ExperimentalComposeUiApi
        actual val Copy = Key(KeyEvent.KEYCODE_COPY)

        /** Paste key. */
        @ExperimentalComposeUiApi
        actual val Paste = Key(KeyEvent.KEYCODE_PASTE)

        /** '`' (backtick) key. */
        @ExperimentalComposeUiApi
        actual val Grave = Key(KeyEvent.KEYCODE_GRAVE)

        /** '[' key. */
        @ExperimentalComposeUiApi
        actual val LeftBracket = Key(KeyEvent.KEYCODE_LEFT_BRACKET)

        /** ']' key. */
        @ExperimentalComposeUiApi
        actual val RightBracket = Key(KeyEvent.KEYCODE_RIGHT_BRACKET)

        /** '/' key. */
        @ExperimentalComposeUiApi
        actual val Slash = Key(KeyEvent.KEYCODE_SLASH)

        /** '\' key. */
        @ExperimentalComposeUiApi
        actual val Backslash = Key(KeyEvent.KEYCODE_BACKSLASH)

        /** ';' key. */
        @ExperimentalComposeUiApi
        actual val Semicolon = Key(KeyEvent.KEYCODE_SEMICOLON)

        /** ''' (apostrophe) key. */
        @ExperimentalComposeUiApi
        actual val Apostrophe = Key(KeyEvent.KEYCODE_APOSTROPHE)

        /** '@' key. */
        @ExperimentalComposeUiApi
        actual val At = Key(KeyEvent.KEYCODE_AT)

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols.
         * This key is not Num Lock; it is more like  [AltLeft].
         */
        @ExperimentalComposeUiApi
        actual val Number = Key(KeyEvent.KEYCODE_NUM)

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        @ExperimentalComposeUiApi
        actual val HeadsetHook = Key(KeyEvent.KEYCODE_HEADSETHOOK)

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        @ExperimentalComposeUiApi
        actual val Focus = Key(KeyEvent.KEYCODE_FOCUS)

        /** Menu key. */
        @ExperimentalComposeUiApi
        actual val Menu = Key(KeyEvent.KEYCODE_MENU)

        /** Notification key. */
        @ExperimentalComposeUiApi
        actual val Notification = Key(KeyEvent.KEYCODE_NOTIFICATION)

        /** Search key. */
        @ExperimentalComposeUiApi
        actual val Search = Key(KeyEvent.KEYCODE_SEARCH)

        /** Page Up key. */
        @ExperimentalComposeUiApi
        actual val PageUp = Key(KeyEvent.KEYCODE_PAGE_UP)

        /** Page Down key. */
        @ExperimentalComposeUiApi
        actual val PageDown = Key(KeyEvent.KEYCODE_PAGE_DOWN)

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        @ExperimentalComposeUiApi
        actual val PictureSymbols = Key(KeyEvent.KEYCODE_PICTSYMBOLS)

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        @ExperimentalComposeUiApi
        actual val SwitchCharset = Key(KeyEvent.KEYCODE_SWITCH_CHARSET)

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A
         * or the first button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonA = Key(KeyEvent.KEYCODE_BUTTON_A)

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B
         * or the second button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonB = Key(KeyEvent.KEYCODE_BUTTON_B)

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C
         * or the third button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonC = Key(KeyEvent.KEYCODE_BUTTON_C)

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X
         * or the first button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonX = Key(KeyEvent.KEYCODE_BUTTON_X)

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y
         * or the second button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonY = Key(KeyEvent.KEYCODE_BUTTON_Y)

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z
         * or the third button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        actual val ButtonZ = Key(KeyEvent.KEYCODE_BUTTON_Z)

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L)
         * or the top left trigger button.
         */
        @ExperimentalComposeUiApi
        actual val ButtonL1 = Key(KeyEvent.KEYCODE_BUTTON_L1)

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R)
         * or the top right trigger button.
         */
        @ExperimentalComposeUiApi
        actual val ButtonR1 = Key(KeyEvent.KEYCODE_BUTTON_R1)

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2
         * or the bottom left trigger button.
         */
        @ExperimentalComposeUiApi
        actual val ButtonL2 = Key(KeyEvent.KEYCODE_BUTTON_L2)

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2
         * or the bottom right trigger button.
         */
        @ExperimentalComposeUiApi
        actual val ButtonR2 = Key(KeyEvent.KEYCODE_BUTTON_R2)

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only)
         * joystick is pressed.
         */
        @ExperimentalComposeUiApi
        actual val ButtonThumbLeft = Key(KeyEvent.KEYCODE_BUTTON_THUMBL)

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right
         * joystick is pressed.
         */
        @ExperimentalComposeUiApi
        actual val ButtonThumbRight = Key(KeyEvent.KEYCODE_BUTTON_THUMBR)

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        @ExperimentalComposeUiApi
        actual val ButtonStart = Key(KeyEvent.KEYCODE_BUTTON_START)

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        @ExperimentalComposeUiApi
        actual val ButtonSelect = Key(KeyEvent.KEYCODE_BUTTON_SELECT)

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        @ExperimentalComposeUiApi
        actual val ButtonMode = Key(KeyEvent.KEYCODE_BUTTON_MODE)

        /** Generic Game Pad Button #1. */
        @ExperimentalComposeUiApi
        actual val Button1 = Key(KeyEvent.KEYCODE_BUTTON_1)

        /** Generic Game Pad Button #2. */
        @ExperimentalComposeUiApi
        actual val Button2 = Key(KeyEvent.KEYCODE_BUTTON_2)

        /** Generic Game Pad Button #3. */
        @ExperimentalComposeUiApi
        actual val Button3 = Key(KeyEvent.KEYCODE_BUTTON_3)

        /** Generic Game Pad Button #4. */
        @ExperimentalComposeUiApi
        actual val Button4 = Key(KeyEvent.KEYCODE_BUTTON_4)

        /** Generic Game Pad Button #5. */
        @ExperimentalComposeUiApi
        actual val Button5 = Key(KeyEvent.KEYCODE_BUTTON_5)

        /** Generic Game Pad Button #6. */
        @ExperimentalComposeUiApi
        actual val Button6 = Key(KeyEvent.KEYCODE_BUTTON_6)

        /** Generic Game Pad Button #7. */
        @ExperimentalComposeUiApi
        actual val Button7 = Key(KeyEvent.KEYCODE_BUTTON_7)

        /** Generic Game Pad Button #8. */
        @ExperimentalComposeUiApi
        actual val Button8 = Key(KeyEvent.KEYCODE_BUTTON_8)

        /** Generic Game Pad Button #9. */
        @ExperimentalComposeUiApi
        actual val Button9 = Key(KeyEvent.KEYCODE_BUTTON_9)

        /** Generic Game Pad Button #10. */
        @ExperimentalComposeUiApi
        actual val Button10 = Key(KeyEvent.KEYCODE_BUTTON_10)

        /** Generic Game Pad Button #11. */
        @ExperimentalComposeUiApi
        actual val Button11 = Key(KeyEvent.KEYCODE_BUTTON_11)

        /** Generic Game Pad Button #12. */
        @ExperimentalComposeUiApi
        actual val Button12 = Key(KeyEvent.KEYCODE_BUTTON_12)

        /** Generic Game Pad Button #13. */
        @ExperimentalComposeUiApi
        actual val Button13 = Key(KeyEvent.KEYCODE_BUTTON_13)

        /** Generic Game Pad Button #14. */
        @ExperimentalComposeUiApi
        actual val Button14 = Key(KeyEvent.KEYCODE_BUTTON_14)

        /** Generic Game Pad Button #15. */
        @ExperimentalComposeUiApi
        actual val Button15 = Key(KeyEvent.KEYCODE_BUTTON_15)

        /** Generic Game Pad Button #16. */
        @ExperimentalComposeUiApi
        actual val Button16 = Key(KeyEvent.KEYCODE_BUTTON_16)

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        @ExperimentalComposeUiApi
        actual val Forward = Key(KeyEvent.KEYCODE_FORWARD)

        /** F1 key. */
        @ExperimentalComposeUiApi
        actual val F1 = Key(KeyEvent.KEYCODE_F1)

        /** F2 key. */
        @ExperimentalComposeUiApi
        actual val F2 = Key(KeyEvent.KEYCODE_F2)

        /** F3 key. */
        @ExperimentalComposeUiApi
        actual val F3 = Key(KeyEvent.KEYCODE_F3)

        /** F4 key. */
        @ExperimentalComposeUiApi
        actual val F4 = Key(KeyEvent.KEYCODE_F4)

        /** F5 key. */
        @ExperimentalComposeUiApi
        actual val F5 = Key(KeyEvent.KEYCODE_F5)

        /** F6 key. */
        @ExperimentalComposeUiApi
        actual val F6 = Key(KeyEvent.KEYCODE_F6)

        /** F7 key. */
        @ExperimentalComposeUiApi
        actual val F7 = Key(KeyEvent.KEYCODE_F7)

        /** F8 key. */
        @ExperimentalComposeUiApi
        actual val F8 = Key(KeyEvent.KEYCODE_F8)

        /** F9 key. */
        @ExperimentalComposeUiApi
        actual val F9 = Key(KeyEvent.KEYCODE_F9)

        /** F10 key. */
        @ExperimentalComposeUiApi
        actual val F10 = Key(KeyEvent.KEYCODE_F10)

        /** F11 key. */
        @ExperimentalComposeUiApi
        actual val F11 = Key(KeyEvent.KEYCODE_F11)

        /** F12 key. */
        @ExperimentalComposeUiApi
        actual val F12 = Key(KeyEvent.KEYCODE_F12)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        @ExperimentalComposeUiApi
        actual val NumLock = Key(KeyEvent.KEYCODE_NUM_LOCK)

        /** Numeric keypad '0' key. */
        @ExperimentalComposeUiApi
        actual val NumPad0 = Key(KeyEvent.KEYCODE_NUMPAD_0)

        /** Numeric keypad '1' key. */
        @ExperimentalComposeUiApi
        actual val NumPad1 = Key(KeyEvent.KEYCODE_NUMPAD_1)

        /** Numeric keypad '2' key. */
        @ExperimentalComposeUiApi
        actual val NumPad2 = Key(KeyEvent.KEYCODE_NUMPAD_2)

        /** Numeric keypad '3' key. */
        @ExperimentalComposeUiApi
        actual val NumPad3 = Key(KeyEvent.KEYCODE_NUMPAD_3)

        /** Numeric keypad '4' key. */
        @ExperimentalComposeUiApi
        actual val NumPad4 = Key(KeyEvent.KEYCODE_NUMPAD_4)

        /** Numeric keypad '5' key. */
        @ExperimentalComposeUiApi
        actual val NumPad5 = Key(KeyEvent.KEYCODE_NUMPAD_5)

        /** Numeric keypad '6' key. */
        @ExperimentalComposeUiApi
        actual val NumPad6 = Key(KeyEvent.KEYCODE_NUMPAD_6)

        /** Numeric keypad '7' key. */
        @ExperimentalComposeUiApi
        actual val NumPad7 = Key(KeyEvent.KEYCODE_NUMPAD_7)

        /** Numeric keypad '8' key. */
        @ExperimentalComposeUiApi
        actual val NumPad8 = Key(KeyEvent.KEYCODE_NUMPAD_8)

        /** Numeric keypad '9' key. */
        @ExperimentalComposeUiApi
        actual val NumPad9 = Key(KeyEvent.KEYCODE_NUMPAD_9)

        /** Numeric keypad '/' key (for division). */
        @ExperimentalComposeUiApi
        actual val NumPadDivide = Key(KeyEvent.KEYCODE_NUMPAD_DIVIDE)

        /** Numeric keypad '*' key (for multiplication). */
        @ExperimentalComposeUiApi
        actual val NumPadMultiply = Key(KeyEvent.KEYCODE_NUMPAD_MULTIPLY)

        /** Numeric keypad '-' key (for subtraction). */
        @ExperimentalComposeUiApi
        actual val NumPadSubtract = Key(KeyEvent.KEYCODE_NUMPAD_SUBTRACT)

        /** Numeric keypad '+' key (for addition). */
        @ExperimentalComposeUiApi
        actual val NumPadAdd = Key(KeyEvent.KEYCODE_NUMPAD_ADD)

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        @ExperimentalComposeUiApi
        actual val NumPadDot = Key(KeyEvent.KEYCODE_NUMPAD_DOT)

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        @ExperimentalComposeUiApi
        actual val NumPadComma = Key(KeyEvent.KEYCODE_NUMPAD_COMMA)

        /** Numeric keypad Enter key. */
        @ExperimentalComposeUiApi
        actual val NumPadEnter = Key(KeyEvent.KEYCODE_NUMPAD_ENTER)

        /** Numeric keypad '=' key. */
        @ExperimentalComposeUiApi
        actual val NumPadEquals = Key(KeyEvent.KEYCODE_NUMPAD_EQUALS)

        /** Numeric keypad '(' key. */
        @ExperimentalComposeUiApi
        actual val NumPadLeftParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN)

        /** Numeric keypad ')' key. */
        @ExperimentalComposeUiApi
        actual val NumPadRightParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN)

        /** Play media key. */
        @ExperimentalComposeUiApi
        actual val MediaPlay = Key(KeyEvent.KEYCODE_MEDIA_PLAY)

        /** Pause media key. */
        @ExperimentalComposeUiApi
        actual val MediaPause = Key(KeyEvent.KEYCODE_MEDIA_PAUSE)

        /** Play/Pause media key. */
        @ExperimentalComposeUiApi
        actual val MediaPlayPause = Key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

        /** Stop media key. */
        @ExperimentalComposeUiApi
        actual val MediaStop = Key(KeyEvent.KEYCODE_MEDIA_STOP)

        /** Record media key. */
        @ExperimentalComposeUiApi
        actual val MediaRecord = Key(KeyEvent.KEYCODE_MEDIA_RECORD)

        /** Play Next media key. */
        @ExperimentalComposeUiApi
        actual val MediaNext = Key(KeyEvent.KEYCODE_MEDIA_NEXT)

        /** Play Previous media key. */
        @ExperimentalComposeUiApi
        actual val MediaPrevious = Key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        /** Rewind media key. */
        @ExperimentalComposeUiApi
        actual val MediaRewind = Key(KeyEvent.KEYCODE_MEDIA_REWIND)

        /** Fast Forward media key. */
        @ExperimentalComposeUiApi
        actual val MediaFastForward = Key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        @ExperimentalComposeUiApi
        actual val MediaClose = Key(KeyEvent.KEYCODE_MEDIA_CLOSE)

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        @ExperimentalComposeUiApi
        actual val MediaAudioTrack = Key(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        @ExperimentalComposeUiApi
        actual val MediaEject = Key(KeyEvent.KEYCODE_MEDIA_EJECT)

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        @ExperimentalComposeUiApi
        actual val MediaTopMenu = Key(KeyEvent.KEYCODE_MEDIA_TOP_MENU)

        /** Skip forward media key. */
        @ExperimentalComposeUiApi
        actual val MediaSkipForward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)

        /** Skip backward media key. */
        @ExperimentalComposeUiApi
        actual val MediaSkipBackward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        @ExperimentalComposeUiApi
        actual val MediaStepForward = Key(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD)

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        @ExperimentalComposeUiApi
        actual val MediaStepBackward = Key(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        @ExperimentalComposeUiApi
        actual val MicrophoneMute = Key(KeyEvent.KEYCODE_MUTE)

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press
         * mutes the speaker and the second press restores the original volume.
         */
        @ExperimentalComposeUiApi
        actual val VolumeMute = Key(KeyEvent.KEYCODE_VOLUME_MUTE)

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is
         * currently being viewed.
         */
        @ExperimentalComposeUiApi
        actual val Info = Key(KeyEvent.KEYCODE_INFO)

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        @ExperimentalComposeUiApi
        actual val ChannelUp = Key(KeyEvent.KEYCODE_CHANNEL_UP)

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        @ExperimentalComposeUiApi
        actual val ChannelDown = Key(KeyEvent.KEYCODE_CHANNEL_DOWN)

        /** Zoom in key. */
        @ExperimentalComposeUiApi
        actual val ZoomIn = Key(KeyEvent.KEYCODE_ZOOM_IN)

        /** Zoom out key. */
        @ExperimentalComposeUiApi
        actual val ZoomOut = Key(KeyEvent.KEYCODE_ZOOM_OUT)

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        @ExperimentalComposeUiApi
        actual val Tv = Key(KeyEvent.KEYCODE_TV)

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions.
         * On Android Wear devices, triggers a display offset.
         */
        @ExperimentalComposeUiApi
        actual val Window = Key(KeyEvent.KEYCODE_WINDOW)

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        @ExperimentalComposeUiApi
        actual val Guide = Key(KeyEvent.KEYCODE_GUIDE)

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        @ExperimentalComposeUiApi
        actual val Dvr = Key(KeyEvent.KEYCODE_DVR)

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        @ExperimentalComposeUiApi
        actual val Bookmark = Key(KeyEvent.KEYCODE_BOOKMARK)

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        @ExperimentalComposeUiApi
        actual val Captions = Key(KeyEvent.KEYCODE_CAPTIONS)

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        @ExperimentalComposeUiApi
        actual val Settings = Key(KeyEvent.KEYCODE_SETTINGS)

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        @ExperimentalComposeUiApi
        actual val TvPower = Key(KeyEvent.KEYCODE_TV_POWER)

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        @ExperimentalComposeUiApi
        actual val TvInput = Key(KeyEvent.KEYCODE_TV_INPUT)

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        @ExperimentalComposeUiApi
        actual val SetTopBoxPower = Key(KeyEvent.KEYCODE_STB_POWER)

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        @ExperimentalComposeUiApi
        actual val SetTopBoxInput = Key(KeyEvent.KEYCODE_STB_INPUT)

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        @ExperimentalComposeUiApi
        actual val AvReceiverPower = Key(KeyEvent.KEYCODE_AVR_POWER)

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        @ExperimentalComposeUiApi
        actual val AvReceiverInput = Key(KeyEvent.KEYCODE_AVR_INPUT)

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        actual val ProgramRed = Key(KeyEvent.KEYCODE_PROG_RED)

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        actual val ProgramGreen = Key(KeyEvent.KEYCODE_PROG_GREEN)

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        actual val ProgramYellow = Key(KeyEvent.KEYCODE_PROG_YELLOW)

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        actual val ProgramBlue = Key(KeyEvent.KEYCODE_PROG_BLUE)

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        @ExperimentalComposeUiApi
        actual val AppSwitch = Key(KeyEvent.KEYCODE_APP_SWITCH)

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on
         * a QWERTY keyboard.  On some devices, the same function may be performed by
         * pressing Shift+Space.
         */
        @ExperimentalComposeUiApi
        actual val LanguageSwitch = Key(KeyEvent.KEYCODE_LANGUAGE_SWITCH)

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely
         * in certain settings such as on a crowded train.  On some devices, the key may only
         * operate when long-pressed.
         */
        @ExperimentalComposeUiApi
        actual val MannerMode = Key(KeyEvent.KEYCODE_MANNER_MODE)

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        @ExperimentalComposeUiApi
        actual val Toggle2D3D = Key(KeyEvent.KEYCODE_3D_MODE)

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        @ExperimentalComposeUiApi
        actual val Contacts = Key(KeyEvent.KEYCODE_CONTACTS)

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        @ExperimentalComposeUiApi
        actual val Calendar = Key(KeyEvent.KEYCODE_CALENDAR)

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        @ExperimentalComposeUiApi
        actual val Music = Key(KeyEvent.KEYCODE_MUSIC)

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        @ExperimentalComposeUiApi
        actual val Calculator = Key(KeyEvent.KEYCODE_CALCULATOR)

        /** Japanese full-width / half-width key. */
        @ExperimentalComposeUiApi
        actual val ZenkakuHankaru = Key(KeyEvent.KEYCODE_ZENKAKU_HANKAKU)

        /** Japanese alphanumeric key. */
        @ExperimentalComposeUiApi
        actual val Eisu = Key(KeyEvent.KEYCODE_EISU)

        /** Japanese non-conversion key. */
        @ExperimentalComposeUiApi
        actual val Muhenkan = Key(KeyEvent.KEYCODE_MUHENKAN)

        /** Japanese conversion key. */
        @ExperimentalComposeUiApi
        actual val Henkan = Key(KeyEvent.KEYCODE_HENKAN)

        /** Japanese katakana / hiragana key. */
        @ExperimentalComposeUiApi
        actual val KatakanaHiragana = Key(KeyEvent.KEYCODE_KATAKANA_HIRAGANA)

        /** Japanese Yen key. */
        @ExperimentalComposeUiApi
        actual val Yen = Key(KeyEvent.KEYCODE_YEN)

        /** Japanese Ro key. */
        @ExperimentalComposeUiApi
        actual val Ro = Key(KeyEvent.KEYCODE_RO)

        /** Japanese kana key. */
        @ExperimentalComposeUiApi
        actual val Kana = Key(KeyEvent.KEYCODE_KANA)

        /**
         * Assist key.
         *
         * Launches the global assist activity.  Not delivered to applications.
         */
        @ExperimentalComposeUiApi
        actual val Assist = Key(KeyEvent.KEYCODE_ASSIST)

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        @ExperimentalComposeUiApi
        actual val BrightnessDown = Key(KeyEvent.KEYCODE_BRIGHTNESS_DOWN)

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        @ExperimentalComposeUiApi
        actual val BrightnessUp = Key(KeyEvent.KEYCODE_BRIGHTNESS_UP)

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it
         * has no effect if the device is already asleep.
         */
        @ExperimentalComposeUiApi
        actual val Sleep = Key(KeyEvent.KEYCODE_SLEEP)

        /**
         * Wakeup key.
         *
         * Wakes up the device.  Behaves somewhat like [Power] but it
         * has no effect if the device is already awake.
         */
        @ExperimentalComposeUiApi
        actual val WakeUp = Key(KeyEvent.KEYCODE_WAKEUP)

        /** Put device to sleep unless a wakelock is held.  */
        @ExperimentalComposeUiApi
        actual val SoftSleep = Key(KeyEvent.KEYCODE_SOFT_SLEEP)

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control
         * devices or game controllers, especially if no other input mode is
         * available.
         */
        @ExperimentalComposeUiApi
        actual val Pairing = Key(KeyEvent.KEYCODE_PAIRING)

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        @ExperimentalComposeUiApi
        actual val LastChannel = Key(KeyEvent.KEYCODE_LAST_CHANNEL)

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        @ExperimentalComposeUiApi
        actual val TvDataService = Key(KeyEvent.KEYCODE_TV_DATA_SERVICE)

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        @ExperimentalComposeUiApi
        actual val VoiceAssist = Key(KeyEvent.KEYCODE_VOICE_ASSIST)

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        @ExperimentalComposeUiApi
        actual val TvRadioService = Key(KeyEvent.KEYCODE_TV_RADIO_SERVICE)

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        @ExperimentalComposeUiApi
        actual val TvTeletext = Key(KeyEvent.KEYCODE_TV_TELETEXT)

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned
         * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
         * User Control Code.
         */
        @ExperimentalComposeUiApi
        actual val TvNumberEntry = Key(KeyEvent.KEYCODE_TV_NUMBER_ENTRY)

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        @ExperimentalComposeUiApi
        actual val TvTerrestrialAnalog = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG)

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        @ExperimentalComposeUiApi
        actual val TvTerrestrialDigital = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL)

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        @ExperimentalComposeUiApi
        actual val TvSatellite = Key(KeyEvent.KEYCODE_TV_SATELLITE)

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        @ExperimentalComposeUiApi
        actual val TvSatelliteBs = Key(KeyEvent.KEYCODE_TV_SATELLITE_BS)

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        @ExperimentalComposeUiApi
        actual val TvSatelliteCs = Key(KeyEvent.KEYCODE_TV_SATELLITE_CS)

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        @ExperimentalComposeUiApi
        actual val TvSatelliteService = Key(KeyEvent.KEYCODE_TV_SATELLITE_SERVICE)

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        @ExperimentalComposeUiApi
        actual val TvNetwork = Key(KeyEvent.KEYCODE_TV_NETWORK)

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        @ExperimentalComposeUiApi
        actual val TvAntennaCable = Key(KeyEvent.KEYCODE_TV_ANTENNA_CABLE)

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        @ExperimentalComposeUiApi
        actual val TvInputHdmi1 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_1)

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        @ExperimentalComposeUiApi
        actual val TvInputHdmi2 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_2)

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        @ExperimentalComposeUiApi
        actual val TvInputHdmi3 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_3)

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        @ExperimentalComposeUiApi
        actual val TvInputHdmi4 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_4)

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        @ExperimentalComposeUiApi
        actual val TvInputComposite1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1)

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        @ExperimentalComposeUiApi
        actual val TvInputComposite2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2)

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        @ExperimentalComposeUiApi
        actual val TvInputComponent1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1)

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        @ExperimentalComposeUiApi
        actual val TvInputComponent2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2)

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        @ExperimentalComposeUiApi
        actual val TvInputVga1 = Key(KeyEvent.KEYCODE_TV_INPUT_VGA_1)

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        @ExperimentalComposeUiApi
        actual val TvAudioDescription = Key(KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION)

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        @ExperimentalComposeUiApi
        actual val TvAudioDescriptionMixingVolumeUp = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP)

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        @ExperimentalComposeUiApi
        actual val TvAudioDescriptionMixingVolumeDown = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN)

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        @ExperimentalComposeUiApi
        actual val TvZoomMode = Key(KeyEvent.KEYCODE_TV_ZOOM_MODE)

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        @ExperimentalComposeUiApi
        actual val TvContentsMenu = Key(KeyEvent.KEYCODE_TV_CONTENTS_MENU)

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
         * Menu (0x11) of CEC User Control Code.
         */
        @ExperimentalComposeUiApi
        actual val TvMediaContextMenu = Key(KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU)

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
         * CEC User Control Code.
         */
        @ExperimentalComposeUiApi
        actual val TvTimerProgramming = Key(KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING)

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        @ExperimentalComposeUiApi
        actual val StemPrimary = Key(KeyEvent.KEYCODE_STEM_PRIMARY)

        /** Generic stem key 1 for Wearables. */
        @ExperimentalComposeUiApi
        actual val Stem1 = Key(KeyEvent.KEYCODE_STEM_1)

        /** Generic stem key 2 for Wearables. */
        @ExperimentalComposeUiApi
        actual val Stem2 = Key(KeyEvent.KEYCODE_STEM_2)

        /** Generic stem key 3 for Wearables. */
        @ExperimentalComposeUiApi
        actual val Stem3 = Key(KeyEvent.KEYCODE_STEM_3)

        /** Show all apps. */
        @ExperimentalComposeUiApi
        actual val AllApps = Key(KeyEvent.KEYCODE_ALL_APPS)

        /** Refresh key. */
        @ExperimentalComposeUiApi
        actual val Refresh = Key(KeyEvent.KEYCODE_REFRESH)

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        @ExperimentalComposeUiApi
        actual val ThumbsUp = Key(KeyEvent.KEYCODE_THUMBS_UP)

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        @ExperimentalComposeUiApi
        actual val ThumbsDown = Key(KeyEvent.KEYCODE_THUMBS_DOWN)

        /**
         * Used to switch current [account][android.accounts.Account] that is
         * consuming content. May be consumed by system to set account globally.
         */
        @ExperimentalComposeUiApi
        actual val ProfileSwitch = Key(KeyEvent.KEYCODE_PROFILE_SWITCH)
    }

    actual override fun toString(): String = "Key code: $keyCode"
}

/**
 * The native keycode corresponding to this [Key].
 */
val Key.nativeKeyCode: Int
    get() = unpackInt1(keyCode)

fun Key(nativeKeyCode: Int): Key = Key(packInts(nativeKeyCode, 0))
