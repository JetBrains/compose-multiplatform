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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp

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
fun Modifier.border(width: Dp, brush: Brush, shape: Shape): Modifier = composed(
    factory = {
        this.then(
            Modifier.drawWithCache {
                val outline: Outline = shape.createOutline(size, layoutDirection, this)
                val borderSize = if (width == Dp.Hairline) 1f else width.toPx()

                var insetOutline: Outline? = null // outline used for roundrect/generic shapes
                var stroke: Stroke? = null // stroke to draw border for all outline types
                var pathClip: Path? = null // path to clip roundrect/generic shapes
                var inset = 0f // inset to translate before drawing the inset outline
                // path to draw generic shapes or roundrects with different corner radii
                var insetPath: Path? = null

                if (borderSize > 0 && size.minDimension > 0f) {
                    if (outline is Outline.Rectangle) {
                        stroke = Stroke(borderSize)
                    } else {
                        // Multiplier to apply to the border size to get a stroke width that is
                        // large enough to cover the corners while not being too large to overly
                        // square off the internal shape. The resultant shape will be
                        // clipped to the desired shape. Any value lower will show artifacts in
                        // the corners of shapes. A value too large will always square off
                        // the internal shape corners. For example, for a rounded rect border
                        // a large multiplier will always have squared off edges within the
                        // inner section of the stroke, however, having a smaller multiplier
                        // will still keep the rounded effect for the inner section of the
                        // border
                        val strokeWidth = 1.2f * borderSize
                        inset = borderSize - strokeWidth / 2
                        val insetSize = Size(
                            size.width - inset * 2,
                            size.height - inset * 2
                        )
                        insetOutline = shape.createOutline(insetSize, layoutDirection, this)
                        stroke = Stroke(strokeWidth)
                        pathClip = if (outline is Outline.Rounded) {
                            Path().apply { addRoundRect(outline.roundRect) }
                        } else if (outline is Outline.Generic) {
                            outline.path
                        } else {
                            // should not get here because we check for Outline.Rectangle
                            // above
                            null
                        }

                        insetPath =
                            if (insetOutline is Outline.Rounded &&
                                !insetOutline.roundRect.isSimple
                            ) {
                                // Rounded rect with non equal corner radii needs a path
                                // to be pre-translated
                                Path().apply {
                                    addRoundRect(insetOutline.roundRect)
                                    translate(Offset(inset, inset))
                                }
                            } else if (insetOutline is Outline.Generic) {
                                // Generic paths must be created and pre-translated
                                Path().apply {
                                    addPath(insetOutline.path, Offset(inset, inset))
                                }
                            } else {
                                // Drawing a round rect with equal corner radii without
                                // usage of a path
                                null
                            }
                    }
                }

                onDrawWithContent {
                    drawContent()
                    // Only draw the border if a have a valid stroke parameter. If we have
                    // an invalid border size we will just draw the content
                    if (stroke != null) {
                        if (insetOutline != null && pathClip != null) {
                            val isSimpleRoundRect = insetOutline is Outline.Rounded &&
                                insetOutline.roundRect.isSimple
                            withTransform({
                                clipPath(pathClip)
                                // we are drawing the round rect not as a path so we must
                                // translate ourselves othe
                                if (isSimpleRoundRect) {
                                    translate(inset, inset)
                                }
                            }) {
                                if (isSimpleRoundRect) {
                                    // If we don't have an insetPath then we are drawing
                                    // a simple round rect with the corner radii all identical
                                    val rrect = (insetOutline as Outline.Rounded).roundRect
                                    drawRoundRect(
                                        brush = brush,
                                        topLeft = Offset(rrect.left, rrect.top),
                                        size = Size(rrect.width, rrect.height),
                                        cornerRadius = rrect.topLeftCornerRadius,
                                        style = stroke
                                    )
                                } else if (insetPath != null) {
                                    drawPath(insetPath, brush, style = stroke)
                                }
                            }
                            // Clip rect to ensure the stroke does not extend the bounds
                            // of the composable.
                            clipRect {
                                // Draw a hairline stroke to cover up non-anti-aliased pixels
                                // generated from the clip
                                if (isSimpleRoundRect) {
                                    val rrect = (outline as Outline.Rounded).roundRect
                                    drawRoundRect(
                                        brush = brush,
                                        topLeft = Offset(rrect.left, rrect.top),
                                        size = Size(rrect.width, rrect.height),
                                        cornerRadius = rrect.topLeftCornerRadius,
                                        style = HairlineBorderStroke
                                    )
                                } else {
                                    drawPath(pathClip, brush = brush, style = HairlineBorderStroke)
                                }
                            }
                        } else {
                            // Rectangular border fast path
                            val strokeWidth = stroke.width
                            val halfStrokeWidth = strokeWidth / 2
                            drawRect(
                                brush = brush,
                                topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
                                size = Size(
                                    size.width - strokeWidth,
                                    size.height - strokeWidth
                                ),
                                style = stroke
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

// Hairline stroke to cover aliasing of clipping
private val HairlineBorderStroke = Stroke(Stroke.HairlineWidth)