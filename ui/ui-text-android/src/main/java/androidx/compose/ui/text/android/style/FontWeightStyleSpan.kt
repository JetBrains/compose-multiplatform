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

package androidx.compose.ui.text.android.style

import android.graphics.Typeface
import android.graphics.fonts.FontStyle
import android.os.Build
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.android.InternalPlatformTextApi

private const val AndroidBoldWeight = 600

/**
 * The span which changes the font weight and the font style of the affected text.
 *
 * @param weight the target font weight. If it's 0, this span won't modify the font weight.
 * @param style the font style specified by this span. It can be STYLE_NONE, STYLE_NORMAL and
 * STYLE_ITALIC. If STYLE_NONE is given, this span won't modify the font style.
 *
 * @suppress
 */
@InternalPlatformTextApi
class FontWeightStyleSpan(
    @IntRange(from = 0, to = 1000)
    val weight: Int,
    @FontStyleMode
    val style: Int
) : MetricAffectingSpan() {
    companion object {
        const val STYLE_NONE = -1
        const val STYLE_NORMAL = 0
        const val STYLE_ITALIC = 2

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            STYLE_NONE,
            STYLE_NORMAL,
            STYLE_ITALIC
        )
        internal annotation class FontStyleMode
    }

    @VisibleForTesting
    @RequiresApi(value = 28)
    internal fun updatePaint(textPaint: TextPaint) {
        val oldTypeface: Typeface? = textPaint.typeface
        val weightChanged =
            (weight != 0 && weight != oldTypeface?.weight)
        val styleChanged =
            (style != STYLE_NONE && (style == STYLE_ITALIC) != oldTypeface?.isItalic)
        val needsUpdate = weightChanged || styleChanged
        // Nothing to change. Early return.
        if (!needsUpdate) {
            return
        }

        val newWeight = if (weight != 0) {
            weight
        } else {
            oldTypeface?.weight ?: FontStyle.FONT_WEIGHT_NORMAL
        }

        val newItalic = when (style) {
            STYLE_NORMAL -> false
            STYLE_ITALIC -> true
            else -> oldTypeface?.isItalic ?: false
        }

        textPaint.typeface = Typeface.create(oldTypeface, newWeight, newItalic)
    }

    @VisibleForTesting
    internal fun legacyUpdatePaint(textPaint: TextPaint) {
        val oldTypeface: Typeface? = textPaint.typeface
        val boldChanged =
            (weight != 0 || isBold(weight) != oldTypeface?.isBold)
        val styleChanged =
            (style != STYLE_NONE && (style == STYLE_ITALIC) != oldTypeface?.isItalic)
        val needsUpdate = boldChanged || styleChanged
        // Nothing to change. Early return.
        if (!needsUpdate) {
            return
        }
        val newBold = if (weight != 0) {
            isBold(weight)
        } else {
            oldTypeface?.isBold ?: false
        }

        val newItalic = when (style) {
            STYLE_NORMAL -> false
            STYLE_ITALIC -> true
            else -> oldTypeface?.isItalic ?: false
        }

        textPaint.typeface = Typeface.create(oldTypeface, getTypefaceStyle(newBold, newItalic))
    }

    override fun updateDrawState(textPaint: TextPaint) {
        if (Build.VERSION.SDK_INT >= 28) {
            updatePaint(textPaint)
        } else {
            legacyUpdatePaint(textPaint)
        }
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        if (Build.VERSION.SDK_INT >= 28) {
            updatePaint(textPaint)
        } else {
            legacyUpdatePaint(textPaint)
        }
    }
}

// Helper function that checks if the given weight is bold or not.
// It assumes weight is within [1, 1000].
private fun isBold(weight: Int): Boolean {
    return weight >= AndroidBoldWeight
}

internal fun getTypefaceStyle(isBold: Boolean, isItalic: Boolean): Int {
    return if (isBold && isItalic) {
        Typeface.BOLD_ITALIC
    } else if (isBold) {
        Typeface.BOLD
    } else if (isItalic) {
        Typeface.ITALIC
    } else {
        Typeface.NORMAL
    }
}