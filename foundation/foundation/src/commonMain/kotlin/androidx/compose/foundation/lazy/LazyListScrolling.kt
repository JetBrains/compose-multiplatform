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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private class ItemFoundInScroll(
    val item: LazyListItemInfo,
    val previousAnimation: AnimationState<Float, AnimationVector1D>
) : CancellationException()

private val TargetDistance = 2500.dp
private val BoundDistance = 1500.dp

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("LazyListScrolling: ${generateMsg()}")
    }
}

internal suspend fun LazyListState.doSmoothScrollToItem(
    index: Int,
    scrollOffset: Int
) {
    require(index >= 0f) { "Index should be non-negative ($index)" }
    fun getTargetItem() = layoutInfo.visibleItemsInfo.fastFirstOrNull {
        it.index == index
    }
    scroll {
        try {
            val targetDistancePx = with(density) { TargetDistance.toPx() }
            val boundDistancePx = with(density) { BoundDistance.toPx() }
            var loop = true
            var anim = AnimationState(0f)
            val targetItemInitialInfo = getTargetItem()
            if (targetItemInitialInfo != null) {
                // It's already visible, just animate directly
                throw ItemFoundInScroll(targetItemInitialInfo, anim)
            }
            val forward = index > firstVisibleItemIndex

            fun isOvershot(): Boolean {
                // Did we scroll past the item?
                @Suppress("RedundantIf") // It's way easier to understand the logic this way
                return if (forward) {
                    if (firstVisibleItemIndex > index) {
                        true
                    } else if (
                        firstVisibleItemIndex == index &&
                        firstVisibleItemScrollOffset > scrollOffset
                    ) {
                        true
                    } else {
                        false
                    }
                } else { // backward
                    if (firstVisibleItemIndex < index) {
                        true
                    } else if (
                        firstVisibleItemIndex == index &&
                        firstVisibleItemScrollOffset < scrollOffset
                    ) {
                        true
                    } else {
                        false
                    }
                }
            }

            var loops = 1
            while (loop && layoutInfo.totalItemsCount > 0) {
                val visibleItems = layoutInfo.visibleItemsInfo
                val averageSize = visibleItems.fastSumBy { it.size } / visibleItems.size
                val indexesDiff = index - firstVisibleItemIndex
                val expectedDistance = (averageSize * indexesDiff).toFloat() +
                    scrollOffset - firstVisibleItemScrollOffset
                val target = if (abs(expectedDistance) < targetDistancePx) {
                    expectedDistance
                } else {
                    if (forward) targetDistancePx else -targetDistancePx
                }

                debugLog {
                    "Scrolling to index=$index offset=$scrollOffset from " +
                        "index=$firstVisibleItemIndex offset=$firstVisibleItemScrollOffset with " +
                        "averageSize=$averageSize and calculated target=$target"
                }

                anim = anim.copy(value = 0f)
                var prevValue = 0f
                anim.animateTo(
                    target,
                    sequentialAnimation = (anim.velocity != 0f)
                ) {
                    // If we haven't found the item yet, check if it's visible.
                    var targetItem = getTargetItem()

                    if (targetItem == null) {
                        // Springs can overshoot their target, clamp to the desired range
                        val coercedValue = if (target > 0) {
                            value.coerceAtMost(target)
                        } else {
                            value.coerceAtLeast(target)
                        }
                        val delta = coercedValue - prevValue
                        debugLog {
                            "Scrolling by $delta (target: $target, coercedValue: $coercedValue)"
                        }

                        val consumed = scrollBy(delta)
                        targetItem = getTargetItem()
                        if (targetItem != null) {
                            debugLog { "Found the item after performing scrollBy()" }
                        } else if (!isOvershot()) {
                            if (delta != consumed) {
                                debugLog { "Hit end without finding the item" }
                                cancelAnimation()
                                loop = false
                                return@animateTo
                            }
                            prevValue += delta
                            if (forward) {
                                if (value > boundDistancePx) {
                                    debugLog { "Struck bound going forward" }
                                    cancelAnimation()
                                }
                            } else {
                                if (value < -boundDistancePx) {
                                    debugLog { "Struck bound going backward" }
                                    cancelAnimation()
                                }
                            }

                            // Magic constants for teleportation chosen arbitrarily by experiment
                            if (forward) {
                                if (
                                    loops >= 2 &&
                                    index - layoutInfo.visibleItemsInfo.last().index > 100
                                ) {
                                    // Teleport
                                    debugLog { "Teleport forward" }
                                    snapToItemIndexInternal(index = index - 100, scrollOffset = 0)
                                }
                            } else {
                                if (
                                    loops >= 2 &&
                                    layoutInfo.visibleItemsInfo.first().index - index > 100
                                ) {
                                    // Teleport
                                    debugLog { "Teleport backward" }
                                    snapToItemIndexInternal(index = index + 100, scrollOffset = 0)
                                }
                            }
                        }
                    }

                    // We don't throw ItemFoundInScroll when we snap, because once we've snapped to
                    // the final position, there's no need to animate to it.
                    if (isOvershot()) {
                        debugLog { "Overshot" }
                        snapToItemIndexInternal(index = index, scrollOffset = scrollOffset)
                        loop = false
                        cancelAnimation()
                        return@animateTo
                    } else if (targetItem != null) {
                        debugLog { "Found item" }
                        throw ItemFoundInScroll(targetItem, anim)
                    }
                }

                loops++
            }
        } catch (itemFound: ItemFoundInScroll) {
            // We found it, animate to it
            // Bring to the requested position - will be automatically stopped if not possible
            val anim = itemFound.previousAnimation.copy(value = 0f)
            val target = (itemFound.item.offset + scrollOffset).toFloat()
            var prevValue = 0f
            debugLog {
                "Seeking by $target at velocity ${itemFound.previousAnimation.velocity}"
            }
            anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
                // Springs can overshoot their target, clamp to the desired range
                val coercedValue = when {
                    target > 0 -> {
                        value.coerceAtMost(target)
                    }
                    target < 0 -> {
                        value.coerceAtLeast(target)
                    }
                    else -> {
                        debugLog { "WARNING: somehow ended up seeking 0px, this shouldn't happen" }
                        0f
                    }
                }
                val delta = coercedValue - prevValue
                debugLog { "Seeking by $delta (coercedValue = $coercedValue)" }
                val consumed = scrollBy(delta)
                if (delta != consumed /* hit the end, stop */ ||
                    coercedValue != value /* would have overshot, stop */
                ) {
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