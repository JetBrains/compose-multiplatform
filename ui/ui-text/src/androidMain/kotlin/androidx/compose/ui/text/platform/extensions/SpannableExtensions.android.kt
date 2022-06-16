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

package androidx.compose.ui.text.platform.extensions

import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.style.BaselineShiftSpan
import androidx.compose.ui.text.android.style.FontFeatureSpan
import androidx.compose.ui.text.android.style.LetterSpacingSpanEm
import androidx.compose.ui.text.android.style.LetterSpacingSpanPx
import androidx.compose.ui.text.android.style.LineHeightSpan
import androidx.compose.ui.text.android.style.LineHeightStyleSpan
import androidx.compose.ui.text.android.style.ShadowSpan
import androidx.compose.ui.text.android.style.SkewXSpan
import androidx.compose.ui.text.android.style.TextDecorationSpan
import androidx.compose.ui.text.android.style.TypefaceSpan
import androidx.compose.ui.text.fastFilter
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intersect
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.style.ShaderBrushSpan
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class SpanRange(
    val span: Any,
    val start: Int,
    val end: Int
)

internal fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

@Suppress("DEPRECATION")
internal fun Spannable.setTextIndent(
    textIndent: TextIndent?,
    contextFontSize: Float,
    density: Density
) {
    textIndent?.let { indent ->
        if (indent.firstLine == 0.sp && indent.restLine == 0.sp) return@let
        if (indent.firstLine.isUnspecified || indent.restLine.isUnspecified) return@let
        with(density) {
            val firstLine = when (indent.firstLine.type) {
                TextUnitType.Sp -> indent.firstLine.toPx()
                TextUnitType.Em -> indent.firstLine.value * contextFontSize
                else -> 0f
            }
            val restLine = when (indent.restLine.type) {
                TextUnitType.Sp -> indent.restLine.toPx()
                TextUnitType.Em -> indent.restLine.value * contextFontSize
                else -> 0f
            }
            setSpan(
                LeadingMarginSpan.Standard(
                    ceil(firstLine).toInt(),
                    ceil(restLine).toInt()
                ),
                0,
                length
            )
        }
    }
}

@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
internal fun Spannable.setLineHeight(
    lineHeight: TextUnit,
    contextFontSize: Float,
    density: Density,
    lineHeightStyle: LineHeightStyle
) {
    val resolvedLineHeight = resolveLineHeightInPx(lineHeight, contextFontSize, density)
    if (!resolvedLineHeight.isNaN()) {
        setSpan(
            span = LineHeightStyleSpan(
                lineHeight = resolvedLineHeight,
                startIndex = 0,
                endIndex = length,
                trimFirstLineTop = lineHeightStyle.trim.isTrimFirstLineTop(),
                trimLastLineBottom = lineHeightStyle.trim.isTrimLastLineBottom(),
                topPercentage = lineHeightStyle.alignment.topPercentage
            ),
            start = 0,
            end = length
        )
    }
}

@OptIn(InternalPlatformTextApi::class)
internal fun Spannable.setLineHeight(
    lineHeight: TextUnit,
    contextFontSize: Float,
    density: Density
) {
    val resolvedLineHeight = resolveLineHeightInPx(lineHeight, contextFontSize, density)
    if (!resolvedLineHeight.isNaN()) {
        setSpan(
            span = LineHeightSpan(lineHeight = resolvedLineHeight),
            start = 0,
            end = length
        )
    }
}

private fun resolveLineHeightInPx(
    lineHeight: TextUnit,
    contextFontSize: Float,
    density: Density
): Float {
    return when (lineHeight.type) {
        TextUnitType.Sp -> with(density) { lineHeight.toPx() }
        TextUnitType.Em -> lineHeight.value * contextFontSize
        else -> Float.NaN
    }
}

internal fun Spannable.setSpanStyles(
    contextTextStyle: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    density: Density,
    resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface,
) {

    setFontAttributes(contextTextStyle, spanStyles, resolveTypeface)

    // LetterSpacingSpanPx/LetterSpacingSpanSP has lower priority than normal spans. Because
    // letterSpacing relies on the fontSize on [Paint] to compute Px/Sp from Em. So it must be
    // applied after all spans that changes the fontSize.
    val lowPrioritySpans = ArrayList<SpanRange>()

    for (i in spanStyles.indices) {
        val spanStyleRange = spanStyles[i]
        val start = spanStyleRange.start
        val end = spanStyleRange.end

        if (start < 0 || start >= length || end <= start || end > length) continue

        setSpanStyle(
            spanStyleRange,
            density,
            lowPrioritySpans
        )
    }

    lowPrioritySpans.fastForEach { (span, start, end) ->
        setSpan(span, start, end)
    }
}

