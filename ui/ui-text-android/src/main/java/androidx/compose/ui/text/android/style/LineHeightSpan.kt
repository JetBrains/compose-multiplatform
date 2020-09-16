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
package androidx.compose.ui.text.android.style

import android.graphics.Paint.FontMetricsInt
import androidx.compose.ui.text.android.InternalPlatformTextApi

/**
 * The span which modifies the height of the covered paragraphs. A paragraph is defined as a
 * segment of string divided by '\n' character. To make sure the span work as expected, the
 * boundary of this span should align with paragraph boundary.
 * @constructor Create a LineHeightSpan which sets the line height to `height` physical pixels.
 * @param lineHeight The specified line height in pixel unit, which is the space between the
 * baseline of adjacent lines.
 *
 * @suppress
 */
@InternalPlatformTextApi
class LineHeightSpan(val lineHeight: Int) : android.text.style.LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartVertical: Int,
        lineHeight: Int,
        fontMetricsInt: FontMetricsInt
    ) { // In StaticLayout, line height is computed with descent - ascent
        val currentHeight = fontMetricsInt.descent - fontMetricsInt.ascent
        // If current height is not positive, do nothing.
        if (currentHeight <= 0) {
            return
        }
        val ratio = this.lineHeight * 1.0f / currentHeight
        fontMetricsInt.descent =
            Math.ceil(fontMetricsInt.descent * ratio.toDouble()).toInt()
        fontMetricsInt.ascent = fontMetricsInt.descent - this.lineHeight
    }
}