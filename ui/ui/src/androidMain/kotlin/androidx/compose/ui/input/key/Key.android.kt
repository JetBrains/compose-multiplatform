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
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN
import androidx.compose.ui.input.key.Key.Companion.Number
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1

/**
 * Actual implementation of [Key] for Android.
 *
 * @param keyCode an integer code representing the key pressed.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
actual inline class Key(val keyCode: Long) {
    actual companion object {
        /** Unknown key. */
        actual val Unknown = Key(KeyEvent.KEYCODE_UNKNOWN)

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom left
         * of the display.
         */
        actual val SoftLeft = Key(KeyEvent.KEYCODE_SOFT_LEFT)

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom right
         * of the display.
         */
        actual val SoftRight = Key(KeyEvent.KEYCODE_SOFT_RIGHT)

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        actual val Home = Key(KeyEvent.KEYCODE_HOME)

        /** Back key. */
        actual val Back = Key(KeyEvent.KEYCODE_BACK)

        /** Help key. */
        actual val Help = Key(KeyEvent.KEYCODE_HELP)

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        actual val NavigatePrevious = Key(KeyEvent.KEYCODE_NAVIGATE_PREVIOUS)

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        actual val NavigateNext = Key(KeyEvent.KEYCODE_NAVIGATE_NEXT)

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        actual val NavigateIn = Key(KeyEvent.KEYCODE_NAVIGATE_IN)

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        actual val NavigateOut = Key(KeyEvent.KEYCODE_NAVIGATE_OUT)

        /** Consumed by the system for navigation up. */
        actual val SystemNavigationUp = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP)

        /** Consumed by the system for navigation down. */
        actual val SystemNavigationDown = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN)

        /** Consumed by the system for navigation left. */
        actual val SystemNavigationLeft = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT)

        /** Consumed by the system for navigation right. */
        actual val SystemNavigationRight = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT)

        /** Call key. */
        actual val Call = Key(KeyEvent.KEYCODE_CALL)

        /** End Call key. */
        actual val EndCall = Key(KeyEvent.KEYCODE_ENDCALL)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionUp = Key(KeyEvent.KEYCODE_DPAD_UP)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionDown = Key(KeyEvent.KEYCODE_DPAD_DOWN)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionLeft = Key(KeyEvent.KEYCODE_DPAD_LEFT)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionRight = Key(KeyEvent.KEYCODE_DPAD_RIGHT)

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionCenter = Key(KeyEvent.KEYCODE_DPAD_CENTER)

        /** Directional Pad Up-Left. */
        actual val DirectionUpLeft = Key(KeyEvent.KEYCODE_DPAD_UP_LEFT)

        /** Directional Pad Down-Left. */
        actual val DirectionDownLeft = Key(KeyEvent.KEYCODE_DPAD_DOWN_LEFT)

        /** Directional Pad Up-Right. */
        actual val DirectionUpRight = Key(KeyEvent.KEYCODE_DPAD_UP_RIGHT)

        /** Directional Pad Down-Right. */
        actual val DirectionDownRight = Key(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT)

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        actual val VolumeUp = Key(KeyEvent.KEYCODE_VOLUME_UP)

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        actual val VolumeDown = Key(KeyEvent.KEYCODE_VOLUME_DOWN)

        /** Power key.  */
        actual val Power = Key(KeyEvent.KEYCODE_POWER)

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        actual val Camera = Key(KeyEvent.KEYCODE_CAMERA)

        /** Clear key. */
        actual val Clear = Key(KeyEvent.KEYCODE_CLEAR)

        /** '0' key. */
        actual val Zero = Key(KeyEvent.KEYCODE_0)

        /** '1' key. */
        actual val One = Key(KeyEvent.KEYCODE_1)

        /** '2' key. */
        actual val Two = Key(KeyEvent.KEYCODE_2)

        /** '3' key. */
        actual val Three = Key(KeyEvent.KEYCODE_3)

        /** '4' key. */
        actual val Four = Key(KeyEvent.KEYCODE_4)

        /** '5' key. */
        actual val Five = Key(KeyEvent.KEYCODE_5)

        /** '6' key. */
        actual val Six = Key(KeyEvent.KEYCODE_6)

        /** '7' key. */
        actual val Seven = Key(KeyEvent.KEYCODE_7)

        /** '8' key. */
        actual val Eight = Key(KeyEvent.KEYCODE_8)

        /** '9' key. */
        actual val Nine = Key(KeyEvent.KEYCODE_9)

        /** '+' key. */
        actual val Plus = Key(KeyEvent.KEYCODE_PLUS)

        /** '-' key. */
        actual val Minus = Key(KeyEvent.KEYCODE_MINUS)

        /** '*' key. */
        actual val Multiply = Key(KeyEvent.KEYCODE_STAR)

        /** '=' key. */
        actual val Equals = Key(KeyEvent.KEYCODE_EQUALS)

        /** '#' key. */
        actual val Pound = Key(KeyEvent.KEYCODE_POUND)

        /** 'A' key. */
        actual val A = Key(KeyEvent.KEYCODE_A)

        /** 'B' key. */
        actual val B = Key(KeyEvent.KEYCODE_B)

        /** 'C' key. */
        actual val C = Key(KeyEvent.KEYCODE_C)

        /** 'D' key. */
        actual val D = Key(KeyEvent.KEYCODE_D)

        /** 'E' key. */
        actual val E = Key(KeyEvent.KEYCODE_E)

        /** 'F' key. */
        actual val F = Key(KeyEvent.KEYCODE_F)

        /** 'G' key. */
        actual val G = Key(KeyEvent.KEYCODE_G)

        /** 'H' key. */
        actual val H = Key(KeyEvent.KEYCODE_H)

        /** 'I' key. */
        actual val I = Key(KeyEvent.KEYCODE_I)

        /** 'J' key. */
        actual val J = Key(KeyEvent.KEYCODE_J)

        /** 'K' key. */
        actual val K = Key(KeyEvent.KEYCODE_K)

        /** 'L' key. */
        actual val L = Key(KeyEvent.KEYCODE_L)

        /** 'M' key. */
        actual val M = Key(KeyEvent.KEYCODE_M)

        /** 'N' key. */
        actual val N = Key(KeyEvent.KEYCODE_N)

        /** 'O' key. */
        actual val O = Key(KeyEvent.KEYCODE_O)

        /** 'P' key. */
        actual val P = Key(KeyEvent.KEYCODE_P)

        /** 'Q' key. */
        actual val Q = Key(KeyEvent.KEYCODE_Q)

        /** 'R' key. */
        actual val R = Key(KeyEvent.KEYCODE_R)

        /** 'S' key. */
        actual val S = Key(KeyEvent.KEYCODE_S)

        /** 'T' key. */
        actual val T = Key(KeyEvent.KEYCODE_T)

        /** 'U' key. */
        actual val U = Key(KeyEvent.KEYCODE_U)

        /** 'V' key. */
        actual val V = Key(KeyEvent.KEYCODE_V)

        /** 'W' key. */
        actual val W = Key(KeyEvent.KEYCODE_W)

        /** 'X' key. */
        actual val X = Key(KeyEvent.KEYCODE_X)

        /** 'Y' key. */
        actual val Y = Key(KeyEvent.KEYCODE_Y)

        /** 'Z' key. */
        actual val Z = Key(KeyEvent.KEYCODE_Z)

        /** ',' key. */
        actual val Comma = Key(KeyEvent.KEYCODE_COMMA)

        /** '.' key. */
        actual val Period = Key(KeyEvent.KEYCODE_PERIOD)

        /** Left Alt modifier key. */
        actual val AltLeft = Key(KeyEvent.KEYCODE_ALT_LEFT)

        /** Right Alt modifier key. */
        actual val AltRight = Key(KeyEvent.KEYCODE_ALT_RIGHT)

        /** Left Shift modifier key. */
        actual val ShiftLeft = Key(KeyEvent.KEYCODE_SHIFT_LEFT)

        /** Right Shift modifier key. */
        actual val ShiftRight = Key(KeyEvent.KEYCODE_SHIFT_RIGHT)

        /** Tab key. */
        actual val Tab = Key(KeyEvent.KEYCODE_TAB)

        /** Space key. */
        actual val Spacebar = Key(KeyEvent.KEYCODE_SPACE)

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        actual val Symbol = Key(KeyEvent.KEYCODE_SYM)

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        actual val Browser = Key(KeyEvent.KEYCODE_EXPLORER)

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        actual val Envelope = Key(KeyEvent.KEYCODE_ENVELOPE)

        /** Enter key. */
        actual val Enter = Key(KeyEvent.KEYCODE_ENTER)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        actual val Backspace = Key(KeyEvent.KEYCODE_DEL)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        actual val Delete = Key(KeyEvent.KEYCODE_FORWARD_DEL)

        /** Escape key. */
        actual val Escape = Key(KeyEvent.KEYCODE_ESCAPE)

        /** Left Control modifier key. */
        actual val CtrlLeft = Key(KeyEvent.KEYCODE_CTRL_LEFT)

        /** Right Control modifier key. */
        actual val CtrlRight = Key(KeyEvent.KEYCODE_CTRL_RIGHT)

        /** Caps Lock key. */
        actual val CapsLock = Key(KeyEvent.KEYCODE_CAPS_LOCK)

        /** Scroll Lock key. */
        actual val ScrollLock = Key(KeyEvent.KEYCODE_SCROLL_LOCK)

        /** Left Meta modifier key. */
        actual val MetaLeft = Key(KeyEvent.KEYCODE_META_LEFT)

        /** Right Meta modifier key. */
        actual val MetaRight = Key(KeyEvent.KEYCODE_META_RIGHT)

        /** Function modifier key. */
        actual val Function = Key(KeyEvent.KEYCODE_FUNCTION)

        /** System Request / Print Screen key. */
        actual val PrintScreen = Key(KeyEvent.KEYCODE_SYSRQ)

        /** Break / Pause key. */
        actual val Break = Key(KeyEvent.KEYCODE_BREAK)

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line
         * or to the top of a list.
         */
        actual val MoveHome = Key(KeyEvent.KEYCODE_MOVE_HOME)

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line
         * or to the bottom of a list.
         */
        actual val MoveEnd = Key(KeyEvent.KEYCODE_MOVE_END)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        actual val Insert = Key(KeyEvent.KEYCODE_INSERT)

        /** Cut key. */
        actual val Cut = Key(KeyEvent.KEYCODE_CUT)

        /** Copy key. */
        actual val Copy = Key(KeyEvent.KEYCODE_COPY)

        /** Paste key. */
        actual val Paste = Key(KeyEvent.KEYCODE_PASTE)

        /** '`' (backtick) key. */
        actual val Grave = Key(KeyEvent.KEYCODE_GRAVE)

        /** '[' key. */
        actual val LeftBracket = Key(KeyEvent.KEYCODE_LEFT_BRACKET)

        /** ']' key. */
        actual val RightBracket = Key(KeyEvent.KEYCODE_RIGHT_BRACKET)

        /** '/' key. */
        actual val Slash = Key(KeyEvent.KEYCODE_SLASH)

        /** '\' key. */
        actual val Backslash = Key(KeyEvent.KEYCODE_BACKSLASH)

        /** ';' key. */
        actual val Semicolon = Key(KeyEvent.KEYCODE_SEMICOLON)

        /** ''' (apostrophe) key. */
        actual val Apostrophe = Key(KeyEvent.KEYCODE_APOSTROPHE)

        /** '@' key. */
        actual val At = Key(KeyEvent.KEYCODE_AT)

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols.
         * This key is not Num Lock; it is more like  [AltLeft].
         */
        actual val Number = Key(KeyEvent.KEYCODE_NUM)

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        actual val HeadsetHook = Key(KeyEvent.KEYCODE_HEADSETHOOK)

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        actual val Focus = Key(KeyEvent.KEYCODE_FOCUS)

        /** Menu key. */
        actual val Menu = Key(KeyEvent.KEYCODE_MENU)

        /** Notification key. */
        actual val Notification = Key(KeyEvent.KEYCODE_NOTIFICATION)

        /** Search key. */
        actual val Search = Key(KeyEvent.KEYCODE_SEARCH)

        /** Page Up key. */
        actual val PageUp = Key(KeyEvent.KEYCODE_PAGE_UP)

        /** Page Down key. */
        actual val PageDown = Key(KeyEvent.KEYCODE_PAGE_DOWN)

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        actual val PictureSymbols = Key(KeyEvent.KEYCODE_PICTSYMBOLS)

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        actual val SwitchCharset = Key(KeyEvent.KEYCODE_SWITCH_CHARSET)

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A
         * or the first button on the bottom row of controller buttons.
         */
        actual val ButtonA = Key(KeyEvent.KEYCODE_BUTTON_A)

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B
         * or the second button on the bottom row of controller buttons.
         */
        actual val ButtonB = Key(KeyEvent.KEYCODE_BUTTON_B)

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C
         * or the third button on the bottom row of controller buttons.
         */
        actual val ButtonC = Key(KeyEvent.KEYCODE_BUTTON_C)

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X
         * or the first button on the upper row of controller buttons.
         */
        actual val ButtonX = Key(KeyEvent.KEYCODE_BUTTON_X)

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y
         * or the second button on the upper row of controller buttons.
         */
        actual val ButtonY = Key(KeyEvent.KEYCODE_BUTTON_Y)

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z
         * or the third button on the upper row of controller buttons.
         */
        actual val ButtonZ = Key(KeyEvent.KEYCODE_BUTTON_Z)

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L)
         * or the top left trigger button.
         */
        actual val ButtonL1 = Key(KeyEvent.KEYCODE_BUTTON_L1)

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R)
         * or the top right trigger button.
         */
        actual val ButtonR1 = Key(KeyEvent.KEYCODE_BUTTON_R1)

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2
         * or the bottom left trigger button.
         */
        actual val ButtonL2 = Key(KeyEvent.KEYCODE_BUTTON_L2)

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2
         * or the bottom right trigger button.
         */
        actual val ButtonR2 = Key(KeyEvent.KEYCODE_BUTTON_R2)

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only)
         * joystick is pressed.
         */
        actual val ButtonThumbLeft = Key(KeyEvent.KEYCODE_BUTTON_THUMBL)

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right
         * joystick is pressed.
         */
        actual val ButtonThumbRight = Key(KeyEvent.KEYCODE_BUTTON_THUMBR)

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        actual val ButtonStart = Key(KeyEvent.KEYCODE_BUTTON_START)

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        actual val ButtonSelect = Key(KeyEvent.KEYCODE_BUTTON_SELECT)

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        actual val ButtonMode = Key(KeyEvent.KEYCODE_BUTTON_MODE)

        /** Generic Game Pad Button #1. */
        actual val Button1 = Key(KeyEvent.KEYCODE_BUTTON_1)

        /** Generic Game Pad Button #2. */
        actual val Button2 = Key(KeyEvent.KEYCODE_BUTTON_2)

        /** Generic Game Pad Button #3. */
        actual val Button3 = Key(KeyEvent.KEYCODE_BUTTON_3)

        /** Generic Game Pad Button #4. */
        actual val Button4 = Key(KeyEvent.KEYCODE_BUTTON_4)

        /** Generic Game Pad Button #5. */
        actual val Button5 = Key(KeyEvent.KEYCODE_BUTTON_5)

        /** Generic Game Pad Button #6. */
        actual val Button6 = Key(KeyEvent.KEYCODE_BUTTON_6)

        /** Generic Game Pad Button #7. */
        actual val Button7 = Key(KeyEvent.KEYCODE_BUTTON_7)

        /** Generic Game Pad Button #8. */
        actual val Button8 = Key(KeyEvent.KEYCODE_BUTTON_8)

        /** Generic Game Pad Button #9. */
        actual val Button9 = Key(KeyEvent.KEYCODE_BUTTON_9)

        /** Generic Game Pad Button #10. */
        actual val Button10 = Key(KeyEvent.KEYCODE_BUTTON_10)

        /** Generic Game Pad Button #11. */
        actual val Button11 = Key(KeyEvent.KEYCODE_BUTTON_11)

        /** Generic Game Pad Button #12. */
        actual val Button12 = Key(KeyEvent.KEYCODE_BUTTON_12)

        /** Generic Game Pad Button #13. */
        actual val Button13 = Key(KeyEvent.KEYCODE_BUTTON_13)

        /** Generic Game Pad Button #14. */
        actual val Button14 = Key(KeyEvent.KEYCODE_BUTTON_14)

        /** Generic Game Pad Button #15. */
        actual val Button15 = Key(KeyEvent.KEYCODE_BUTTON_15)

        /** Generic Game Pad Button #16. */
        actual val Button16 = Key(KeyEvent.KEYCODE_BUTTON_16)

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        actual val Forward = Key(KeyEvent.KEYCODE_FORWARD)

        /** F1 key. */
        actual val F1 = Key(KeyEvent.KEYCODE_F1)

        /** F2 key. */
        actual val F2 = Key(KeyEvent.KEYCODE_F2)

        /** F3 key. */
        actual val F3 = Key(KeyEvent.KEYCODE_F3)

        /** F4 key. */
        actual val F4 = Key(KeyEvent.KEYCODE_F4)

        /** F5 key. */
        actual val F5 = Key(KeyEvent.KEYCODE_F5)

        /** F6 key. */
        actual val F6 = Key(KeyEvent.KEYCODE_F6)

        /** F7 key. */
        actual val F7 = Key(KeyEvent.KEYCODE_F7)

        /** F8 key. */
        actual val F8 = Key(KeyEvent.KEYCODE_F8)

        /** F9 key. */
        actual val F9 = Key(KeyEvent.KEYCODE_F9)

        /** F10 key. */
        actual val F10 = Key(KeyEvent.KEYCODE_F10)

        /** F11 key. */
        actual val F11 = Key(KeyEvent.KEYCODE_F11)

        /** F12 key. */
        actual val F12 = Key(KeyEvent.KEYCODE_F12)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        actual val NumLock = Key(KeyEvent.KEYCODE_NUM_LOCK)

        /** Numeric keypad '0' key. */
        actual val NumPad0 = Key(KeyEvent.KEYCODE_NUMPAD_0)

        /** Numeric keypad '1' key. */
        actual val NumPad1 = Key(KeyEvent.KEYCODE_NUMPAD_1)

        /** Numeric keypad '2' key. */
        actual val NumPad2 = Key(KeyEvent.KEYCODE_NUMPAD_2)

        /** Numeric keypad '3' key. */
        actual val NumPad3 = Key(KeyEvent.KEYCODE_NUMPAD_3)

        /** Numeric keypad '4' key. */
        actual val NumPad4 = Key(KeyEvent.KEYCODE_NUMPAD_4)

        /** Numeric keypad '5' key. */
        actual val NumPad5 = Key(KeyEvent.KEYCODE_NUMPAD_5)

        /** Numeric keypad '6' key. */
        actual val NumPad6 = Key(KeyEvent.KEYCODE_NUMPAD_6)

        /** Numeric keypad '7' key. */
        actual val NumPad7 = Key(KeyEvent.KEYCODE_NUMPAD_7)

        /** Numeric keypad '8' key. */
        actual val NumPad8 = Key(KeyEvent.KEYCODE_NUMPAD_8)

        /** Numeric keypad '9' key. */
        actual val NumPad9 = Key(KeyEvent.KEYCODE_NUMPAD_9)

        /** Numeric keypad '/' key (for division). */
        actual val NumPadDivide = Key(KeyEvent.KEYCODE_NUMPAD_DIVIDE)

        /** Numeric keypad '*' key (for multiplication). */
        actual val NumPadMultiply = Key(KeyEvent.KEYCODE_NUMPAD_MULTIPLY)

        /** Numeric keypad '-' key (for subtraction). */
        actual val NumPadSubtract = Key(KeyEvent.KEYCODE_NUMPAD_SUBTRACT)

        /** Numeric keypad '+' key (for addition). */
        actual val NumPadAdd = Key(KeyEvent.KEYCODE_NUMPAD_ADD)

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        actual val NumPadDot = Key(KeyEvent.KEYCODE_NUMPAD_DOT)

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        actual val NumPadComma = Key(KeyEvent.KEYCODE_NUMPAD_COMMA)

        /** Numeric keypad Enter key. */
        actual val NumPadEnter = Key(KeyEvent.KEYCODE_NUMPAD_ENTER)

        /** Numeric keypad '=' key. */
        actual val NumPadEquals = Key(KeyEvent.KEYCODE_NUMPAD_EQUALS)

        /** Numeric keypad '(' key. */
        actual val NumPadLeftParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN)

        /** Numeric keypad ')' key. */
        actual val NumPadRightParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN)

        /** Play media key. */
        actual val MediaPlay = Key(KeyEvent.KEYCODE_MEDIA_PLAY)

        /** Pause media key. */
        actual val MediaPause = Key(KeyEvent.KEYCODE_MEDIA_PAUSE)

        /** Play/Pause media key. */
        actual val MediaPlayPause = Key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

        /** Stop media key. */
        actual val MediaStop = Key(KeyEvent.KEYCODE_MEDIA_STOP)

        /** Record media key. */
        actual val MediaRecord = Key(KeyEvent.KEYCODE_MEDIA_RECORD)

        /** Play Next media key. */
        actual val MediaNext = Key(KeyEvent.KEYCODE_MEDIA_NEXT)

        /** Play Previous media key. */
        actual val MediaPrevious = Key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        /** Rewind media key. */
        actual val MediaRewind = Key(KeyEvent.KEYCODE_MEDIA_REWIND)

        /** Fast Forward media key. */
        actual val MediaFastForward = Key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        actual val MediaClose = Key(KeyEvent.KEYCODE_MEDIA_CLOSE)

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        actual val MediaAudioTrack = Key(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        actual val MediaEject = Key(KeyEvent.KEYCODE_MEDIA_EJECT)

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        actual val MediaTopMenu = Key(KeyEvent.KEYCODE_MEDIA_TOP_MENU)

        /** Skip forward media key. */
        actual val MediaSkipForward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)

        /** Skip backward media key. */
        actual val MediaSkipBackward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        actual val MediaStepForward = Key(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD)

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        actual val MediaStepBackward = Key(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        actual val MicrophoneMute = Key(KeyEvent.KEYCODE_MUTE)

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press
         * mutes the speaker and the second press restores the original volume.
         */
        actual val VolumeMute = Key(KeyEvent.KEYCODE_VOLUME_MUTE)

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is
         * currently being viewed.
         */
        actual val Info = Key(KeyEvent.KEYCODE_INFO)

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        actual val ChannelUp = Key(KeyEvent.KEYCODE_CHANNEL_UP)

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        actual val ChannelDown = Key(KeyEvent.KEYCODE_CHANNEL_DOWN)

        /** Zoom in key. */
        actual val ZoomIn = Key(KeyEvent.KEYCODE_ZOOM_IN)

        /** Zoom out key. */
        actual val ZoomOut = Key(KeyEvent.KEYCODE_ZOOM_OUT)

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        actual val Tv = Key(KeyEvent.KEYCODE_TV)

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions.
         * On Android Wear devices, triggers a display offset.
         */
        actual val Window = Key(KeyEvent.KEYCODE_WINDOW)

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        actual val Guide = Key(KeyEvent.KEYCODE_GUIDE)

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        actual val Dvr = Key(KeyEvent.KEYCODE_DVR)

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        actual val Bookmark = Key(KeyEvent.KEYCODE_BOOKMARK)

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        actual val Captions = Key(KeyEvent.KEYCODE_CAPTIONS)

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        actual val Settings = Key(KeyEvent.KEYCODE_SETTINGS)

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        actual val TvPower = Key(KeyEvent.KEYCODE_TV_POWER)

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        actual val TvInput = Key(KeyEvent.KEYCODE_TV_INPUT)

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        actual val SetTopBoxPower = Key(KeyEvent.KEYCODE_STB_POWER)

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        actual val SetTopBoxInput = Key(KeyEvent.KEYCODE_STB_INPUT)

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        actual val AvReceiverPower = Key(KeyEvent.KEYCODE_AVR_POWER)

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        actual val AvReceiverInput = Key(KeyEvent.KEYCODE_AVR_INPUT)

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        actual val ProgramRed = Key(KeyEvent.KEYCODE_PROG_RED)

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        actual val ProgramGreen = Key(KeyEvent.KEYCODE_PROG_GREEN)

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        actual val ProgramYellow = Key(KeyEvent.KEYCODE_PROG_YELLOW)

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        actual val ProgramBlue = Key(KeyEvent.KEYCODE_PROG_BLUE)

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        actual val AppSwitch = Key(KeyEvent.KEYCODE_APP_SWITCH)

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on
         * a QWERTY keyboard.  On some devices, the same function may be performed by
         * pressing Shift+Space.
         */
        actual val LanguageSwitch = Key(KeyEvent.KEYCODE_LANGUAGE_SWITCH)

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely
         * in certain settings such as on a crowded train.  On some devices, the key may only
         * operate when long-pressed.
         */
        actual val MannerMode = Key(KeyEvent.KEYCODE_MANNER_MODE)

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        actual val Toggle2D3D = Key(KeyEvent.KEYCODE_3D_MODE)

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        actual val Contacts = Key(KeyEvent.KEYCODE_CONTACTS)

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        actual val Calendar = Key(KeyEvent.KEYCODE_CALENDAR)

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        actual val Music = Key(KeyEvent.KEYCODE_MUSIC)

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        actual val Calculator = Key(KeyEvent.KEYCODE_CALCULATOR)

        /** Japanese full-width / half-width key. */
        actual val ZenkakuHankaru = Key(KeyEvent.KEYCODE_ZENKAKU_HANKAKU)

        /** Japanese alphanumeric key. */
        actual val Eisu = Key(KeyEvent.KEYCODE_EISU)

        /** Japanese non-conversion key. */
        actual val Muhenkan = Key(KeyEvent.KEYCODE_MUHENKAN)

        /** Japanese conversion key. */
        actual val Henkan = Key(KeyEvent.KEYCODE_HENKAN)

        /** Japanese katakana / hiragana key. */
        actual val KatakanaHiragana = Key(KeyEvent.KEYCODE_KATAKANA_HIRAGANA)

        /** Japanese Yen key. */
        actual val Yen = Key(KeyEvent.KEYCODE_YEN)

        /** Japanese Ro key. */
        actual val Ro = Key(KeyEvent.KEYCODE_RO)

        /** Japanese kana key. */
        actual val Kana = Key(KeyEvent.KEYCODE_KANA)

        /**
         * Assist key.
         *
         * Launches the global assist activity.  Not delivered to applications.
         */
        actual val Assist = Key(KeyEvent.KEYCODE_ASSIST)

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        actual val BrightnessDown = Key(KeyEvent.KEYCODE_BRIGHTNESS_DOWN)

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        actual val BrightnessUp = Key(KeyEvent.KEYCODE_BRIGHTNESS_UP)

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it
         * has no effect if the device is already asleep.
         */
        actual val Sleep = Key(KeyEvent.KEYCODE_SLEEP)

        /**
         * Wakeup key.
         *
         * Wakes up the device.  Behaves somewhat like [Power] but it
         * has no effect if the device is already awake.
         */
        actual val WakeUp = Key(KeyEvent.KEYCODE_WAKEUP)

        /** Put device to sleep unless a wakelock is held.  */
        actual val SoftSleep = Key(KeyEvent.KEYCODE_SOFT_SLEEP)

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control
         * devices or game controllers, especially if no other input mode is
         * available.
         */
        actual val Pairing = Key(KeyEvent.KEYCODE_PAIRING)

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        actual val LastChannel = Key(KeyEvent.KEYCODE_LAST_CHANNEL)

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        actual val TvDataService = Key(KeyEvent.KEYCODE_TV_DATA_SERVICE)

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        actual val VoiceAssist = Key(KeyEvent.KEYCODE_VOICE_ASSIST)

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        actual val TvRadioService = Key(KeyEvent.KEYCODE_TV_RADIO_SERVICE)

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        actual val TvTeletext = Key(KeyEvent.KEYCODE_TV_TELETEXT)

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned
         * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
         * User Control Code.
         */
        actual val TvNumberEntry = Key(KeyEvent.KEYCODE_TV_NUMBER_ENTRY)

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        actual val TvTerrestrialAnalog = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG)

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        actual val TvTerrestrialDigital = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL)

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        actual val TvSatellite = Key(KeyEvent.KEYCODE_TV_SATELLITE)

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        actual val TvSatelliteBs = Key(KeyEvent.KEYCODE_TV_SATELLITE_BS)

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        actual val TvSatelliteCs = Key(KeyEvent.KEYCODE_TV_SATELLITE_CS)

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        actual val TvSatelliteService = Key(KeyEvent.KEYCODE_TV_SATELLITE_SERVICE)

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        actual val TvNetwork = Key(KeyEvent.KEYCODE_TV_NETWORK)

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        actual val TvAntennaCable = Key(KeyEvent.KEYCODE_TV_ANTENNA_CABLE)

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        actual val TvInputHdmi1 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_1)

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        actual val TvInputHdmi2 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_2)

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        actual val TvInputHdmi3 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_3)

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        actual val TvInputHdmi4 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_4)

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        actual val TvInputComposite1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1)

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        actual val TvInputComposite2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2)

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        actual val TvInputComponent1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1)

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        actual val TvInputComponent2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2)

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        actual val TvInputVga1 = Key(KeyEvent.KEYCODE_TV_INPUT_VGA_1)

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        actual val TvAudioDescription = Key(KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION)

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        actual val TvAudioDescriptionMixingVolumeUp = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP)

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        actual val TvAudioDescriptionMixingVolumeDown = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN)

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        actual val TvZoomMode = Key(KeyEvent.KEYCODE_TV_ZOOM_MODE)

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        actual val TvContentsMenu = Key(KeyEvent.KEYCODE_TV_CONTENTS_MENU)

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
         * Menu (0x11) of CEC User Control Code.
         */
        actual val TvMediaContextMenu = Key(KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU)

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
         * CEC User Control Code.
         */
        actual val TvTimerProgramming = Key(KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING)

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        actual val StemPrimary = Key(KeyEvent.KEYCODE_STEM_PRIMARY)

        /** Generic stem key 1 for Wearables. */
        actual val Stem1 = Key(KeyEvent.KEYCODE_STEM_1)

        /** Generic stem key 2 for Wearables. */
        actual val Stem2 = Key(KeyEvent.KEYCODE_STEM_2)

        /** Generic stem key 3 for Wearables. */
        actual val Stem3 = Key(KeyEvent.KEYCODE_STEM_3)

        /** Show all apps. */
        actual val AllApps = Key(KeyEvent.KEYCODE_ALL_APPS)

        /** Refresh key. */
        actual val Refresh = Key(KeyEvent.KEYCODE_REFRESH)

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        actual val ThumbsUp = Key(KeyEvent.KEYCODE_THUMBS_UP)

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        actual val ThumbsDown = Key(KeyEvent.KEYCODE_THUMBS_DOWN)

        /**
         * Used to switch current [account][android.accounts.Account] that is
         * consuming content. May be consumed by system to set account globally.
         */
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
