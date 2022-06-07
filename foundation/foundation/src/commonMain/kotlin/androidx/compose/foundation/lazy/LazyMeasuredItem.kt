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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyMeasuredItem @ExperimentalFoundationApi constructor(
    val index: Int,
    private val placeables: Array<Placeable>,
    private val isVertical: Boolean,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    private val layoutDirection: LayoutDirection,
    private val reverseLayout: Boolean,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    private val placementAnimator: LazyListItemPlacementAnimator,
    /**
     * Extra spacing to be added to [size] aside from the sum of the [placeables] size. It
     * is usually representing the spacing after the item.
     */
    private val spacing: Int,
    /**
     * The offset which shouldn't affect any calculations but needs to be applied for the final
     * value passed into the place() call.
     */
    private val visualOffset: IntOffset,
    val key: Any,
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
            mainAxisSize += if (isVertical) it.height else it.width
            maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) it.height else it.width)
        }
        size = mainAxisSize
        sizeWithSpacings = size + spacing
        crossAxisSize = maxCrossAxis
    }

    /**
     * Calculates positions for the inner placeables at [offset] main axis position.
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
            val it = placeables[index]
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
            minMainAxisOffset = -if (!reverseLayout) beforeContentPadding else afterContentPadding,
            maxMainAxisOffset = mainAxisLayoutSize +
                if (!reverseLayout) afterContentPadding else beforeContentPadding,
            isVertical = isVertical,
            wrappers = wrappers,
            placementAnimator = placementAnimator,
            visualOffset = visualOffset
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
    private val wrappers: List<LazyListPlaceableWrapper>,
    private val placementAnimator: LazyListItemPlacementAnimator,
    private val visualOffset: IntOffset
) : LazyListItemInfo {
    val placeablesCount: Int get() = wrappers.size

    fun getOffset(index: Int) = wrappers[index].offset

    fun getMainAxisSize(index: Int) = wrappers[index].placeable.mainAxisSize

    @Suppress("UNCHECKED_CAST")
    fun getAnimationSpec(index: Int) =
        wrappers[index].parentData as? FiniteAnimationSpec<IntOffset>?

    val hasAnimations = run {
        repeat(placeablesCount) { index ->
            if (getAnimationSpec(index) != null) {
                return@run true
            }
        }
        false
    }

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            val placeable = wrappers[index].placeable
            val minOffset = minMainAxisOffset - placeable.mainAxisSize
            val maxOffset = maxMainAxisOffset
            val offset = if (getAnimationSpec(index) != null) {
                placementAnimator.getAnimatedOffset(
                    key, index, minOffset, maxOffset, getOffset(index)
                )
            } else {
                getOffset(index)
            }
            if (isVertical) {
                placeable.placeWithLayer(offset + visualOffset)
            } else {
                placeable.placeRelativeWithLayer(offset + visualOffset)
            }
        }
    }

    private val Placeable.mainAxisSize get() = if (isVertical) height else width
}

internal class LazyListPlaceableWrapper(
    val offset: IntOffset,
    val placeable: Placeable,
    val parentData: Any?
)
