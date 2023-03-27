/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles the item placement animations when it is set via
 * [LazyGridItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyGridItemPlacementAnimator(
    private val scope: CoroutineScope,
    private val isVertical: Boolean
) {
    // state containing an animation and all relevant info for each item.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyToIndexMap: Map<Any, Int> = emptyMap()

    // keeps the index of the first visible item.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = LinkedHashSet<Any>()
    private val movingInFromStartBound = mutableListOf<LazyGridPositionedItem>()
    private val movingInFromEndBound = mutableListOf<LazyGridPositionedItem>()
    private val movingAwayToStartBound = mutableListOf<LazyGridMeasuredItem>()
    private val movingAwayToEndBound = mutableListOf<LazyGridMeasuredItem>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        positionedItems: MutableList<LazyGridPositionedItem>,
        itemProvider: LazyMeasuredItemProvider,
        spanLayoutProvider: LazyGridSpanLayoutProvider
    ) {
        if (!positionedItems.fastAny { it.hasAnimations } && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed
            reset()
            return
        }

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0
        val previousKeyToIndexMap = keyToIndexMap
        keyToIndexMap = itemProvider.keyToIndexMap

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val notAnimatableDelta = consumedScroll.toOffset()

        // first add all items we had in the previous run
        movingAwayKeys.addAll(keyToItemInfoMap.keys)
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are not disappearing.
            movingAwayKeys.remove(item.key)
            if (item.hasAnimations) {
                val itemInfo = keyToItemInfoMap[item.key]
                // there is no state associated with this item yet
                if (itemInfo == null) {
                    val previousIndex = previousKeyToIndexMap[item.key]
                    if (previousIndex != null && item.index != previousIndex) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else {
                        keyToItemInfoMap[item.key] = createItemInfo(item)
                    }
                } else {
                    // this item was visible and is still visible.
                    itemInfo.notAnimatableDelta += notAnimatableDelta // apply new scroll delta
                    itemInfo.crossAxisSize = item.getCrossAxisSize()
                    itemInfo.crossAxisOffset = item.getCrossAxisOffset()
                    startAnimationsIfNeeded(item, itemInfo)
                }
            } else {
                // no animation, clean up if needed
                keyToItemInfoMap.remove(item.key)
            }
        }

        var currentMainAxisOffset = 0
        movingInFromStartBound.sortByDescending { previousKeyToIndexMap[it.key] }
        var previousLine = -1
        var previousLineMainAxisSize = 0
        movingInFromStartBound.fastForEach { item ->
            val line = item.line
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.getMainAxisSize())
            } else {
                currentMainAxisOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.getMainAxisSize()
                previousLine = line
            }
            val mainAxisOffset = 0 - currentMainAxisOffset - item.getMainAxisSize()
            val itemInfo = createItemInfo(item, mainAxisOffset)
            keyToItemInfoMap[item.key] = itemInfo
            startAnimationsIfNeeded(item, itemInfo)
        }
        currentMainAxisOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingInFromEndBound.sortBy { previousKeyToIndexMap[it.key] }
        movingInFromEndBound.fastForEach { item ->
            val line = item.line
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.getMainAxisSize())
            } else {
                currentMainAxisOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.getMainAxisSize()
                previousLine = line
            }
            val mainAxisOffset = mainAxisLayoutSize + currentMainAxisOffset
            val itemInfo = createItemInfo(item, mainAxisOffset)
            keyToItemInfoMap[item.key] = itemInfo
            startAnimationsIfNeeded(item, itemInfo)
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val itemInfo = keyToItemInfoMap.getValue(key)
            val newIndex = keyToIndexMap[key]

            // whether the animation associated with the item has been finished or not yet started
            val inProgress = itemInfo.placeables.fastAny { it.inProgress }
            if (itemInfo.placeables.isEmpty() ||
                newIndex == null ||
                (!inProgress && newIndex == previousKeyToIndexMap[key]) ||
                (!inProgress && !itemInfo.isWithinBounds(mainAxisLayoutSize))
            ) {
                keyToItemInfoMap.remove(key)
            } else {
                val item = itemProvider.getAndMeasure(
                    ItemIndex(newIndex),
                    constraints = if (isVertical) {
                        Constraints.fixedWidth(itemInfo.crossAxisSize)
                    } else {
                        Constraints.fixedHeight(itemInfo.crossAxisSize)
                    }
                )
                if (newIndex < firstVisibleIndex) {
                    movingAwayToStartBound.add(item)
                } else {
                    movingAwayToEndBound.add(item)
                }
            }
        }

        currentMainAxisOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingAwayToStartBound.sortByDescending { keyToIndexMap[it.key] }
        movingAwayToStartBound.fastForEach { item ->
            val line = spanLayoutProvider.getLineIndexOfItem(item.index.value).value
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                currentMainAxisOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = 0 - currentMainAxisOffset - item.mainAxisSize

            val itemInfo = keyToItemInfoMap.getValue(item.key)

            val positionedItem = item.position(
                mainAxisOffset,
                itemInfo.crossAxisOffset,
                layoutWidth,
                layoutHeight,
                LazyGridItemInfo.UnknownRow,
                LazyGridItemInfo.UnknownColumn
            )
            positionedItems.add(positionedItem)
            startAnimationsIfNeeded(positionedItem, itemInfo)
        }
        currentMainAxisOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingAwayToEndBound.sortBy { keyToIndexMap[it.key] }
        movingAwayToEndBound.fastForEach { item ->
            val line = spanLayoutProvider.getLineIndexOfItem(item.index.value).value
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                currentMainAxisOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = mainAxisLayoutSize + currentMainAxisOffset

            val itemInfo = keyToItemInfoMap.getValue(item.key)
            val positionedItem = item.position(
                mainAxisOffset,
                itemInfo.crossAxisOffset,
                layoutWidth,
                layoutHeight,
                LazyGridItemInfo.UnknownRow,
                LazyGridItemInfo.UnknownColumn
            )

            positionedItems.add(positionedItem)
            startAnimationsIfNeeded(positionedItem, itemInfo)
        }

        movingInFromStartBound.clear()
        movingInFromEndBound.clear()
        movingAwayToStartBound.clear()
        movingAwayToEndBound.clear()
        movingAwayKeys.clear()
    }

    /**
     * Returns the current animated item placement offset. By calling it only during the layout
     * phase we can skip doing remeasure on every animation frame.
     */
    fun getAnimatedOffset(
        key: Any,
        placeableIndex: Int,
        minOffset: Int,
        maxOffset: Int,
        rawOffset: IntOffset
    ): IntOffset {
        val itemInfo = keyToItemInfoMap[key] ?: return rawOffset
        val item = itemInfo.placeables[placeableIndex]
        val currentValue = item.animatedOffset.value + itemInfo.notAnimatableDelta
        val currentTarget = item.targetOffset + itemInfo.notAnimatableDelta

        // cancel the animation if it is fully out of the bounds.
        if (item.inProgress &&
            ((currentTarget.mainAxis <= minOffset && currentValue.mainAxis < minOffset) ||
            (currentTarget.mainAxis >= maxOffset && currentValue.mainAxis > maxOffset))
        ) {
            scope.launch {
                item.animatedOffset.snapTo(item.targetOffset)
                item.inProgress = false
            }
        }

        return currentValue
    }

    /**
     * Should be called when the animations are not needed for the next positions change,
     * for example when we snap to a new position.
     */
    fun reset() {
        keyToItemInfoMap.clear()
        keyToIndexMap = emptyMap()
        firstVisibleIndex = -1
    }

    private fun createItemInfo(
        item: LazyGridPositionedItem,
        mainAxisOffset: Int = item.offset.mainAxis
    ): ItemInfo {
        val newItemInfo = ItemInfo(item.getCrossAxisSize(), item.getCrossAxisOffset())
        val targetOffset = if (isVertical) {
            item.offset.copy(y = mainAxisOffset)
        } else {
            item.offset.copy(x = mainAxisOffset)
        }

        // populate placeable info list
        repeat(item.placeablesCount) { placeableIndex ->
            newItemInfo.placeables.add(
                PlaceableInfo(
                    targetOffset,
                    item.getMainAxisSize(placeableIndex)
                )
            )
        }
        return newItemInfo
    }

    private fun startAnimationsIfNeeded(item: LazyGridPositionedItem, itemInfo: ItemInfo) {
        // first we make sure our item info is up to date (has the item placeables count)
        while (itemInfo.placeables.size > item.placeablesCount) {
            itemInfo.placeables.removeLast()
        }
        while (itemInfo.placeables.size < item.placeablesCount) {
            val newPlaceableInfoIndex = itemInfo.placeables.size
            val rawOffset = item.offset
            itemInfo.placeables.add(
                PlaceableInfo(
                    rawOffset - itemInfo.notAnimatableDelta,
                    item.getMainAxisSize(newPlaceableInfoIndex)
                )
            )
        }

        itemInfo.placeables.fastForEachIndexed { index, placeableInfo ->
            val currentTarget = placeableInfo.targetOffset + itemInfo.notAnimatableDelta
            val currentOffset = item.offset
            placeableInfo.mainAxisSize = item.getMainAxisSize(index)
            val animationSpec = item.getAnimationSpec(index)
            if (currentTarget != currentOffset) {
                placeableInfo.targetOffset = currentOffset - itemInfo.notAnimatableDelta
                if (animationSpec != null) {
                    placeableInfo.inProgress = true
                    scope.launch {
                        val finalSpec = if (placeableInfo.animatedOffset.isRunning) {
                            // when interrupted, use the default spring, unless the spec is a spring.
                            if (animationSpec is SpringSpec<IntOffset>) animationSpec else
                                InterruptionSpec
                        } else {
                            animationSpec
                        }

                        try {
                            placeableInfo.animatedOffset.animateTo(
                                placeableInfo.targetOffset,
                                finalSpec
                            )
                            placeableInfo.inProgress = false
                        } catch (_: CancellationException) {
                            // we don't reset inProgress in case of cancellation as it means
                            // there is a new animation started which would reset it later
                        }
                    }
                }
            }
        }
    }

    /**
     * Whether at least one placeable is within the viewport bounds.
     */
    private fun ItemInfo.isWithinBounds(mainAxisLayoutSize: Int): Boolean {
        return placeables.fastAny {
            val currentTarget = it.targetOffset + notAnimatableDelta
            currentTarget.mainAxis + it.mainAxisSize > 0 &&
                currentTarget.mainAxis < mainAxisLayoutSize
        }
    }

    private fun Int.toOffset() =
        IntOffset(if (isVertical) 0 else this, if (!isVertical) 0 else this)

    private val IntOffset.mainAxis get() = if (isVertical) y else x

    private val LazyGridPositionedItem.line get() = if (isVertical) row else column
}

private class ItemInfo(
    var crossAxisSize: Int,
    var crossAxisOffset: Int
) {
    var notAnimatableDelta: IntOffset = IntOffset.Zero
    val placeables = mutableListOf<PlaceableInfo>()
}

private class PlaceableInfo(initialOffset: IntOffset, var mainAxisSize: Int) {
    val animatedOffset = Animatable(initialOffset, IntOffset.VectorConverter)
    var targetOffset: IntOffset = initialOffset
    var inProgress by mutableStateOf(false)
}

/**
 * We switch to this spec when a duration based animation is being interrupted.
 */
private val InterruptionSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)
