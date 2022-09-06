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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
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
): SpanStyle {
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

    when (style.letterSpacing.type) {
        TextUnitType.Em -> { letterSpacing = style.letterSpacing.value }
        TextUnitType.Sp -> {} // Sp will be handled by applying a span
        else -> {} // Do nothing
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
    // Skip textDecoration (b/199939617). TextDecoration should be applied as a span.

    // letterSpacing with unit Sp needs to be handled by span.
    // baselineShift and bgColor is reset in the Android Layout constructor,
    // therefore we cannot apply them on paint, have to use spans.
    return SpanStyle(
        letterSpacing = if (style.letterSpacing.type == TextUnitType.Sp &&
            style.letterSpacing.value != 0f
        ) {
            style.letterSpacing
        } else {
            TextUnit.Unspecified
        },
        background = if (style.background == Color.Transparent) {
            Color.Unspecified // No need to add transparent background for default text style.
        } else {
            style.background
        },
        baselineShift = if (style.baselineShift == BaselineShift.None) {
            null
        } else {
            style.baselineShift
        },
        textDecoration = style.textDecoration.takeIf {
            style.textDecoration != TextDecoration.None
        }
    )
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