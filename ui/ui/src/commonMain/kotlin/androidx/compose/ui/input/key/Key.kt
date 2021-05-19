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
 * Represents keys on a keyboard.
 *
 * @param keyCode a Long value representing the key pressed. Note: This keycode can be used to
 * uniquely identify a hardware key. It is different from the native keycode.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
expect inline class Key(val keyCode: Long) {
    companion object {
        /** Unknown key. */
        val Unknown: Key

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom left
         * of the display.
         */
        val SoftLeft: Key

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function
         * feature key for selecting a software defined function shown on the bottom right
         * of the display.
         */
        val SoftRight: Key

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        val Home: Key

        /** Back key. */
        val Back: Key

        /** Help key. */
        val Help: Key

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        val NavigatePrevious: Key

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        val NavigateNext: Key

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        val NavigateIn: Key

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        val NavigateOut: Key

        /** Consumed by the system for navigation up. */
        val SystemNavigationUp: Key

        /** Consumed by the system for navigation down. */
        val SystemNavigationDown: Key

        /** Consumed by the system for navigation left. */
        val SystemNavigationLeft: Key

        /** Consumed by the system for navigation right. */
        val SystemNavigationRight: Key

        /** Call key. */
        val Call: Key

        /** End Call key. */
        val EndCall: Key

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        val DirectionUp: Key

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        val DirectionDown: Key

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        val DirectionLeft: Key

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        val DirectionRight: Key

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        val DirectionCenter: Key

        /** Directional Pad Up-Left. */
        val DirectionUpLeft: Key

        /** Directional Pad Down-Left. */
        val DirectionDownLeft: Key

        /** Directional Pad Up-Right. */
        val DirectionUpRight: Key

        /** Directional Pad Down-Right. */
        val DirectionDownRight: Key

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        val VolumeUp: Key

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        val VolumeDown: Key

        /** Power key.  */
        val Power: Key

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        val Camera: Key

        /** Clear key. */
        val Clear: Key

        /** '0' key. */
        val Zero: Key

        /** '1' key. */
        val One: Key

        /** '2' key. */
        val Two: Key

        /** '3' key. */
        val Three: Key

        /** '4' key. */
        val Four: Key

        /** '5' key. */
        val Five: Key

        /** '6' key. */
        val Six: Key

        /** '7' key. */
        val Seven: Key

        /** '8' key. */
        val Eight: Key

        /** '9' key. */
        val Nine: Key

        /** '+' key. */
        val Plus: Key

        /** '-' key. */
        val Minus: Key

        /** '*' key. */
        val Multiply: Key

        /** '=' key. */
        val Equals: Key

        /** '#' key. */
        val Pound: Key

        /** 'A' key. */
        val A: Key

        /** 'B' key. */
        val B: Key

        /** 'C' key. */
        val C: Key

        /** 'D' key. */
        val D: Key

        /** 'E' key. */
        val E: Key

        /** 'F' key. */
        val F: Key

        /** 'G' key. */
        val G: Key

        /** 'H' key. */
        val H: Key

        /** 'I' key. */
        val I: Key

        /** 'J' key. */
        val J: Key

        /** 'K' key. */
        val K: Key

        /** 'L' key. */
        val L: Key

        /** 'M' key. */
        val M: Key

        /** 'N' key. */
        val N: Key

        /** 'O' key. */
        val O: Key

        /** 'P' key. */
        val P: Key

        /** 'Q' key. */
        val Q: Key

        /** 'R' key. */
        val R: Key

        /** 'S' key. */
        val S: Key

        /** 'T' key. */
        val T: Key

        /** 'U' key. */
        val U: Key

        /** 'V' key. */
        val V: Key

        /** 'W' key. */
        val W: Key

        /** 'X' key. */
        val X: Key

        /** 'Y' key. */
        val Y: Key

        /** 'Z' key. */
        val Z: Key

        /** ',' key. */
        val Comma: Key

        /** '.' key. */
        val Period: Key

        /** Left Alt modifier key. */
        val AltLeft: Key

        /** Right Alt modifier key. */
        val AltRight: Key

        /** Left Shift modifier key. */
        val ShiftLeft: Key

        /** Right Shift modifier key. */
        val ShiftRight: Key

        /** Tab key. */
        val Tab: Key

        /** Space key. */
        val Spacebar: Key

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        val Symbol: Key

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        val Browser: Key

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        val Envelope: Key

        /** Enter key. */
        val Enter: Key

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        val Backspace: Key

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        val Delete: Key

        /** Escape key. */
        val Escape: Key

        /** Left Control modifier key. */
        val CtrlLeft: Key

        /** Right Control modifier key. */
        val CtrlRight: Key

        /** Caps Lock key. */
        val CapsLock: Key

        /** Scroll Lock key. */
        val ScrollLock: Key

        /** Left Meta modifier key. */
        val MetaLeft: Key

        /** Right Meta modifier key. */
        val MetaRight: Key

        /** Function modifier key. */
        val Function: Key

        /** System Request / Print Screen key. */
        val PrintScreen: Key

        /** Break / Pause key. */
        val Break: Key

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line
         * or to the top of a list.
         */
        val MoveHome: Key

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line
         * or to the bottom of a list.
         */
        val MoveEnd: Key

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        val Insert: Key

        /** Cut key. */
        val Cut: Key

        /** Copy key. */
        val Copy: Key

        /** Paste key. */
        val Paste: Key

        /** '`' (backtick) key. */
        val Grave: Key

        /** '[' key. */
        val LeftBracket: Key

        /** ']' key. */
        val RightBracket: Key

        /** '/' key. */
        val Slash: Key

        /** '\' key. */
        val Backslash: Key

        /** ';' key. */
        val Semicolon: Key

        /** ''' (apostrophe) key. */
        val Apostrophe: Key

        /** '@' key. */
        val At: Key

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols.
         * This key is not Num Lock; it is more like  [AltLeft].
         */
        val Number: Key

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        val HeadsetHook: Key

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        val Focus: Key

        /** Menu key. */
        val Menu: Key

        /** Notification key. */
        val Notification: Key

        /** Search key. */
        val Search: Key

        /** Page Up key. */
        val PageUp: Key

        /** Page Down key. */
        val PageDown: Key

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        val PictureSymbols: Key

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        val SwitchCharset: Key

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A
         * or the first button on the bottom row of controller buttons.
         */
        val ButtonA: Key

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B
         * or the second button on the bottom row of controller buttons.
         */
        val ButtonB: Key

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C
         * or the third button on the bottom row of controller buttons.
         */
        val ButtonC: Key

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X
         * or the first button on the upper row of controller buttons.
         */
        val ButtonX: Key

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y
         * or the second button on the upper row of controller buttons.
         */
        val ButtonY: Key

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z
         * or the third button on the upper row of controller buttons.
         */
        val ButtonZ: Key

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L)
         * or the top left trigger button.
         */
        val ButtonL1: Key

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R)
         * or the top right trigger button.
         */
        val ButtonR1: Key

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2
         * or the bottom left trigger button.
         */
        val ButtonL2: Key

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2
         * or the bottom right trigger button.
         */
        val ButtonR2: Key

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only)
         * joystick is pressed.
         */
        val ButtonThumbLeft: Key

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right
         * joystick is pressed.
         */
        val ButtonThumbRight: Key

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        val ButtonStart: Key

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        val ButtonSelect: Key

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        val ButtonMode: Key

        /** Generic Game Pad Button #1. */
        val Button1: Key

        /** Generic Game Pad Button #2. */
        val Button2: Key

        /** Generic Game Pad Button #3. */
        val Button3: Key

        /** Generic Game Pad Button #4. */
        val Button4: Key

        /** Generic Game Pad Button #5. */
        val Button5: Key

        /** Generic Game Pad Button #6. */
        val Button6: Key

        /** Generic Game Pad Button #7. */
        val Button7: Key

        /** Generic Game Pad Button #8. */
        val Button8: Key

        /** Generic Game Pad Button #9. */
        val Button9: Key

        /** Generic Game Pad Button #10. */
        val Button10: Key

        /** Generic Game Pad Button #11. */
        val Button11: Key

        /** Generic Game Pad Button #12. */
        val Button12: Key

        /** Generic Game Pad Button #13. */
        val Button13: Key

        /** Generic Game Pad Button #14. */
        val Button14: Key

        /** Generic Game Pad Button #15. */
        val Button15: Key

        /** Generic Game Pad Button #16. */
        val Button16: Key

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        val Forward: Key

        /** F1 key. */
        val F1: Key

        /** F2 key. */
        val F2: Key

        /** F3 key. */
        val F3: Key

        /** F4 key. */
        val F4: Key

        /** F5 key. */
        val F5: Key

        /** F6 key. */
        val F6: Key

        /** F7 key. */
        val F7: Key

        /** F8 key. */
        val F8: Key

        /** F9 key. */
        val F9: Key

        /** F10 key. */
        val F10: Key

        /** F11 key. */
        val F11: Key

        /** F12 key. */
        val F12: Key

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        val NumLock: Key

        /** Numeric keypad '0' key. */
        val NumPad0: Key

        /** Numeric keypad '1' key. */
        val NumPad1: Key

        /** Numeric keypad '2' key. */
        val NumPad2: Key

        /** Numeric keypad '3' key. */
        val NumPad3: Key

        /** Numeric keypad '4' key. */
        val NumPad4: Key

        /** Numeric keypad '5' key. */
        val NumPad5: Key

        /** Numeric keypad '6' key. */
        val NumPad6: Key

        /** Numeric keypad '7' key. */
        val NumPad7: Key

        /** Numeric keypad '8' key. */
        val NumPad8: Key

        /** Numeric keypad '9' key. */
        val NumPad9: Key

        /** Numeric keypad '/' key (for division). */
        val NumPadDivide: Key

        /** Numeric keypad '*' key (for multiplication). */
        val NumPadMultiply: Key

        /** Numeric keypad '-' key (for subtraction). */
        val NumPadSubtract: Key

        /** Numeric keypad '+' key (for addition). */
        val NumPadAdd: Key

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        val NumPadDot: Key

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        val NumPadComma: Key

        /** Numeric keypad Enter key. */
        val NumPadEnter: Key

        /** Numeric keypad '=' key. */
        val NumPadEquals: Key

        /** Numeric keypad '(' key. */
        val NumPadLeftParenthesis: Key

        /** Numeric keypad ')' key. */
        val NumPadRightParenthesis: Key

        /** Play media key. */
        val MediaPlay: Key

        /** Pause media key. */
        val MediaPause: Key

        /** Play/Pause media key. */
        val MediaPlayPause: Key

        /** Stop media key. */
        val MediaStop: Key

        /** Record media key. */
        val MediaRecord: Key

        /** Play Next media key. */
        val MediaNext: Key

        /** Play Previous media key. */
        val MediaPrevious: Key

        /** Rewind media key. */
        val MediaRewind: Key

        /** Fast Forward media key. */
        val MediaFastForward: Key

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        val MediaClose: Key

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        val MediaAudioTrack: Key

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        val MediaEject: Key

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        val MediaTopMenu: Key

        /** Skip forward media key. */
        val MediaSkipForward: Key

        /** Skip backward media key. */
        val MediaSkipBackward: Key

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        val MediaStepForward: Key

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        val MediaStepBackward: Key

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        val MicrophoneMute: Key

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press
         * mutes the speaker and the second press restores the original volume.
         */
        val VolumeMute: Key

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is
         * currently being viewed.
         */
        val Info: Key

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        val ChannelUp: Key

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        val ChannelDown: Key

        /** Zoom in key. */
        val ZoomIn: Key

        /** Zoom out key. */
        val ZoomOut: Key

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        val Tv: Key

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions.
         * On Android Wear devices, triggers a display offset.
         */
        val Window: Key

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        val Guide: Key

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        val Dvr: Key

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        val Bookmark: Key

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        val Captions: Key

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        val Settings: Key

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        val TvPower: Key

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        val TvInput: Key

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        val SetTopBoxPower: Key

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        val SetTopBoxInput: Key

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        val AvReceiverPower: Key

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        val AvReceiverInput: Key

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        val ProgramRed: Key

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        val ProgramGreen: Key

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        val ProgramYellow: Key

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        val ProgramBlue: Key

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        val AppSwitch: Key

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on
         * a QWERTY keyboard.  On some devices, the same function may be performed by
         * pressing Shift+Space.
         */
        val LanguageSwitch: Key

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely
         * in certain settings such as on a crowded train.  On some devices, the key may only
         * operate when long-pressed.
         */
        val MannerMode: Key

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        val Toggle2D3D: Key

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        val Contacts: Key

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        val Calendar: Key

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        val Music: Key

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        val Calculator: Key

        /** Japanese full-width / half-width key. */
        val ZenkakuHankaru: Key

        /** Japanese alphanumeric key. */
        val Eisu: Key

        /** Japanese non-conversion key. */
        val Muhenkan: Key

        /** Japanese conversion key. */
        val Henkan: Key

        /** Japanese katakana / hiragana key. */
        val KatakanaHiragana: Key

        /** Japanese Yen key. */
        val Yen: Key

        /** Japanese Ro key. */
        val Ro: Key

        /** Japanese kana key. */
        val Kana: Key

        /**
         * Assist key.
         *
         * Launches the global assist activity.  Not delivered to applications.
         */
        val Assist: Key

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        val BrightnessDown: Key

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        val BrightnessUp: Key

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it
         * has no effect if the device is already asleep.
         */
        val Sleep: Key

        /**
         * Wakeup key.
         *
         * Wakes up the device.  Behaves somewhat like [Power] but it
         * has no effect if the device is already awake.
         */
        val WakeUp: Key

        /** Put device to sleep unless a wakelock is held.  */
        val SoftSleep: Key

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control
         * devices or game controllers, especially if no other input mode is
         * available.
         */
        val Pairing: Key

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        val LastChannel: Key

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        val TvDataService: Key

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        val VoiceAssist: Key

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        val TvRadioService: Key

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        val TvTeletext: Key

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned
         * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
         * User Control Code.
         */
        val TvNumberEntry: Key

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        val TvTerrestrialAnalog: Key

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        val TvTerrestrialDigital: Key

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        val TvSatellite: Key

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        val TvSatelliteBs: Key

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        val TvSatelliteCs: Key

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        val TvSatelliteService: Key

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        val TvNetwork: Key

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        val TvAntennaCable: Key

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        val TvInputHdmi1: Key

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        val TvInputHdmi2: Key

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        val TvInputHdmi3: Key

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        val TvInputHdmi4: Key

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        val TvInputComposite1: Key

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        val TvInputComposite2: Key

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        val TvInputComponent1: Key

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        val TvInputComponent2: Key

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        val TvInputVga1: Key

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        val TvAudioDescription: Key

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        val TvAudioDescriptionMixingVolumeUp: Key

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        val TvAudioDescriptionMixingVolumeDown: Key

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        val TvZoomMode: Key

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        val TvContentsMenu: Key

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
         * Menu (0x11) of CEC User Control Code.
         */
        val TvMediaContextMenu: Key

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
         * CEC User Control Code.
         */
        val TvTimerProgramming: Key

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        val StemPrimary: Key

        /** Generic stem key 1 for Wearables. */
        val Stem1: Key

        /** Generic stem key 2 for Wearables. */
        val Stem2: Key

        /** Generic stem key 3 for Wearables. */
        val Stem3: Key

        /** Show all apps. */
        val AllApps: Key

        /** Refresh key. */
        val Refresh: Key

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        val ThumbsUp: Key

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        val ThumbsDown: Key

        /**
         * Used to switch current [account][android.accounts.Account] that is
         * consuming content. May be consumed by system to set account globally.
         */
        val ProfileSwitch: Key
    }

    override fun toString(): String
}
