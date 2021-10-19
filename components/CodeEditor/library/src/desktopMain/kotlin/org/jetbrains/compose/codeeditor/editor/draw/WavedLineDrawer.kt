package org.jetbrains.compose.codeeditor.editor.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

internal class WavedLineDrawer(
    private val lineColor: Color,
    zIndex: Int
) : HighlightDrawer(zIndex) {

    private val paint = Paint().apply {
        color = lineColor
        isAntiAlias = false
        filterQuality = FilterQuality.None
        style = PaintingStyle.Stroke
        strokeWidth = 1f
        pathEffect = wavedPathEffect
    }

    override fun draw(lineSegments: List<LineSegment>, drawScope: DrawScope) {
        drawScope.drawIntoCanvas { canvas ->
            lineSegments.forEach { (xl, xr, y) ->
                canvas.save()
                val ny = y + lineBottomPadding
                canvas.clipRect(xl, ny, xr, ny + wavedLineHeight)
                canvas.drawLine(
                    Offset(xl - lineHorizontalPadding, ny),
                    Offset(xr + lineHorizontalPadding, ny),
                    paint
                )
                canvas.restore()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WavedLineDrawer

        if (lineColor != other.lineColor) return false

        return true
    }

    override fun hashCode(): Int {
        return lineColor.hashCode()
    }

    companion object {
        private const val lineHorizontalPadding = 3f
        private const val lineBottomPadding = -1f
        private const val wavedLineHeight = 3f

        private val wavedPath = Path().apply {
            lineTo(wavedLineHeight, wavedLineHeight)
            lineTo(wavedLineHeight * 2, 0f)
            lineTo(wavedLineHeight * 2 - 1, 0f)
            lineTo(wavedLineHeight, wavedLineHeight - 1)
            lineTo(1f, 0f)
            close()
        }

        private val wavedPathEffect = PathEffect.stampedPathEffect(
            shape = wavedPath,
            advance = wavedLineHeight * 2 - 2,
            phase = 0f,
            style = StampedPathEffectStyle.Translate
        )
    }
}
