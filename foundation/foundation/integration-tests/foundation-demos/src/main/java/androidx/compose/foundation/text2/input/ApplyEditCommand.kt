/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input

import java.text.BreakIterator

/**
 * Applies a given [EditCommand] on this [EditingBuffer].
 *
 * Usually calls a dedicated extension function for a given subclass of [EditCommand].
 *
 * @throws IllegalArgumentException if EditCommand is not recognized.
 */
internal fun EditingBuffer.update(editCommand: EditCommand) {
    when (editCommand) {
        is BackspaceCommand -> applyBackspaceCommand()
        is CommitTextCommand -> applyCommitTextCommand(editCommand)
        is DeleteAllCommand -> replace(0, length, "")
        is DeleteSurroundingTextCommand -> applyDeleteSurroundingTextCommand(editCommand)
        is DeleteSurroundingTextInCodePointsCommand ->
            applyDeleteSurroundingTextInCodePointsCommand(editCommand)
        is FinishComposingTextCommand -> commitComposition()
        is MoveCursorCommand -> applyMoveCursorCommand(editCommand)
        is SetComposingRegionCommand -> applySetComposingRegionCommand(editCommand)
        is SetComposingTextCommand -> applySetComposingTextCommand(editCommand)
        is SetSelectionCommand -> applySetSelectionCommand(editCommand)
    }
}

private fun EditingBuffer.applySetSelectionCommand(setSelectionCommand: SetSelectionCommand) {
    // Sanitize the input: reverse if reversed, clamped into valid range.
    val clampedStart = setSelectionCommand.start.coerceIn(0, length)
    val clampedEnd = setSelectionCommand.end.coerceIn(0, length)
    if (clampedStart < clampedEnd) {
        setSelection(clampedStart, clampedEnd)
    } else {
        setSelection(clampedEnd, clampedStart)
    }
}

private fun EditingBuffer.applySetComposingTextCommand(
    setComposingTextCommand: SetComposingTextCommand
) {
    val text = setComposingTextCommand.text
    val newCursorPosition = setComposingTextCommand.newCursorPosition

    if (hasComposition()) {
        // API doc says, if there is ongoing composing text, replace it with new text.
        val compositionStart = compositionStart
        replace(compositionStart, compositionEnd, text)
        if (text.isNotEmpty()) {
            setComposition(compositionStart, compositionStart + text.length)
        }
    } else {
        // If there is no composing text, insert composing text into cursor position with
        // removing selected text if any.
        val selectionStart = selectionStart
        replace(selectionStart, selectionEnd, text)
        if (text.isNotEmpty()) {
            setComposition(selectionStart, selectionStart + text.length)
        }
    }

    // After replace function is called, the editing buffer places the cursor at the end of the
    // modified range.
    val newCursor = cursor

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer = if (newCursorPosition > 0) {
        newCursor + newCursorPosition - 1
    } else {
        newCursor + newCursorPosition - text.length
    }

    cursor = newCursorInBuffer.coerceIn(0, length)
}

private fun EditingBuffer.applySetComposingRegionCommand(
    setComposingRegionCommand: SetComposingRegionCommand
) {
    // The API description says, different from SetComposingText, SetComposingRegion must
    // preserve the ongoing composition text and set new composition.
    if (hasComposition()) {
        commitComposition()
    }

    // Sanitize the input: reverse if reversed, clamped into valid range, ignore empty range.
    val clampedStart = setComposingRegionCommand.start.coerceIn(0, length)
    val clampedEnd = setComposingRegionCommand.end.coerceIn(0, length)
    if (clampedStart == clampedEnd) {
        // do nothing. empty composition range is not allowed.
    } else if (clampedStart < clampedEnd) {
        setComposition(clampedStart, clampedEnd)
    } else {
        setComposition(clampedEnd, clampedStart)
    }
}

private fun EditingBuffer.applyMoveCursorCommand(moveCursorCommand: MoveCursorCommand) {
    if (cursor == -1) {
        cursor = selectionStart
    }

    var newCursor = selectionStart
    val bufferText = toString()
    if (moveCursorCommand.amount > 0) {
        for (i in 0 until moveCursorCommand.amount) {
            val next = bufferText.findFollowingBreak(newCursor)
            if (next == -1) break
            newCursor = next
        }
    } else {
        for (i in 0 until -moveCursorCommand.amount) {
            val prev = bufferText.findPrecedingBreak(newCursor)
            if (prev == -1) break
            newCursor = prev
        }
    }

    cursor = newCursor
}

