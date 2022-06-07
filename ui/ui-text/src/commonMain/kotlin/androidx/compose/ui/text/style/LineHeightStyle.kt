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

import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * The configuration for line height such as alignment of the line in the provided line height,
 * whether to apply additional space as a result of line height to top of first line top and
 * bottom of last line.
 *
 * The configuration is applied only when a line height is defined on the text.
 *
 * [trim] feature is available only when [PlatformParagraphStyle.includeFontPadding] is false.
 *
 * Please check [Trim] and [Alignment] for more description.
 *
 * @param alignment defines how to align the line in the space provided by the line height.
 * @param trim defines whether the space that would be added to the top of first line, and
 * bottom of the last line should be trimmed or not. This feature is available only when
 * [PlatformParagraphStyle.includeFontPadding] is false.
 */
@ExperimentalTextApi
class LineHeightStyle(
    val alignment: Alignment,
    val trim: Trim
) {
    companion object {
        /**
         * The default configuration for [LineHeightStyle]:
         * - alignment = [Alignment.Proportional]
         * - trim = [Trim.Both]
         */
        val Default = LineHeightStyle(
            alignment = Alignment.Proportional,
            trim = Trim.Both
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineHeightStyle) return false

        if (alignment != other.alignment) return false
        if (trim != other.trim) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = 31 * result + trim.hashCode()
        return result
    }

    override fun toString(): String {
        return "LineHeightStyle(" +
            "alignment=$alignment, " +
            "trim=$trim" +
            ")"
    }

    /**
     * Defines whether the space that would be added to the top of first line, and bottom of the
     * last line should be trimmed or not. This feature is available only when
     * [PlatformParagraphStyle.includeFontPadding] is false.
     */
    @kotlin.jvm.JvmInline
    @ExperimentalTextApi
    value class Trim private constructor(private val value: Int) {

        override fun toString(): String {
            return when (value) {
                FirstLineTop.value -> "LineHeightStyle.Trim.FirstLineTop"
                LastLineBottom.value -> "LineHeightStyle.Trim.LastLineBottom"
                Both.value -> "LineHeightStyle.Trim.Both"
                None.value -> "LineHeightStyle.Trim.None"
                else -> "Invalid"
            }
        }

        companion object {
            private const val FlagTrimTop = 0x00000001
            private const val FlagTrimBottom = 0x00000010

            /**
             * Trim the space that would be added to the top of the first line as a result of the
             * line height. Single line text is both the first and last line. This feature is
             * available only when [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is
             * [Alignment.Center], the first line has 2.em height and the height from
             * first line baseline to second line baseline is still 3.em:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            val FirstLineTop = Trim(FlagTrimTop)

            /**
             * Trim the space that would be added to the bottom of the last line as a result of the
             * line height. Single line text is both the first and last line. This feature is
             * available only when [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is
             * [Alignment.Center], the last line has 2.em height and the height from
             * first line baseline to second line baseline is still 3.em:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            val LastLineBottom = Trim(FlagTrimBottom)

            /**
             * Trim the space that would be added to the top of the first line and bottom of the last
             * line as a result of the line height. This feature is available only when
             * [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is
             * [Alignment.Center], the first and last line has 2.em height and the height
             * from first line baseline to second line baseline is still 3.em:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            val Both = Trim(FlagTrimTop or FlagTrimBottom)

            /**
             * Do not trim first line top or last line bottom.
             *
             * For example, when line height is 3.em, and [Alignment] is
             * [Alignment.Center], the first line height, last line height and the height
             * from first line baseline to second line baseline are 3.em:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            val None = Trim(0)
        }

        internal fun isTrimFirstLineTop(): Boolean {
            return value and FlagTrimTop > 0
        }

        internal fun isTrimLastLineBottom(): Boolean {
            return value and FlagTrimBottom > 0
        }
    }

    /**
     * Defines how to align the line in the space provided by the line height.
     */
    @kotlin.jvm.JvmInline
    @ExperimentalTextApi
    value class Alignment private constructor(internal val topPercentage: Int) {

        init {
            check(topPercentage in 0..100 || topPercentage == -1) {
                "topRatio should be in [0..100] range or -1"
            }
        }

        override fun toString(): String {
            return when (topPercentage) {
                Top.topPercentage -> "LineHeightStyle.Alignment.Top"
                Center.topPercentage -> "LineHeightStyle.Alignment.Center"
                Proportional.topPercentage -> "LineHeightStyle.Alignment.Proportional"
                Bottom.topPercentage -> "LineHeightStyle.Alignment.Bottom"
                else -> "LineHeightStyle.Alignment(topPercentage = $topPercentage)"
            }
        }

        companion object {
            /**
             * Align the line to the top of the space reserved for that line. This means that all extra
             * space as a result of line height is applied to the bottom of the line. When the provided
             * line height value is smaller than the actual line height, the line will still be aligned
             * to the top, therefore the required difference will be subtracted from the bottom of the
             * line.
             *
             * For example, when line height is 3.em, the lines are aligned to the top of 3.em
             * height:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |        |
             * |--------|
             * | Line2  |
             * |        |
             * |        |
             * +--------+
             * </pre>
             */
            val Top = Alignment(topPercentage = 0)

            /**
             * Align the line to the center of the space reserved for the line. This configuration
             * distributes additional space evenly between top and bottom of the line.
             *
             * For example, when line height is 3.em, the lines are aligned to the center of 3.em
             * height:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            val Center = Alignment(topPercentage = 50)

            /**
             * Align the line proportional to the ascent and descent values of the line. For example
             * if ascent is 8 units of length, and descent is 2 units; an additional space of 10 units
             * will be distributed as 8 units to top, and 2 units to the bottom of the line. This is
             * the default behavior.
             */
            val Proportional = Alignment(topPercentage = -1)

            /**
             * Align the line to the bottom of the space reserved for that line. This means that all
             * extra space as a result of line height is applied to the top of the line. When the
             * provided line height value is smaller than the actual line height, the line will still
             * be aligned to the bottom, therefore the required difference will be subtracted from the
             * top of the line.
             *
             * For example, when line height is 3.em, the lines are aligned to the bottom of 3.em
             * height:
             * <pre>
             * +--------+
             * |        |
             * |        |
             * | Line1  |
             * |--------|
             * |        |
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            val Bottom = Alignment(topPercentage = 100)
        }
    }
}
