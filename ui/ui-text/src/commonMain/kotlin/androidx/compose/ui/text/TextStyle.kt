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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextDrawStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit

/**
 * Styling configuration for a `Text`.
 *
 * @sample androidx.compose.ui.text.samples.TextStyleSample
 *
 * @param platformStyle Platform specific [TextStyle] parameters.
 *
 * @see AnnotatedString
 * @see SpanStyle
 * @see ParagraphStyle
 */
@Immutable
class TextStyle
@OptIn(ExperimentalTextApi::class)
internal constructor(
    internal val spanStyle: SpanStyle,
    internal val paragraphStyle: ParagraphStyle,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi val platformStyle: PlatformTextStyle? = null,
) {
    @OptIn(ExperimentalTextApi::class)
    internal constructor(
        spanStyle: SpanStyle,
        paragraphStyle: ParagraphStyle,
    ) : this(
        spanStyle = spanStyle,
        paragraphStyle = paragraphStyle,
        platformStyle = createPlatformTextStyleInternal(
            spanStyle.platformStyle,
            paragraphStyle.platformStyle
        )
    )

    /**
     * Styling configuration for a `Text`.
     *
     * @sample androidx.compose.ui.text.samples.TextStyleSample
     *
     * @param color The text color.
     * @param fontSize The size of glyphs to use when painting the text. This
     * may be [TextUnit.Unspecified] for inheriting from another [TextStyle].
     * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
     * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     * or style cannot be found in the provided font family.
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontFeatureSettings The advanced typography settings provided by font. The format is
     * the same as the CSS font-feature-settings attribute:
     * https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param letterSpacing The amount of space to add between each letter.
     * @param baselineShift The amount by which the text is shifted up from the current baseline.
     * @param textGeometricTransform The geometric transformation applied the text.
     * @param localeList The locale list used to select region-specific glyphs.
     * @param background The background color for the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param shadow The shadow effect applied on the text.
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param textDirection The algorithm to be used to resolve the final text and paragraph
     * direction: Left To Right or Right To Left. If no value is provided the system will use the
     * [LayoutDirection] as the primary signal.
     * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
     * @param textIndent The indentation of the paragraph.
     */
    @OptIn(ExperimentalTextApi::class)
    constructor(
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null
    ) : this(
        SpanStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow,
            platformStyle = null
        ),
        ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = null,
            lineHeightStyle = null
        ),
        platformStyle = null
    )

    /**
     * Styling configuration for a `Text`.
     *
     * @sample androidx.compose.ui.text.samples.TextStyleSample
     *
     * @param color The text color.
     * @param fontSize The size of glyphs to use when painting the text. This
     * may be [TextUnit.Unspecified] for inheriting from another [TextStyle].
     * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
     * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     * or style cannot be found in the provided font family.
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontFeatureSettings The advanced typography settings provided by font. The format is
     * the same as the CSS font-feature-settings attribute:
     * https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param letterSpacing The amount of space to add between each letter.
     * @param baselineShift The amount by which the text is shifted up from the current baseline.
     * @param textGeometricTransform The geometric transformation applied the text.
     * @param localeList The locale list used to select region-specific glyphs.
     * @param background The background color for the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param shadow The shadow effect applied on the text.
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param textDirection The algorithm to be used to resolve the final text and paragraph
     * direction: Left To Right or Right To Left. If no value is provided the system will use the
     * [LayoutDirection] as the primary signal.
     * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
     * @param textIndent The indentation of the paragraph.
     * @param platformStyle Platform specific [TextStyle] parameters.
     * @param lineHeightStyle the configuration for line height such as vertical alignment of the
     * line, whether to apply additional space as a result of line height to top of first line top
     * and bottom of last line. The configuration is applied only when a [lineHeight] is defined.
     * When null, [LineHeightStyle.Default] is used.
     */
    @ExperimentalTextApi
    constructor(
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformTextStyle? = null,
        lineHeightStyle: LineHeightStyle? = null
    ) : this(
        SpanStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow,
            platformStyle = platformStyle?.spanStyle
        ),
        ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle?.paragraphStyle,
            lineHeightStyle = lineHeightStyle
        ),
        platformStyle = platformStyle
    )

    /**
     * Styling configuration for a `Text`.
     *
     * @sample androidx.compose.ui.text.samples.TextStyleBrushSample
     *
     * @param brush The brush to use when painting the text. If brush is given as null, it will be
     * treated as unspecified. It is equivalent to calling the alternative color constructor with
     * [Color.Unspecified]
     * @param alpha Opacity to be applied to [brush] from 0.0f to 1.0f representing fully
     * transparent to fully opaque respectively.
     * @param fontSize The size of glyphs to use when painting the text. This
     * may be [TextUnit.Unspecified] for inheriting from another [TextStyle].
     * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
     * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     * or style cannot be found in the provided font family.
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontFeatureSettings The advanced typography settings provided by font. The format is
     * the same as the CSS font-feature-settings attribute:
     * https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param letterSpacing The amount of space to add between each letter.
     * @param baselineShift The amount by which the text is shifted up from the current baseline.
     * @param textGeometricTransform The geometric transformation applied the text.
     * @param localeList The locale list used to select region-specific glyphs.
     * @param background The background color for the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param shadow The shadow effect applied on the text.
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param textDirection The algorithm to be used to resolve the final text and paragraph
     * direction: Left To Right or Right To Left. If no value is provided the system will use the
     * [LayoutDirection] as the primary signal.
     * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
     * @param textIndent The indentation of the paragraph.
     * @param platformStyle Platform specific [TextStyle] parameters.
     * @param lineHeightStyle the configuration for line height such as vertical alignment of the
     * line, whether to apply additional space as a result of line height to top of first line top
     * and bottom of last line. The configuration is applied only when a [lineHeight] is defined.
     */
    @ExperimentalTextApi
    constructor(
        brush: Brush?,
        alpha: Float = Float.NaN,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformTextStyle? = null,
        lineHeightStyle: LineHeightStyle? = null
    ) : this(
        SpanStyle(
            brush = brush,
            alpha = alpha,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow,
            platformStyle = platformStyle?.spanStyle
        ),
        ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle?.paragraphStyle,
            lineHeightStyle = lineHeightStyle
        ),
        platformStyle = platformStyle
    )

    @Stable
    fun toSpanStyle(): SpanStyle = spanStyle

    @Stable
    fun toParagraphStyle(): ParagraphStyle = paragraphStyle

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * [other] text style's null or inherit properties are replaced with the non-null properties of
     * this text style. Another way to think of it is that the "missing" properties of the [other]
     * style are _filled_ by the properties of this style.
     *
     * If the given text style is null, returns this text style.
     */
    @Stable
    fun merge(other: TextStyle? = null): TextStyle {
        if (other == null || other == Default) return this
        return TextStyle(
            spanStyle = toSpanStyle().merge(other.toSpanStyle()),
            paragraphStyle = toParagraphStyle().merge(other.toParagraphStyle())
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: SpanStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle().merge(other),
            paragraphStyle = toParagraphStyle()
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: ParagraphStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle(),
            paragraphStyle = toParagraphStyle().merge(other)
        )
    }

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: TextStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: ParagraphStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: SpanStyle): TextStyle = this.merge(other)

    @OptIn(ExperimentalTextApi::class)
    fun copy(
        color: Color = this.spanStyle.color,
        fontSize: TextUnit = this.spanStyle.fontSize,
        fontWeight: FontWeight? = this.spanStyle.fontWeight,
        fontStyle: FontStyle? = this.spanStyle.fontStyle,
        fontSynthesis: FontSynthesis? = this.spanStyle.fontSynthesis,
        fontFamily: FontFamily? = this.spanStyle.fontFamily,
        fontFeatureSettings: String? = this.spanStyle.fontFeatureSettings,
        letterSpacing: TextUnit = this.spanStyle.letterSpacing,
        baselineShift: BaselineShift? = this.spanStyle.baselineShift,
        textGeometricTransform: TextGeometricTransform? = this.spanStyle.textGeometricTransform,
        localeList: LocaleList? = this.spanStyle.localeList,
        background: Color = this.spanStyle.background,
        textDecoration: TextDecoration? = this.spanStyle.textDecoration,
        shadow: Shadow? = this.spanStyle.shadow,
        textAlign: TextAlign? = this.paragraphStyle.textAlign,
        textDirection: TextDirection? = this.paragraphStyle.textDirection,
        lineHeight: TextUnit = this.paragraphStyle.lineHeight,
        textIndent: TextIndent? = this.paragraphStyle.textIndent
    ): TextStyle {
        return TextStyle(
            spanStyle = SpanStyle(
                textDrawStyle = if (color == this.spanStyle.color) {
                    spanStyle.textDrawStyle
                } else {
                    TextDrawStyle.from(color)
                },
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = textDecoration,
                shadow = shadow,
                platformStyle = this.spanStyle.platformStyle
            ),
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                textDirection = textDirection,
                lineHeight = lineHeight,
                textIndent = textIndent,
                platformStyle = this.paragraphStyle.platformStyle,
                lineHeightStyle = this.lineHeightStyle
            ),
            platformStyle = this.platformStyle
        )
    }

    @ExperimentalTextApi
    fun copy(
        color: Color = this.spanStyle.color,
        fontSize: TextUnit = this.spanStyle.fontSize,
        fontWeight: FontWeight? = this.spanStyle.fontWeight,
        fontStyle: FontStyle? = this.spanStyle.fontStyle,
        fontSynthesis: FontSynthesis? = this.spanStyle.fontSynthesis,
        fontFamily: FontFamily? = this.spanStyle.fontFamily,
        fontFeatureSettings: String? = this.spanStyle.fontFeatureSettings,
        letterSpacing: TextUnit = this.spanStyle.letterSpacing,
        baselineShift: BaselineShift? = this.spanStyle.baselineShift,
        textGeometricTransform: TextGeometricTransform? = this.spanStyle.textGeometricTransform,
        localeList: LocaleList? = this.spanStyle.localeList,
        background: Color = this.spanStyle.background,
        textDecoration: TextDecoration? = this.spanStyle.textDecoration,
        shadow: Shadow? = this.spanStyle.shadow,
        textAlign: TextAlign? = this.paragraphStyle.textAlign,
        textDirection: TextDirection? = this.paragraphStyle.textDirection,
        lineHeight: TextUnit = this.paragraphStyle.lineHeight,
        textIndent: TextIndent? = this.paragraphStyle.textIndent,
        platformStyle: PlatformTextStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.paragraphStyle.lineHeightStyle
    ): TextStyle {
        return TextStyle(
            spanStyle = SpanStyle(
                textDrawStyle = if (color == this.spanStyle.color) {
                    spanStyle.textDrawStyle
                } else {
                    TextDrawStyle.from(color)
                },
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = textDecoration,
                shadow = shadow,
                platformStyle = platformStyle?.spanStyle
            ),
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                textDirection = textDirection,
                lineHeight = lineHeight,
                textIndent = textIndent,
                platformStyle = platformStyle?.paragraphStyle,
                lineHeightStyle = lineHeightStyle
            ),
            platformStyle = platformStyle
        )
    }

    @ExperimentalTextApi
    fun copy(
        brush: Brush?,
        alpha: Float = this.spanStyle.alpha,
        fontSize: TextUnit = this.spanStyle.fontSize,
        fontWeight: FontWeight? = this.spanStyle.fontWeight,
        fontStyle: FontStyle? = this.spanStyle.fontStyle,
        fontSynthesis: FontSynthesis? = this.spanStyle.fontSynthesis,
        fontFamily: FontFamily? = this.spanStyle.fontFamily,
        fontFeatureSettings: String? = this.spanStyle.fontFeatureSettings,
        letterSpacing: TextUnit = this.spanStyle.letterSpacing,
        baselineShift: BaselineShift? = this.spanStyle.baselineShift,
        textGeometricTransform: TextGeometricTransform? = this.spanStyle.textGeometricTransform,
        localeList: LocaleList? = this.spanStyle.localeList,
        background: Color = this.spanStyle.background,
        textDecoration: TextDecoration? = this.spanStyle.textDecoration,
        shadow: Shadow? = this.spanStyle.shadow,
        textAlign: TextAlign? = this.paragraphStyle.textAlign,
        textDirection: TextDirection? = this.paragraphStyle.textDirection,
        lineHeight: TextUnit = this.paragraphStyle.lineHeight,
        textIndent: TextIndent? = this.paragraphStyle.textIndent,
        platformStyle: PlatformTextStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.paragraphStyle.lineHeightStyle
    ): TextStyle {
        return TextStyle(
            spanStyle = SpanStyle(
                brush = brush,
                alpha = alpha,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = textDecoration,
                shadow = shadow,
                platformStyle = platformStyle?.spanStyle
            ),
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                textDirection = textDirection,
                lineHeight = lineHeight,
                textIndent = textIndent,
                platformStyle = platformStyle?.paragraphStyle,
                lineHeightStyle = lineHeightStyle
            ),
            platformStyle = platformStyle
        )
    }

    /**
     * The brush to use when drawing text. If not null, overrides [color].
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi
    val brush: Brush? get() = this.spanStyle.brush

    /**
     * The text color.
     */
    val color: Color get() = this.spanStyle.color

    /**
     * Opacity of text. This value is either provided along side Brush, or via alpha channel in
     * color.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi
    val alpha: Float get() = this.spanStyle.alpha

    /**
     * The size of glyphs to use when painting the text. This
     * may be [TextUnit.Unspecified] for inheriting from another [TextStyle].
     */
    val fontSize: TextUnit get() = this.spanStyle.fontSize

    /**
     * The typeface thickness to use when painting the text (e.g., bold).
      */
    val fontWeight: FontWeight? get() = this.spanStyle.fontWeight

    /**
     * The typeface variant to use when drawing the letters (e.g., italic).
     */
    val fontStyle: FontStyle? get() = this.spanStyle.fontStyle

    /**
     * Whether to synthesize font weight and/or style when the requested weight or
     *  style cannot be found in the provided font family.
     */
    val fontSynthesis: FontSynthesis? get() = this.spanStyle.fontSynthesis

    /**
     * The font family to be used when rendering the text.
     */
    val fontFamily: FontFamily? get() = this.spanStyle.fontFamily

    /**
     * The advanced typography settings provided by font. The format is the
     *  same as the CSS font-feature-settings attribute:
     *  https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     */
    val fontFeatureSettings: String? get() = this.spanStyle.fontFeatureSettings

    /**
     * The amount of space to add between each letter.
     */
    val letterSpacing: TextUnit get() = this.spanStyle.letterSpacing

    /**
     * The amount by which the text is shifted up from the current baseline.
     */
    val baselineShift: BaselineShift? get() = this.spanStyle.baselineShift

    /**
     * The geometric transformation applied the text.
     */
    val textGeometricTransform: TextGeometricTransform? get() =
        this.spanStyle.textGeometricTransform

    /**
     * The locale list used to select region-specific glyphs.
     */
    val localeList: LocaleList? get() = this.spanStyle.localeList

    /**
     * The background color for the text.
     */
    val background: Color get() = this.spanStyle.background

    /**
     * The decorations to paint on the text (e.g., an underline).
     */
    val textDecoration: TextDecoration? get() = this.spanStyle.textDecoration

    /**
     * The shadow effect applied on the text.
     */
    val shadow: Shadow? get() = this.spanStyle.shadow

    /**
     * The alignment of the text within the lines of the paragraph.
     */
    val textAlign: TextAlign? get() = this.paragraphStyle.textAlign

    /**
     * The algorithm to be used to resolve the final text and paragraph
     * direction: Left To Right or Right To Left. If no value is provided the system will use the
     * [LayoutDirection] as the primary signal.
     */
    val textDirection: TextDirection? get() = this.paragraphStyle.textDirection

    /**
     * Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
     */
    val lineHeight: TextUnit get() = this.paragraphStyle.lineHeight

    /**
     * The indentation of the paragraph.
     */
    val textIndent: TextIndent? get() = this.paragraphStyle.textIndent

    /**
     * The configuration for line height such as vertical alignment of the line, whether to apply
     * additional space as a result of line height to top of first line top and bottom of last line.
     *
     * The configuration is applied only when a [lineHeight] is defined.
     *
     * When null, [LineHeightStyle.Default] is used.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalTextApi
    @get:ExperimentalTextApi
    val lineHeightStyle: LineHeightStyle? get() = this.paragraphStyle.lineHeightStyle

    @OptIn(ExperimentalTextApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextStyle) return false

        if (spanStyle != other.spanStyle) return false
        if (paragraphStyle != other.paragraphStyle) return false
        if (platformStyle != other.platformStyle) return false

        return true
    }

    /**
     * Returns true if text layout affecting attributes between this TextStyle and other are the
     * same.
     *
     * The attributes that do not require a layout change are color, textDecoration and shadow.
     *
     * Majority of attributes change text layout, and examples are line height, font properties,
     * font size, locale etc.
     *
     * This function can be used to identify if a new text layout is required for a given TextStyle.
     *
     * @param other The TextStyle to compare to.
     */
    @OptIn(ExperimentalTextApi::class)
    fun hasSameLayoutAffectingAttributes(other: TextStyle): Boolean {
        return (this === other) || (paragraphStyle == other.paragraphStyle &&
            spanStyle.hasSameLayoutAffectingAttributes(other.spanStyle))
    }

    @OptIn(ExperimentalTextApi::class)
    override fun hashCode(): Int {
        var result = spanStyle.hashCode()
        result = 31 * result + paragraphStyle.hashCode()
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        return result
    }

    @OptIn(ExperimentalTextApi::class)
    override fun toString(): String {
        return "TextStyle(" +
            "color=$color, " +
            "brush=$brush, " +
            "alpha=$alpha, " +
            "fontSize=$fontSize, " +
            "fontWeight=$fontWeight, " +
            "fontStyle=$fontStyle, " +
            "fontSynthesis=$fontSynthesis, " +
            "fontFamily=$fontFamily, " +
            "fontFeatureSettings=$fontFeatureSettings, " +
            "letterSpacing=$letterSpacing, " +
            "baselineShift=$baselineShift, " +
            "textGeometricTransform=$textGeometricTransform, " +
            "localeList=$localeList, " +
            "background=$background, " +
            "textDecoration=$textDecoration, " +
            "shadow=$shadow, textAlign=$textAlign, " +
            "textDirection=$textDirection, " +
            "lineHeight=$lineHeight, " +
            "textIndent=$textIndent, " +
            "platformStyle=$platformStyle" +
            "lineHeightStyle=$lineHeightStyle" +
            ")"
    }

    companion object {
        /**
         * Constant for default text style.
         */
        @Stable
        val Default = TextStyle()
    }
}

