/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.DesktopPlatform
import java.awt.event.KeyEvent as AwtKeyEvent

internal actual val platformDefaultKeyMapping: KeyMapping =
    when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> {
            val common = commonKeyMapping(KeyEvent::isMetaPressed)
            object : KeyMapping {
                override fun map(event: KeyEvent): KeyCommand? {
                    return when {
                        event.isShiftPressed && event.isAltPressed ->
                            when (event.key) {
                                MappedKeys.DirectionLeft -> KeyCommand.SELECT_LEFT_WORD
                                MappedKeys.DirectionRight -> KeyCommand.SELECT_RIGHT_WORD
                                MappedKeys.DirectionUp -> KeyCommand.SELECT_PREV_PARAGRAPH
                                MappedKeys.DirectionDown -> KeyCommand.SELECT_NEXT_PARAGRAPH
                                else -> null
                            }
                        event.isShiftPressed && event.isMetaPressed ->
                            when (event.key) {
                                MappedKeys.DirectionLeft -> KeyCommand.SELECT_LINE_LEFT
                                MappedKeys.DirectionRight -> KeyCommand.SELECT_LINE_RIGHT
                                MappedKeys.DirectionUp -> KeyCommand.SELECT_HOME
                                MappedKeys.DirectionDown -> KeyCommand.SELECT_END
                                else -> null
                            }

                        event.isMetaPressed ->
                            when (event.key) {
                                MappedKeys.DirectionLeft -> KeyCommand.LINE_LEFT
                                MappedKeys.DirectionRight -> KeyCommand.LINE_RIGHT
                                MappedKeys.DirectionUp -> KeyCommand.HOME
                                MappedKeys.DirectionDown -> KeyCommand.END
                                MappedKeys.Backspace -> KeyCommand.DELETE_FROM_LINE_START
                                else -> null
                            }

                        // Emacs-like shortcuts
                        event.isCtrlPressed && event.isShiftPressed && event.isAltPressed -> {
                            when (event.key) {
                                MappedKeys.F -> KeyCommand.SELECT_RIGHT_WORD
                                MappedKeys.B -> KeyCommand.SELECT_LEFT_WORD
                                else -> null
                            }
                        }
                        event.isCtrlPressed && event.isAltPressed -> {
                            when (event.key) {
                                MappedKeys.F -> KeyCommand.RIGHT_WORD
                                MappedKeys.B -> KeyCommand.LEFT_WORD
                                else -> null
                            }
                        }
                        event.isCtrlPressed && event.isShiftPressed -> {
                            when (event.key) {
                                MappedKeys.F -> KeyCommand.SELECT_RIGHT_CHAR
                                MappedKeys.B -> KeyCommand.SELECT_LEFT_CHAR
                                MappedKeys.P -> KeyCommand.SELECT_UP
                                MappedKeys.N -> KeyCommand.SELECT_DOWN
                                MappedKeys.A -> KeyCommand.SELECT_LINE_START
                                MappedKeys.E -> KeyCommand.SELECT_LINE_END
                                else -> null
                            }
                        }
                        event.isCtrlPressed -> {
                            when (event.key) {
                                MappedKeys.F -> KeyCommand.LEFT_CHAR
                                MappedKeys.B -> KeyCommand.RIGHT_CHAR
                                MappedKeys.P -> KeyCommand.UP
                                MappedKeys.N -> KeyCommand.DOWN
                                MappedKeys.A -> KeyCommand.LINE_START
                                MappedKeys.E -> KeyCommand.LINE_END
                                MappedKeys.H -> KeyCommand.DELETE_PREV_CHAR
                                MappedKeys.D -> KeyCommand.DELETE_NEXT_CHAR
                                MappedKeys.K -> KeyCommand.DELETE_TO_LINE_END
                                MappedKeys.O -> KeyCommand.NEW_LINE
                                else -> null
                            }
                        }
                        // end of emacs-like shortcuts

                        event.isShiftPressed ->
                            when (event.key) {
                                MappedKeys.MoveHome -> KeyCommand.SELECT_HOME
                                MappedKeys.MoveEnd -> KeyCommand.SELECT_END
                                else -> null
                            }
                        event.isAltPressed ->
                            when (event.key) {
                                MappedKeys.DirectionLeft -> KeyCommand.LEFT_WORD
                                MappedKeys.DirectionRight -> KeyCommand.RIGHT_WORD
                                MappedKeys.DirectionUp -> KeyCommand.PREV_PARAGRAPH
                                MappedKeys.DirectionDown -> KeyCommand.NEXT_PARAGRAPH
                                MappedKeys.Delete -> KeyCommand.DELETE_NEXT_WORD
                                MappedKeys.Backspace -> KeyCommand.DELETE_PREV_WORD
                                else -> null
                            }
                        else -> null
                    } ?: common.map(event)
                }
            }
        }

        else -> defaultKeyMapping
    }

internal actual object MappedKeys {
    actual val A: Key = Key(AwtKeyEvent.VK_A)
    val B: Key = Key(AwtKeyEvent.VK_B)
    val D: Key = Key(AwtKeyEvent.VK_D)
    actual val C: Key = Key(AwtKeyEvent.VK_C)
    val E: Key = Key(AwtKeyEvent.VK_E)
    val F: Key = Key(AwtKeyEvent.VK_F)
    actual val H: Key = Key(AwtKeyEvent.VK_H)
    val K: Key = Key(AwtKeyEvent.VK_K)
    val N: Key = Key(AwtKeyEvent.VK_N)
    val O: Key = Key(AwtKeyEvent.VK_O)
    val P: Key = Key(AwtKeyEvent.VK_P)
    actual val V: Key = Key(AwtKeyEvent.VK_V)
    actual val X: Key = Key(AwtKeyEvent.VK_X)
    actual val Z: Key = Key(AwtKeyEvent.VK_Z)
    actual val Backslash: Key = Key(AwtKeyEvent.VK_BACK_SLASH)
    actual val DirectionLeft: Key = Key(AwtKeyEvent.VK_LEFT)
    actual val DirectionRight: Key = Key(AwtKeyEvent.VK_RIGHT)
    actual val DirectionUp: Key = Key(AwtKeyEvent.VK_UP)
    actual val DirectionDown: Key = Key(AwtKeyEvent.VK_DOWN)
    actual val PageUp: Key = Key(AwtKeyEvent.VK_PAGE_UP)
    actual val PageDown: Key = Key(AwtKeyEvent.VK_PAGE_DOWN)
    actual val MoveHome: Key = Key(AwtKeyEvent.VK_HOME)
    actual val MoveEnd: Key = Key(AwtKeyEvent.VK_END)
    actual val Insert: Key = Key(AwtKeyEvent.VK_INSERT)
    actual val Enter: Key = Key(AwtKeyEvent.VK_ENTER)
    actual val Backspace: Key = Key(AwtKeyEvent.VK_BACK_SPACE)
    actual val Delete: Key = Key(AwtKeyEvent.VK_DELETE)
    actual val Paste: Key = Key(AwtKeyEvent.VK_PASTE)
    actual val Cut: Key = Key(AwtKeyEvent.VK_CUT)
    val Copy: Key = Key(AwtKeyEvent.VK_COPY)
    actual val Tab: Key = Key(AwtKeyEvent.VK_TAB)
}
