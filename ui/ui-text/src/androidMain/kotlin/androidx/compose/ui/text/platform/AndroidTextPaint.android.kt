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

package androidx.compose.ui.text.platform

import android.graphics.Paint
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.platform.extensions.correctBlurRadius
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.modulate
import kotlin.math.roundToInt

internal class AndroidTextPaint(flags: Int, density: Float) : TextPaint(flags) {
    init {
        this.density = density
    }

    private var textDecoration: TextDecoration = TextDecoration.None
    private var shadow: Shadow = Shadow.None

    @VisibleForTesting
    internal var brush: Brush? = null

    @VisibleForTesting
    internal var brushSize: Size? = null
    private var drawStyle: DrawStyle? = null

    fun setTextDecoration(textDecoration: TextDecoration?) {
        if (textDecoration == null) return
        if (this.textDecoration != textDecoration) {
            this.textDecoration = textDecoration
            isUnderlineText = TextDecoration.Underline in this.textDecoration
            isStrikeThruText = TextDecoration.LineThrough in this.textDecoration
        }
    }

    fun setShadow(shadow: Shadow?) {
        if (shadow == null) return
        if (this.shadow != shadow) {
            this.shadow = shadow
            if (this.shadow == Shadow.None) {
                clearShadowLayer()
            } else {
                setShadowLayer(
                    correctBlurRadius(this.shadow.blurRadius),
                    this.shadow.offset.x,
                    this.shadow.offset.y,
                    this.shadow.color.toArgb()
                )
            }
        }
    }

    fun setColor(color: Color) {
        if (color.isSpecified) {
            val argbColor = color.toArgb()
            if (this.color != argbColor) {
                this.color = argbColor
            }
            this.shader = null
            this.brush = null
            this.brushSize = null
        }
    }

    fun setBrush(brush: Brush?, size: Size, alpha: Float = Float.NaN) {
        when (brush) {
            null -> {
                this.shader = null
                this.brush = null
                this.brushSize = null
            }
            is SolidColor -> {
                setColor(brush.value.modulate(alpha))
            }
            is ShaderBrush -> {
                if (this.shader == null || this.brush != brush || this.brushSize != size) {
                    if (size.isSpecified) {
                        this.brush = brush
                        this.brushSize = size
                        this.shader = brush.createShader(size)
                    }
                }
                setAlpha(alpha)
            }
        }
    }

    fun setDrawStyle(drawStyle: DrawStyle?) {
        if (drawStyle == null) return
        if (this.drawStyle != drawStyle) {
            this.drawStyle = drawStyle
            when (drawStyle) {
                Fill -> {
                    // Stroke properties such as strokeWidth, strokeMiter are not re-set because
                    // Fill style should make those properties no-op. Next time the style is set
                    // as Stroke, stroke properties get re-set as well.
                    style = Style.FILL
                }
                is Stroke -> {
                    style = Style.STROKE
                    strokeWidth = drawStyle.width
                    strokeMiter = drawStyle.miter
                    strokeJoin = drawStyle.join.toAndroidJoin()
                    strokeCap = drawStyle.cap.toAndroidCap()
                    pathEffect = drawStyle.pathEffect?.asAndroidPathEffect()
                }
            }
        }
    }
}

private fun StrokeJoin.toAndroidJoin(): Paint.Join {
    return when (this) {
        StrokeJoin.Miter -> Paint.Join.MITER
        StrokeJoin.Round -> Paint.Join.ROUND
        StrokeJoin.Bevel -> Paint.Join.BEVEL
        else -> Paint.Join.MITER
    }
}

private fun StrokeCap.toAndroidCap(): Paint.Cap {
    return when (this) {
        StrokeCap.Butt -> Paint.Cap.BUTT
        StrokeCap.Round -> Paint.Cap.ROUND
        StrokeCap.Square -> Paint.Cap.SQUARE
        else -> Paint.Cap.BUTT
    }
}

/**
 * Accepts an alpha value in the range [0f, 1f] then maps to an integer value
 * in [0, 255] range.
 */
internal fun TextPaint.setAlpha(alpha: Float) {
    if (!alpha.isNaN()) {
        val alphaInt = alpha.coerceIn(0f, 1f).times(255).roundToInt()
        setAlpha(alphaInt)
    }
}
