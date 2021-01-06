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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.listSaver
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.constrain
import androidx.compose.ui.text.substring
import kotlin.math.max
import kotlin.math.min

/**
 * Stores an input state for IME
 *
 * IME can request editor state with calling getTextBeforeCursor, getSelectedText, etc.
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering these information retrieval requests.
 */
@Immutable
@Deprecated(
    "Please use androidx.compose.ui.text.input.TextFieldValue instead",
    ReplaceWith("TextFieldValue", "androidx.compose.ui.text.input.TextFieldValue")
)
data class EditorValue(
    /**
     * A text visible to IME
     */
    val text: String = "",

    /**
     * A selection range visible to IME.
     * The selection range must be valid range in the given text.
     */
    val selection: TextRange = TextRange.Zero,

    /**
     * A composition range visible to IME.
     * If null, there is no composition range.
     * If non-null, the composition range must be valid range in the given text.
     */
    val composition: TextRange? = null
) {
    /**
     * Helper function for getting text before selection range.
     */
    fun getTextBeforeSelection(maxChars: Int): String =
        text.substring(max(0, selection.min - maxChars), selection.min)

    /**
     * Helper function for getting text after selection range.
     */
    fun getTextAfterSelection(maxChars: Int): String =
        text.substring(selection.max, min(selection.max + maxChars, text.length))

    /**
     * Helper function for getting text currently selected.
     */
    fun getSelectedText(): String = text.substring(selection)
}

/**
 * A class holding information about the editing state.
 *
 * The input service updates text selection, cursor, text and text composition. This class
 * represents those values and it is possible to observe changes to those values in the text
 * editing composables.
 *
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering IME requests such as getTextBeforeCursor, getSelectedText.
 *
 * @param text the text to be rendered.
 * @param selection the selection range. If the selection is collapsed, it represents cursor
 * location. When selection range is out of bounds, it is constrained with the text length.
 * @param composition the composition range, null means empty composition or commit if a
 * composition exists on the text.
 */
@Immutable
class TextFieldValue internal constructor(
    val text: String = "",
    selection: TextRange = TextRange.Zero,
    composition: TextRange? = null
) {
    /**
     * @param text the text to be rendered.
     * @param selection the selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    constructor(
        text: String = "",
        selection: TextRange = TextRange.Zero
    ) : this(text, selection, null)

    /**
     * The selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    @OptIn(InternalTextApi::class)
    val selection = selection.constrain(0, text.length)

    /**
     * Composition range created by  IME. If null, there is no composition range.
     *
     * Composition can be set on the by the system, however it is possible to commit an existing
     * composition using [commitComposition].
     *
     * Input service composition is an instance of text produced by IME. An example visual for the
     * composition is that the currently composed word is visually separated from others with
     * underline, or text background. For description of
     * composition please check [W3C IME Composition](https://www.w3.org/TR/ime-api/#ime-composition)
     */
    @OptIn(InternalTextApi::class)
    val composition: TextRange? = composition?.constrain(0, text.length)

    /**
     * Returns a copy of the TextField.
     */
    fun copy(text: String = this.text, selection: TextRange = this.selection): TextFieldValue {
        return TextFieldValue(text, selection, composition)
    }

    /**
     * Returns a copy of [TextFieldValue] in which [composition] is set to null. When a
     * [TextFieldValue] with null [composition] is passed to a TextField, if there was an
     * active [composition] on the text, the changes will be committed.
     *
     * @see composition
     */
    fun commitComposition() = TextFieldValue(
        text = text,
        selection = selection,
        composition = null
    )

    // auto generated equals method
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldValue) return false
        return text == other.text &&
            selection == other.selection &&
            composition == other.composition
    }

    // auto generated hashCode method
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + selection.hashCode()
        result = 31 * result + (composition?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "TextFieldValue(text='$text', selection=$selection, composition=$composition)"
    }

    companion object {
        /**
         * The default [Saver] implementation for [TextFieldValue].
         */
        val Saver = listSaver<TextFieldValue, Any>(
            save = {
                listOf(it.text, it.selection.start, it.selection.end)
            },
            restore = {
                TextFieldValue(it[0] as String, TextRange(it[1] as Int, it[2] as Int))
            }
        )
    }
}

/**
 * Returns the text before the selection.
 */
fun TextFieldValue.getTextBeforeSelection(maxChars: Int): String =
    text.substring(max(0, selection.min - maxChars), selection.min)

/**
 * Returns the text after the selection.
 */
fun TextFieldValue.getTextAfterSelection(maxChars: Int): String =
    text.substring(selection.max, min(selection.max + maxChars, text.length))

/**
 * Returns the currently selected text.
 */
fun TextFieldValue.getSelectedText(): String = text.substring(selection)

/**
 * Temporary constructor until we figure out how to enforce composition for internal values
 * while enforcing higher level API not to accept composition modification.
 *
 * @suppress
 */
@InternalTextApi
fun buildTextFieldValue(
    text: String,
    selection: TextRange,
    composition: TextRange?
): TextFieldValue = TextFieldValue(text, selection, composition)
