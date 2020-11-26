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

package androidx.compose.foundation.lazy

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
internal class LazyMeasuredItem(
    private val placeables: List<Placeable>,
    private val isVertical: Boolean,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    private val layoutDirection: LayoutDirection,
    private val startContentPadding: Int,
    private val endContentPadding: Int,
    /**
     * Extra size to be added to [mainAxisSize] aside from the sum of the [placeables] size.
     */
    val extraMainAxisSize: Int
) {
    /**
     * Sum of the main axis sizes of all the inner placeables.
     */
    val mainAxisSize: Int

    /**
     * Max of the cross axis sizes of all the inner placeables.
     */
    val crossAxisSize: Int

    init {
        var mainAxisSize = extraMainAxisSize
        var maxCrossAxis = 0
        placeables.fastForEach {
            mainAxisSize += if (isVertical) it.height else it.width
            maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) it.height else it.width)
        }
        this.mainAxisSize = mainAxisSize
        this.crossAxisSize = maxCrossAxis
    }

    /**
     * Perform placing for all the inner placeables at [offset] main axis position. [layoutWidth]
     * and [layoutHeight] should be provided to not place placeables which are ended up outside of
     * the viewport (for example one item consist of 2 placeables, and the first one is not going
     * to be visible, so we don't place it as an optimization, but place the second one).
     * If [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun place(
        scope: Placeable.PlacementScope,
        layoutWidth: Int,
        layoutHeight: Int,
        offset: Int,
        reverseOrder: Boolean
    ) = with(scope) {
        var mainAxisOffset = offset
        val indices = if (reverseOrder) placeables.lastIndex downTo 0 else placeables.indices
        for (index in indices) {
            val it = placeables[index]
            if (isVertical) {
                val x = requireNotNull(horizontalAlignment)
                    .align(it.width, layoutWidth, layoutDirection)
                if (mainAxisOffset + it.height > -startContentPadding &&
                    mainAxisOffset < layoutHeight + endContentPadding
                ) {
                    it.placeWithLayer(x, mainAxisOffset)
                }
                mainAxisOffset += it.height
            } else {
                val y = requireNotNull(verticalAlignment).align(it.height, layoutHeight)
                if (mainAxisOffset + it.width > -startContentPadding &&
                    mainAxisOffset < layoutWidth + endContentPadding
                ) {
                    it.placeRelativeWithLayer(mainAxisOffset, y)
                }
                mainAxisOffset += it.width
            }
        }
    }
}
