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
import androidx.compose.ui.unit.dp

/**
 * A shape describing the rectangle with cut corners.
 * Corner size is representing the cut length - the size of both legs of the cut's right triangle.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
class CutCornerShape(
    topLeft: CornerSize,
    topRight: CornerSize,
    bottomRight: CornerSize,
    bottomLeft: CornerSize
) : CornerBasedShape(topLeft, topRight, bottomRight, bottomLeft) {

    override fun createOutline(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ) = if (topLeft + topRight + bottomLeft + bottomRight == 0.0f) {
        Outline.Rectangle(size.toRect())
    } else Outline.Generic(
        Path().apply {
            var cornerSize = topLeft
            moveTo(0f, cornerSize)
            lineTo(cornerSize, 0f)
            cornerSize = topRight
            lineTo(size.width - cornerSize, 0f)
            lineTo(size.width, cornerSize)
            cornerSize = bottomRight
            lineTo(size.width, size.height - cornerSize)
            lineTo(size.width - cornerSize, size.height)
            cornerSize = bottomLeft
            lineTo(cornerSize, size.height)
            lineTo(0f, size.height - cornerSize)
            close()
        }
    )

    override fun copy(
        topLeft: CornerSize,
        topRight: CornerSize,
        bottomRight: CornerSize,
        bottomLeft: CornerSize
    ) = CutCornerShape(
        topLeft = topLeft,
        topRight = topRight,
        bottomRight = bottomRight,
        bottomLeft = bottomLeft
    )

    override fun toString(): String {
        return "CutCornerShape(topLeft = $topLeft, topRight = $topRight, bottomRight = " +
            "$bottomRight, bottomLeft = $bottomLeft)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CutCornerShape) return false

        if (topLeft != other.topLeft) return false
        if (topRight != other.topRight) return false
        if (bottomRight != other.bottomRight) return false
        if (bottomLeft != other.bottomLeft) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + topRight.hashCode()
        result = 31 * result + bottomRight.hashCode()
        result = 31 * result + bottomLeft.hashCode()
        return result
    }
}

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param corner [CornerSize] to apply.
 */
fun CutCornerShape(corner: CornerSize) = CutCornerShape(corner, corner, corner, corner)

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param size Size in [Dp] to apply.
 */
fun CutCornerShape(size: Dp) = CutCornerShape(CornerSize(size))

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param size Size in pixels to apply.
 */
fun CutCornerShape(size: Float) = CutCornerShape(CornerSize(size))

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param percent Size in percents to apply.
 */
fun CutCornerShape(percent: Int) = CutCornerShape(CornerSize(percent))

/**
 * Creates [CutCornerShape] with sizes defined in [Dp].
 */
fun CutCornerShape(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = CutCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CutCornerShape] with sizes defined in float.
 */
fun CutCornerShape(
    topLeft: Float = 0.0f,
    topRight: Float = 0.0f,
    bottomRight: Float = 0.0f,
    bottomLeft: Float = 0.0f
) = CutCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CutCornerShape] with sizes defined in percents of the shape's smaller side.
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
fun CutCornerShape(
    /*@IntRange(from = 0, to = 100)*/
    topLeftPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    topRightPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomRightPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomLeftPercent: Int = 0
) = CutCornerShape(
    CornerSize(topLeftPercent),
    CornerSize(topRightPercent),
    CornerSize(bottomRightPercent),
    CornerSize(bottomLeftPercent)
)
