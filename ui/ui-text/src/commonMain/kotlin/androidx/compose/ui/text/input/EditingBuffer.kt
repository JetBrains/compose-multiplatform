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

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange

/**
 * The editing buffer
 *
 * This class manages the all editing relate states, editing buffers, selection, styles, etc.
 */
@OptIn(InternalTextApi::class)
class EditingBuffer(
    /**
     * The initial text of this editing buffer
     */
    initialText: String,

    /**
     * The initial selection range of this buffer.
     * If you provide collapsed selection, it is treated as the cursor position. The cursor and
     * selection cannot exists at the same time.
     * The selection must points the valid index of the initialText, otherwise
     * IndexOutOfBoundsException will be thrown.
     */
    initialSelection: TextRange // The initial selection range
) {
    internal companion object {
        const val NOWHERE = -1
    }

    private val gapBuffer = PartialGapBuffer(initialText)

    /**
     * The inclusive selection start offset
     */
    internal var selectionStart = initialSelection.min
        private set

    /**
     * The exclusive selection end offset
     */
    internal var selectionEnd = initialSelection.max
        private set

    /**
     * The inclusive composition start offset
     *
     * If there is no composing text, returns -1
     */
    internal var compositionStart = NOWHERE
        private set

    /**
     * The exclusive composition end offset
     *
     * If there is no composing text, returns -1
     */
    internal var compositionEnd = NOWHERE
        private set

    /**
     * Helper function that returns true if the editing buffer has composition text
     */
    internal fun hasComposition(): Boolean = compositionStart != NOWHERE

    /**
     * Helper accessor for cursor offset
     */
    internal var cursor: Int
        /**
         * Return the cursor offset.
         *
         * Since selection and cursor cannot exist at the same time, return -1 if there is a
         * selection.
         */
        get() = if (selectionStart == selectionEnd) selectionEnd else -1
        /**
         * Set the cursor offset.
         *
         * Since selection and cursor cannot exist at the same time, cancel selection if there is.
         */
        set(cursor) = setSelection(cursor, cursor)

    /**
     * [] operator for the character at the index.
     */
    internal operator fun get(index: Int): Char = gapBuffer[index]

    /**
     * Returns the length of the buffer.
     */
    internal val length: Int get() = gapBuffer.length

    init {
        val start = initialSelection.min
        val end = initialSelection.max
        if (start < 0 || start > initialText.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${initialText.length}"
            )
        }

        if (end < 0 || end > initialText.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${initialText.length}"
            )
        }

        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }
    }

    /**
     * Replace the text and move the cursor to the end of inserted text.
     *
     * This function cancels selection if there.
     *
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer
     * @throws IllegalArgumentException if start is larger than end. (reversed range)
     */
    internal fun replace(start: Int, end: Int, text: String) {

        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}"
            )
        }

        if (end < 0 || end > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}"
            )
        }

        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }

        gapBuffer.replace(start, end, text)

        // On Android, all text modification APIs also provides explicit cursor location. On the
        // hand, desktop application usually doesn't. So, here tentatively move the cursor to the
        // end offset of the editing area for desktop like application. In case of Android,
        // implementation will call setSelection immediately after replace function to update this
        // tentative cursor location.
        selectionStart = start + text.length
        selectionEnd = start + text.length

        // Similarly, if text modification happens, cancel ongoing composition. If caller want to
        // change the composition text, it is caller responsibility to call setComposition again
        // to set composition range after replace function.
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    /**
     * Remove the given range of text.
     *
     * Different from replace method, this doesn't move cursor location to the end of modified text.
     * Instead, preserve the selection with adjusting the deleted text.
     */
    internal fun delete(start: Int, end: Int) {
        val deleteRange = TextRange(start, end)
        if (deleteRange.intersects(TextRange(selectionStart, selectionEnd))) {
            // Currently only target for deleteSurroundingText/deleteSurroundingTextInCodePoints.
            TODO("support deletion within selection range.")
        }

        gapBuffer.replace(start, end, "")
        if (end <= selectionStart) {
            selectionStart -= deleteRange.length
            selectionEnd -= deleteRange.length
        }

        if (!hasComposition()) {
            return
        }

        val compositionRange = TextRange(compositionStart, compositionEnd)

        // Following figure shows the deletion range and composition range.
        // |---| represents a range to be deleted.
        // |===| represents a composition range.
        if (deleteRange.intersects(compositionRange)) {
            if (deleteRange.contains(compositionRange)) {
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :      |-------------|
                //   Composition:          |======|
                //
                // Result:
                //   Buffer     : ABCDETUVWXYZ
                //   Composition:
                compositionStart = NOWHERE
                compositionEnd = NOWHERE
            } else if (compositionRange.contains(deleteRange)) {
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :          |------|
                //   Composition:        |==========|
                //
                // Result:
                //   Buffer     : ABCDEFGHIQRSTUVWXYZ
                //   Composition:        |===|
                compositionEnd -= deleteRange.length
            } else if (deleteRange.contains(compositionStart)) {
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :      |---------|
                //   Composition:            |========|
                //
                // Result:
                //   Buffer     : ABCDEFPQRSTUVWXYZ
                //   Composition:       |=====|
                compositionStart = deleteRange.min
                compositionEnd -= deleteRange.length
            } else { // deleteRange contains compositionEnd
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :         |---------|
                //   Composition:    |=======|
                //
                // Result:
                //   Buffer     : ABCDEFGHSTUVWXYZ
                //   Composition:    |====|
                compositionEnd = deleteRange.min
            }
        } else {
            if (compositionStart <= deleteRange.min) {
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :            |-------|
                //   Composition:  |=======|
                //
                // Result:
                //   Buffer     : ABCDEFGHIJKLTUVWXYZ
                //   Composition:  |=======|
                // do nothing
            } else {
                // Input:
                //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
                //   Delete     :  |-------|
                //   Composition:            |=======|
                //
                // Result:
                //   Buffer     : AJKLMNOPQRSTUVWXYZ
                //   Composition:    |=======|
                compositionStart -= deleteRange.length
                compositionEnd -= deleteRange.length
            }
        }
    }

    /**
     * Mark the specified area of the text as selected text.
     *
     * You can set cursor by specifying the same value to `start` and `end`.
     * The reversed range is not allowed.
     * @param start the inclusive start offset of the selection
     * @param end the exclusive end offset of the selection
     *
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer.
     * @throws IllegalArgumentException if start is larger than end. (reversed range)
     */
    internal fun setSelection(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (end < 0 || end> gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }

        selectionStart = start
        selectionEnd = end
    }

    /**
     * Mark the specified area of the text as composition text.
     *
     * The empty range or reversed range is not allowed.
     * Use clearComposition in case of clearing composition.
     *
     * @param start the inclusive start offset of the composition
     * @param end the exclusive end offset of the composition
     *
     * @throws IndexOutOfBoundsException if start or end offset is ouside of current buffer
     * @throws IllegalArgumentException if start is larger than or equal to end. (reversed or
     *                                  collapsed range)
     */
    internal fun setComposition(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (end < 0 || end> gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (start >= end) {
            throw IllegalArgumentException("Do not set reversed or empty range: $start > $end")
        }

        compositionStart = start
        compositionEnd = end
    }

    /**
     * Removes the ongoing composition text and reset the composition range.
     */
    internal fun cancelComposition() {
        replace(compositionStart, compositionEnd, "")
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    /**
     * Commits the ongoing composition text and reset the composition range.
     */
    internal fun commitComposition() {
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    override fun toString(): String = gapBuffer.toString()
}