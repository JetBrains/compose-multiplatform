/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.background(
    color: Color,
    shape: Shape = RectangleShape
) = this.then(
    Background(
        color = color,
        shape = shape,
        inspectorInfo = debugInspectorInfo {
            name = "background"
            value = color
            properties["color"] = color
            properties["shape"] = shape
        }
    )
)

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 * @param alpha Opacity to be applied to the [brush], with `0` being completely transparent and
 * `1` being completely opaque. The value must be between `0` and `1`.
 */
fun Modifier.background(
    brush: Brush,
    shape: Shape = RectangleShape,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    alpha: Float = 1.0f
) = this.then(
    Background(
        brush = brush,
        alpha = alpha,
        shape = shape,
        inspectorInfo = debugInspectorInfo {
            name = "background"
            properties["alpha"] = alpha
            properties["brush"] = brush
            properties["shape"] = shape
        }
    )
)

private class Background constructor(
    private val color: Color? = null,
    private val brush: Brush? = null,
    private val alpha: Float = 1.0f,
    private val shape: Shape,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {

    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            drawOutline()
        }
        drawContent()
    }

    private fun ContentDrawScope.drawRect() {
        color?.let { drawRect(color = it) }
        brush?.let { drawRect(brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.drawOutline() {
        val outline =
            if (size == lastSize && layoutDirection == lastLayoutDirection) {
                lastOutline!!
            } else {
                shape.createOutline(size, layoutDirection, this)
            }
        color?.let { drawOutline(outline, color = color) }
        brush?.let { drawOutline(outline, brush = brush, alpha = alpha) }
        lastOutline = outline
        lastSize = size
    }

    override fun hashCode(): Int {
        var result = color?.hashCode() ?: 0
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + alpha.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? Background ?: return false
        return color == otherModifier.color &&
            brush == otherModifier.brush &&
            alpha == otherModifier.alpha &&
            shape == otherModifier.shape
    }

    override fun toString(): String =
        "Background(color=$color, brush=$brush, alpha = $alpha, shape=$shape)"
}