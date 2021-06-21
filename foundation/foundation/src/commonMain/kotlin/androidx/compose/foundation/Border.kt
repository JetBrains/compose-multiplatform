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

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isRect
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Modify element to add border with appearance specified with a [border] and a [shape] and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSample()
 *
 * @param border [BorderStroke] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape) =
    border(width = border.width, brush = border.brush, shape = shape)

/**
 * Modify element to add border with appearance specified with a [width], a [color] and a [shape]
 * and clip it.
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
 * Modify element to add border with appearance specified with a [width], a [brush] and a [shape]
 * and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithBrush()
 *
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
// TODO: b/191017532 remove Modifier.composed
@Suppress("UnnecessaryComposedModifier")
fun Modifier.border(width: Dp, brush: Brush, shape: Shape): Modifier = composed(
    factory = {
        this.then(
            Modifier.drawWithCache {
                val hasValidBorderParams = width.toPx() > 0f && size.minDimension > 0f
                val outline = shape.createOutline(size, layoutDirection, this)
                val strokeWidthPx = min(
                    if (width == Dp.Hairline) 1f else width.toPx(), ceil(size.minDimension / 2)
                )
                val borderStroke = Stroke(strokeWidthPx)
                val halfStroke = strokeWidthPx / 2f
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val borderPath: Path? = if (outline is Outline.Generic) {
                    createBorderPath(outline, strokeWidthPx, shape)
                } else if (outline is Outline.Rounded && !outline.roundRect.isSimple) {
                    createRoundRectPath(outline.roundRect, strokeWidthPx)
                } else {
                    null // Either a simple rect or rounded rect, no need for path operations
                }

                val isRect = outline is Outline.Rectangle ||
                    (outline is Outline.Rounded && outline.roundRect.isRect)

                val isSimpleRoundRect: Boolean
                val cornerRadius: CornerRadius
                if (outline is Outline.Rounded && outline.roundRect.isSimple) {
                    isSimpleRoundRect = true
                    cornerRadius = outline.roundRect.topLeftCornerRadius
                } else {
                    isSimpleRoundRect = false
                    cornerRadius = CornerRadius.Zero
                }
                // The stroke is larger than the drawing area so just draw a full shape instead
                val fillArea = (strokeWidthPx * 2) > size.minDimension
                onDrawWithContent {
                    drawContent()
                    if (hasValidBorderParams && borderPath != null) {
                        // If we have a path, that means we are drawing either a generic shape
                        // or a rounded rect with different corner radii across the 4 corners
                        drawPath(borderPath, brush)
                    } else if (hasValidBorderParams && isRect) {
                        // If we are drawing a rectangular stroke, just offset it by half the stroke
                        // width as strokes are always drawn centered on their geometry.
                        // If the border is larger than the drawing area, just fill the area with a
                        // solid rectangle
                        drawRect(
                            brush = brush,
                            topLeft = if (fillArea) Offset.Zero else topLeft,
                            size = if (fillArea) size else borderSize,
                            style = if (fillArea) Fill else borderStroke
                        )
                    } else if (hasValidBorderParams && isSimpleRoundRect) {
                        if (fillArea) {
                            // If the drawing area is smaller than the stroke being drawn
                            // drawn all around it just draw a filled in rounded rect
                            drawRoundRect(brush, cornerRadius = cornerRadius)
                        } else if (cornerRadius.x < halfStroke) {
                            // If the corner radius is smaller than half of the stroke width
                            // then the interior curvature of the stroke will be a sharp edge
                            // In this case just draw a normal filled in rounded rect with the
                            // desired corner radius but clipping out the interior rectangle
                            clipRect(
                                strokeWidthPx,
                                strokeWidthPx,
                                size.width - strokeWidthPx,
                                size.height - strokeWidthPx,
                                clipOp = ClipOp.Difference
                            ) {
                                drawRoundRect(brush, cornerRadius = cornerRadius)
                            }
                        } else {
                            // Otherwise draw a stroked rounded rect with the corner radius
                            // shrunk by half of the stroke width. This will ensure that the
                            // outer curvature of the rounded rectangle will have the desired
                            // corner radius.
                            drawRoundRect(
                                brush = brush,
                                topLeft = topLeft,
                                size = borderSize,
                                cornerRadius = cornerRadius.shrink(halfStroke),
                                style = borderStroke
                            )
                        }
                    }
                }
            }
        )
    },
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
 * Helper method to create a path with the inner section removed from it based on the
 * stroke size
 */
private fun CacheDrawScope.createBorderPath(outline: Outline, widthPx: Float, shape: Shape): Path =
    // We already have a generic shape that leverages path so create another path that is subtracted
    // from the center that is smaller than the given path by 2 times the stroke width
    Path().apply {
        addOutline(outline)
        // If the stroke width is large enough to fully occupy the bounds we are drawing
        // in, just return the outline path itself, otherwise subtract off a smaller path
        // from the outline minus the stroke width
        if (widthPx * 2 < size.minDimension) {
            val insetSize = Size(size.width - widthPx * 2, size.height - widthPx * 2)
            val insetPath = Path().apply {
                addOutline(shape.createOutline(insetSize, layoutDirection, this@createBorderPath))
                translate(Offset(widthPx, widthPx))
            }
            op(this, insetPath, PathOperation.Difference)
        }
    }

private fun CacheDrawScope.createRoundRectPath(
    roundedRect: RoundRect,
    strokeWidth: Float
): Path =
    Path().apply {
        addRoundRect(roundedRect)
        if (strokeWidth * 2 < size.minDimension) {
            val insetPath = Path().apply {
                addRoundRect(createInsetRoundedRect(strokeWidth, roundedRect))
            }
            op(this, insetPath, PathOperation.Difference)
        }
    }

private fun createInsetRoundedRect(
    widthPx: Float,
    roundedRect: RoundRect
) = RoundRect(
    left = widthPx,
    top = widthPx,
    right = roundedRect.width - widthPx,
    bottom = roundedRect.height - widthPx,
    topLeftCornerRadius = roundedRect.topLeftCornerRadius.shrink(widthPx),
    topRightCornerRadius = roundedRect.topRightCornerRadius.shrink(widthPx),
    bottomLeftCornerRadius = roundedRect.bottomLeftCornerRadius.shrink(widthPx),
    bottomRightCornerRadius = roundedRect.bottomRightCornerRadius.shrink(widthPx)
)

/**
 * Helper method to shrink the corner radius by the given value, clamping to 0
 * if the resultant corner radius would be negative
 */
private fun CornerRadius.shrink(value: Float): CornerRadius = CornerRadius(
    max(0f, this.x - value),
    max(0f, this.y - value)
)
