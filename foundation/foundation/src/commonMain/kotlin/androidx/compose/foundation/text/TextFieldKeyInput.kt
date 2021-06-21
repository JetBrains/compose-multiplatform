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

import androidx.compose.foundation.text.selection.TextFieldPreparedSelection
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.TextPreparedSelectionState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

// AWT and Android have similar but different key event models. In android there are two main
// types of events: ACTION_DOWN and ACTION_UP. In AWT there is additional KEY_TYPED which should
// be used to get "typed character". By this simple function we are introducing common
// denominator for both systems: if KeyEvent.isTypedEvent then it's safe to use
// KeyEvent.utf16CodePoint
internal expect val KeyEvent.isTypedEvent: Boolean

/**
 * It handles [KeyEvent]s and either process them as typed events or maps to
 * [KeyCommand] via [KeyMapping]. [KeyCommand] then is executed
 * using utility class [TextFieldPreparedSelection]
 */
internal class TextFieldKeyInput(
    val state: TextFieldState,
    val selectionManager: TextFieldSelectionManager,
    val value: TextFieldValue = TextFieldValue(),
    val editable: Boolean = true,
    val singleLine: Boolean = false,
    val preparedSelectionState: TextPreparedSelectionState,
    val offsetMapping: OffsetMapping = OffsetMapping.Identity,
    val undoManager: UndoManager? = null,
    private val keyMapping: KeyMapping = platformDefaultKeyMapping,
) {
    private fun EditCommand.apply() {
        state.onValueChange(state.processor.apply(listOf(FinishComposingTextCommand(), this)))
    }

    private fun typedCommand(event: KeyEvent): CommitTextCommand? =
        if (event.isTypedEvent) {
            val text = StringBuilder().appendCodePointX(event.utf16CodePoint)
                .toString()
            CommitTextCommand(text, 1)
        } else {
            null
        }

    fun process(event: KeyEvent): Boolean {
        typedCommand(event)?.let {
            return if (editable) {
                it.apply()
                preparedSelectionState.resetCachedX()
                true
            } else {
                false
            }
        }
        if (event.type != KeyEventType.KeyDown) {
            return false
        }
        val command = keyMapping.map(event)
        if (command == null || (command.editsText && !editable)) {
            return false
        }
        var consumed = true
        commandExecutionContext {
            when (command) {
                KeyCommand.COPY -> selectionManager.copy(false)
                KeyCommand.PASTE -> selectionManager.paste()
                KeyCommand.CUT -> selectionManager.cut()
                KeyCommand.LEFT_CHAR -> collapseLeftOr { moveCursorLeft() }
                KeyCommand.RIGHT_CHAR -> collapseRightOr { moveCursorRight() }
                KeyCommand.LEFT_WORD -> moveCursorLeftByWord()
                KeyCommand.RIGHT_WORD -> moveCursorRightByWord()
                KeyCommand.PREV_PARAGRAPH -> moveCursorPrevByParagraph()
                KeyCommand.NEXT_PARAGRAPH -> moveCursorNextByParagraph()
                KeyCommand.UP -> moveCursorUpByLine()
                KeyCommand.DOWN -> moveCursorDownByLine()
                KeyCommand.PAGE_UP -> moveCursorUpByPage()
                KeyCommand.PAGE_DOWN -> moveCursorDownByPage()
                KeyCommand.LINE_START -> moveCursorToLineStart()
                KeyCommand.LINE_END -> moveCursorToLineEnd()
                KeyCommand.LINE_LEFT -> moveCursorToLineLeftSide()
                KeyCommand.LINE_RIGHT -> moveCursorToLineRightSide()
                KeyCommand.HOME -> moveCursorToHome()
                KeyCommand.END -> moveCursorToEnd()
                KeyCommand.DELETE_PREV_CHAR ->
                    deleteIfSelectedOr {
                        moveCursorPrev().selectMovement().deleteSelected()
                    }
                KeyCommand.DELETE_NEXT_CHAR -> {
                    deleteIfSelectedOr {
                        moveCursorNext().selectMovement().deleteSelected()
                    }
                }
                KeyCommand.DELETE_PREV_WORD ->
                    deleteIfSelectedOr {
                        moveCursorPrevByWord().selectMovement().deleteSelected()
                    }
                KeyCommand.DELETE_NEXT_WORD ->
                    deleteIfSelectedOr {
                        moveCursorNextByWord().selectMovement().deleteSelected()
                    }
                KeyCommand.DELETE_FROM_LINE_START ->
                    deleteIfSelectedOr {
                        moveCursorToLineStart().selectMovement().deleteSelected()
                    }
                KeyCommand.DELETE_TO_LINE_END ->
                    deleteIfSelectedOr {
                        moveCursorToLineEnd().selectMovement().deleteSelected()
                    }
                KeyCommand.NEW_LINE ->
                    if (!singleLine) {
                        CommitTextCommand("\n", 1).apply()
                    } else {
                        consumed = false
                    }
                KeyCommand.TAB ->
                    if (!singleLine) {
                        CommitTextCommand("\t", 1).apply()
                    } else {
                        consumed = false
                    }
                KeyCommand.SELECT_ALL -> selectAll()
                KeyCommand.SELECT_LEFT_CHAR -> moveCursorLeft().selectMovement()
                KeyCommand.SELECT_RIGHT_CHAR -> moveCursorRight().selectMovement()
                KeyCommand.SELECT_LEFT_WORD -> moveCursorLeftByWord().selectMovement()
                KeyCommand.SELECT_RIGHT_WORD -> moveCursorRightByWord().selectMovement()
                KeyCommand.SELECT_PREV_PARAGRAPH -> moveCursorPrevByParagraph().selectMovement()
                KeyCommand.SELECT_NEXT_PARAGRAPH -> moveCursorNextByParagraph().selectMovement()
                KeyCommand.SELECT_LINE_START -> moveCursorToLineStart().selectMovement()
                KeyCommand.SELECT_LINE_END -> moveCursorToLineEnd().selectMovement()
                KeyCommand.SELECT_LINE_LEFT -> moveCursorToLineLeftSide().selectMovement()
                KeyCommand.SELECT_LINE_RIGHT -> moveCursorToLineRightSide().selectMovement()
                KeyCommand.SELECT_UP -> moveCursorUpByLine().selectMovement()
                KeyCommand.SELECT_DOWN -> moveCursorDownByLine().selectMovement()
                KeyCommand.SELECT_PAGE_UP -> moveCursorUpByPage().selectMovement()
                KeyCommand.SELECT_PAGE_DOWN -> moveCursorDownByPage().selectMovement()
                KeyCommand.SELECT_HOME -> moveCursorToHome().selectMovement()
                KeyCommand.SELECT_END -> moveCursorToEnd().selectMovement()
                KeyCommand.DESELECT -> deselect()
                KeyCommand.UNDO -> {
                    undoManager?.makeSnapshot(value)
                    undoManager?.undo()?.let { this@TextFieldKeyInput.state.onValueChange(it) }
                }
                KeyCommand.REDO -> {
                    undoManager?.redo()?.let { this@TextFieldKeyInput.state.onValueChange(it) }
                }
            }
        }
        undoManager?.forceNextSnapshot()
        return consumed
    }

    private fun commandExecutionContext(block: TextFieldPreparedSelection.() -> Unit) {
        val preparedSelection = TextFieldPreparedSelection(
            currentValue = value,
            offsetMapping = offsetMapping,
            layoutResultProxy = state.layoutResult,
            state = preparedSelectionState
        )
        block(preparedSelection)
        if (preparedSelection.selection != value.selection ||
            preparedSelection.annotatedString != value.annotatedString
        ) {
            state.onValueChange(preparedSelection.value)
        }
    }
}

@Suppress("ModifierInspectorInfo")
internal fun Modifier.textFieldKeyInput(
    state: TextFieldState,
    manager: TextFieldSelectionManager,
    value: TextFieldValue,
    editable: Boolean,
    singleLine: Boolean,
    offsetMapping: OffsetMapping,
    undoManager: UndoManager
) = composed {
    val preparedSelectionState = remember { TextPreparedSelectionState() }
    val processor = TextFieldKeyInput(
        state = state,
        selectionManager = manager,
        value = value,
        editable = editable,
        singleLine = singleLine,
        offsetMapping = offsetMapping,
        preparedSelectionState = preparedSelectionState,
        undoManager = undoManager
    )
    Modifier.onKeyEvent(processor::process)
}
