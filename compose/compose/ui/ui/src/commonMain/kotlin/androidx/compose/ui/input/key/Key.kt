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

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Represents keys on a keyboard.
 *
 * @param keyCode a Long value representing the key pressed. Note: This keycode can be used to
 * uniquely identify a hardware key. It is different from the native keycode.
 *
 * @sample androidx.compose.ui.samples.KeyEventIsAltPressedSample
 */
@kotlin.jvm.JvmInline
expect value class Key(val keyCode: Long) {
    companion object {
        /** Unknown key. */
        @ExperimentalComposeUiApi
        val Unknown: Key

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom left
         * of the display.
         */
        @ExperimentalComposeUiApi
        val SoftLeft: Key

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom right
         * of the display.
         */
        @ExperimentalComposeUiApi
        val SoftRight: Key

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        @ExperimentalComposeUiApi
        val Home: Key

        /** Back key. */
        @ExperimentalComposeUiApi
        val Back: Key

        /** Help key. */
        @ExperimentalComposeUiApi
        val Help: Key

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        @ExperimentalComposeUiApi
        val NavigatePrevious: Key

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        @ExperimentalComposeUiApi
        val NavigateNext: Key

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        @ExperimentalComposeUiApi
        val NavigateIn: Key

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        @ExperimentalComposeUiApi
        val NavigateOut: Key

        /** Consumed by the system for navigation up. */
        @ExperimentalComposeUiApi
        val SystemNavigationUp: Key

        /** Consumed by the system for navigation down. */
        @ExperimentalComposeUiApi
        val SystemNavigationDown: Key

        /** Consumed by the system for navigation left. */
        @ExperimentalComposeUiApi
        val SystemNavigationLeft: Key

        /** Consumed by the system for navigation right. */
        @ExperimentalComposeUiApi
        val SystemNavigationRight: Key

        /** Call key. */
        @ExperimentalComposeUiApi
        val Call: Key

        /** End Call key. */
        @ExperimentalComposeUiApi
        val EndCall: Key

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        val DirectionUp: Key

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        val DirectionDown: Key

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        val DirectionLeft: Key

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        val DirectionRight: Key

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        @ExperimentalComposeUiApi
        val DirectionCenter: Key

        /** Directional Pad Up-Left. */
        @ExperimentalComposeUiApi
        val DirectionUpLeft: Key

        /** Directional Pad Down-Left. */
        @ExperimentalComposeUiApi
        val DirectionDownLeft: Key

        /** Directional Pad Up-Right. */
        @ExperimentalComposeUiApi
        val DirectionUpRight: Key

        /** Directional Pad Down-Right. */
        @ExperimentalComposeUiApi
        val DirectionDownRight: Key

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        @ExperimentalComposeUiApi
        val VolumeUp: Key

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        @ExperimentalComposeUiApi
        val VolumeDown: Key

        /** Power key.  */
        @ExperimentalComposeUiApi
        val Power: Key

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        @ExperimentalComposeUiApi
        val Camera: Key

        /** Clear key. */
        @ExperimentalComposeUiApi
        val Clear: Key

        /** '0' key. */
        @ExperimentalComposeUiApi
        val Zero: Key

        /** '1' key. */
        @ExperimentalComposeUiApi
        val One: Key

        /** '2' key. */
        @ExperimentalComposeUiApi
        val Two: Key

        /** '3' key. */
        @ExperimentalComposeUiApi
        val Three: Key

        /** '4' key. */
        @ExperimentalComposeUiApi
        val Four: Key

        /** '5' key. */
        @ExperimentalComposeUiApi
        val Five: Key

        /** '6' key. */
        @ExperimentalComposeUiApi
        val Six: Key

        /** '7' key. */
        @ExperimentalComposeUiApi
        val Seven: Key

        /** '8' key. */
        @ExperimentalComposeUiApi
        val Eight: Key

        /** '9' key. */
        @ExperimentalComposeUiApi
        val Nine: Key

        /** '+' key. */
        @ExperimentalComposeUiApi
        val Plus: Key

        /** '-' key. */
        @ExperimentalComposeUiApi
        val Minus: Key

        /** '*' key. */
        @ExperimentalComposeUiApi
        val Multiply: Key

        /** '=' key. */
        @ExperimentalComposeUiApi
        val Equals: Key

        /** '#' key. */
        @ExperimentalComposeUiApi
        val Pound: Key

        /** 'A' key. */
        @ExperimentalComposeUiApi
        val A: Key

        /** 'B' key. */
        @ExperimentalComposeUiApi
        val B: Key

        /** 'C' key. */
        @ExperimentalComposeUiApi
        val C: Key

        /** 'D' key. */
        @ExperimentalComposeUiApi
        val D: Key

        /** 'E' key. */
        @ExperimentalComposeUiApi
        val E: Key

        /** 'F' key. */
        @ExperimentalComposeUiApi
        val F: Key

        /** 'G' key. */
        @ExperimentalComposeUiApi
        val G: Key

        /** 'H' key. */
        @ExperimentalComposeUiApi
        val H: Key

        /** 'I' key. */
        @ExperimentalComposeUiApi
        val I: Key

        /** 'J' key. */
        @ExperimentalComposeUiApi
        val J: Key

        /** 'K' key. */
        @ExperimentalComposeUiApi
        val K: Key

        /** 'L' key. */
        @ExperimentalComposeUiApi
        val L: Key

        /** 'M' key. */
        @ExperimentalComposeUiApi
        val M: Key

        /** 'N' key. */
        @ExperimentalComposeUiApi
        val N: Key

        /** 'O' key. */
        @ExperimentalComposeUiApi
        val O: Key

        /** 'P' key. */
        @ExperimentalComposeUiApi
        val P: Key

        /** 'Q' key. */
        @ExperimentalComposeUiApi
        val Q: Key

        /** 'R' key. */
        @ExperimentalComposeUiApi
        val R: Key

        /** 'S' key. */
        @ExperimentalComposeUiApi
        val S: Key

        /** 'T' key. */
        @ExperimentalComposeUiApi
        val T: Key

        /** 'U' key. */
        @ExperimentalComposeUiApi
        val U: Key

        /** 'V' key. */
        @ExperimentalComposeUiApi
        val V: Key

        /** 'W' key. */
        @ExperimentalComposeUiApi
        val W: Key

        /** 'X' key. */
        @ExperimentalComposeUiApi
        val X: Key

        /** 'Y' key. */
        @ExperimentalComposeUiApi
        val Y: Key

        /** 'Z' key. */
        @ExperimentalComposeUiApi
        val Z: Key

        /** ',' key. */
        @ExperimentalComposeUiApi
        val Comma: Key

        /** '.' key. */
        @ExperimentalComposeUiApi
        val Period: Key

        /** Left Alt modifier key. */
        @ExperimentalComposeUiApi
        val AltLeft: Key

        /** Right Alt modifier key. */
        @ExperimentalComposeUiApi
        val AltRight: Key

        /** Left Shift modifier key. */
        @ExperimentalComposeUiApi
        val ShiftLeft: Key

        /** Right Shift modifier key. */
        @ExperimentalComposeUiApi
        val ShiftRight: Key

        /** Tab key. */
        @ExperimentalComposeUiApi
        val Tab: Key

        /** Space key. */
        @ExperimentalComposeUiApi
        val Spacebar: Key

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        @ExperimentalComposeUiApi
        val Symbol: Key

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        @ExperimentalComposeUiApi
        val Browser: Key

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        @ExperimentalComposeUiApi
        val Envelope: Key

        /** Enter key. */
        @ExperimentalComposeUiApi
        val Enter: Key

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        @ExperimentalComposeUiApi
        val Backspace: Key

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        @ExperimentalComposeUiApi
        val Delete: Key

        /** Escape key. */
        @ExperimentalComposeUiApi
        val Escape: Key

        /** Left Control modifier key. */
        @ExperimentalComposeUiApi
        val CtrlLeft: Key

        /** Right Control modifier key. */
        @ExperimentalComposeUiApi
        val CtrlRight: Key

        /** Caps Lock key. */
        @ExperimentalComposeUiApi
        val CapsLock: Key

        /** Scroll Lock key. */
        @ExperimentalComposeUiApi
        val ScrollLock: Key

        /** Left Meta modifier key. */
        @ExperimentalComposeUiApi
        val MetaLeft: Key

        /** Right Meta modifier key. */
        @ExperimentalComposeUiApi
        val MetaRight: Key

        /** Function modifier key. */
        @ExperimentalComposeUiApi
        val Function: Key

        /** System Request / Print Screen key. */
        @ExperimentalComposeUiApi
        val PrintScreen: Key

        /** Break / Pause key. */
        @ExperimentalComposeUiApi
        val Break: Key

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line
         * or to the top of a list.
         */
        @ExperimentalComposeUiApi
        val MoveHome: Key

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line
         * or to the bottom of a list.
         */
        @ExperimentalComposeUiApi
        val MoveEnd: Key

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        @ExperimentalComposeUiApi
        val Insert: Key

        /** Cut key. */
        @ExperimentalComposeUiApi
        val Cut: Key

        /** Copy key. */
        @ExperimentalComposeUiApi
        val Copy: Key

        /** Paste key. */
        @ExperimentalComposeUiApi
        val Paste: Key

        /** '`' (backtick) key. */
        @ExperimentalComposeUiApi
        val Grave: Key

        /** '[' key. */
        @ExperimentalComposeUiApi
        val LeftBracket: Key

        /** ']' key. */
        @ExperimentalComposeUiApi
        val RightBracket: Key

        /** '/' key. */
        @ExperimentalComposeUiApi
        val Slash: Key

        /** '\' key. */
        @ExperimentalComposeUiApi
        val Backslash: Key

        /** ';' key. */
        @ExperimentalComposeUiApi
        val Semicolon: Key

        /** ''' (apostrophe) key. */
        @ExperimentalComposeUiApi
        val Apostrophe: Key

        /** '@' key. */
        @ExperimentalComposeUiApi
        val At: Key

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols.
         * This key is not Num Lock; it is more like  [AltLeft].
         */
        @ExperimentalComposeUiApi
        val Number: Key

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        @ExperimentalComposeUiApi
        val HeadsetHook: Key

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        @ExperimentalComposeUiApi
        val Focus: Key

        /** Menu key. */
        @ExperimentalComposeUiApi
        val Menu: Key

        /** Notification key. */
        @ExperimentalComposeUiApi
        val Notification: Key

        /** Search key. */
        @ExperimentalComposeUiApi
        val Search: Key

        /** Page Up key. */
        @ExperimentalComposeUiApi
        val PageUp: Key

        /** Page Down key. */
        @ExperimentalComposeUiApi
        val PageDown: Key

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        @ExperimentalComposeUiApi
        val PictureSymbols: Key

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        @ExperimentalComposeUiApi
        val SwitchCharset: Key

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A
         * or the first button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonA: Key

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B
         * or the second button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonB: Key

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C
         * or the third button on the bottom row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonC: Key

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X
         * or the first button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonX: Key

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y
         * or the second button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonY: Key

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z
         * or the third button on the upper row of controller buttons.
         */
        @ExperimentalComposeUiApi
        val ButtonZ: Key

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L)
         * or the top left trigger button.
         */
        @ExperimentalComposeUiApi
        val ButtonL1: Key

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R)
         * or the top right trigger button.
         */
        @ExperimentalComposeUiApi
        val ButtonR1: Key

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2
         * or the bottom left trigger button.
         */
        @ExperimentalComposeUiApi
        val ButtonL2: Key

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2
         * or the bottom right trigger button.
         */
        @ExperimentalComposeUiApi
        val ButtonR2: Key

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only)
         * joystick is pressed.
         */
        @ExperimentalComposeUiApi
        val ButtonThumbLeft: Key

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right
         * joystick is pressed.
         */
        @ExperimentalComposeUiApi
        val ButtonThumbRight: Key

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        @ExperimentalComposeUiApi
        val ButtonStart: Key

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        @ExperimentalComposeUiApi
        val ButtonSelect: Key

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        @ExperimentalComposeUiApi
        val ButtonMode: Key

        /** Generic Game Pad Button #1. */
        @ExperimentalComposeUiApi
        val Button1: Key

        /** Generic Game Pad Button #2. */
        @ExperimentalComposeUiApi
        val Button2: Key

        /** Generic Game Pad Button #3. */
        @ExperimentalComposeUiApi
        val Button3: Key

        /** Generic Game Pad Button #4. */
        @ExperimentalComposeUiApi
        val Button4: Key

        /** Generic Game Pad Button #5. */
        @ExperimentalComposeUiApi
        val Button5: Key

        /** Generic Game Pad Button #6. */
        @ExperimentalComposeUiApi
        val Button6: Key

        /** Generic Game Pad Button #7. */
        @ExperimentalComposeUiApi
        val Button7: Key

        /** Generic Game Pad Button #8. */
        @ExperimentalComposeUiApi
        val Button8: Key

        /** Generic Game Pad Button #9. */
        @ExperimentalComposeUiApi
        val Button9: Key

        /** Generic Game Pad Button #10. */
        @ExperimentalComposeUiApi
        val Button10: Key

        /** Generic Game Pad Button #11. */
        @ExperimentalComposeUiApi
        val Button11: Key

        /** Generic Game Pad Button #12. */
        @ExperimentalComposeUiApi
        val Button12: Key

        /** Generic Game Pad Button #13. */
        @ExperimentalComposeUiApi
        val Button13: Key

        /** Generic Game Pad Button #14. */
        @ExperimentalComposeUiApi
        val Button14: Key

        /** Generic Game Pad Button #15. */
        @ExperimentalComposeUiApi
        val Button15: Key

        /** Generic Game Pad Button #16. */
        @ExperimentalComposeUiApi
        val Button16: Key

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        @ExperimentalComposeUiApi
        val Forward: Key

        /** F1 key. */
        @ExperimentalComposeUiApi
        val F1: Key

        /** F2 key. */
        @ExperimentalComposeUiApi
        val F2: Key

        /** F3 key. */
        @ExperimentalComposeUiApi
        val F3: Key

        /** F4 key. */
        @ExperimentalComposeUiApi
        val F4: Key

        /** F5 key. */
        @ExperimentalComposeUiApi
        val F5: Key

        /** F6 key. */
        @ExperimentalComposeUiApi
        val F6: Key

        /** F7 key. */
        @ExperimentalComposeUiApi
        val F7: Key

        /** F8 key. */
        @ExperimentalComposeUiApi
        val F8: Key

        /** F9 key. */
        @ExperimentalComposeUiApi
        val F9: Key

        /** F10 key. */
        @ExperimentalComposeUiApi
        val F10: Key

        /** F11 key. */
        @ExperimentalComposeUiApi
        val F11: Key

        /** F12 key. */
        @ExperimentalComposeUiApi
        val F12: Key

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        @ExperimentalComposeUiApi
        val NumLock: Key

        /** Numeric keypad '0' key. */
        @ExperimentalComposeUiApi
        val NumPad0: Key

        /** Numeric keypad '1' key. */
        @ExperimentalComposeUiApi
        val NumPad1: Key

        /** Numeric keypad '2' key. */
        @ExperimentalComposeUiApi
        val NumPad2: Key

        /** Numeric keypad '3' key. */
        @ExperimentalComposeUiApi
        val NumPad3: Key

        /** Numeric keypad '4' key. */
        @ExperimentalComposeUiApi
        val NumPad4: Key

        /** Numeric keypad '5' key. */
        @ExperimentalComposeUiApi
        val NumPad5: Key

        /** Numeric keypad '6' key. */
        @ExperimentalComposeUiApi
        val NumPad6: Key

        /** Numeric keypad '7' key. */
        @ExperimentalComposeUiApi
        val NumPad7: Key

        /** Numeric keypad '8' key. */
        @ExperimentalComposeUiApi
        val NumPad8: Key

        /** Numeric keypad '9' key. */
        @ExperimentalComposeUiApi
        val NumPad9: Key

        /** Numeric keypad '/' key (for division). */
        @ExperimentalComposeUiApi
        val NumPadDivide: Key

        /** Numeric keypad '*' key (for multiplication). */
        @ExperimentalComposeUiApi
        val NumPadMultiply: Key

        /** Numeric keypad '-' key (for subtraction). */
        @ExperimentalComposeUiApi
        val NumPadSubtract: Key

        /** Numeric keypad '+' key (for addition). */
        @ExperimentalComposeUiApi
        val NumPadAdd: Key

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        @ExperimentalComposeUiApi
        val NumPadDot: Key

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        @ExperimentalComposeUiApi
        val NumPadComma: Key

        /** Numeric keypad Enter key. */
        @ExperimentalComposeUiApi
        val NumPadEnter: Key

        /** Numeric keypad '=' key. */
        @ExperimentalComposeUiApi
        val NumPadEquals: Key

        /** Numeric keypad '(' key. */
        @ExperimentalComposeUiApi
        val NumPadLeftParenthesis: Key

        /** Numeric keypad ')' key. */
        @ExperimentalComposeUiApi
        val NumPadRightParenthesis: Key

        /** Play media key. */
        @ExperimentalComposeUiApi
        val MediaPlay: Key

        /** Pause media key. */
        @ExperimentalComposeUiApi
        val MediaPause: Key

        /** Play/Pause media key. */
        @ExperimentalComposeUiApi
        val MediaPlayPause: Key

        /** Stop media key. */
        @ExperimentalComposeUiApi
        val MediaStop: Key

        /** Record media key. */
        @ExperimentalComposeUiApi
        val MediaRecord: Key

        /** Play Next media key. */
        @ExperimentalComposeUiApi
        val MediaNext: Key

        /** Play Previous media key. */
        @ExperimentalComposeUiApi
        val MediaPrevious: Key

        /** Rewind media key. */
        @ExperimentalComposeUiApi
        val MediaRewind: Key

        /** Fast Forward media key. */
        @ExperimentalComposeUiApi
        val MediaFastForward: Key

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        @ExperimentalComposeUiApi
        val MediaClose: Key

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        @ExperimentalComposeUiApi
        val MediaAudioTrack: Key

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        @ExperimentalComposeUiApi
        val MediaEject: Key

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        @ExperimentalComposeUiApi
        val MediaTopMenu: Key

        /** Skip forward media key. */
        @ExperimentalComposeUiApi
        val MediaSkipForward: Key

        /** Skip backward media key. */
        @ExperimentalComposeUiApi
        val MediaSkipBackward: Key

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        @ExperimentalComposeUiApi
        val MediaStepForward: Key

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        @ExperimentalComposeUiApi
        val MediaStepBackward: Key

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        @ExperimentalComposeUiApi
        val MicrophoneMute: Key

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press
         * mutes the speaker and the second press restores the original volume.
         */
        @ExperimentalComposeUiApi
        val VolumeMute: Key

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is
         * currently being viewed.
         */
        @ExperimentalComposeUiApi
        val Info: Key

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        @ExperimentalComposeUiApi
        val ChannelUp: Key

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        @ExperimentalComposeUiApi
        val ChannelDown: Key

        /** Zoom in key. */
        @ExperimentalComposeUiApi
        val ZoomIn: Key

        /** Zoom out key. */
        @ExperimentalComposeUiApi
        val ZoomOut: Key

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        @ExperimentalComposeUiApi
        val Tv: Key

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions.
         * On Android Wear devices, triggers a display offset.
         */
        @ExperimentalComposeUiApi
        val Window: Key

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        @ExperimentalComposeUiApi
        val Guide: Key

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        @ExperimentalComposeUiApi
        val Dvr: Key

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        @ExperimentalComposeUiApi
        val Bookmark: Key

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        @ExperimentalComposeUiApi
        val Captions: Key

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        @ExperimentalComposeUiApi
        val Settings: Key

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        @ExperimentalComposeUiApi
        val TvPower: Key

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        @ExperimentalComposeUiApi
        val TvInput: Key

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        @ExperimentalComposeUiApi
        val SetTopBoxPower: Key

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        @ExperimentalComposeUiApi
        val SetTopBoxInput: Key

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        @ExperimentalComposeUiApi
        val AvReceiverPower: Key

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        @ExperimentalComposeUiApi
        val AvReceiverInput: Key

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        val ProgramRed: Key

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        val ProgramGreen: Key

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        val ProgramYellow: Key

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        @ExperimentalComposeUiApi
        val ProgramBlue: Key

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        @ExperimentalComposeUiApi
        val AppSwitch: Key

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on
         * a QWERTY keyboard.  On some devices, the same function may be performed by
         * pressing Shift+Space.
         */
        @ExperimentalComposeUiApi
        val LanguageSwitch: Key

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely
         * in certain settings such as on a crowded train.  On some devices, the key may only
         * operate when long-pressed.
         */
        @ExperimentalComposeUiApi
        val MannerMode: Key

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        @ExperimentalComposeUiApi
        val Toggle2D3D: Key

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        @ExperimentalComposeUiApi
        val Contacts: Key

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        @ExperimentalComposeUiApi
        val Calendar: Key

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        @ExperimentalComposeUiApi
        val Music: Key

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        @ExperimentalComposeUiApi
        val Calculator: Key

        /** Japanese full-width / half-width key. */
        @ExperimentalComposeUiApi
        val ZenkakuHankaru: Key

        /** Japanese alphanumeric key. */
        @ExperimentalComposeUiApi
        val Eisu: Key

        /** Japanese non-conversion key. */
        @ExperimentalComposeUiApi
        val Muhenkan: Key

        /** Japanese conversion key. */
        @ExperimentalComposeUiApi
        val Henkan: Key

        /** Japanese katakana / hiragana key. */
        @ExperimentalComposeUiApi
        val KatakanaHiragana: Key

        /** Japanese Yen key. */
        @ExperimentalComposeUiApi
        val Yen: Key

        /** Japanese Ro key. */
        @ExperimentalComposeUiApi
        val Ro: Key

        /** Japanese kana key. */
        @ExperimentalComposeUiApi
        val Kana: Key

        /**
         * Assist key.
         *
         * Launches the global assist activity.  Not delivered to applications.
         */
        @ExperimentalComposeUiApi
        val Assist: Key

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        @ExperimentalComposeUiApi
        val BrightnessDown: Key

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        @ExperimentalComposeUiApi
        val BrightnessUp: Key

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it
         * has no effect if the device is already asleep.
         */
        @ExperimentalComposeUiApi
        val Sleep: Key

        /**
         * Wakeup key.
         *
         * Wakes up the device.  Behaves somewhat like [Power] but it
         * has no effect if the device is already awake.
         */
        @ExperimentalComposeUiApi
        val WakeUp: Key

        /** Put device to sleep unless a wakelock is held.  */
        @ExperimentalComposeUiApi
        val SoftSleep: Key

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control
         * devices or game controllers, especially if no other input mode is
         * available.
         */
        @ExperimentalComposeUiApi
        val Pairing: Key

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        @ExperimentalComposeUiApi
        val LastChannel: Key

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        @ExperimentalComposeUiApi
        val TvDataService: Key

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        @ExperimentalComposeUiApi
        val VoiceAssist: Key

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        @ExperimentalComposeUiApi
        val TvRadioService: Key

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        @ExperimentalComposeUiApi
        val TvTeletext: Key

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned
         * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
         * User Control Code.
         */
        @ExperimentalComposeUiApi
        val TvNumberEntry: Key

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        @ExperimentalComposeUiApi
        val TvTerrestrialAnalog: Key

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        @ExperimentalComposeUiApi
        val TvTerrestrialDigital: Key

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        @ExperimentalComposeUiApi
        val TvSatellite: Key

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        @ExperimentalComposeUiApi
        val TvSatelliteBs: Key

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        @ExperimentalComposeUiApi
        val TvSatelliteCs: Key

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        @ExperimentalComposeUiApi
        val TvSatelliteService: Key

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        @ExperimentalComposeUiApi
        val TvNetwork: Key

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        @ExperimentalComposeUiApi
        val TvAntennaCable: Key

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        @ExperimentalComposeUiApi
        val TvInputHdmi1: Key

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        @ExperimentalComposeUiApi
        val TvInputHdmi2: Key

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        @ExperimentalComposeUiApi
        val TvInputHdmi3: Key

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        @ExperimentalComposeUiApi
        val TvInputHdmi4: Key

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        @ExperimentalComposeUiApi
        val TvInputComposite1: Key

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        @ExperimentalComposeUiApi
        val TvInputComposite2: Key

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        @ExperimentalComposeUiApi
        val TvInputComponent1: Key

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        @ExperimentalComposeUiApi
        val TvInputComponent2: Key

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        @ExperimentalComposeUiApi
        val TvInputVga1: Key

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        @ExperimentalComposeUiApi
        val TvAudioDescription: Key

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        @ExperimentalComposeUiApi
        val TvAudioDescriptionMixingVolumeUp: Key

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        @ExperimentalComposeUiApi
        val TvAudioDescriptionMixingVolumeDown: Key

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        @ExperimentalComposeUiApi
        val TvZoomMode: Key

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        @ExperimentalComposeUiApi
        val TvContentsMenu: Key

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
         * Menu (0x11) of CEC User Control Code.
         */
        @ExperimentalComposeUiApi
        val TvMediaContextMenu: Key

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
         * CEC User Control Code.
         */
        @ExperimentalComposeUiApi
        val TvTimerProgramming: Key

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        @ExperimentalComposeUiApi
        val StemPrimary: Key

        /** Generic stem key 1 for Wearables. */
        @ExperimentalComposeUiApi
        val Stem1: Key

        /** Generic stem key 2 for Wearables. */
        @ExperimentalComposeUiApi
        val Stem2: Key

        /** Generic stem key 3 for Wearables. */
        @ExperimentalComposeUiApi
        val Stem3: Key

        /** Show all apps. */
        @ExperimentalComposeUiApi
        val AllApps: Key

        /** Refresh key. */
        @ExperimentalComposeUiApi
        val Refresh: Key

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        @ExperimentalComposeUiApi
        val ThumbsUp: Key

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        @ExperimentalComposeUiApi
        val ThumbsDown: Key

        /**
         * Used to switch current [account][android.accounts.Account] that is
         * consuming content. May be consumed by system to set account globally.
         */
        @ExperimentalComposeUiApi
        val ProfileSwitch: Key
    }

    override fun toString(): String
}
