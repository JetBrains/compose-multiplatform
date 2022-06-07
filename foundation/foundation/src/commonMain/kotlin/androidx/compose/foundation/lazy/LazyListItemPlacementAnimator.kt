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

package androidx.compose.foundation.lazy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles the item placement animations when it is set via [LazyItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyListItemPlacementAnimator(
    private val scope: CoroutineScope,
    private val isVertical: Boolean
) {
    // state containing an animation and all relevant info for each item.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyToIndexMap: Map<Any, Int> = emptyMap()

    // keeps the first and the last items positioned in the viewport and their visible part sizes.
    private var viewportStartItemIndex = -1
    private var viewportStartItemNotVisiblePartSize = 0
    private var viewportEndItemIndex = -1
    private var viewportEndItemNotVisiblePartSize = 0

    // stored to not allocate it every pass.
    private val positionedKeys = mutableSetOf<Any>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        reverseLayout: Boolean,
        positionedItems: MutableList<LazyListPositionedItem>,
        itemProvider: LazyMeasuredItemProvider
    ) {
        if (!positionedItems.fastAny { it.hasAnimations }) {
            // no animations specified - no work needed
            reset()
            return
        }

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val notAnimatableDelta = (if (reverseLayout) -consumedScroll else consumedScroll).toOffset()

        val newFirstItem = positionedItems.first()
        val newLastItem = positionedItems.last()

        var totalItemsSize = 0
        // update known indexes and calculate the average size
        positionedItems.fastForEach { item ->
            keyToItemInfoMap[item.key]?.index = item.index
            totalItemsSize += item.sizeWithSpacings
        }
        val averageItemSize = totalItemsSize / positionedItems.size

        positionedKeys.clear()
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            positionedKeys.add(item.key)
            val itemInfo = keyToItemInfoMap[item.key]
            if (itemInfo == null) {
                // there is no state associated with this item yet
                if (item.hasAnimations) {
                    val newItemInfo = ItemInfo(item.index)
                    val previousIndex = keyToIndexMap[item.key]
                    val firstPlaceableOffset = item.getOffset(0)
                    val firstPlaceableSize = item.getMainAxisSize(0)

                    val targetFirstPlaceableOffsetMainAxis = if (previousIndex == null) {
                        // it is a completely new item. no animation is needed
                        firstPlaceableOffset.mainAxis
                    } else {
                        val fallback = if (!reverseLayout) {
                            firstPlaceableOffset.mainAxis
                        } else {
                            firstPlaceableOffset.mainAxis - item.sizeWithSpacings +
                                firstPlaceableSize
                        }
                        calculateExpectedOffset(
                            index = previousIndex,
                            sizeWithSpacings = item.sizeWithSpacings,
                            averageItemsSize = averageItemSize,
                            scrolledBy = notAnimatableDelta,
                            fallback = fallback,
                            reverseLayout = reverseLayout,
                            mainAxisLayoutSize = mainAxisLayoutSize,
                            visibleItems = positionedItems
                        ) + if (reverseLayout) {
                            item.size - firstPlaceableSize
                        } else {
                            0
                        }
                    }
                    val targetFirstPlaceableOffset = if (isVertical) {
                        firstPlaceableOffset.copy(y = targetFirstPlaceableOffsetMainAxis)
                    } else {
                        firstPlaceableOffset.copy(x = targetFirstPlaceableOffsetMainAxis)
                    }

                    // populate placeable info list
                    repeat(item.placeablesCount) { placeableIndex ->
                        val diffToFirstPlaceableOffset =
                            item.getOffset(placeableIndex) - firstPlaceableOffset
                        newItemInfo.placeables.add(
                            PlaceableInfo(
                                targetFirstPlaceableOffset + diffToFirstPlaceableOffset,
                                item.getMainAxisSize(placeableIndex)
                            )
                        )
                    }
                    keyToItemInfoMap[item.key] = newItemInfo
                    startAnimationsIfNeeded(item, newItemInfo)
                }
            } else {
                if (item.hasAnimations) {
                    // apply new not animatable offset
                    itemInfo.notAnimatableDelta += notAnimatableDelta
                    startAnimationsIfNeeded(item, itemInfo)
                } else {
                    // no animation, clean up if needed
                    keyToItemInfoMap.remove(item.key)
                }
            }
        }

        // previously we were animating items which are visible in the end state so we had to
        // compare the current state with the state used for the previous measuring.
        // now we will animate disappearing items so the current state is their starting state
        // so we can update current viewport start/end items
        if (!reverseLayout) {
            viewportStartItemIndex = newFirstItem.index
            viewportStartItemNotVisiblePartSize = newFirstItem.offset
            viewportEndItemIndex = newLastItem.index
            viewportEndItemNotVisiblePartSize =
                newLastItem.offset + newLastItem.sizeWithSpacings - mainAxisLayoutSize
        } else {
            viewportStartItemIndex = newLastItem.index
            viewportStartItemNotVisiblePartSize =
                mainAxisLayoutSize - newLastItem.offset - newLastItem.size
            viewportEndItemIndex = newFirstItem.index
            viewportEndItemNotVisiblePartSize =
                -newFirstItem.offset + (newFirstItem.sizeWithSpacings - newFirstItem.size)
        }

        val iterator = keyToItemInfoMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!positionedKeys.contains(entry.key)) {
                // found an item which was in our map previously but is not a part of the
                // positionedItems now
                val itemInfo = entry.value
                // apply new not animatable delta for this item
                itemInfo.notAnimatableDelta += notAnimatableDelta

                val index = itemProvider.keyToIndexMap[entry.key]

                // whether at least one placeable is within the viewport bounds.
                // this usually means that we will start animation for it right now
                val withinBounds = itemInfo.placeables.fastAny {
                    val currentTarget = it.targetOffset + itemInfo.notAnimatableDelta
                    currentTarget.mainAxis + it.size > 0 &&
                        currentTarget.mainAxis < mainAxisLayoutSize
                }

                // whether the animation associated with the item has been finished
                val isFinished = !itemInfo.placeables.fastAny { it.inProgress }

                if ((!withinBounds && isFinished) ||
                    index == null ||
                    itemInfo.placeables.isEmpty()
                ) {
                    iterator.remove()
                } else {

                    val measuredItem = itemProvider.getAndMeasure(DataIndex(index))

                    // calculate the target offset for the animation.
                    val absoluteTargetOffset = calculateExpectedOffset(
                        index = index,
                        sizeWithSpacings = measuredItem.sizeWithSpacings,
                        averageItemsSize = averageItemSize,
                        scrolledBy = notAnimatableDelta,
                        fallback = mainAxisLayoutSize,
                        reverseLayout = reverseLayout,
                        mainAxisLayoutSize = mainAxisLayoutSize,
                        visibleItems = positionedItems
                    )
                    val targetOffset = if (reverseLayout) {
                        mainAxisLayoutSize - absoluteTargetOffset - measuredItem.size
                    } else {
                        absoluteTargetOffset
                    }

                    val item = measuredItem.position(targetOffset, layoutWidth, layoutHeight)
                    positionedItems.add(item)
                    startAnimationsIfNeeded(item, itemInfo)
                }
            }
        }

        keyToIndexMap = itemProvider.keyToIndexMap
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
            ((currentTarget.mainAxis < minOffset && currentValue.mainAxis < minOffset) ||
            (currentTarget.mainAxis > maxOffset && currentValue.mainAxis > maxOffset))
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
        viewportStartItemIndex = -1
        viewportStartItemNotVisiblePartSize = 0
        viewportEndItemIndex = -1
        viewportEndItemNotVisiblePartSize = 0
    }

    /**
     * Estimates the outside of the viewport offset for the item. Used to understand from
     * where to start animation for the item which wasn't visible previously or where it should
     * end for the item which is not going to be visible in the end.
     */
    private fun calculateExpectedOffset(
        index: Int,
        sizeWithSpacings: Int,
        averageItemsSize: Int,
        scrolledBy: IntOffset,
        reverseLayout: Boolean,
        mainAxisLayoutSize: Int,
        fallback: Int,
        visibleItems: List<LazyListPositionedItem>
    ): Int {
        val afterViewportEnd =
            if (!reverseLayout) viewportEndItemIndex < index else viewportEndItemIndex > index
        val beforeViewportStart =
            if (!reverseLayout) viewportStartItemIndex > index else viewportStartItemIndex < index
        return when {
            afterViewportEnd -> {
                var itemsSizes = 0
                // add sizes of the items between the last visible one and this one.
                val range = if (!reverseLayout) {
                    viewportEndItemIndex + 1 until index
                } else {
                    index + 1 until viewportEndItemIndex
                }
                for (i in range) {
                    itemsSizes += visibleItems.getItemSize(
                        itemIndex = i,
                        fallback = averageItemsSize
                    )
                }
                mainAxisLayoutSize + viewportEndItemNotVisiblePartSize + itemsSizes +
                    scrolledBy.mainAxis
            }
            beforeViewportStart -> {
                // add the size of this item as we need the start offset of this item.
                var itemsSizes = sizeWithSpacings
                // add sizes of the items between the first visible one and this one.
                val range = if (!reverseLayout) {
                    index + 1 until viewportStartItemIndex
                } else {
                    viewportStartItemIndex + 1 until index
                }
                for (i in range) {
                    itemsSizes += visibleItems.getItemSize(
                        itemIndex = i,
                        fallback = averageItemsSize
                    )
                }
                viewportStartItemNotVisiblePartSize - itemsSizes + scrolledBy.mainAxis
            }
            else -> {
                fallback
            }
        }
    }

    private fun List<LazyListPositionedItem>.getItemSize(itemIndex: Int, fallback: Int): Int {
        if (isEmpty() || itemIndex < first().index || itemIndex > last().index) return fallback
        if ((itemIndex - first().index) < (last().index - itemIndex)) {
            for (index in indices) {
                val item = get(index)
                if (item.index == itemIndex) return item.sizeWithSpacings
                if (item.index > itemIndex) break
            }
        } else {
            for (index in lastIndex downTo 0) {
                val item = get(index)
                if (item.index == itemIndex) return item.sizeWithSpacings
                if (item.index < itemIndex) break
            }
        }
        return fallback
    }

    private fun startAnimationsIfNeeded(item: LazyListPositionedItem, itemInfo: ItemInfo) {
        // first we make sure our item info is up to date (has the item placeables count)
        while (itemInfo.placeables.size > item.placeablesCount) {
            itemInfo.placeables.removeLast()
        }
        while (itemInfo.placeables.size < item.placeablesCount) {
            val newPlaceableInfoIndex = itemInfo.placeables.size
            val rawOffset = item.getOffset(newPlaceableInfoIndex)
            itemInfo.placeables.add(
                PlaceableInfo(
                    rawOffset - itemInfo.notAnimatableDelta,
                    item.getMainAxisSize(newPlaceableInfoIndex)
                )
            )
        }

        itemInfo.placeables.fastForEachIndexed { index, placeableInfo ->
            val currentTarget = placeableInfo.targetOffset + itemInfo.notAnimatableDelta
            val currentOffset = item.getOffset(index)
            placeableInfo.size = item.getMainAxisSize(index)
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

    private fun Int.toOffset() =
        IntOffset(if (isVertical) 0 else this, if (!isVertical) 0 else this)

    private val IntOffset.mainAxis get() = if (isVertical) y else x
}

private class ItemInfo(var index: Int) {
    var notAnimatableDelta: IntOffset = IntOffset.Zero
    val placeables = mutableListOf<PlaceableInfo>()
}

private class PlaceableInfo(
    initialOffset: IntOffset,
    var size: Int
) {
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
