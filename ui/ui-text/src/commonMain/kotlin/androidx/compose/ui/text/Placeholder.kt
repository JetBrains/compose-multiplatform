/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified

/**
 * A placeholder is a rectangle box inserted into text, which tells the text processor to leave an
 * empty space. It is typically used to insert inline image, custom emoji, etc into the text
 * paragraph.
 *
 * @param width the width of the placeholder, it must be specified in sp or em.
 * [TextUnit.Unspecified] is not allowed.
 * @param height the height of the placeholder, it must be specified in sp or em.
 * [TextUnit.Unspecified] is not allowed.
 * @param placeholderVerticalAlign the vertical alignment of the placeholder within the text line.
 * Check [PlaceholderVerticalAlign] for more information.
 * @throws IllegalArgumentException if [TextUnit.Unspecified] is passed to [width] or [height].
 */
@Immutable
class Placeholder(
    val width: TextUnit,
    val height: TextUnit,
    val placeholderVerticalAlign: PlaceholderVerticalAlign
) {
    init {
        require(!width.isUnspecified) { "width cannot be TextUnit.Unspecified" }
        require(!height.isUnspecified) { "height cannot be TextUnit.Unspecified" }
    }

    fun copy(
        width: TextUnit = this.width,
        height: TextUnit = this.height,
        placeholderVerticalAlign: PlaceholderVerticalAlign = this.placeholderVerticalAlign
    ): Placeholder {
        return Placeholder(
            width = width,
            height = height,
            placeholderVerticalAlign = placeholderVerticalAlign
        )
    }

    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Placeholder) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (placeholderVerticalAlign != other.placeholderVerticalAlign) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + placeholderVerticalAlign.hashCode()
        return result
    }

    override fun toString(): String {
        return "Placeholder(" +
            "width=$width, " +
            "height=$height, " +
            "placeholderVerticalAlign=$placeholderVerticalAlign" +
            ")"
    }
}
/**
 * The settings used to specify how a placeholder is vertically aligned within a text line.
 * @see Placeholder
 */
@kotlin.jvm.JvmInline
value class PlaceholderVerticalAlign internal constructor(
    @Suppress("unused") private val value: Int
) {

    override fun toString(): String {
        return when (this) {
            AboveBaseline -> "AboveBaseline"
            Top -> "Top"
            Bottom -> "Bottom"
            Center -> "Center"
            TextTop -> "TextTop"
            TextBottom -> "TextBottom"
            TextCenter -> "TextCenter"
            else -> "Invalid"
        }
    }

    companion object {
        /** Align the bottom of the placeholder with the baseline. */
        val AboveBaseline = PlaceholderVerticalAlign(1)
        /** Align the top of the placeholder with the top of the entire line. */
        val Top = PlaceholderVerticalAlign(2)
        /** Align the bottom of the placeholder with the bottom of the entire line. */
        val Bottom = PlaceholderVerticalAlign(3)
        /** Align the center of the placeholder with the center of the entire line. */
        val Center = PlaceholderVerticalAlign(4)
        /**
         *  Align the top of the placeholder with the top of the proceeding text.
         *  It is different from the [Top] when there are texts with different font size, font or other
         *  styles in the same line. This option will use the proceeding text's top instead of the
         *  whole line's top.
         */
        val TextTop = PlaceholderVerticalAlign(5)
        /**
         * Align the bottom of the placeholder with the bottom of the proceeding text.
         * It is different from the [TextBottom] when there are texts with different font size, font or
         * other styles in the same line. This option will use the proceeding text's bottom instead of
         * the whole line's bottom.
         */
        val TextBottom = PlaceholderVerticalAlign(6)
        /**
         * Align the center of the placeholder with the center of the proceeding text.
         * It is different from the [Center] when there are texts with different font size, font or
         * other styles in the same line. This option will use the proceeding text's center instead of
         * the whole line's center.
         */
        val TextCenter = PlaceholderVerticalAlign(7)
    }
}