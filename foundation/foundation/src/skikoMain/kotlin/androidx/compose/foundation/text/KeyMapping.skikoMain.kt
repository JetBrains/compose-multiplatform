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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

internal expect val MappedKeys.Space: Key
internal expect val MappedKeys.F: Key
internal expect val MappedKeys.B: Key
internal expect val MappedKeys.P: Key
internal expect val MappedKeys.N: Key
internal expect val MappedKeys.E: Key
internal expect val MappedKeys.D: Key
internal expect val MappedKeys.K: Key
internal expect val MappedKeys.O: Key

internal fun createMacosDefaultKeyMapping(): KeyMapping {
    val common = commonKeyMapping(KeyEvent::isMetaPressed)
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when {
                event.isMetaPressed && event.isCtrlPressed ->
                    when (event.key) {
                        MappedKeys.Space -> KeyCommand.CHARACTER_PALETTE
                        else -> null
                    }

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
