package org.jetbrains.compose.codeeditor.editor.draw

import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

internal class BackgroundDrawer(
    private val backgroundColor: Color? = null,
    private val borderColor: Color? = null,
    private val height: State<Float>,
    zIndex: Int
) : HighlightDrawer(zIndex) {

    private val paintFill = backgroundColor?.let {
        Paint().apply {
            color = backgroundColor
            isAntiAlias = false
            filterQuality = FilterQuality.None
        }
    }

    private val paintBorder = borderColor?.let {
        Paint().apply {
            color = borderColor
            style = PaintingStyle.Stroke
            strokeWidth = 1f
            isAntiAlias = false
            filterQuality = FilterQuality.None
        }
    }

    override fun draw(lineSegments: List<LineSegment>, drawScope: DrawScope) {
        drawScope.drawIntoCanvas { canvas ->
            lineSegments.forEach { (xl, xr, y) ->
                paintFill?.let {
                    canvas.drawRect(xl, y - height.value, xr, y, paintFill)
                }
                paintBorder?.let {
                    canvas.drawRect(xl - strokeShift, y - height.value - strokeShift,
                        xr + strokeShift, y + strokeShift, paintBorder)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackgroundDrawer

        if (backgroundColor != other.backgroundColor) return false
        if (borderColor != other.borderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor?.hashCode() ?: 0
        result = 31 * result + (borderColor?.hashCode() ?: 0)
        return result
    }

    companion object {
        private const val strokeShift = .5f
    }
}
