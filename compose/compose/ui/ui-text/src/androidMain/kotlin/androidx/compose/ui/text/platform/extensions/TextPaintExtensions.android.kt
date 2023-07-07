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
import android.text.TextPaint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.AndroidTextPaint
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

/**
 * Applies given SpanStyle to this AndroidTextPaint.
 *
 * Although most attributes in SpanStyle can be applied to TextPaint, some are only applicable as
 * regular platform spans such as background, baselineShift. This function also returns a new
 * SpanStyle that consists of attributes that were not applied to the TextPaint.
 */
@OptIn(ExperimentalTextApi::class)
internal fun AndroidTextPaint.applySpanStyle(
    style: SpanStyle,
    resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface,
    density: Density,
    requiresLetterSpacing: Boolean = false,
): SpanStyle? {
    when (style.fontSize.type) {
        TextUnitType.Sp -> with(density) {
            textSize = style.fontSize.toPx()
        }
        TextUnitType.Em -> {
            textSize *= style.fontSize.value
        }
        else -> {} // Do nothing
    }

    if (style.hasFontAttributes()) {
        typeface = resolveTypeface(
            style.fontFamily,
            style.fontWeight ?: FontWeight.Normal,
            style.fontStyle ?: FontStyle.Normal,
            style.fontSynthesis ?: FontSynthesis.All
        )
    }

    if (style.localeList != null && style.localeList != LocaleList.current) {
        if (Build.VERSION.SDK_INT >= 24) {
            LocaleListHelperMethods.setTextLocales(this, style.localeList)
        } else {
            val locale = if (style.localeList.isEmpty()) {
                Locale.current
            } else {
                style.localeList[0]
            }
            textLocale = locale.toJavaLocale()
        }
    }

    if (style.fontFeatureSettings != null && style.fontFeatureSettings != "") {
        fontFeatureSettings = style.fontFeatureSettings
    }

    if (style.textGeometricTransform != null &&
        style.textGeometricTransform != TextGeometricTransform.None
    ) {
        textScaleX *= style.textGeometricTransform.scaleX
        textSkewX += style.textGeometricTransform.skewX
    }

    // these parameters are also updated by the Paragraph.paint

    setColor(style.color)
    // setBrush draws the text with given Brush. ShaderBrush requires Size to
    // create a Shader. However, Size is unavailable at this stage of the layout.
    // Paragraph.paint will receive a proper Size after layout is completed.
    setBrush(style.brush, Size.Unspecified, style.alpha)
    setShadow(style.shadow)
    setTextDecoration(style.textDecoration)
    setDrawStyle(style.drawStyle)

    // apply para level leterspacing
    if (style.letterSpacing.type == TextUnitType.Sp && style.letterSpacing.value != 0.0f) {
        val emWidth = textSize * textScaleX
        val letterSpacingPx = with(density) {
            style.letterSpacing.toPx()
        }
        // Do nothing if emWidth is 0.0f.
        if (emWidth != 0.0f) {
            letterSpacing = letterSpacingPx / emWidth
        }
    } else if (style.letterSpacing.type == TextUnitType.Em) {
        letterSpacing = style.letterSpacing.value
    }

    return generateFallbackSpanStyle(
        style.letterSpacing,
        requiresLetterSpacing,
        style.background,
        style.baselineShift
    )
}

private fun generateFallbackSpanStyle(
    letterSpacing: TextUnit,
    requiresLetterSpacing: Boolean,
    background: Color,
    baselineShift: BaselineShift?
): SpanStyle? {
    // letterSpacing needs to be reset at every metricsEffectingSpan transition - so generate
    // a span for it only if there are other spans
    val hasLetterSpacing = requiresLetterSpacing &&
        (letterSpacing.type == TextUnitType.Sp && letterSpacing.value != 0f)

    // baselineShift and bgColor is reset in the Android Layout constructor,
    // therefore we cannot apply them on paint, have to use spans.
    val hasBackgroundColor = background != Color.Unspecified && background != Color.Transparent
    val hasBaselineShift = baselineShift != null && baselineShift != BaselineShift.None

    return if (!hasLetterSpacing && !hasBackgroundColor && !hasBaselineShift) {
        null
    } else {
        SpanStyle(
            letterSpacing = if (hasLetterSpacing) { letterSpacing } else { TextUnit.Unspecified },
            background = if (hasBackgroundColor) { background } else { Color.Unspecified },
            baselineShift = if (hasBaselineShift) { baselineShift } else { null }
        )
    }
}

@OptIn(ExperimentalTextApi::class)
internal fun AndroidTextPaint.setTextMotion(textMotion: TextMotion?) {
    val finalTextMotion = textMotion ?: TextMotion.Static
    flags = if (finalTextMotion.subpixelTextPositioning) {
        flags or TextPaint.SUBPIXEL_TEXT_FLAG
    } else {
        flags and TextPaint.SUBPIXEL_TEXT_FLAG.inv()
    }
    when (finalTextMotion.linearity) {
        TextMotion.Linearity.Linear -> {
            flags = flags or TextPaint.LINEAR_TEXT_FLAG
            hinting = TextPaint.HINTING_OFF
        }
        TextMotion.Linearity.FontHinting -> {
            flags and TextPaint.LINEAR_TEXT_FLAG.inv()
            hinting = TextPaint.HINTING_ON
        }
        TextMotion.Linearity.None -> {
            flags and TextPaint.LINEAR_TEXT_FLAG.inv()
            hinting = TextPaint.HINTING_OFF
        }
        else -> flags
    }
}

/**
 * Returns true if this [SpanStyle] contains any font style attributes set.
 */
internal fun SpanStyle.hasFontAttributes(): Boolean {
    return fontFamily != null || fontStyle != null || fontWeight != null
}

/**
 * Platform shadow layer turns off shadow when blur is zero. Where as developers expect when blur
 * is zero, the shadow is still visible but without any blur. This utility function is used
 * while setting shadow on spans or paint in order to replace 0 with Float.MIN_VALUE so that the
 * shadow will still be visible and the blur is practically 0.
 */
internal fun correctBlurRadius(blurRadius: Float) = if (blurRadius == 0f) {
    Float.MIN_VALUE
} else {
    blurRadius
}