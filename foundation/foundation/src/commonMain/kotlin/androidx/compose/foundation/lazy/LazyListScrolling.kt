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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.coroutines.cancellation.CancellationException

private class ItemFoundInScroll(val item: LazyListItemInfo) : CancellationException()

private val TargetDistance = 2500.dp
private val BoundDistance = 1500.dp

internal suspend fun LazyListState.doSmoothScrollToItem(
    index: Int,
    scrollOffset: Int
) {
    val animationSpec: AnimationSpec<Float> = spring()
    fun getTargetItem() = layoutInfo.visibleItemsInfo.fastFirstOrNull {
        it.index == index
    }
    scroll {
        val targetDistancePx = with(density) { TargetDistance.toPx() }
        val boundDistancePx = with(density) { BoundDistance.toPx() }
        var prevValue = 0f
        val anim = AnimationState(0f)
        var target: Float
        var loop = true
        try {
            val targetItemInitialInfo = getTargetItem()
            if (targetItemInitialInfo != null) {
                // It's already visible, just animate directly
                throw ItemFoundInScroll(targetItemInitialInfo)
            }
            val forward = index > firstVisibleItemIndex
            while (loop) {
                val bound: Float
                // Magic constants for teleportation chosen arbitrarily by experiment
                if (forward) {
                    if (anim.value >= targetDistancePx * 2 &&
                        index - layoutInfo.visibleItemsInfo.last().index > 100
                    ) {
                        // Teleport
                        snapToItemIndexInternal(index = index - 100, scrollOffset = 0)
                    }
                    target = anim.value + targetDistancePx
                    bound = anim.value + boundDistancePx
                } else {
                    if (anim.value >= targetDistancePx * -2 &&
                        layoutInfo.visibleItemsInfo.first().index - index > 100
                    ) {
                        // Teleport
                        snapToItemIndexInternal(index = index + 100, scrollOffset = 0)
                    }
                    target = anim.value - targetDistancePx
                    bound = anim.value - boundDistancePx
                }
                anim.animateTo(
                    target,
                    animationSpec = animationSpec,
                    sequentialAnimation = (anim.velocity != 0f)
                ) {
                    // If we haven't found the item yet, check if it's visible.
                    val targetItem = getTargetItem()
                    // Did we scroll far enough that we're completely past the item?
                    val pastItem = targetItem == null && (
                        (forward && firstVisibleItemIndex > index) ||
                            (!forward && layoutInfo.visibleItemsInfo.last().index < index)
                        )
                    // We don't throw ItemFoundInScroll when we snap, because once we've snapped to
                    // the final position, there's no need to animate to it.
                    if (pastItem) {
                        snapToItemIndexInternal(index = index, scrollOffset = scrollOffset)
                        cancelAnimation()
                        return@animateTo
                    }
                    if (targetItem != null) {
                        // Check for overshoot on the offset
                        val overshotButVisible = when {
                            forward && targetItem.offset < scrollOffset -> true
                            !forward && targetItem.offset > scrollOffset -> true
                            else -> false
                        }
                        if (overshotButVisible) {
                            snapToItemIndexInternal(index = index, scrollOffset = scrollOffset)
                            cancelAnimation()
                            return@animateTo
                        } else {
                            throw ItemFoundInScroll(targetItem)
                        }
                    }

                    val delta = value - prevValue
                    val consumed = scrollBy(delta)
                    if (delta != consumed) {
                        cancelAnimation()
                        loop = false
                    }
                    prevValue += delta
                    if (forward) {
                        if (value > bound) {
                            cancelAnimation()
                        }
                    } else {
                        if (value < bound) {
                            cancelAnimation()
                        }
                    }
                }
            }
        } catch (itemFound: ItemFoundInScroll) {
            // We found it, animate to it
            // Bring to the requested position - will be automatically stopped if not possible
            target = anim.value + itemFound.item.offset + scrollOffset
            prevValue = anim.value
            anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
                val delta = value - prevValue
                val consumed = scrollBy(delta)
                if (delta != consumed) {
                    cancelAnimation()
                }
                prevValue += delta
            }
            // Once we're finished the animation, snap to the exact position to account for
            // rounding error (otherwise we tend to end up with the previous item scrolled the
            // tiniest bit onscreen)
            // TODO: prevent temporarily scrolling *past* the item
            snapToItemIndexInternal(index = index, scrollOffset = scrollOffset)
        }
    }
}