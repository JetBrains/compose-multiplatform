/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.text.ExperimentalTextApi

/**
 * The configuration for line height such as alignment of the line in the provided line height,
 * whether to apply additional space as a result of line height to top of first line top and
 * bottom of last line.
 *
 * The configuration is applied only when a line height is defined on the text.
 *
 * [trimFirstLineTop] and [trimLastLineBottom] features are available only when includeFontPadding
 * is false.
 *
 * @param alignment defines how to align the line in the space provided by the line height.
 * @param trimFirstLineTop When true, the space that would be added to the top of the first line
 * as a result of the line height is not added. Single line text is both the first and last line.
 * This feature is available only when includeFontPadding is false.
 * @param trimLastLineBottom When true, the space that would be added to the bottom of the last line
 * as a result of the line height is not added.  Single line text is both the first and last line.
 * This feature is available only when includeFontPadding is false.
 */
@ExperimentalTextApi
class LineHeightBehavior(
    val alignment: LineVerticalAlignment = LineVerticalAlignment.Proportional,
    val trimFirstLineTop: Boolean = false,
    val trimLastLineBottom: Boolean = false,
) {
    companion object {
        /**
         * The default configuration for LineHeightBehavior:
         * - alignment = LineVerticalAlignment.Proportional
         * - trimFirstLineTop = true
         * - trimLastLineBottom = true
         */
        val Default = LineHeightBehavior(
            alignment = LineVerticalAlignment.Proportional,
            trimFirstLineTop = true,
            trimLastLineBottom = true
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineHeightBehavior) return false

        if (alignment != other.alignment) return false
        if (trimFirstLineTop != other.trimFirstLineTop) return false
        if (trimLastLineBottom != other.trimLastLineBottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = 31 * result + trimFirstLineTop.hashCode()
        result = 31 * result + trimLastLineBottom.hashCode()
        return result
    }

    override fun toString(): String {
        return "LineHeightBehavior(" +
            "distribution=$alignment, " +
            "applyToFirstTop=$trimFirstLineTop, " +
            "applyToLastBottom=$trimLastLineBottom" +
            ")"
    }
}

/**
 * Defines how to align the line in the space provided by the line height.
 */
@kotlin.jvm.JvmInline
@ExperimentalTextApi
value class LineVerticalAlignment private constructor(internal val topPercentage: Int) {

    init {
        check(topPercentage in 0..100 || topPercentage == -1) {
            "topRatio should be in [0..100] range or -1"
        }
    }

    override fun toString(): String {
        return when (topPercentage) {
            Top.topPercentage -> "LineVerticalAlignment.Top"
            Center.topPercentage -> "LineVerticalAlignment.Center"
            Proportional.topPercentage -> "LineVerticalAlignment.Proportional"
            Bottom.topPercentage -> "LineVerticalAlignment.Bottom"
            else -> "LineVerticalAlignment(topPercentage = $topPercentage)"
        }
    }

    companion object {
        /**
         * Align the line to the top of the space reserved for that line. This means that all extra
         * space as a result of line height is applied to the bottom of the line. When the provided
         * line height value is smaller than the actual line height, the line will still be aligned
         * to the top, therefore the required difference will be subtracted from the bottom of the
         * line.
         */
        val Top = LineVerticalAlignment(topPercentage = 0)

        /**
         * Align the line to the center of the space reserved for the line. This configuration
         * distributes additional space evenly between top and bottom of the line.
         */
        val Center = LineVerticalAlignment(topPercentage = 50)

        /**
         * Align the line proportional to the ascent and descent values of the line. For example
         * if ascent is 8 units of length, and descent is 2 units; an additional space of 10 units
         * will be distributed as 8 units to top, and 2 units to the bottom of the line. This is
         * the default behavior.
         */
        val Proportional = LineVerticalAlignment(topPercentage = -1)

        /**
         * Align the line to the bottom of the space reserved for that line. This means that all
         * extra space as a result of line height is applied to the top of the line. When the
         * provided line height value is smaller than the actual line height, the line will still
         * be aligned to the bottom, therefore the required difference will be subtracted from the
         * top of the line.
         */
        val Bottom = LineVerticalAlignment(topPercentage = 100)
    }
}