/**
 * Interpolate between two text styles.
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
fun lerp(start: TextStyle, stop: TextStyle, fraction: Float): TextStyle {
    return TextStyle(
        spanStyle = lerp(start.toSpanStyle(), stop.toSpanStyle(), fraction),
        paragraphStyle = lerp(start.toParagraphStyle(), stop.toParagraphStyle(), fraction)
    )
}

/**
 * Fills missing values in TextStyle with default values and resolve [TextDirection].
 *
 * This function will fill all null or [TextUnit.Unspecified] field with actual values.
 * @param style a text style to be resolved
 * @param direction a layout direction to be used for resolving text layout direction algorithm
 * @return resolved text style.
 */
@OptIn(ExperimentalTextApi::class)
fun resolveDefaults(style: TextStyle, direction: LayoutDirection) = TextStyle(
    spanStyle = resolveSpanStyleDefaults(style.spanStyle),
    paragraphStyle = resolveParagraphStyleDefaults(style.paragraphStyle, direction),
    platformStyle = style.platformStyle
)

/**
 * If [textDirection] is null returns a [TextDirection] based on [layoutDirection].
 */
internal fun resolveTextDirection(
    layoutDirection: LayoutDirection,
    textDirection: TextDirection?
): TextDirection {
    return when (textDirection) {
        TextDirection.Content -> when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirection.ContentOrLtr
            LayoutDirection.Rtl -> TextDirection.ContentOrRtl
        }
        null -> when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirection.Ltr
            LayoutDirection.Rtl -> TextDirection.Rtl
        }
        else -> textDirection
    }
}

@OptIn(ExperimentalTextApi::class)
private fun createPlatformTextStyleInternal(
    platformSpanStyle: PlatformSpanStyle?,
    platformParagraphStyle: PlatformParagraphStyle?
): PlatformTextStyle? {
    return if (platformSpanStyle == null && platformParagraphStyle == null) {
        null
    } else {
        createPlatformTextStyle(platformSpanStyle, platformParagraphStyle)
    }
}
