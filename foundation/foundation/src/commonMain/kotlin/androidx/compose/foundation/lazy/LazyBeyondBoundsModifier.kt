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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.lazy.LazyListBeyondBoundsInfo.Interval
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.BeyondBoundsScope
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

/**
 * This modifier is used to measure and place additional items when the lazyList receives a
 * request to layout items beyond the visible bounds.
 */
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.lazyListBeyondBoundsModifier(
    state: LazyListState,
    beyondBoundsInfo: LazyListBeyondBoundsInfo,
    reverseLayout: Boolean,
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    return this then remember(state, beyondBoundsInfo, reverseLayout, layoutDirection) {
        LazyListBeyondBoundsModifierLocal(state, beyondBoundsInfo, reverseLayout, layoutDirection)
    }
}

private class LazyListBeyondBoundsModifierLocal(
    private val state: LazyListState,
    private val beyondBoundsInfo: LazyListBeyondBoundsInfo,
    private val reverseLayout: Boolean,
    private val layoutDirection: LayoutDirection
) : ModifierLocalProvider<BeyondBoundsLayout?>, BeyondBoundsLayout {
    override val key: ProvidableModifierLocal<BeyondBoundsLayout?>
        get() = ModifierLocalBeyondBoundsLayout
    override val value: BeyondBoundsLayout
        get() = this

    override fun <T> layout(
        direction: BeyondBoundsLayout.LayoutDirection,
        block: BeyondBoundsScope.() -> T?
    ): T? {
        // We use a new interval each time because this function is re-entrant.
        var interval = beyondBoundsInfo.addInterval(
            state.firstVisibleItemIndex,
            state.layoutInfo.visibleItemsInfo.last().index
        )

        var found: T? = null
        while (found == null && interval.hasMoreContent(direction)) {

            // Add one extra beyond bounds item.
            interval = addNextInterval(interval, direction).also {
                beyondBoundsInfo.removeInterval(interval)
            }
            state.remeasurement?.forceRemeasure()

            // When we invoke this block, the beyond bounds items are present.
            found = block.invoke(
                object : BeyondBoundsScope {
                    override val hasMoreContent: Boolean
                        get() = interval.hasMoreContent(direction)
                }
            )
        }

        // Dispose the items that are beyond the visible bounds.
        beyondBoundsInfo.removeInterval(interval)
        state.remeasurement?.forceRemeasure()
        return found
    }

    private fun addNextInterval(
        currentInterval: Interval,
        direction: BeyondBoundsLayout.LayoutDirection
    ): Interval {
        var start = currentInterval.start
        var end = currentInterval.end
        when (direction) {
            Before -> start--
            After -> end++
            Above -> if (reverseLayout) end++ else start--
            Below -> if (reverseLayout) start-- else end++
            Left -> when (layoutDirection) {
                Ltr -> if (reverseLayout) end++ else start--
                Rtl -> if (reverseLayout) start-- else end++
            }
            Right -> when (layoutDirection) {
                Ltr -> if (reverseLayout) start-- else end++
                Rtl -> if (reverseLayout) end++ else start--
            }
            else -> unsupportedDirection()
        }
        return beyondBoundsInfo.addInterval(start, end)
    }

    private fun Interval.hasMoreContent(direction: BeyondBoundsLayout.LayoutDirection): Boolean {
        fun hasMoreItemsBefore() = start > 0
        fun hasMoreItemsAfter() = end < state.layoutInfo.totalItemsCount - 1
        return when (direction) {
            Before -> hasMoreItemsBefore()
            After -> hasMoreItemsAfter()
            Above -> if (reverseLayout) hasMoreItemsAfter() else hasMoreItemsBefore()
            Below -> if (reverseLayout) hasMoreItemsBefore() else hasMoreItemsAfter()
            Left -> when (layoutDirection) {
                Ltr -> if (reverseLayout) hasMoreItemsAfter() else hasMoreItemsBefore()
                Rtl -> if (reverseLayout) hasMoreItemsBefore() else hasMoreItemsAfter()
            }
            Right -> when (layoutDirection) {
                Ltr -> if (reverseLayout) hasMoreItemsBefore() else hasMoreItemsAfter()
                Rtl -> if (reverseLayout) hasMoreItemsAfter() else hasMoreItemsBefore()
            }
            else -> unsupportedDirection()
        }
    }
}

private fun unsupportedDirection(): Nothing = error(
    "Lazy list does not support beyond bounds layout for the specified direction"
)
