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

import android.text.TextPaint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.modulate
import kotlin.math.roundToInt

internal class AndroidTextPaint(flags: Int, density: Float) : TextPaint(flags) {
    init {
        this.density = density
    }
    private var textDecoration: TextDecoration = TextDecoration.None
    private var shadow: Shadow = Shadow.None
    private var brush: Brush? = null
    private var brushSize: Size? = null

    fun setTextDecoration(textDecoration: TextDecoration?) {
        val tmpTextDecoration = textDecoration ?: TextDecoration.None
        if (this.textDecoration != tmpTextDecoration) {
            this.textDecoration = tmpTextDecoration
            isUnderlineText = TextDecoration.Underline in this.textDecoration
            isStrikeThruText = TextDecoration.LineThrough in this.textDecoration
        }
    }

    fun setShadow(shadow: Shadow?) {
        val tmpShadow = shadow ?: Shadow.None
        if (this.shadow != tmpShadow) {
            this.shadow = tmpShadow
            if (this.shadow == Shadow.None) {
                clearShadowLayer()
            } else {
                setShadowLayer(
                    this.shadow.blurRadius,
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
                this.shader = null
                this.brush = null
                this.brushSize = null
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