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
 * Please check [LineHeightTrim] and [LineVerticalAlignment] for more description.
 *
 * @param alignment defines how to align the line in the space provided by the line height.
 * @param trim defines whether the space that would be added to the top of first line, and
 * bottom of the last line should be trimmed or not. This feature is available only when
 * [PlatformParagraphStyle.includeFontPadding] is false.
 */
@ExperimentalTextApi
class LineHeightBehavior(
    val alignment: LineVerticalAlignment = LineVerticalAlignment.Proportional,
    val trim: LineHeightTrim = LineHeightTrim.Both
) {
    companion object {
        /**
         * The default configuration for LineHeightBehavior:
         * - alignment = LineVerticalAlignment.Proportional
         * - trim = LineHeightTrim.Both
         */
        val Default = LineHeightBehavior(
            alignment = LineVerticalAlignment.Proportional,
            trim = LineHeightTrim.Both
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineHeightBehavior) return false

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
        return "LineHeightBehavior(" +
            "alignment=$alignment, " +
            "trim=$trim" +
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
        val Top = LineVerticalAlignment(topPercentage = 0)

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
        val Bottom = LineVerticalAlignment(topPercentage = 100)
    }
}

/**
 * Defines whether the space that would be added to the top of first line, and bottom of the
 * last line should be trimmed or not. This feature is available only when
 * [PlatformParagraphStyle.includeFontPadding] is false.
 */
@kotlin.jvm.JvmInline
@ExperimentalTextApi
value class LineHeightTrim private constructor(private val value: Int) {

    override fun toString(): String {
        return when (value) {
            FirstLineTop.value -> "LineHeightTrim.FirstLineTop"
            LastLineBottom.value -> "LineHeightTrim.LastLineBottom"
            Both.value -> "LineHeightTrim.Both"
            None.value -> "LineHeightTrim.None"
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
         * For example, when line height is 3.em, and [LineVerticalAlignment] is
         * [LineVerticalAlignment.Center], the first line has 2.em height and the height from
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
        val FirstLineTop = LineHeightTrim(FlagTrimTop)

        /**
         * Trim the space that would be added to the bottom of the last line as a result of the
         * line height. Single line text is both the first and last line. This feature is
         * available only when [PlatformParagraphStyle.includeFontPadding] is false.
         *
         * For example, when line height is 3.em, and [LineVerticalAlignment] is
         * [LineVerticalAlignment.Center], the last line has 2.em height and the height from
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
        val LastLineBottom = LineHeightTrim(FlagTrimBottom)

        /**
         * Trim the space that would be added to the top of the first line and bottom of the last
         * line as a result of the line height. This feature is available only when
         * [PlatformParagraphStyle.includeFontPadding] is false.
         *
         * For example, when line height is 3.em, and [LineVerticalAlignment] is
         * [LineVerticalAlignment.Center], the first and last line has 2.em height and the height
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
        val Both = LineHeightTrim(FlagTrimTop or FlagTrimBottom)

        /**
         * Do not trim first line top or last line bottom.
         *
         * For example, when line height is 3.em, and [LineVerticalAlignment] is
         * [LineVerticalAlignment.Center], the first line height, last line height and the height
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
        val None = LineHeightTrim(0)
    }

    internal fun isTrimFirstLineTop(): Boolean {
        return value and FlagTrimTop > 0
    }

    internal fun isTrimLastLineBottom(): Boolean {
        return value and FlagTrimBottom > 0
    }
}