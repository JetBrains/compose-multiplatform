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

import androidx.compose.ui.text.AnnotatedString

/**
 * [EditCommand] is a command representation for the platform IME API function calls. The commands
 * from the IME as function calls are translated into command pattern. For example, as a result of
 * commit text function call by IME [CommitTextCommand] is created.
 */
// TODO(halilibo): Consider value class or some other alternatives like passing the buffer into
//  InputConnection, eliminating the need for EditCommand.
internal sealed interface EditCommand

/**
 * Commit final [text] to the text box and set the new cursor position.
 *
 * See [`commitText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)).
 *
 * @param annotatedString The text to commit.
 * @param newCursorPosition The cursor position after inserted text.
 */
internal data class CommitTextCommand(
    val annotatedString: AnnotatedString,
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

    override fun toString(): String {
        return "CommitTextCommand(text='$text', newCursorPosition=$newCursorPosition)"
    }
}

/**
 * Mark a certain region of text as composing text.
 *
 * See [`setComposingRegion`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingRegion(int,%2520int)).
 *
 * @param start The inclusive start offset of the composing region.
 * @param end The exclusive end offset of the composing region
 */
internal data class SetComposingRegionCommand(
    val start: Int,
    val end: Int
) : EditCommand {

    override fun toString(): String {
        return "SetComposingRegionCommand(start=$start, end=$end)"
    }
}

/**
 * Replace the currently composing text with the given text, and set the new cursor position. Any
 * composing text set previously will be removed automatically.
 *
 * See [`setComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingText(java.lang.CharSequence,%2520int)).
 *
 * @param annotatedString The composing text.
 * @param newCursorPosition The cursor position after setting composing text.
 */
internal data class SetComposingTextCommand(
    val annotatedString: AnnotatedString,
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
 * See [`deleteSurroundingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingText(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in UTF-16 before the cursor to be deleted.
 * Must be non-negative.
 * @param lengthAfterCursor The number of characters in UTF-16 after the cursor to be deleted.
 * Must be non-negative.
 */
internal data class DeleteSurroundingTextCommand(
    val lengthBeforeCursor: Int,
    val lengthAfterCursor: Int
) : EditCommand {
    init {
        require(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
            "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
                "$lengthBeforeCursor and $lengthAfterCursor respectively."
        }
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
 * See [`deleteSurroundingTextInCodePoints`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingTextInCodePoints(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in Unicode code points before the cursor to be
 * deleted. Must be non-negative.
 * @param lengthAfterCursor The number of characters in Unicode code points after the cursor to be
 * deleted. Must be non-negative.
 */
internal data class DeleteSurroundingTextInCodePointsCommand(
    val lengthBeforeCursor: Int,
    val lengthAfterCursor: Int
) : EditCommand {
    init {
        require(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
            "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
                "$lengthBeforeCursor and $lengthAfterCursor respectively."
        }
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
 * See [`setSelection`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setSelection(int,%2520int)).
 *
 * @param start The inclusive start offset of the selection region.
 * @param end The exclusive end offset of the selection region.
 */
internal data class SetSelectionCommand(
    val start: Int,
    val end: Int
) : EditCommand {

    override fun toString(): String {
        return "SetSelectionCommand(start=$start, end=$end)"
    }
}

/**
 * Finishes the composing text that is currently active. This simply leaves the text as-is,
 * removing any special composing styling or other state that was around it. The cursor position
 * remains unchanged.
 *
 * See [`finishComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#finishComposingText()).
 */
internal object FinishComposingTextCommand : EditCommand {

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
internal object BackspaceCommand : EditCommand {

    override fun toString(): String {
        return "BackspaceCommand()"
    }
}

/**
 * Moves the cursor with [amount] characters.
 *
 * If there is selection, cancel the selection first and move the cursor to the selection start
 * position. Then perform the cursor movement.
 *
 * @param amount The amount of cursor movement. If you want to move backward, pass negative value.
 */
internal data class MoveCursorCommand(val amount: Int) : EditCommand {
    override fun toString(): String {
        return "MoveCursorCommand(amount=$amount)"
    }
}

/**
 * Deletes all the text in the buffer.
 */
internal object DeleteAllCommand : EditCommand {

    override fun toString(): String {
        return "DeleteAllCommand()"
    }
}