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

import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
internal class LazyMeasuredItem(
    val index: Int,
    private val placeables: Array<LazyLayoutPlaceable>,
    private val isVertical: Boolean,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    private val layoutDirection: LayoutDirection,
    private val reverseLayout: Boolean,
    private val startContentPadding: Int,
    private val endContentPadding: Int,
    /**
     * Extra spacing to be added to [size] aside from the sum of the [placeables] size. It
     * is usually representing the spacing after the item.
     */
    private val spacing: Int,
    val key: Any
) {
    /**
     * Sum of the main axis sizes of all the inner placeables.
     */
    val size: Int

    /**
     * Sum of the main axis sizes of all the inner placeables and [spacing].
     */
    val sizeWithSpacings: Int

    /**
     * Max of the cross axis sizes of all the inner placeables.
     */
    val crossAxisSize: Int

    init {
        var mainAxisSize = 0
        var maxCrossAxis = 0
        placeables.forEach {
            val placeable = it.placeable
            mainAxisSize += if (isVertical) placeable.height else placeable.width
            maxCrossAxis =
                maxOf(maxCrossAxis, if (!isVertical) placeable.height else placeable.width)
        }
        size = mainAxisSize
        sizeWithSpacings = size + spacing
        crossAxisSize = maxCrossAxis
    }

    /**
     * Calculates positions for the inner placeables at [offset] main axis position. [layoutWidth]
     * and [layoutHeight] should be provided to not place placeables which are ended up outside of
     * the viewport (for example one item consist of 2 placeables, and the first one is not going
     * to be visible, so we don't place it as an optimization, but place the second one).
     * If [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ): LazyListPositionedItem {
        val wrappers = mutableListOf<LazyListPlaceableWrapper>()
        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        var mainAxisOffset = if (reverseLayout) {
            mainAxisLayoutSize - offset - size
        } else {
            offset
        }
        var index = if (reverseLayout) placeables.lastIndex else 0
        while (if (reverseLayout) index >= 0 else index < placeables.size) {
            val it = placeables[index].placeable
            val addIndex = if (reverseLayout) 0 else wrappers.size
            val placeableOffset = if (isVertical) {
                val x = requireNotNull(horizontalAlignment)
                    .align(it.width, layoutWidth, layoutDirection)
                IntOffset(x, mainAxisOffset)
            } else {
                val y = requireNotNull(verticalAlignment).align(it.height, layoutHeight)
                IntOffset(mainAxisOffset, y)
            }
            mainAxisOffset += if (isVertical) it.height else it.width
            wrappers.add(
                addIndex,
                LazyListPlaceableWrapper(placeableOffset, it, placeables[index].parentData)
            )
            if (reverseLayout) index-- else index++
        }
        return LazyListPositionedItem(
            offset = offset,
            index = this.index,
            key = key,
            size = size,
            sizeWithSpacings = sizeWithSpacings,
            minMainAxisOffset = -startContentPadding,
            maxMainAxisOffset = mainAxisLayoutSize + endContentPadding,
            isVertical = isVertical,
            wrappers = wrappers
        )
    }
}

internal class LazyListPositionedItem(
    override val offset: Int,
    override val index: Int,
    override val key: Any,
    override val size: Int,
    val sizeWithSpacings: Int,
    private val minMainAxisOffset: Int,
    private val maxMainAxisOffset: Int,
    private val isVertical: Boolean,
    private val wrappers: List<LazyListPlaceableWrapper>
) : LazyListItemInfo {

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        wrappers.fastForEach { wrapper ->
            val offset = wrapper.offset
            val placeable = wrapper.placeable
            if (offset.mainAxis + placeable.mainAxisSize > minMainAxisOffset &&
                offset.mainAxis < maxMainAxisOffset
            ) {
                if (isVertical) {
                    placeable.placeWithLayer(offset)
                } else {
                    placeable.placeRelativeWithLayer(offset)
                }
            }
        }
    }

    private val IntOffset.mainAxis get() = if (isVertical) y else x
    private val Placeable.mainAxisSize get() = if (isVertical) height else width
}

internal class LazyListPlaceableWrapper(
    val offset: IntOffset,
    val placeable: Placeable,
    val parentData: Any?
)
