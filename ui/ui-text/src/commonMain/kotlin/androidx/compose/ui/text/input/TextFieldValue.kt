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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.constrain
import androidx.compose.ui.text.emptyAnnotatedString
import kotlin.math.max
import kotlin.math.min

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
 * @param annotatedString the text to be rendered.
 * @param selection the selection range. If the selection is collapsed, it represents cursor
 * location. When selection range is out of bounds, it is constrained with the text length.
 * @param composition the composition range, null means empty composition or commit if a
 * composition exists on the text.
 */
@Immutable
class TextFieldValue internal constructor(
    val annotatedString: AnnotatedString = emptyAnnotatedString(),
    selection: TextRange = TextRange.Zero,
    composition: TextRange? = null
) {
    /**
     * Internal utility constructor.
     */
    internal constructor(
        text: String = "",
        selection: TextRange = TextRange.Zero,
        composition: TextRange? = null
    ) : this(AnnotatedString(text), selection, composition)

    /**
     * @param text the text to be rendered.
     * @param selection the selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    constructor(
        text: String = "",
        selection: TextRange = TextRange.Zero
    ) : this(AnnotatedString(text), selection, null)

    /**
     * TextFieldValue accepts AnnotatedString for providing the API for future, however
     * multi-style text editing is not implemented yet.
     *
     * @param annotatedString the text to be rendered.
     * @param selection the selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    constructor(
        annotatedString: AnnotatedString,
        selection: TextRange = TextRange.Zero
    ) : this(annotatedString, selection, null)

    val text: String get() = annotatedString.text

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
     * Returns a copy of the TextFieldValue.
     */
    fun copy(
        annotatedString: AnnotatedString = this.annotatedString,
        selection: TextRange = this.selection
    ): TextFieldValue {
        return TextFieldValue(annotatedString, selection, composition)
    }

    /**
     * Returns a copy of the TextFieldValue.
     */
    fun copy(
        text: String,
        selection: TextRange = this.selection
    ): TextFieldValue {
        return TextFieldValue(AnnotatedString(text), selection, composition)
    }

    /**
     * Returns a copy of the TextFieldValue.
     */
    fun copy(
        selection: TextRange = this.selection
    ): TextFieldValue {
        return copy(this.annotatedString, selection)
    }

    /**
     * Returns a copy of [TextFieldValue] in which [composition] is set to null. When a
     * [TextFieldValue] with null [composition] is passed to a TextField, if there was an
     * active [composition] on the text, the changes will be committed.
     *
     * @see composition
     */
    fun commitComposition() = TextFieldValue(
        annotatedString = annotatedString,
        selection = selection,
        composition = null
    )

    // auto generated equals method
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldValue) return false
        return annotatedString == other.annotatedString &&
            selection == other.selection &&
            composition == other.composition
    }

    // auto generated hashCode method
    override fun hashCode(): Int {
        var result = annotatedString.hashCode()
        result = 31 * result + selection.hashCode()
        result = 31 * result + (composition?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "TextFieldValue(" +
            "text='$annotatedString', " +
            "selection=$selection, " +
            "composition=$composition)"
    }

    companion object {
        /**
         * The default [Saver] implementation for [TextFieldValue].
         */
        val Saver = listSaver<TextFieldValue, Any>(
            save = {
                listOf(it.annotatedString.toString(), it.selection.start, it.selection.end)
            },
            restore = {
                TextFieldValue(
                    text = it[0] as String,
                    selection = TextRange(it[1] as Int, it[2] as Int)
                )
            }
        )
    }
}

/**
 * Returns the text before the selection.
 */
fun TextFieldValue.getTextBeforeSelection(maxChars: Int): AnnotatedString =
    annotatedString.subSequence(max(0, selection.min - maxChars), selection.min)

/**
 * Returns the text after the selection.
 */
fun TextFieldValue.getTextAfterSelection(maxChars: Int): AnnotatedString =
    annotatedString.subSequence(selection.max, min(selection.max + maxChars, text.length))

/**
 * Returns the currently selected text.
 */
fun TextFieldValue.getSelectedText(): AnnotatedString = annotatedString.subSequence(selection)

/**
 * Temporary constructor until we figure out how to enforce composition for internal values
 * while enforcing higher level API not to accept composition modification.
 *
 * @suppress
 */
@InternalTextApi // Used by tests in foundation since constructor is not accessible
fun buildTextFieldValue(
    text: String,
    selection: TextRange,
    composition: TextRange?
): TextFieldValue = TextFieldValue(AnnotatedString(text), selection, composition)