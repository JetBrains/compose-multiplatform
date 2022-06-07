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
import androidx.compose.ui.text.AnnotatedString

/**
 * The transformed text with offset offset mapping
 */
class TransformedText(
    /**
     * The transformed text
     */
    val text: AnnotatedString,

    /**
     * The map used for bidirectional offset mapping from original to transformed text.
     */
    val offsetMapping: OffsetMapping
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformedText) return false
        if (text != other.text) return false
        if (offsetMapping != other.offsetMapping) return false
        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + offsetMapping.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransformedText(text=$text, offsetMapping=$offsetMapping)"
    }
}

/**
 * Interface used for changing visual output of the input field.
 *
 * This interface can be used for changing visual output of the text in the input field.
 * For example, you can mask characters in password field with asterisk with
 * [PasswordVisualTransformation].
 */
@Immutable
fun interface VisualTransformation {
    /**
     * Change the visual output of given text.
     *
     * Note that the returned text length can be different length from the given text. The composable
     * will call the offset translator for converting offsets for various reasons, cursor drawing
     * position, text selection by gesture, etc.
     *
     * Example: The ASCII only password (replacing with '*' chars)
     *  original text   : thisispassword
     *  transformed text: **************
     *
     *  @sample androidx.compose.ui.text.samples.passwordFilter
     *
     * Example: Credit Card Visual Output (inserting hyphens each 4 digits)
     *  original text   : 1234567890123456
     *  transformed text: 1234-5678-9012-3456
     *
     *  @sample androidx.compose.ui.text.samples.creditCardFilter
     *
     * @param text The original text
     * @return the pair of filtered text and offset translator.
     */
    fun filter(text: AnnotatedString): TransformedText

    companion object {
        /**
         * A special visual transformation object indicating that no transformation is applied.
         */
        @Stable
        val None: VisualTransformation = VisualTransformation { text ->
            TransformedText(text, OffsetMapping.Identity)
        }
    }
}

/**
 * The Visual Filter can be used for password Input Field.
 *
 * Note that this visual filter only works for ASCII characters.
 *
 * @param mask The mask character used instead of original text.
 */
class PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(mask.toString().repeat(text.text.length)),
            OffsetMapping.Identity
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PasswordVisualTransformation) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask.hashCode()
    }
}
