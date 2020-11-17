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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableContract
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.useOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [AmbientTextStyle] defined by a theme. If you are setting your
 * own style, you may want to consider first retrieving [AmbientTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes
 * you want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]),
 * then this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 * from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then
 * [AmbientContentColor] will be used - this allows this [Text] or element containing this [Text] to
 * adapt to different background colors and still maintain contrast and accessibility.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 * this will be [AmbientContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter.
 * See [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * See [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * See [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 * See [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param style Style configuration for the text such as color, font, line height etc.
 */
@Deprecated(
    message = "Use androidx.compose.material.Text for a high level Text component that " +
        "consumes theming information, or androidx.compose.foundation.text.BasicText for a basic " +
        "unopinionated component that does not have default theming",
    replaceWith = ReplaceWith(
        "Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, " +
            "letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, " +
            "onTextLayout, style)",
        "androidx.compose.material.Text"
    )
)
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = AmbientTextStyle.current
) {
    Text(
        AnnotatedString(text),
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        emptyMap(),
        onTextLayout,
        style
    )
}

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [AmbientTextStyle] defined by a theme. If you are setting your
 * own style, you may want to consider first retrieving [AmbientTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes
 * you want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]),
 * then this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 * from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then
 * [AmbientContentColor] will be used - this allows this [Text] or element containing this [Text] to
 * adapt to different background colors and still maintain contrast and accessibility.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 * this will be [AmbientContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter.
 * See [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * See [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * See [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 * See [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's
 * used to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param style Style configuration for the text such as color, font, line height etc.
 */
@Deprecated(
    message = "Use androidx.compose.material.Text for a high level Text component that " +
        "consumes theming information, or androidx.compose.foundation.text.BasicText for a basic " +
        "unopinionated component that does not have default theming",
    replaceWith = ReplaceWith(
        "Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, " +
            "letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, " +
            "inlineContent, onTextLayout, style)",
        "androidx.compose.material.Text"
    )
)
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = AmbientTextStyle.current
) {
    val textColor = color.useOrElse { style.color.useOrElse { AmbientContentColor.current } }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )
    BasicText(
        text,
        modifier,
        mergedStyle,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        inlineContent
    )
}

/**
 * Ambient containing the preferred [TextStyle] that will be used by [Text] components by default.
 * To set the value for this ambient, see [ProvideTextStyle] which will merge any missing
 * [TextStyle] properties with the existing [TextStyle] set in this ambient.
 *
 * @see ProvideTextStyle
 */
@Deprecated(
    message = "AmbientTextStyle has moved to the Material library. For non-Material applications," +
        " create your own design system specific theming ambients.",
    replaceWith = ReplaceWith(
        "AmbientTextStyle", "androidx.compose.material.AmbientTextStyle"
    )
)
val AmbientTextStyle = ambientOf(
    @OptIn(ExperimentalComposeApi::class) structuralEqualityPolicy()
) { TextStyle() }

// TODO: b/156598010 remove this and replace with fold definition on the backing Ambient
/**
 * This function is used to set the current value of [AmbientTextStyle], merging the given style
 * with the current style values for any missing attributes. Any [Text] components included in
 * this component's [children] will be styled with this style unless styled explicitly.
 *
 * @see AmbientTextStyle
 */
@Suppress("ComposableLambdaParameterNaming")
@Deprecated(
    message = "ProvideTextStyle has moved to the Material library. For non-Material applications," +
        " create your own design system specific theming ambients.",
    replaceWith = ReplaceWith(
        "ProvideTextStyle(value, children)",
        "androidx.compose.material.ProvideTextStyle"
    )
)
@Composable
fun ProvideTextStyle(value: TextStyle, children: @Composable () -> Unit) {
    val mergedStyle = AmbientTextStyle.current.merge(value)
    Providers(AmbientTextStyle provides mergedStyle, content = children)
}

/**
 * This effect is used to read the current value of the Text style ambient. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Deprecated(
    message = "Use AmbientTextStyle.current explicitly",
    replaceWith = ReplaceWith(
        "AmbientTextStyle.current",
        "androidx.compose.foundation.AmbientTextStyle"
    )
)
@Composable
@ComposableContract(readonly = true)
fun currentTextStyle(): TextStyle = AmbientTextStyle.current
