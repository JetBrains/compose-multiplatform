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
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp

/**
 * A shape describing the rectangle with cut corners.
 * Corner size is representing the cut length - the size of both legs of the cut's right triangle.
 *
 * This shape will automatically mirror the corner sizes in [LayoutDirection.Rtl], use
 * [AbsoluteCutCornerShape] for the layout direction unaware version of this shape.
 *
 * @param topStart a size of the top start corner
 * @param topEnd a size of the top end corner
 * @param bottomEnd a size of the bottom end corner
 * @param bottomStart a size of the bottom start corner
 */
class CutCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize
) : CornerBasedShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart
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
            var cornerSize = if (layoutDirection == Ltr) topStart else topEnd
            moveTo(0f, cornerSize)
            lineTo(cornerSize, 0f)
            cornerSize = if (layoutDirection == Ltr) topEnd else topStart
            lineTo(size.width - cornerSize, 0f)
            lineTo(size.width, cornerSize)
            cornerSize = if (layoutDirection == Ltr) bottomEnd else bottomStart
            lineTo(size.width, size.height - cornerSize)
            lineTo(size.width - cornerSize, size.height)
            cornerSize = if (layoutDirection == Ltr) bottomStart else bottomEnd
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
    ) = CutCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    override fun toString(): String {
        return "CutCornerShape(topStart = $topStart, topEnd = $topEnd, bottomEnd = " +
            "$bottomEnd, bottomStart = $bottomStart)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CutCornerShape) return false

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
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp
) = CutCornerShape(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)

/**
 * Creates [CutCornerShape] with sizes defined in float.
 */
fun CutCornerShape(
    topStart: Float = 0.0f,
    topEnd: Float = 0.0f,
    bottomEnd: Float = 0.0f,
    bottomStart: Float = 0.0f
) = CutCornerShape(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)

/**
 * Creates [CutCornerShape] with sizes defined in percents of the shape's smaller side.
 *
 * @param topStartPercent The top start corner clip size as a percentage of the smaller side, with a
 * range of 0 - 100.
 * @param topEndPercent The top end corner clip size as a percentage of the smaller side, with a
 * range of 0 - 100.
 * @param bottomEndPercent The bottom end clip size radius as a percentage of the smaller side,
 * with a range of 0 - 100.
 * @param bottomStartPercent The bottom start clip size radius as a percentage of the smaller side,
 * with a range of 0 - 100.
 */
fun CutCornerShape(
    /*@IntRange(from = 0, to = 100)*/
    topStartPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    topEndPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomEndPercent: Int = 0,
    /*@IntRange(from = 0, to = 100)*/
    bottomStartPercent: Int = 0
) = CutCornerShape(
    topStart = CornerSize(topStartPercent),
    topEnd = CornerSize(topEndPercent),
    bottomEnd = CornerSize(bottomEndPercent),
    bottomStart = CornerSize(bottomStartPercent)
)
