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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.annotation.IntRange

/**
 * A shape describing the rectangle with rounded corners.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
class RoundedCornerShape(
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
    } else {
        Outline.Rounded(
            RoundRect(
                rect = size.toRect(),
                topLeft = topLeft.toRadius(),
                topRight = topRight.toRadius(),
                bottomRight = bottomRight.toRadius(),
                bottomLeft = bottomLeft.toRadius()
            )
        )
    }

    override fun copy(
        topLeft: CornerSize,
        topRight: CornerSize,
        bottomRight: CornerSize,
        bottomLeft: CornerSize
    ) = RoundedCornerShape(
        topLeft = topLeft,
        topRight = topRight,
        bottomRight = bottomRight,
        bottomLeft = bottomLeft
    )

    override fun toString(): String {
        return "RoundedCornerShape(topLeft = $topLeft, topRight = $topRight, bottomRight = " +
            "$bottomRight, bottomLeft = $bottomLeft)"
    }

    private /*inline*/ fun Float.toRadius() = CornerRadius(this)
}

/**
 * Circular [Shape] with all the corners sized as the 50 percent of the shape size.
 */
val CircleShape = RoundedCornerShape(50)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param corner [CornerSize] to apply.
 */
/*inline*/ fun RoundedCornerShape(corner: CornerSize) =
    RoundedCornerShape(corner, corner, corner, corner)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param size Size in [Dp] to apply.
 */
/*inline*/ fun RoundedCornerShape(size: Dp) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param size Size in pixels to apply.
 */
/*inline*/ fun RoundedCornerShape(size: Float) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param percent Size in percents to apply.
 */
/*inline*/ fun RoundedCornerShape(percent: Int) =
    RoundedCornerShape(CornerSize(percent))

/**
 * Creates [RoundedCornerShape] with sizes defined in [Dp].
 */
/*inline*/ fun RoundedCornerShape(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = RoundedCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [RoundedCornerShape] with sizes defined in pixels.
 */
/*inline*/ fun RoundedCornerShape(
    topLeft: Float = 0.0f,
    topRight: Float = 0.0f,
    bottomRight: Float = 0.0f,
    bottomLeft: Float = 0.0f
) = RoundedCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [RoundedCornerShape] with sizes defined in percents of the shape's smaller side.
 */
/*inline*/ fun RoundedCornerShape(
    @IntRange(from = 0, to = 100) topLeftPercent: Int = 0,
    @IntRange(from = 0, to = 100) topRightPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomRightPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomLeftPercent: Int = 0
) = RoundedCornerShape(
    CornerSize(topLeftPercent),
    CornerSize(topRightPercent),
    CornerSize(bottomRightPercent),
    CornerSize(bottomLeftPercent)
)
