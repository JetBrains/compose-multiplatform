/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.platform.style

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A span that applies [ShaderBrush] to TextPaint after receiving a specified size
 */
internal class DrawStyleSpan(
    val drawStyle: DrawStyle
) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(textPaint: TextPaint?) {
        textPaint?.run {
            when (drawStyle) {
                Fill -> style = Paint.Style.FILL
                is Stroke -> {
                    style = Paint.Style.STROKE
                    strokeWidth = drawStyle.width
                    strokeMiter = drawStyle.miter
                    strokeJoin = drawStyle.join.toAndroidJoin()
                    strokeCap = drawStyle.cap.toAndroidCap()
                    pathEffect = drawStyle.pathEffect?.asAndroidPathEffect()
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
}