@OptIn(ExperimentalTextApi::class)
private fun Spannable.setSpanStyle(
    spanStyleRange: AnnotatedString.Range<SpanStyle>,
    density: Density,
    lowPrioritySpans: ArrayList<SpanRange>
) {
    val start = spanStyleRange.start
    val end = spanStyleRange.end
    val style = spanStyleRange.item

    // Be aware that SuperscriptSpan needs to be applied before all other spans which
    // affect FontMetrics
    setBaselineShift(style.baselineShift, start, end)

    setColor(style.color, start, end)

    setBrush(style.brush, start, end)

    setTextDecoration(style.textDecoration, start, end)

    setFontSize(style.fontSize, density, start, end)

    setFontFeatureSettings(style.fontFeatureSettings, start, end)

    setGeometricTransform(style.textGeometricTransform, start, end)

    setLocaleList(style.localeList, start, end)

    setBackground(style.background, start, end)

    setShadow(style.shadow, start, end)

    createLetterSpacingSpan(style.letterSpacing, density)?.let {
        lowPrioritySpans.add(
            SpanRange(it, start, end)
        )
    }
}

/**
 * Set font related [SpanStyle]s to this [Spannable].
 *
 * Different from other styles, font related styles needs to be flattened first and then applied.
 * This is required because on certain API levels the [FontWeight] is not supported by framework,
 * and we have to resolve font settings and create typeface first and then set it directly on
 * TextPaint.
 *
 * Notice that a [contextTextStyle] is also required when we flatten the font related styles.
 * For example:
 *  the entire text has the TextStyle(fontFamily = Sans-serif)
 *  Hi Hello World
 *  [ bold ]
 * FontWeight.Bold is set in range [0, 8).
 * The resolved TypefaceSpan should be TypefaceSpan("Sans-serif", "bold") in range [0, 8).
 * As demonstrated above, the fontFamily information is from [contextTextStyle].
 *
 * @see flattenFontStylesAndApply
 *
 * @param contextTextStyle the global [TextStyle] for the entire string.
 * @param spanStyles the [spanStyles] to be applied, this function will first filter out the font
 * related [SpanStyle]s and then apply them to this [Spannable].
 * @param fontFamilyResolver the [Font.ResourceLoader] used to resolve font.
 */
