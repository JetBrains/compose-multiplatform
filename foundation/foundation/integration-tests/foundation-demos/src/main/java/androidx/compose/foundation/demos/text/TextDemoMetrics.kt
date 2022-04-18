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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.demos.text.TextMetricHelper.Alignment.Center
import androidx.compose.foundation.demos.text.TextMetricHelper.Alignment.Left
import androidx.compose.foundation.demos.text.TextMetricHelper.Alignment.Right
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize

@Composable
internal fun TextWithMetrics(
    text: AnnotatedString,
    style: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean = true,
    colors: TextMetricColors? = null
) {
    val textLayout = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        modifier = Modifier.drawTextMetrics(textLayout.value, colors).background(Color.LightGray),
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = {
            textLayout.value = it
        },
        softWrap = softWrap
    )
}

internal class TextMetricColors(
    val background: Color = WinterDoldrums,
    val text: Color = BlackInk,
    val top: Color = MadMagenta,
    val bottom: Color = MandarinOrange,
    val ascent: Color = BlueBlue,
    val descent: Color = YellowYellow,
    val baseline: Color = RedRed,
    val border: Color = Silver,
    val leftRight: Color = CherryTomato
) {
    companion object {
        private val WinterDoldrums = Color(0xfff5f2eb)
        private val BlackInk = Color(0xff44413c)
        private val MadMagenta = Color(0xffce5ec9)
        private val CherryTomato = Color(0xffba2710)
        private val MandarinOrange = Color(0xffff7800)
        private val Silver = Color(0xffbdbdbd)
        private val RedRed = Color(0xffff1744)
        private val YellowYellow = Color(0xffffeb3b)
        private val BlueBlue = Color(0xff2962ff)

        val Default = TextMetricColors()
    }
}

internal fun Modifier.drawTextMetrics(
    textLayoutResult: TextLayoutResult?,
    colors: TextMetricColors?
) = composed {
    val thickness = with(LocalDensity.current) { 1.dp.toPx() }
    val textSize = with(LocalDensity.current) { 12.sp.toPx() }
    val localColors = colors ?: TextMetricColors.Default
    drawWithContent {
        drawContent()
        TextMetricHelper(thickness, textSize, localColors, this).drawTextLayout(textLayoutResult)
    }
}

private class TextMetricHelper(
    val thickness: Float,
    val labelSize: Float,
    val colors: TextMetricColors = TextMetricColors.Default,
    drawScope: DrawScope
) : DrawScope by drawScope {

    private enum class Alignment { Left, Right, Center }

    private val pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    private val overflow = 3 * thickness
    private val textPaint = android.graphics.Paint().apply {
        textSize = labelSize
        setShadowLayer(Float.MIN_VALUE, 4f, 4f, android.graphics.Color.WHITE)
    }

    fun drawTextLayout(textLayout: TextLayoutResult?) {
        if (textLayout == null) return
        val size = textLayout.size.toSize()
        val layoutStart = 0f
        val layoutEnd = size.width
        val x1 = layoutStart
        val x2 = layoutEnd
        val textOffset = labelSize
        drawRect(colors.border, topLeft = Offset.Zero, size = size, style = Stroke(thickness))
        for (lineIndex in 0 until textLayout.lineCount) {
            val lineTop = textLayout.getLineTop(lineIndex)
            val lineBottom = textLayout.getLineBottom(lineIndex)
            horizontal(colors.top, x1, x2, lineTop, "T$lineIndex", Center, -textOffset)
            horizontal(colors.bottom, x1, x2, lineBottom, "B$lineIndex", Center, textOffset)
            vertical(colors.leftRight, textLayout.getLineLeft(lineIndex), lineTop, lineBottom)
            vertical(colors.leftRight, textLayout.getLineRight(lineIndex), lineTop, lineBottom)
        }
    }

    private fun horizontal(
        color: Color,
        startX: Float,
        endX: Float,
        y: Float,
        text: String = "",
        alignment: Alignment = Left,
        textOffset: Float = 0f
    ) {
        drawLine(
            color = color,
            start = Offset(startX - overflow, y),
            end = Offset(endX + overflow, y),
            strokeWidth = thickness,
            pathEffect = pathEffect
        )
        val x = when (alignment) {
            Left -> startX + textOffset
            Right -> endX - labelSize - textOffset
            Center -> startX + (endX - startX) / 2f + textOffset
        }

        if (text.isNotBlank()) {
            text(text, color, x, y)
        }
    }

    private fun vertical(color: Color, x: Float, startY: Float, endY: Float) {
        drawLine(
            color = color,
            start = Offset(x, startY - overflow),
            end = Offset(x, endY + overflow),
            strokeWidth = thickness,
            pathEffect = pathEffect
        )
    }

    private fun text(text: String, color: Color, x: Float, y: Float) {
        textPaint.color = color.toArgb()
        drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)
    }
}
