/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyGridItemInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Represents one measured line of the lazy list. Each item on the line can in fact consist of
 * multiple placeables if the user emit multiple layout nodes in the item callback.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyMeasuredLine constructor(
    val index: LineIndex,
    val firstItemIndex: ItemIndex,
    private val crossAxisSizes: Array<Int>,
    private val placeables: Array<Array<LazyLayoutPlaceable>>,
    private val spans: List<GridItemSpan>,
    val keys: Array<Any>,
    private val isVertical: Boolean,
    private val layoutDirection: LayoutDirection,
    private val reverseLayout: Boolean,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    /**
     * Spacing to be added after [mainAxisSize], in the main axis direction.
     */
    private val mainAxisSpacing: Int,
    private val crossAxisSpacing: Int
    // private val placementAnimator: LazyListItemPlacementAnimator,
) {
    /**
     * Main axis size of the line - the max main axis size of the placeables.
     */
    val mainAxisSize: Int

    /**
     * Sum of the main axis sizes of all the inner placeables and [mainAxisSpacing].
     */
    val sizeWithSpacings: Int

    init {
        var maxMainAxis = 0
        placeables.forEach { placeableArray ->
            placeableArray.forEach {
                val placeable = it.placeable
                maxMainAxis =
                    maxOf(maxMainAxis, if (isVertical) placeable.height else placeable.width)
            }
        }
        mainAxisSize = maxMainAxis
        sizeWithSpacings = mainAxisSize + mainAxisSpacing
    }

    /**
     * Whether this line contains any items.
     */
    fun isEmpty() = placeables.isEmpty()

    /**
     * Calculates positions for the inner placeables at [offset] main axis position.
     * If [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ): List<LazyGridPositionedItem> {
        var usedCrossAxis = 0
        var usedSpan = 0
        return placeables.mapIndexed { placeablesIndex, placeables ->
            val wrappers = mutableListOf<LazyListPlaceableWrapper>()
            val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
            val mainAxisOffset = if (reverseLayout) {
                mainAxisLayoutSize - offset - mainAxisSize
            } else {
                offset
            }
            val crossAxisLayoutSize = if (isVertical) layoutWidth else layoutHeight
            val crossAxisOffset = if (layoutDirection == LayoutDirection.Rtl && isVertical) {
                crossAxisLayoutSize - usedCrossAxis - placeables.first().placeable.width
            } else {
                usedCrossAxis
            }
            val placeableOffset = if (isVertical) {
                IntOffset(crossAxisOffset, mainAxisOffset)
            } else {
                IntOffset(mainAxisOffset, crossAxisOffset)
            }
            usedCrossAxis += crossAxisSizes[placeablesIndex] + crossAxisSpacing
            val column = usedSpan
            usedSpan += spans[placeablesIndex].currentLineSpan

            var index = if (reverseLayout) placeables.lastIndex else 0
            while (if (reverseLayout) index >= 0 else index < placeables.size) {
                val it = placeables[index].placeable
                val addIndex = if (reverseLayout) 0 else wrappers.size
                wrappers.add(
                    addIndex,
                    LazyListPlaceableWrapper(placeableOffset, it, placeables[index].parentData)
                )
                if (reverseLayout) index-- else index++
            }

            LazyGridPositionedItem(
                offset = placeableOffset,
                index = firstItemIndex.value + placeablesIndex,
                key = keys[placeablesIndex],
                row = this@LazyMeasuredLine.index.value,
                column = column,
                size = if (isVertical) {
                    IntSize(crossAxisSizes[placeablesIndex], mainAxisSize)
                } else {
                    IntSize(mainAxisSize, crossAxisSizes[placeablesIndex])
                },
                // sizeWithSpacings = sizeWithSpacings,
                minMainAxisOffset = -if (!reverseLayout) {
                    beforeContentPadding
                } else {
                    afterContentPadding
                },
                maxMainAxisOffset = mainAxisLayoutSize +
                    if (!reverseLayout) afterContentPadding else beforeContentPadding,
                isVertical = isVertical,
                wrappers = wrappers,
                // placementAnimator = placementAnimator
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridPositionedItem(
    override val offset: IntOffset,
    override val index: Int,
    override val key: Any,
    override val row: Int,
    override val column: Int,
    override val size: IntSize,
    // val sizeWithSpacings: Int,
    private val minMainAxisOffset: Int,
    private val maxMainAxisOffset: Int,
    private val isVertical: Boolean,
    private val wrappers: List<LazyListPlaceableWrapper>,
    // private val placementAnimator: LazyListItemPlacementAnimator
) : LazyGridItemInfo {
    val placeablesCount: Int get() = wrappers.size

    fun getOffset(index: Int) = wrappers[index].offset

    // fun getMainAxisSize(index: Int) = wrappers[index].placeable.mainAxisSize

    @Suppress("UNCHECKED_CAST")
    fun getAnimationSpec(index: Int) =
        wrappers[index].parentData as? FiniteAnimationSpec<IntOffset>?

    // val hasAnimations = run {
    //     repeat(placeablesCount) { index ->
    //         if (getAnimationSpec(index) != null) {
    //             return@run true
    //         }
    //     }
    //     false
    // }

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            val placeable = wrappers[index].placeable
            val minOffset = minMainAxisOffset - placeable.mainAxisSize
            val maxOffset = maxMainAxisOffset
            val offset =
            //     if (getAnimationSpec(index) != null) {
            //     placementAnimator.getAnimatedOffset(
            //         key, index, minOffset, maxOffset, getOffset(index)
            //     )
            // } else {
                getOffset(index)
            // }
            if (offset.mainAxis > minOffset && offset.mainAxis < maxOffset) {
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