private fun EditingBuffer.applyDeleteSurroundingTextInCodePointsCommand(
    deleteSurroundingTextInCodePointsCommand: DeleteSurroundingTextInCodePointsCommand
) {
    // Convert code point length into character length. Then call the common logic of the
    // DeleteSurroundingTextEditOp
    var beforeLenInChars = 0
    for (i in 0 until deleteSurroundingTextInCodePointsCommand.lengthBeforeCursor) {
        beforeLenInChars++
        if (selectionStart > beforeLenInChars) {
            val lead = this[selectionStart - beforeLenInChars - 1]
            val trail = this[selectionStart - beforeLenInChars]

            if (isSurrogatePair(lead, trail)) {
                beforeLenInChars++
            }
        }
        if (beforeLenInChars == selectionStart) break
    }

    var afterLenInChars = 0
    for (i in 0 until deleteSurroundingTextInCodePointsCommand.lengthAfterCursor) {
        afterLenInChars++
        if (selectionEnd + afterLenInChars < length) {
            val lead = this[selectionEnd + afterLenInChars - 1]
            val trail = this[selectionEnd + afterLenInChars]

            if (isSurrogatePair(lead, trail)) {
                afterLenInChars++
            }
        }
        if (selectionEnd + afterLenInChars == length) break
    }

    delete(selectionEnd, selectionEnd + afterLenInChars)
    delete(selectionStart - beforeLenInChars, selectionStart)
}

private fun EditingBuffer.applyDeleteSurroundingTextCommand(
    deleteSurroundingTextCommand: DeleteSurroundingTextCommand
) {
    delete(
        selectionEnd,
        minOf(selectionEnd + deleteSurroundingTextCommand.lengthAfterCursor, length)
    )

    delete(
        maxOf(0, selectionStart - deleteSurroundingTextCommand.lengthBeforeCursor),
        selectionStart
    )
}

private fun EditingBuffer.applyBackspaceCommand() {
    if (hasComposition()) {
        delete(compositionStart, compositionEnd)
        return
    }

    if (cursor == -1) {
        val delStart = selectionStart
        val delEnd = selectionEnd
        cursor = selectionStart
        delete(delStart, delEnd)
        return
    }

    if (cursor == 0) {
        return
    }

    val prevCursorPos = toString().findPrecedingBreak(cursor)
    delete(prevCursorPos, cursor)
}

private fun EditingBuffer.applyCommitTextCommand(commitTextCommand: CommitTextCommand) {
    // API description says replace ongoing composition text if there. Then, if there is no
    // composition text, insert text into cursor position or replace selection.
    if (hasComposition()) {
        replace(compositionStart, compositionEnd, commitTextCommand.text)
    } else {
        // In this editing buffer, insert into cursor or replace selection are equivalent.
        replace(selectionStart, selectionEnd, commitTextCommand.text)
    }

    // After replace function is called, the editing buffer places the cursor at the end of the
    // modified range.
    val newCursor = cursor

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer = if (commitTextCommand.newCursorPosition > 0) {
        newCursor + commitTextCommand.newCursorPosition - 1
    } else {
        newCursor + commitTextCommand.newCursorPosition - commitTextCommand.text.length
    }

    cursor = newCursorInBuffer.coerceIn(0, length)
}

/**
 * Helper function that returns true when [high] is a Unicode high-surrogate code unit and [low]
 * is a Unicode low-surrogate code unit.
 */
private fun isSurrogatePair(high: Char, low: Char): Boolean =
    high.isHighSurrogate() && low.isLowSurrogate()

// TODO(halilibo): Remove when migrating back to foundation
private fun String.findPrecedingBreak(index: Int): Int {
    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.preceding(index)
}

// TODO(halilibo): Remove when migrating back to foundation
private fun String.findFollowingBreak(index: Int): Int {
    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.following(index)
}