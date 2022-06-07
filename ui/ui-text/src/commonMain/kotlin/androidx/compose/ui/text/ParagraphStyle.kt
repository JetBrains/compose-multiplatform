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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified

private val DefaultLineHeight = TextUnit.Unspecified

/**
 * Paragraph styling configuration for a paragraph. The difference between [SpanStyle] and
 * `ParagraphStyle` is that, `ParagraphStyle` can be applied to a whole [Paragraph] while
 * [SpanStyle] can be applied at the character level.
 * Once a portion of the text is marked with a `ParagraphStyle`, that portion will be separated from
 * the remaining as if a line feed character was added.
 *
 * @sample androidx.compose.ui.text.samples.ParagraphStyleSample
 * @sample androidx.compose.ui.text.samples.ParagraphStyleAnnotatedStringsSample
 *
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param textDirection The algorithm to be used to resolve the final text direction:
 * Left To Right or Right To Left.
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 * @param textIndent The indentation of the paragraph.
 * @param platformStyle Platform specific [ParagraphStyle] parameters.
 * @param lineHeightStyle the configuration for line height such as vertical alignment of the
 * line, whether to apply additional space as a result of line height to top of first line top and
 * bottom of last line. The configuration is applied only when a [lineHeight] is defined.
 * When null, [LineHeightStyle.Default] is used.
 *
 * @see Paragraph
 * @see AnnotatedString
 * @see SpanStyle
 * @see TextStyle
 */
@Immutable
class ParagraphStyle @ExperimentalTextApi constructor(
    val textAlign: TextAlign? = null,
    val textDirection: TextDirection? = null,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val textIndent: TextIndent? = null,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi val platformStyle: PlatformParagraphStyle? = null,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi val lineHeightStyle: LineHeightStyle? = null
) {

    /**
     * Paragraph styling configuration for a paragraph. The difference between [SpanStyle] and
     * `ParagraphStyle` is that, `ParagraphStyle` can be applied to a whole [Paragraph] while
     * [SpanStyle] can be applied at the character level.
     * Once a portion of the text is marked with a `ParagraphStyle`, that portion will be separated from
     * the remaining as if a line feed character was added.
     *
     * @sample androidx.compose.ui.text.samples.ParagraphStyleSample
     * @sample androidx.compose.ui.text.samples.ParagraphStyleAnnotatedStringsSample
     *
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param textDirection The algorithm to be used to resolve the final text direction:
     * Left To Right or Right To Left.
     * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
     * @param textIndent The indentation of the paragraph.
     *
     * @see Paragraph
     * @see AnnotatedString
     * @see SpanStyle
     * @see TextStyle
     */
    @OptIn(ExperimentalTextApi::class)
    constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null
    ) : this(
        textAlign = textAlign,
        textDirection = textDirection,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = null,
        lineHeightStyle = null
    )

    init {
        if (lineHeight != TextUnit.Unspecified) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            check(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    /**
     * Returns a new paragraph style that is a combination of this style and the given [other]
     * style.
     *
     * If the given paragraph style is null, returns this paragraph style.
     */
    @OptIn(ExperimentalTextApi::class)
    @Stable
    fun merge(other: ParagraphStyle? = null): ParagraphStyle {
        if (other == null) return this

        return ParagraphStyle(
            lineHeight = if (other.lineHeight.isUnspecified) {
                this.lineHeight
            } else {
                other.lineHeight
            },
            textIndent = other.textIndent ?: this.textIndent,
            textAlign = other.textAlign ?: this.textAlign,
            textDirection = other.textDirection ?: this.textDirection,
            platformStyle = mergePlatformStyle(other.platformStyle),
            lineHeightStyle = other.lineHeightStyle ?: this.lineHeightStyle
        )
    }

    @OptIn(ExperimentalTextApi::class)
    private fun mergePlatformStyle(other: PlatformParagraphStyle?): PlatformParagraphStyle? {
        if (platformStyle == null) return other
        if (other == null) return platformStyle
        return platformStyle.merge(other)
    }

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: ParagraphStyle): ParagraphStyle = this.merge(other)

    @OptIn(ExperimentalTextApi::class)
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = this.platformStyle,
            lineHeightStyle = this.lineHeightStyle
        )
    }

    @ExperimentalTextApi
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle
        )
    }

    @OptIn(ExperimentalTextApi::class)
    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParagraphStyle) return false

        if (textAlign != other.textAlign) return false
        if (textDirection != other.textDirection) return false
        if (lineHeight != other.lineHeight) return false
        if (textIndent != other.textIndent) return false
        if (platformStyle != other.platformStyle) return false
        if (lineHeightStyle != other.lineHeightStyle) return false

        return true
    }

    @OptIn(ExperimentalTextApi::class)
    override fun hashCode(): Int {
        var result = textAlign?.hashCode() ?: 0
        result = 31 * result + (textDirection?.hashCode() ?: 0)
        result = 31 * result + lineHeight.hashCode()
        result = 31 * result + (textIndent?.hashCode() ?: 0)
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (lineHeightStyle?.hashCode() ?: 0)
        return result
    }

    @OptIn(ExperimentalTextApi::class)
    override fun toString(): String {
        return "ParagraphStyle(" +
            "textAlign=$textAlign, " +
            "textDirection=$textDirection, " +
            "lineHeight=$lineHeight, " +
            "textIndent=$textIndent, " +
            "platformStyle=$platformStyle, " +
            "lineHeightStyle=$lineHeightStyle" +
            ")"
    }
}

/**
 * Interpolate between two [ParagraphStyle]s.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
@OptIn(ExperimentalTextApi::class)
@Stable
fun lerp(start: ParagraphStyle, stop: ParagraphStyle, fraction: Float): ParagraphStyle {
    return ParagraphStyle(
        textAlign = lerpDiscrete(start.textAlign, stop.textAlign, fraction),
        textDirection = lerpDiscrete(
            start.textDirection,
            stop.textDirection,
            fraction
        ),
        lineHeight = lerpTextUnitInheritable(start.lineHeight, stop.lineHeight, fraction),
        textIndent = lerp(
            start.textIndent ?: TextIndent.None,
            stop.textIndent ?: TextIndent.None,
            fraction
        ),
        platformStyle = lerpPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        lineHeightStyle = lerpDiscrete(
            start.lineHeightStyle,
            stop.lineHeightStyle,
            fraction
        )
    )
}

@OptIn(ExperimentalTextApi::class)
private fun lerpPlatformStyle(
    start: PlatformParagraphStyle?,
    stop: PlatformParagraphStyle?,
    fraction: Float
): PlatformParagraphStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformParagraphStyle.Default
    val stopNonNull = stop ?: PlatformParagraphStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}

@OptIn(ExperimentalTextApi::class)
internal fun resolveParagraphStyleDefaults(
    style: ParagraphStyle,
    direction: LayoutDirection
) = ParagraphStyle(
    textAlign = style.textAlign ?: TextAlign.Start,
    textDirection = resolveTextDirection(direction, style.textDirection),
    lineHeight = if (style.lineHeight.isUnspecified) DefaultLineHeight else style.lineHeight,
    textIndent = style.textIndent ?: TextIndent.None,
    platformStyle = style.platformStyle,
    lineHeightStyle = style.lineHeightStyle
)
