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
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.android.InternalPlatformTextApi

/**
 * Span that changes the typeface of the text.
 *
 * @param getTypeface a lambda function that returns the typeface used to render the affected text.
 * @suppress
 */
@InternalPlatformTextApi
class FontSpan(
    val getTypeface: (weight: Int, isItalic: Boolean) -> Typeface
) : MetricAffectingSpan() {

    @VisibleForTesting
    internal fun updatePaint(textPaint: TextPaint) {
        val oldTypeface: Typeface? = textPaint.typeface
        if (oldTypeface == null) {
            textPaint.typeface = getTypeface(FontStyle.FONT_WEIGHT_NORMAL, false)
            return
        }
        val weight = if (Build.VERSION.SDK_INT >= 28) {
            oldTypeface.weight
        } else {
            if (oldTypeface.isBold) {
                FontStyle.FONT_WEIGHT_BOLD
            } else {
                FontStyle.FONT_WEIGHT_NORMAL
            }
        }
        textPaint.typeface = getTypeface(weight, oldTypeface.isItalic)
    }

    override fun updateDrawState(textPaint: TextPaint) {
        updatePaint(textPaint)
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        updatePaint(textPaint)
    }
}