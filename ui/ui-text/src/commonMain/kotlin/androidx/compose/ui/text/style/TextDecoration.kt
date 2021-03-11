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
package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.fastFold
import androidx.compose.ui.text.fastJoinToString

/**
 * Defines a horizontal line to be drawn on the text.
 */
@Immutable
class TextDecoration internal constructor(val mask: Int) {

    companion object {
        @Stable
        val None: TextDecoration = TextDecoration(0x0)

        /**
         * Draws a horizontal line below the text.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationUnderlineSample
         */
        @Stable
        val Underline: TextDecoration = TextDecoration(0x1)

        /**
         * Draws a horizontal line over the text.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationLineThroughSample
         */
        @Stable
        val LineThrough: TextDecoration = TextDecoration(0x2)

        /**
         * Creates a decoration that includes all the given decorations.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationCombinedSample
         *
         * @param decorations The decorations to be added
         */
        fun combine(decorations: List<TextDecoration>): TextDecoration {
            val mask = decorations.fastFold(0) { acc, decoration ->
                acc or decoration.mask
            }
            return TextDecoration(mask)
        }
    }

    /**
     * Creates a decoration that includes both of the TextDecorations.
     *
     * @sample androidx.compose.ui.text.samples.TextDecorationCombinedSample
     */
    operator fun plus(decoration: TextDecoration): TextDecoration {
        return TextDecoration(this.mask or decoration.mask)
    }

    /**
     * Check whether this [TextDecoration] contains the given decoration.
     *
     * @param other The [TextDecoration] to be checked.
     */
    operator fun contains(other: TextDecoration): Boolean {
        return (mask or other.mask) == mask
    }

    override fun toString(): String {
        if (mask == 0) {
            return "TextDecoration.None"
        }

        val values: MutableList<String> = mutableListOf()
        if ((mask and Underline.mask) != 0) {
            values.add("Underline")
        }
        if ((mask and LineThrough.mask) != 0) {
            values.add("LineThrough")
        }
        if ((values.size == 1)) {
            return "TextDecoration.${values[0]}"
        }
        return "TextDecoration[${values.fastJoinToString(separator = ", ")}]"
    }

    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextDecoration) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask
    }
}
