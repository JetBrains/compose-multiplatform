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

/**
 * Copied from [Key] as the constants there are experimental
 */
internal expect object MappedKeys {
    val A: Key
    val C: Key
    val H: Key
    val V: Key
    val X: Key
    val Z: Key
    val Backslash: Key
    val DirectionLeft: Key
    val DirectionRight: Key
    val DirectionUp: Key
    val DirectionDown: Key
    val PageUp: Key
    val PageDown: Key
    val MoveHome: Key
    val MoveEnd: Key
    val Insert: Key
    val Enter: Key
    val Backspace: Key
    val Delete: Key
    val Paste: Key
    val Cut: Key
    val Tab: Key
}

// It's common for all platforms key mapping
internal fun commonKeyMapping(
    shortcutModifier: (KeyEvent) -> Boolean
): KeyMapping {
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when {
                shortcutModifier(event) && event.isShiftPressed ->
                    when (event.key) {
                        MappedKeys.Z -> KeyCommand.REDO
                        else -> null
                    }
                shortcutModifier(event) ->
                    when (event.key) {
                        MappedKeys.C, MappedKeys.Insert -> KeyCommand.COPY
                        MappedKeys.V -> KeyCommand.PASTE
                        MappedKeys.X -> KeyCommand.CUT
                        MappedKeys.A -> KeyCommand.SELECT_ALL
                        MappedKeys.Z -> KeyCommand.UNDO
                        else -> null
                    }
                event.isCtrlPressed -> null
                event.isShiftPressed ->
                    when (event.key) {
                        MappedKeys.DirectionLeft -> KeyCommand.SELECT_LEFT_CHAR
                        MappedKeys.DirectionRight -> KeyCommand.SELECT_RIGHT_CHAR
                        MappedKeys.DirectionUp -> KeyCommand.SELECT_UP
                        MappedKeys.DirectionDown -> KeyCommand.SELECT_DOWN
                        MappedKeys.PageUp -> KeyCommand.SELECT_PAGE_UP
                        MappedKeys.PageDown -> KeyCommand.SELECT_PAGE_DOWN
                        MappedKeys.MoveHome -> KeyCommand.SELECT_LINE_START
                        MappedKeys.MoveEnd -> KeyCommand.SELECT_LINE_END
                        MappedKeys.Insert -> KeyCommand.PASTE
                        else -> null
                    }
                else ->
                    when (event.key) {
                        MappedKeys.DirectionLeft -> KeyCommand.LEFT_CHAR
                        MappedKeys.DirectionRight -> KeyCommand.RIGHT_CHAR
                        MappedKeys.DirectionUp -> KeyCommand.UP
                        MappedKeys.DirectionDown -> KeyCommand.DOWN
                        MappedKeys.PageUp -> KeyCommand.PAGE_UP
                        MappedKeys.PageDown -> KeyCommand.PAGE_DOWN
                        MappedKeys.MoveHome -> KeyCommand.LINE_START
                        MappedKeys.MoveEnd -> KeyCommand.LINE_END
                        MappedKeys.Enter -> KeyCommand.NEW_LINE
                        MappedKeys.Backspace -> KeyCommand.DELETE_PREV_CHAR
                        MappedKeys.Delete -> KeyCommand.DELETE_NEXT_CHAR
                        MappedKeys.Paste -> KeyCommand.PASTE
                        MappedKeys.Cut -> KeyCommand.CUT
                        MappedKeys.Tab -> KeyCommand.TAB
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
                            MappedKeys.DirectionLeft -> KeyCommand.SELECT_LEFT_WORD
                            MappedKeys.DirectionRight -> KeyCommand.SELECT_RIGHT_WORD
                            MappedKeys.DirectionUp -> KeyCommand.SELECT_PREV_PARAGRAPH
                            MappedKeys.DirectionDown -> KeyCommand.SELECT_NEXT_PARAGRAPH
                            else -> null
                        }
                    event.isCtrlPressed ->
                        when (event.key) {
                            MappedKeys.DirectionLeft -> KeyCommand.LEFT_WORD
                            MappedKeys.DirectionRight -> KeyCommand.RIGHT_WORD
                            MappedKeys.DirectionUp -> KeyCommand.PREV_PARAGRAPH
                            MappedKeys.DirectionDown -> KeyCommand.NEXT_PARAGRAPH
                            MappedKeys.H -> KeyCommand.DELETE_PREV_CHAR
                            MappedKeys.Delete -> KeyCommand.DELETE_NEXT_WORD
                            MappedKeys.Backspace -> KeyCommand.DELETE_PREV_WORD
                            MappedKeys.Backslash -> KeyCommand.DESELECT
                            else -> null
                        }
                    event.isShiftPressed ->
                        when (event.key) {
                            MappedKeys.MoveHome -> KeyCommand.SELECT_HOME
                            MappedKeys.MoveEnd -> KeyCommand.SELECT_END
                            else -> null
                        }
                    else -> null
                } ?: common.map(event)
            }
        }
    }