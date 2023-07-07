/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

// Copied from AndroidTextPaint.

internal fun Paint.applyBrush(brush: Brush?, size: Size, alpha: Float = Float.NaN) {
    // if size is unspecified and brush is not null, nothing should be done.
    // it basically means brush is given but size is not yet calculated at this time.
    if ((brush is SolidColor && brush.value.isSpecified) ||
        (brush is ShaderBrush && size.isSpecified)) {
        // alpha is always applied even if Float.NaN is passed to applyTo function.
        // if it's actually Float.NaN, we simply send the current value
        brush.applyTo(
            size,
            this,
            if (alpha.isNaN()) 1f else alpha.coerceIn(0f, 1f)
        )
    } else if (brush == null) {
        shader = null
    }
}

internal fun Paint.applyDrawStyle(drawStyle: DrawStyle?) {
    when (drawStyle) {
        Fill, null -> {
            // Stroke properties such as strokeWidth, strokeMiter are not re-set because
            // Fill style should make those properties no-op. Next time the style is set
            // as Stroke, stroke properties get re-set as well.
            style = PaintingStyle.Fill
        }
        is Stroke -> {
            style = PaintingStyle.Stroke
            strokeWidth = drawStyle.width
            strokeMiterLimit = drawStyle.miter
            strokeJoin = drawStyle.join
            strokeCap = drawStyle.cap
            pathEffect = drawStyle.pathEffect
        }
    }
}