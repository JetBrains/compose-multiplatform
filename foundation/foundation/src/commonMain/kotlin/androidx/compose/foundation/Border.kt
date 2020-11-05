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

package androidx.compose.foundation

import androidx.compose.runtime.remember
import androidx.compose.ui.ContentDrawScope
import androidx.compose.ui.DrawModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.boundingRect
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.nativeClass

/**
 * Modify element to add border with appearance specified with a [border] and a [shape], pad the
 * content by the [BorderStroke.width] and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSample()
 *
 * @param border [Border] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape) =
    border(width = border.width, brush = border.brush, shape = shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [width], [color] and a
 * [shape], pads the content by the [width] and clips it.
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithDataClass()
 *
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param color color to paint the border with
 * @param shape shape of the border
 */
fun Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape) =
    border(width, SolidColor(color), shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [width], [brush] and a
 * [shape], pads the content by the [width] and clips it.
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithBrush()
 *
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
fun Modifier.border(width: Dp, brush: Brush, shape: Shape): Modifier = composed(
    factory = { BorderModifier(remember { BorderModifierCache() }, shape, width, brush) },
    inspectorInfo = debugInspectorInfo {
        name = "border"
        properties["width"] = width
        if (brush is SolidColor) {
            properties["color"] = brush.value
            value = brush.value
        } else {
            properties["brush"] = brush
        }
        properties["shape"] = shape
    }
)

/**
 * Returns a [Modifier] that adds border with appearance specified with a [border] and a [shape]
 *
 * @sample androidx.compose.foundation.samples.BorderSample()
 *
 * @param border [Border] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
@Deprecated(
    "Use Modifier.border instead",
    replaceWith = ReplaceWith(
        "this.border(BorderStroke(border.size, border.brush), shape)",
        "androidx.ui.foundation.border"
    )
)
@Suppress("DEPRECATION")
fun Modifier.drawBorder(border: Border, shape: Shape = RectangleShape) =
    drawBorder(size = border.size, brush = border.brush, shape = shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [size], [color] and a
 * [shape]
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithDataClass()
 *
 * @param size width of the border. Use [Dp.Hairline] for a hairline border.
 * @param color color to paint the border with
 * @param shape shape of the border
 */
@Deprecated(
    "Use Modifier.border instead",
    replaceWith = ReplaceWith(
        "this.border(size, color, shape)",
        "androidx.ui.foundation.border"
    )
)
@Suppress("DEPRECATION")
fun Modifier.drawBorder(size: Dp, color: Color, shape: Shape = RectangleShape) =
    border(size, SolidColor(color), shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [size], [brush] and a
 * [shape]
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithBrush()
 *
 * @param size width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
@Deprecated(
    "Use Modifier.border instead",
    replaceWith = ReplaceWith(
        "this.border(size, brush, shape)",
        "androidx.ui.foundation.border"
    )
)
fun Modifier.drawBorder(size: Dp, brush: Brush, shape: Shape): Modifier = composed(
    factory = { BorderModifier(remember { BorderModifierCache() }, shape, size, brush) },
    inspectorInfo = debugInspectorInfo {
        name = "drawBorder"
        properties["size"] = size
        properties["brush"] = brush
        properties["shape"] = shape
    }
)

private class BorderModifier(
    private val cache: BorderModifierCache,
    private val shape: Shape,
    private val borderWidth: Dp,
    private val brush: Brush
) : DrawModifier {

    // put params to constructor to ensure proper equals and update cache after construction
    init {
        cache.lastShape = shape
        cache.borderSize = borderWidth
    }

    override fun ContentDrawScope.draw() {
        val density = this
        with(cache) {
            drawContent()
            modifierSize = size
            val outline = modifierSizeOutline(density)
            val borderSize =
                if (borderWidth == Dp.Hairline) 1f else borderWidth.value * density.density
            if (borderSize <= 0 || size.minDimension <= 0.0f) {
                return
            } else if (outline is Outline.Rectangle) {
                // shortcut to make rectangle shapes draw faster
                drawRoundRectBorder(borderSize, outline.rect, 0f, brush)
            } else if (outline is Outline.Rounded && outline.roundRect.isSimple) {
                // shortcut to make rounded rectangles draw faster
                val radius = outline.roundRect.bottomLeftCornerRadius.y
                drawRoundRectBorder(
                    borderSize,
                    outline.roundRect.boundingRect,
                    radius,
                    brush
                )
            } else {
                // general path-difference (rect-difference) implementation for the rest
                drawPath(borderPath(density, borderSize), brush)
            }
        }
    }

    private fun DrawScope.drawRoundRectBorder(
        borderSize: Float,
        rect: Rect,
        radius: Float,
        brush: Brush
    ) {
        val fillWithBorder = borderSize * 2 >= rect.minDimension
        val style = if (fillWithBorder) Fill else Stroke(borderSize)

        val delta = if (fillWithBorder) 0f else borderSize / 2
        drawRoundRect(
            brush,
            topLeft = Offset(rect.left + delta, rect.top + delta),
            size = Size(rect.width - 2 * delta, rect.height - 2 * delta),
            cornerRadius = CornerRadius(radius),
            style = style
        )
    }

    // cannot make DrawBorder data class because of the cache, though need hashcode/equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this.nativeClass() != other?.nativeClass()) return false

        other as BorderModifier

        if (shape != other.shape) return false
        if (borderWidth != other.borderWidth) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }
}

private class BorderModifierCache {
    private val outerPath = Path()
    private val innerPath = Path()
    private val diffPath = Path()
    private var dirtyPath = true
    private var dirtyOutline = true
    private var outline: Outline? = null

    var lastShape: Shape? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
                dirtyOutline = true
            }
        }

    var borderSize: Dp = Dp.Unspecified
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
            }
        }

    var modifierSize: Size? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
                dirtyOutline = true
            }
        }

    fun modifierSizeOutline(density: Density): Outline {
        if (dirtyOutline) {
            outline = lastShape?.createOutline(modifierSize!!, density)
            dirtyOutline = false
        }
        return outline!!
    }

    fun borderPath(density: Density, borderPixelSize: Float): Path {
        if (dirtyPath) {
            val size = modifierSize!!
            diffPath.reset()
            outerPath.reset()
            innerPath.reset()
            if (borderPixelSize * 2 >= size.minDimension) {
                diffPath.addOutline(modifierSizeOutline(density))
            } else {
                outerPath.addOutline(lastShape!!.createOutline(size, density))
                val sizeMinusBorder =
                    Size(
                        size.width - borderPixelSize * 2,
                        size.height - borderPixelSize * 2
                    )
                innerPath.addOutline(lastShape!!.createOutline(sizeMinusBorder, density))
                innerPath.translate(Offset(borderPixelSize, borderPixelSize))

                // now we calculate the diff between the inner and the outer paths
                diffPath.op(outerPath, innerPath, PathOperation.difference)
            }
            dirtyPath = false
        }
        return diffPath
    }
}