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
 * The map interface used for bidirectional offset mapping from original to transformed text.
 */
interface OffsetMap {
    /**
     * Convert offset in original text into the offset in transformed text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the original text, the cursor in the transformed text must advance or stay there.
     *
     * @param offset offset in original text.
     * @return offset in transformed text
     * @see VisualTransformation
     */
    fun originalToTransformed(offset: Int): Int

    /**
     * Convert offset in transformed text into the offset in original text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the transformed text, the cusrsor in the original text must advance or stay
     * there.
     *
     * @param offset offset in transformed text
     * @return offset in original text
     * @see VisualTransformation
     */
    fun transformedToOriginal(offset: Int): Int

    companion object {
        /**
         * The offset map used for identity mapping.
         */
        val identityOffsetMap = object : OffsetMap {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }
    }
}

/**
 * The transformed text with offset offset mapping
 */
data class TransformedText(
    /**
     * The transformed text
     */
    val transformedText: AnnotatedString,

    /**
     * The map used for bidirectional offset mapping from original to transformed text.
     */
    val offsetMap: OffsetMap
)

/**
 * Interface used for changing visual output of the input field.
 *
 * This interface can be used for changing visual output of the text in the input field.
 * For example, you can mask characters in password filed with asterisk with
 * PasswordVisualTransformation.
 */
@Immutable
interface VisualTransformation {
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
        val None: VisualTransformation = object : VisualTransformation {
            override fun filter(text: AnnotatedString) =
                TransformedText(text, OffsetMap.identityOffsetMap)
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
data class PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(mask.toString().repeat(text.text.length)),
            OffsetMap.identityOffsetMap
        )
    }
}
