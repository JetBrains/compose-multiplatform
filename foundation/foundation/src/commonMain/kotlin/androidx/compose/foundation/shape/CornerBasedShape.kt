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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Base class for [Shape]s defined by four [CornerSize]s.
 *
 * @see RoundedCornerShape for an example of the usage.
 *
 * @param topStart a size of the top start corner
 * @param topEnd a size of the top end corner
 * @param bottomEnd a size of the bottom end corner
 * @param bottomStart a size of the bottom start corner
 */
abstract class CornerBasedShape(
    val topStart: CornerSize,
    val topEnd: CornerSize,
    val bottomEnd: CornerSize,
    val bottomStart: CornerSize
) : Shape {

    final override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        var topStart = topStart.toPx(size, density)
        var topEnd = topEnd.toPx(size, density)
        var bottomEnd = bottomEnd.toPx(size, density)
        var bottomStart = bottomStart.toPx(size, density)
        val minDimension = size.minDimension
        if (topStart + bottomStart > minDimension) {
            val scale = minDimension / (topStart + bottomStart)
            topStart *= scale
            bottomStart *= scale
        }
        if (topEnd + bottomEnd > minDimension) {
            val scale = minDimension / (topEnd + bottomEnd)
            topEnd *= scale
            bottomEnd *= scale
        }
        require(topStart >= 0.0f && topEnd >= 0.0f && bottomEnd >= 0.0f && bottomStart >= 0.0f) {
            "Corner size in Px can't be negative(topStart = $topStart, topEnd = $topEnd, " +
                "bottomEnd = $bottomEnd, bottomStart = $bottomStart)!"
        }
        return createOutline(
            size = size,
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
            layoutDirection = layoutDirection
        )
    }

    /**
     * Creates [Outline] of this shape for the given [size].
     *
     * @param size the size of the shape boundary.
     * @param topStart the resolved size of the top start corner
     * @param topEnd the resolved size for the top end corner
     * @param bottomEnd the resolved size for the bottom end corner
     * @param bottomStart the resolved size for the bottom start corner
     * @param layoutDirection the current layout direction.
     */
    abstract fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline

    /**
     * Creates a copy of this Shape with a new corner sizes.
     *
     * @param topStart a size of the top start corner
     * @param topEnd a size of the top end corner
     * @param bottomEnd a size of the bottom end corner
     * @param bottomStart a size of the bottom start corner
     */
    abstract fun copy(
        topStart: CornerSize = this.topStart,
        topEnd: CornerSize = this.topEnd,
        bottomEnd: CornerSize = this.bottomEnd,
        bottomStart: CornerSize = this.bottomStart
    ): CornerBasedShape

    /**
     * Creates a copy of this Shape with a new corner size.
     * @param all a size to apply for all four corners
     */
    fun copy(all: CornerSize): CornerBasedShape = copy(all, all, all, all)
}