@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setFontAttributes(
    contextTextStyle: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface,
) {
    val fontRelatedSpanStyles = spanStyles.fastFilter {
        it.item.hasFontAttributes() || it.item.fontSynthesis != null
    }

    // Create a SpanStyle if contextTextStyle has font related attributes, otherwise use
    // null to avoid unnecessary object creation.
    val contextFontSpanStyle = if (contextTextStyle.hasFontAttributes()) {
        SpanStyle(
            fontFamily = contextTextStyle.fontFamily,
            fontWeight = contextTextStyle.fontWeight,
            fontStyle = contextTextStyle.fontStyle,
            fontSynthesis = contextTextStyle.fontSynthesis
        )
    } else {
        null
    }

    flattenFontStylesAndApply(
        contextFontSpanStyle,
        fontRelatedSpanStyles
    ) { spanStyle, start, end ->
        setSpan(
            TypefaceSpan(
                resolveTypeface(
                    spanStyle.fontFamily,
                    spanStyle.fontWeight ?: FontWeight.Normal,
                    spanStyle.fontStyle ?: FontStyle.Normal,
                    spanStyle.fontSynthesis ?: FontSynthesis.All
                )
            ),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

/**
 * Flatten styles in the [spanStyles], so that overlapping styles are merged, and then apply the
 * [block] on the merged [SpanStyle].
 *
 * @param contextFontSpanStyle the global [SpanStyle]. It act as if every [spanStyles] is applied
 * on top of it. This parameter is nullable. A null value is exactly the same as a default
 * SpanStyle, but avoids unnecessary object creation.
 * @param spanStyles the input [SpanStyle] ranges to be flattened.
 * @param block the function to be applied on the merged [SpanStyle].
 */
internal fun flattenFontStylesAndApply(
    contextFontSpanStyle: SpanStyle?,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    block: (SpanStyle, Int, Int) -> Unit
) {
    // quick way out for single SpanStyle or empty list.
    if (spanStyles.size <= 1) {
        if (spanStyles.isNotEmpty()) {
            block(
                contextFontSpanStyle.merge(spanStyles[0].item),
                spanStyles[0].start,
                spanStyles[0].end
            )
        }
        return
    }

    // Sort all span start and end points.
    // S1--S2--E1--S3--E3--E2
    val spanCount = spanStyles.size
    val transitionOffsets = Array(spanCount * 2) { 0 }
    spanStyles.fastForEachIndexed { idx, spanStyle ->
        transitionOffsets[idx] = spanStyle.start
        transitionOffsets[idx + spanCount] = spanStyle.end
    }
    transitionOffsets.sort()

    // S1--S2--E1--S3--E3--E2
    // - Go through all minimum intervals
    // - Find Spans that intersect with the given interval
    // - Merge all spans in order, starting from contextFontSpanStyle
    // - Apply the merged SpanStyle to the minimal interval
    var lastTransitionOffsets = transitionOffsets.first()
    for (transitionOffset in transitionOffsets) {
        // There might be duplicated transition offsets, we skip them here.
        if (transitionOffset == lastTransitionOffsets) {
            continue
        }

        // Check all spans that intersects with this transition range.
        var mergedSpanStyle = contextFontSpanStyle
        spanStyles.fastForEach { spanStyle ->
            // Empty spans do not intersect with anything, skip them.
            if (
                spanStyle.start != spanStyle.end &&
                intersect(lastTransitionOffsets, transitionOffset, spanStyle.start, spanStyle.end)
            ) {
                mergedSpanStyle = mergedSpanStyle.merge(spanStyle.item)
            }
        }

        mergedSpanStyle?.let {
            block(it, lastTransitionOffsets, transitionOffset)
        }

        lastTransitionOffsets = transitionOffset
    }
}

@OptIn(InternalPlatformTextApi::class)
@Suppress("DEPRECATION")
private fun createLetterSpacingSpan(
    letterSpacing: TextUnit,
    density: Density
): MetricAffectingSpan? {
    return when (letterSpacing.type) {
        TextUnitType.Sp -> with(density) {
            LetterSpacingSpanPx(letterSpacing.toPx())
        }
        TextUnitType.Em -> {
            LetterSpacingSpanEm(letterSpacing.value)
        }
        else -> {
            null
        }
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setShadow(shadow: Shadow?, start: Int, end: Int) {
    shadow?.let {
        setSpan(
            ShadowSpan(it.color.toArgb(), it.offset.x, it.offset.y, it.blurRadius),
            start,
            end
        )
    }
}

internal fun Spannable.setBackground(color: Color, start: Int, end: Int) {
    if (color.isSpecified) {
        setSpan(
            BackgroundColorSpan(color.toArgb()),
            start,
            end
        )
    }
}

internal fun Spannable.setLocaleList(localeList: LocaleList?, start: Int, end: Int) {
    localeList?.let {
        setSpan(
            if (Build.VERSION.SDK_INT >= 24) {
                LocaleListHelperMethods.localeSpan(it)
            } else {
                val locale = if (it.isEmpty()) Locale.current else it[0]
                LocaleSpan(locale.toJavaLocale())
            },
            start,
            end
        )
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setGeometricTransform(
    textGeometricTransform: TextGeometricTransform?,
    start: Int,
    end: Int
) {
    textGeometricTransform?.let {
        setSpan(ScaleXSpan(it.scaleX), start, end)
        setSpan(SkewXSpan(it.skewX), start, end)
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setFontFeatureSettings(fontFeatureSettings: String?, start: Int, end: Int) {
    fontFeatureSettings?.let {
        setSpan(FontFeatureSpan(it), start, end)
    }
}

@Suppress("DEPRECATION")
internal fun Spannable.setFontSize(fontSize: TextUnit, density: Density, start: Int, end: Int) {
    when (fontSize.type) {
        TextUnitType.Sp -> with(density) {
            setSpan(
                AbsoluteSizeSpan(/* size */ fontSize.toPx().roundToInt(), /* dip */ false),
                start,
                end
            )
        }
        TextUnitType.Em -> {
            setSpan(RelativeSizeSpan(fontSize.value), start, end)
        }
        else -> {
        } // Do nothing
    }
}

@OptIn(InternalPlatformTextApi::class)
internal fun Spannable.setTextDecoration(textDecoration: TextDecoration?, start: Int, end: Int) {
    textDecoration?.let {
        val textDecorationSpan = TextDecorationSpan(
            isUnderlineText = TextDecoration.Underline in it,
            isStrikethroughText = TextDecoration.LineThrough in it
        )
        setSpan(textDecorationSpan, start, end)
    }
}

internal fun Spannable.setColor(color: Color, start: Int, end: Int) {
    if (color.isSpecified) {
        setSpan(ForegroundColorSpan(color.toArgb()), start, end)
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setBaselineShift(baselineShift: BaselineShift?, start: Int, end: Int) {
    baselineShift?.let {
        setSpan(BaselineShiftSpan(it.multiplier), start, end)
    }
}

private fun Spannable.setBrush(
    brush: Brush?,
    start: Int,
    end: Int
) {
    brush?.let {
        when (brush) {
            is SolidColor -> {
                setColor(brush.value, start, end)
            }
            is ShaderBrush -> {
                setSpan(ShaderBrushSpan(brush), start, end)
            }
        }
    }
}

/**
 * Returns true if there is any font settings on this [TextStyle].
 * @see hasFontAttributes
 */
private fun TextStyle.hasFontAttributes(): Boolean {
    return toSpanStyle().hasFontAttributes() || fontSynthesis != null
}

/**
 * Helper function that merges a nullable [SpanStyle] with another [SpanStyle].
 */
private fun SpanStyle?.merge(spanStyle: SpanStyle): SpanStyle {
    if (this == null) return spanStyle
    return this.merge(spanStyle)
}
