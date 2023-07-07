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

package androidx.compose.foundation.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A shape describing the rectangle with cut corners.
 * Corner size is representing the cut length - the size of both legs of the cut's right triangle.
 *
 * This shape will not automatically mirror the corner sizes in [LayoutDirection.Rtl], use
 * [CutCornerShape] for the layout direction aware version of this shape.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom right corner
 * @param bottomLeft a size of the bottom left corner
 */
class AbsoluteCutCornerShape(
    topLeft: CornerSize,
    topRight: CornerSize,
    bottomRight: CornerSize,
    bottomLeft: CornerSize
) : CornerBasedShape(
    topStart = topLeft,
    topEnd = topRight,
    bottomEnd = bottomRight,
    bottomStart = bottomLeft
) {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ) = if (topStart + topEnd + bottomStart + bottomEnd == 0.0f) {
        Outline.Rectangle(size.toRect())
    } else Outline.Generic(
        Path().apply {
            var cornerSize = topStart
            moveTo(0f, cornerSize)
            lineTo(cornerSize, 0f)
            cornerSize = topEnd
            lineTo(size.width - cornerSize, 0f)
            lineTo(size.width, cornerSize)
            cornerSize = bottomEnd
            lineTo(size.width, size.height - cornerSize)
            lineTo(size.width - cornerSize, size.height)
            cornerSize = bottomStart
            lineTo(cornerSize, size.height)
            lineTo(0f, size.height - cornerSize)
            close()
        }
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ) = AbsoluteCutCornerShape(
        topLeft = topStart,
        topRight = topEnd,
        bottomRight = bottomEnd,
        bottomLeft = bottomStart
    )

    override fun toString(): String {
        return "AbsoluteCutCornerShape(topLeft = $topStart, topRight = $topEnd, bottomRight = " +
            "$bottomEnd, bottomLeft = $bottomStart)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbsoluteCutCornerShape) return false

        if (topStart != other.topStart) return false
        if (topEnd != other.topEnd) return false
        if (bottomEnd != other.bottomEnd) return false
        if (bottomStart != other.bottomStart) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }
}

/**
 * Creates [AbsoluteCutCornerShape] with the same size applied for all four corners.
 * @param corner [CornerSize] to apply.
 */
fun AbsoluteCutCornerShape(corner: CornerSize) =
    AbsoluteCutCornerShape(corner, corner, corner, corner)

/**
 * Creates [AbsoluteCutCornerShape] with the same size applied for all four corners.
 * @param size Size in [Dp] to apply.
 */
fun AbsoluteCutCornerShape(size: Dp) = AbsoluteCutCornerShape(CornerSize(size))

/**
 * Creates [AbsoluteCutCornerShape] with the same size applied for all four corners.
 * @param size Size in pixels to apply.
 */
fun AbsoluteCutCornerShape(size: Float) = AbsoluteCutCornerShape(CornerSize(size))

/**
 * Creates [AbsoluteCutCornerShape] with the same size applied for all four corners.
 * @param percent Size in percents to apply.
 */
fun AbsoluteCutCornerShape(percent: Int) = AbsoluteCutCornerShape(CornerSize(percent))

/**
 * Creates [AbsoluteCutCornerShape] with sizes defined in [Dp].
 */
fun AbsoluteCutCornerShape(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = AbsoluteCutCornerShape(
    topLeft = CornerSize(topLeft),
    topRight = CornerSize(topRight),
    bottomRight = CornerSize(bottomRight),
    bottomLeft = CornerSize(bottomLeft)
)

/**
 * Creates [AbsoluteCutCornerShape] with sizes defined in float.
 */
fun AbsoluteCutCornerShape(
    topLeft: Float = 0.0f,
    topRight: Float = 0.0f,
    bottomRight: Float = 0.0f,
    bottomLeft: Float = 0.0f
) = AbsoluteCutCornerShape(
    topLeft = CornerSize(topLeft),
    topRight = CornerSize(topRight),
    bottomRight = CornerSize(bottomRight),
    bottomLeft = CornerSize(bottomLeft)
)

/**
 * Creates [AbsoluteCutCornerShape] with sizes defined in percents of the shape's smaller side.
 *
 * @param topLeftPercent The top left corner clip size as a percentage of the smaller side, with a
 * range of 0 - 100.
 * @param topRightPercent The top right corner clip size as a percentage of the smaller side, with a
 * range of 0 - 100.
 * @param bottomRightPercent The bottom right clip size radius as a percentage of the smaller side,
 * with a range of 0 - 100.
 * @param bottomLeftPercent The bottom left clip size radius as a percentage of the smaller side,
 * with a range of 0 - 100.
 */
fun AbsoluteCutCornerShape(
    /*@IntRange(from = 0, to = 100)*/
    topLeftPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    topRightPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomRightPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomLeftPercent: Int = 0
) = AbsoluteCutCornerShape(
    topLeft = CornerSize(topLeftPercent),
    topRight = CornerSize(topRightPercent),
    bottomRight = CornerSize(bottomRightPercent),
    bottomLeft = CornerSize(bottomLeftPercent)
)
