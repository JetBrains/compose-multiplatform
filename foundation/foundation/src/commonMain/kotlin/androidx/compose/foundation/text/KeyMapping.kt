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
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

internal interface KeyMapping {
    fun map(event: KeyEvent): KeyCommand?
}

// each platform can define its own key mapping, on Android its just defaultKeyMapping, but on
// desktop, the value depends on the current OS
internal expect val platformDefaultKeyMapping: KeyMapping

// It's common for all platforms key mapping
internal fun commonKeyMapping(
    shortcutModifier: (KeyEvent) -> Boolean
): KeyMapping {
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when {
                shortcutModifier(event) && event.isShiftPressed ->
                    when (event.key) {
                        Key.Z -> KeyCommand.REDO
                        else -> null
                    }
                shortcutModifier(event) ->
                    when (event.key) {
                        Key.C, Key.Insert -> KeyCommand.COPY
                        Key.V -> KeyCommand.PASTE
                        Key.X -> KeyCommand.CUT
                        Key.A -> KeyCommand.SELECT_ALL
                        Key.Z -> KeyCommand.UNDO
                        else -> null
                    }
                event.isCtrlPressed -> null
                event.isShiftPressed ->
                    when (event.key) {
                        Key.DirectionLeft -> KeyCommand.SELECT_LEFT_CHAR
                        Key.DirectionRight -> KeyCommand.SELECT_RIGHT_CHAR
                        Key.DirectionUp -> KeyCommand.SELECT_UP
                        Key.DirectionDown -> KeyCommand.SELECT_DOWN
                        Key.PageUp -> KeyCommand.SELECT_PAGE_UP
                        Key.PageDown -> KeyCommand.SELECT_PAGE_DOWN
                        Key.MoveHome -> KeyCommand.SELECT_LINE_START
                        Key.MoveEnd -> KeyCommand.SELECT_LINE_END
                        Key.Insert -> KeyCommand.PASTE
                        else -> null
                    }
                else ->
                    when (event.key) {
                        Key.DirectionLeft -> KeyCommand.LEFT_CHAR
                        Key.DirectionRight -> KeyCommand.RIGHT_CHAR
                        Key.DirectionUp -> KeyCommand.UP
                        Key.DirectionDown -> KeyCommand.DOWN
                        Key.PageUp -> KeyCommand.PAGE_UP
                        Key.PageDown -> KeyCommand.PAGE_DOWN
                        Key.MoveHome -> KeyCommand.LINE_START
                        Key.MoveEnd -> KeyCommand.LINE_END
                        Key.Enter -> KeyCommand.NEW_LINE
                        Key.Backspace -> KeyCommand.DELETE_PREV_CHAR
                        Key.Delete -> KeyCommand.DELETE_NEXT_CHAR
                        Key.Paste -> KeyCommand.PASTE
                        Key.Cut -> KeyCommand.CUT
                        Key.Tab -> KeyCommand.TAB
                        else -> null
                    }
            }
        }
    }
}

// It's "default" or actually "non macOS" key mapping
internal val defaultKeyMapping: KeyMapping =
    commonKeyMapping(KeyEvent::isCtrlPressed).let { common ->
        object : KeyMapping {
            override fun map(event: KeyEvent): KeyCommand? {
                return when {
                    event.isShiftPressed && event.isCtrlPressed ->
                        when (event.key) {
                            Key.DirectionLeft -> KeyCommand.SELECT_LEFT_WORD
                            Key.DirectionRight -> KeyCommand.SELECT_RIGHT_WORD
                            Key.DirectionUp -> KeyCommand.SELECT_PREV_PARAGRAPH
                            Key.DirectionDown -> KeyCommand.SELECT_NEXT_PARAGRAPH
                            else -> null
                        }
                    event.isCtrlPressed ->
                        when (event.key) {
                            Key.DirectionLeft -> KeyCommand.LEFT_WORD
                            Key.DirectionRight -> KeyCommand.RIGHT_WORD
                            Key.DirectionUp -> KeyCommand.PREV_PARAGRAPH
                            Key.DirectionDown -> KeyCommand.NEXT_PARAGRAPH
                            Key.H -> KeyCommand.DELETE_PREV_CHAR
                            Key.Delete -> KeyCommand.DELETE_NEXT_WORD
                            Key.Backspace -> KeyCommand.DELETE_PREV_WORD
                            Key.Backslash -> KeyCommand.DESELECT
                            else -> null
                        }
                    event.isShiftPressed ->
                        when (event.key) {
                            Key.MoveHome -> KeyCommand.SELECT_HOME
                            Key.MoveEnd -> KeyCommand.SELECT_END
                            else -> null
                        }
                    else -> null
                } ?: common.map(event)
            }
        }
    }