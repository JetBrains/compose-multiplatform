/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.findFollowingBreak
import androidx.compose.ui.text.findPrecedingBreak

/**
 * [EditCommand] is a command representation for the platform IME API function calls. The
 * commands from the IME as function calls are translated into command pattern and used by
 * [TextInputService.startInput]. For example, as a result of commit text function call by IME
 * [CommitTextCommand] is created.
 */
interface EditCommand {
    /**
     * Apply the command on the editing buffer.
     */
    fun applyTo(buffer: EditingBuffer)
}

/**
 * Commit final [text] to the text box and set the new cursor position.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)>
 */
class CommitTextCommand(
    /**
     * The text to commit.
     */
    val annotatedString: AnnotatedString,

    /**
     * The cursor position after inserted text.
     */
    val newCursorPosition: Int
) : EditCommand {

    constructor(
        /**
         * The text to commit. We ignore any styles in the original API.
         */
        text: String,
        /**
         * The cursor position after setting composing text.
         */
        newCursorPosition: Int
    ) : this(AnnotatedString(text), newCursorPosition)

    val text: String get() = annotatedString.text

    override fun applyTo(buffer: EditingBuffer) {
        // API description says replace ongoing composition text if there. Then, if there is no
        // composition text, insert text into cursor position or replace selection.
        if (buffer.hasComposition()) {
            buffer.replace(buffer.compositionStart, buffer.compositionEnd, text)
        } else {
            // In this editing buffer, insert into cursor or replace selection are equivalent.
            buffer.replace(buffer.selectionStart, buffer.selectionEnd, text)
        }

        // After replace function is called, the editing buffer places the cursor at the end of the
        // modified range.
        val newCursor = buffer.cursor

        // See above API description for the meaning of newCursorPosition.
        val newCursorInBuffer = if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

        buffer.cursor = newCursorInBuffer.coerceIn(0, buffer.length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommitTextCommand) return false

        if (text != other.text) return false
        if (newCursorPosition != other.newCursorPosition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + newCursorPosition
        return result
    }

    override fun toString(): String {
        return "CommitTextCommand(text='$text', newCursorPosition=$newCursorPosition)"
    }
}

/**
 * Mark a certain region of text as composing text.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingRegion(int,%2520int)>
 */
class SetComposingRegionCommand(
    /**
     * The inclusive start offset of the composing region.
     */
    val start: Int,

    /**
     * The exclusive end offset of the composing region
     */
    val end: Int
) : EditCommand {

    override fun applyTo(buffer: EditingBuffer) {
        // The API description says, different from SetComposingText, SetComposingRegion must
        // preserve the ongoing composition text and set new composition.
        if (buffer.hasComposition()) {
            buffer.commitComposition()
        }

        // Sanitize the input: reverse if reversed, clamped into valid range, ignore empty range.
        val clampedStart = start.coerceIn(0, buffer.length)
        val clampedEnd = end.coerceIn(0, buffer.length)
        if (clampedStart == clampedEnd) {
            // do nothing. empty composition range is not allowed.
        } else if (clampedStart < clampedEnd) {
            buffer.setComposition(clampedStart, clampedEnd)
        } else {
            buffer.setComposition(clampedEnd, clampedStart)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetComposingRegionCommand) return false

        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    override fun toString(): String {
        return "SetComposingRegionCommand(start=$start, end=$end)"
    }
}

/**
 * Replace the currently composing text with the given text, and set the new cursor position. Any
 * composing text set previously will be removed automatically.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingText(java.lang.CharSequence,%2520int)>
 */
class SetComposingTextCommand(
    /**
     * The composing text.
     */
    val annotatedString: AnnotatedString,
    /**
     * The cursor position after setting composing text.
     */
    val newCursorPosition: Int
) : EditCommand {

    constructor(
        /**
         * The composing text.
         */
        text: String,
        /**
         * The cursor position after setting composing text.
         */
        newCursorPosition: Int
    ) : this(AnnotatedString(text), newCursorPosition)

    val text: String get() = annotatedString.text

    override fun applyTo(buffer: EditingBuffer) {
        if (buffer.hasComposition()) {
            // API doc says, if there is ongoing composing text, replace it with new text.
            val compositionStart = buffer.compositionStart
            buffer.replace(buffer.compositionStart, buffer.compositionEnd, text)
            if (text.isNotEmpty()) {
                buffer.setComposition(compositionStart, compositionStart + text.length)
            }
        } else {
            // If there is no composing text, insert composing text into cursor position with
            // removing selected text if any.
            val selectionStart = buffer.selectionStart
            buffer.replace(buffer.selectionStart, buffer.selectionEnd, text)
            if (text.isNotEmpty()) {
                buffer.setComposition(selectionStart, selectionStart + text.length)
            }
        }

        // After replace function is called, the editing buffer places the cursor at the end of the
        // modified range.
        val newCursor = buffer.cursor

        // See above API description for the meaning of newCursorPosition.
        val newCursorInBuffer = if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

        buffer.cursor = newCursorInBuffer.coerceIn(0, buffer.length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetComposingTextCommand) return false

        if (text != other.text) return false
        if (newCursorPosition != other.newCursorPosition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + newCursorPosition
        return result
    }

    override fun toString(): String {
        return "SetComposingTextCommand(text='$text', newCursorPosition=$newCursorPosition)"
    }
}

/**
 * Delete [lengthBeforeCursor] characters of text before the current cursor position, and delete
 * [lengthAfterCursor] characters of text after the current cursor position, excluding the selection.
 *
 * Before and after refer to the order of the characters in the string, not to their visual
 * representation.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingText(int,%2520int)>
 */
class DeleteSurroundingTextCommand(
    /**
     * The number of characters in UTF-16 before the cursor to be deleted.
     * Must be non-negative.
     */
    val lengthBeforeCursor: Int,
    /**
     * The number of characters in UTF-16 after the cursor to be deleted.
     * Must be non-negative.
     */
    val lengthAfterCursor: Int
) : EditCommand {
    init {
        require(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
            "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
                "$lengthBeforeCursor and $lengthAfterCursor respectively."
        }
    }

    override fun applyTo(buffer: EditingBuffer) {
        buffer.delete(
            buffer.selectionEnd,
            minOf(buffer.selectionEnd + lengthAfterCursor, buffer.length)
        )

        buffer.delete(
            maxOf(0, buffer.selectionStart - lengthBeforeCursor),
            buffer.selectionStart
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeleteSurroundingTextCommand) return false

        if (lengthBeforeCursor != other.lengthBeforeCursor) return false
        if (lengthAfterCursor != other.lengthAfterCursor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lengthBeforeCursor
        result = 31 * result + lengthAfterCursor
        return result
    }

    override fun toString(): String {
        return "DeleteSurroundingTextCommand(lengthBeforeCursor=$lengthBeforeCursor, " +
            "lengthAfterCursor=$lengthAfterCursor)"
    }
}

/**
 * A variant of [DeleteSurroundingTextCommand]. The difference is that
 * * The lengths are supplied in code points, not in chars.
 * * This command does nothing if there are one or more invalid surrogate pairs
 * in the requested range.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingTextInCodePoints(int,%2520int)>
 */
class DeleteSurroundingTextInCodePointsCommand(
    /**
     * The number of characters in Unicode code points before the cursor to be deleted.
     * Must be non-negative.
     */
    val lengthBeforeCursor: Int,
    /**
     * The number of characters in Unicode code points after the cursor to be deleted.
     * Must be non-negative.
     */
    val lengthAfterCursor: Int
) : EditCommand {
    init {
        require(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
            "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
                "$lengthBeforeCursor and $lengthAfterCursor respectively."
        }
    }

    override fun applyTo(buffer: EditingBuffer) {
        // Convert code point length into character length. Then call the common logic of the
        // DeleteSurroundingTextEditOp
        var beforeLenInChars = 0
        for (i in 0 until lengthBeforeCursor) {
            beforeLenInChars++
            if (buffer.selectionStart > beforeLenInChars) {
                val lead = buffer[buffer.selectionStart - beforeLenInChars - 1]
                val trail = buffer[buffer.selectionStart - beforeLenInChars]

                if (isSurrogatePair(lead, trail)) {
                    beforeLenInChars++
                }
            }
            if (beforeLenInChars == buffer.selectionStart) break
        }

        var afterLenInChars = 0
        for (i in 0 until lengthAfterCursor) {
            afterLenInChars++
            if (buffer.selectionEnd + afterLenInChars < buffer.length) {
                val lead = buffer[buffer.selectionEnd + afterLenInChars - 1]
                val trail = buffer[buffer.selectionEnd + afterLenInChars]

                if (isSurrogatePair(lead, trail)) {
                    afterLenInChars++
                }
            }
            if (buffer.selectionEnd + afterLenInChars == buffer.length) break
        }

        buffer.delete(buffer.selectionEnd, buffer.selectionEnd + afterLenInChars)
        buffer.delete(buffer.selectionStart - beforeLenInChars, buffer.selectionStart)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeleteSurroundingTextInCodePointsCommand) return false

        if (lengthBeforeCursor != other.lengthBeforeCursor) return false
        if (lengthAfterCursor != other.lengthAfterCursor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lengthBeforeCursor
        result = 31 * result + lengthAfterCursor
        return result
    }

    override fun toString(): String {
        return "DeleteSurroundingTextInCodePointsCommand(lengthBeforeCursor=$lengthBeforeCursor, " +
            "lengthAfterCursor=$lengthAfterCursor)"
    }
}

/**
 * Sets the selection on the text. When [start] and [end] have the same value, it sets the cursor
 * position.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setSelection(int,%2520int)>
 */
class SetSelectionCommand(
    /**
     * The inclusive start offset of the selection region.
     */
    val start: Int,
    /**
     * The exclusive end offset of the selection region.
     */
    val end: Int
) : EditCommand {

    override fun applyTo(buffer: EditingBuffer) {
        // Sanitize the input: reverse if reversed, clamped into valid range.
        val clampedStart = start.coerceIn(0, buffer.length)
        val clampedEnd = end.coerceIn(0, buffer.length)
        if (clampedStart < clampedEnd) {
            buffer.setSelection(clampedStart, clampedEnd)
        } else {
            buffer.setSelection(clampedEnd, clampedStart)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetSelectionCommand) return false

        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    override fun toString(): String {
        return "SetSelectionCommand(start=$start, end=$end)"
    }
}

/**
 * Finishes the composing text that is currently active. This simply leaves the text as-is,
 * removing any special composing styling or other state that was around it. The cursor position
 * remains unchanged.
 *
 * See <https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#finishComposingText()>
 */
class FinishComposingTextCommand : EditCommand {

    override fun applyTo(buffer: EditingBuffer) {
        buffer.commitComposition()
    }

    override fun equals(other: Any?): Boolean = other is FinishComposingTextCommand
    override fun hashCode(): Int = this::class.hashCode()

    override fun toString(): String {
        return "FinishComposingTextCommand()"
    }
}

/**
 * Represents a backspace operation at the cursor position.
 *
 * If there is composition, delete the text in the composition range.
 * If there is no composition but there is selection, delete whole selected range.
 * If there is no composition and selection, perform backspace key event at the cursor position.
 */
class BackspaceCommand : EditCommand {

    override fun applyTo(buffer: EditingBuffer) {
        if (buffer.hasComposition()) {
            buffer.delete(buffer.compositionStart, buffer.compositionEnd)
            return
        }

        if (buffer.cursor == -1) {
            val delStart = buffer.selectionStart
            val delEnd = buffer.selectionEnd
            buffer.cursor = buffer.selectionStart
            buffer.delete(delStart, delEnd)
            return
        }

        if (buffer.cursor == 0) {
            return
        }

        val prevCursorPos = buffer.toString().findPrecedingBreak(buffer.cursor)
        buffer.delete(prevCursorPos, buffer.cursor)
    }

    override fun equals(other: Any?): Boolean = other is BackspaceCommand

    override fun hashCode(): Int = this::class.hashCode()

    override fun toString(): String {
        return "BackspaceCommand()"
    }
}

/**
 * Moves the cursor with [amount] characters.
 *
 * If there is selection, cancel the selection first and move the cursor to the selection start
 * position. Then perform the cursor movement.
 */
class MoveCursorCommand(
    /**
     * The amount of cursor movement.
     *
     * If you want to move backward, pass negative value.
     */
    val amount: Int
) : EditCommand {

    override fun applyTo(buffer: EditingBuffer) {
        if (buffer.cursor == -1) {
            buffer.cursor = buffer.selectionStart
        }

        var newCursor = buffer.selectionStart
        val bufferText = buffer.toString()
        if (amount > 0) {
            for (i in 0 until amount) {
                val next = bufferText.findFollowingBreak(newCursor)
                if (next == -1) break
                newCursor = next
            }
        } else {
            for (i in 0 until -amount) {
                val prev = bufferText.findPrecedingBreak(newCursor)
                if (prev == -1) break
                newCursor = prev
            }
        }

        buffer.cursor = newCursor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoveCursorCommand) return false

        if (amount != other.amount) return false

        return true
    }

    override fun hashCode(): Int {
        return amount
    }

    override fun toString(): String {
        return "MoveCursorCommand(amount=$amount)"
    }
}

/**
 * Deletes all the text in the buffer.
 */
class DeleteAllCommand : EditCommand {
    override fun applyTo(buffer: EditingBuffer) {
        buffer.replace(0, buffer.length, "")
    }

    override fun equals(other: Any?): Boolean = other is DeleteAllCommand

    override fun hashCode(): Int = this::class.hashCode()

    override fun toString(): String {
        return "DeleteAllCommand()"
    }
}

/**
 * Helper function that returns true when [high] is a Unicode high-surrogate code unit and [low]
 * is a Unicode low-surrogate code unit.
 */
private fun isSurrogatePair(high: Char, low: Char): Boolean =
    high.isHighSurrogate() && low.isLowSurrogate()