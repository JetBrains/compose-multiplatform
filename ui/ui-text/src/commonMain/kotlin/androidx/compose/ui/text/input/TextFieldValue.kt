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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.listSaver
import androidx.compose.ui.text.TextRange
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
 * Input service composition is an instance of text produced by IME. An example visual for the
 * composition is that the currently composed word is visually separated from others with
 * underline, or text background. For description of
 * composition please check [W3C IME Composition](https://www.w3.org/TR/ime-api/#ime-composition)
 *
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering IME requests such as getTextBeforeCursor, getSelectedText.
 *
 * @param text the text will be rendered
 * @param selection the selection range. If the selection is collapsed, it represents cursor
 * location. Selection range must be within the bounds of the [text], otherwise an exception will be
 * thrown.
 * @param composition A composition range visible to IME. If null, there is no composition range.
 * Composition range must be within the bounds of the [text], otherwise an exception will be
 * thrown. For description of composition please check [W3C IME Composition](https://www.w3
 * .org/TR/ime-api/#ime-composition).
 */
@Immutable
data class TextFieldValue(
    @Stable
    val text: String = "",
    @Stable
    val selection: TextRange = TextRange.Zero,
    @Stable
    val composition: TextRange? = null
) {
    init {
        // TextRange end is exclusive therefore can be at the end of the text
        require(selection.end <= text.length) {
            "Selection is out of bounds. [selection: $selection, text.length = ${text.length}]"
        }

        // TextRange end is exclusive therefore can be at the end of the text
        composition?.let {
            require(composition.end <= text.length) {
                "Composition is out of bounds. " +
                        "[composition: $selection, text.length = ${text.length}]"
            }
        }
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
 * Helper function for getting text before selection range.
 */
fun TextFieldValue.getTextBeforeSelection(maxChars: Int): String =
    text.substring(max(0, selection.min - maxChars), selection.min)

/**
 * Helper function for getting text after selection range.
 */
fun TextFieldValue.getTextAfterSelection(maxChars: Int): String =
    text.substring(selection.max, min(selection.max + maxChars, text.length))

/**
 * Helper function for getting text currently selected.
 */
fun TextFieldValue.getSelectedText(): String = text.substring(selection)
