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

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Adapts nested scroll from View to Compose. This class is used by [ComposeView] to bridge
 * nested scrolling across View and Compose. It acts as both:
 * 1) [androidx.core.view.NestedScrollingChild3] by using an instance of
 * [NestedScrollingChildHelper] to dispatch scroll deltas up to a consuming parent on the view side.
 * 2) [NestedScrollingChildHelper] by implementing this interface it should be able to receive
 * deltas from dispatching children on the Compose side.
 */
internal class NestedScrollInteropConnection(
    private val view: View
) : NestedScrollConnection {

    private val nestedScrollChildHelper = NestedScrollingChildHelper(view).apply {
        isNestedScrollingEnabled = true
    }

    private val consumedScrollCache = IntArray(2)

    init {
        // Enables nested scrolling for the root view [AndroidComposeView].
        // Like in Compose, nested scrolling is a default implementation
        ViewCompat.setNestedScrollingEnabled(view, true)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Using the return of startNestedScroll to determine if nested scrolling will happen.
        if (nestedScrollChildHelper.startNestedScroll(
                available.scrollAxes,
                source.toViewType()
            )
        ) {
            // reuse
            consumedScrollCache.fill(0)

            nestedScrollChildHelper.dispatchNestedPreScroll(
                composeToViewOffset(available.x),
                composeToViewOffset(available.y),
                consumedScrollCache,
                null,
                source.toViewType()
            )

            return toOffset(consumedScrollCache, available)
        }

        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Using the return of startNestedScroll to determine if nested scrolling will happen.
        if (nestedScrollChildHelper.startNestedScroll(
                available.scrollAxes,
                source.toViewType()
            )
        ) {
            consumedScrollCache.fill(0)

            nestedScrollChildHelper.dispatchNestedScroll(
                composeToViewOffset(consumed.x),
                composeToViewOffset(consumed.y),
                composeToViewOffset(available.x),
                composeToViewOffset(available.y),
                null,
                source.toViewType(),
                consumedScrollCache,
            )

            return toOffset(consumedScrollCache, available)
        }

        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {

        val result = if (nestedScrollChildHelper.dispatchNestedPreFling(
                available.x.toViewVelocity(),
                available.y.toViewVelocity(),
            )
        ) {
            available
        } else {
            Velocity.Zero
        }

        interruptOngoingScrolls()

        return result
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val result = if (nestedScrollChildHelper.dispatchNestedFling(
                available.x.toViewVelocity(),
                available.y.toViewVelocity(),
                true
            )
        ) {
            available
        } else {
            Velocity.Zero
        }

        interruptOngoingScrolls()

        return result
    }

    private fun interruptOngoingScrolls() {
        if (nestedScrollChildHelper.hasNestedScrollingParent(TYPE_TOUCH)) {
            nestedScrollChildHelper.stopNestedScroll(TYPE_TOUCH)
        }

        if (nestedScrollChildHelper.hasNestedScrollingParent(TYPE_NON_TOUCH)) {
            nestedScrollChildHelper.stopNestedScroll(TYPE_NON_TOUCH)
        }
    }
}

// Relative ceil for rounding. Ceiling away from zero to avoid missing scrolling deltas to rounding
// issues.
private fun Float.ceilAwayFromZero(): Float = if (this >= 0) ceil(this) else floor(this)

// Compose coordinate system is the opposite of view's system
internal fun composeToViewOffset(offset: Float): Int = offset.ceilAwayFromZero().toInt() * -1

// Compose scrolling sign system is the opposite of view's system
private fun Int.reverseAxis(): Float = this * -1f

private fun Float.toViewVelocity(): Float = this * -1f

/**
 * Converts the view world array into compose [Offset] entity. This is bound by the values in the
 * available [Offset] in order to account for rounding errors produced by the Int to Float
 * conversions.
 */
private fun toOffset(consumed: IntArray, available: Offset): Offset {
    val offsetX = if (available.x >= 0) {
        consumed[0].reverseAxis().coerceAtMost(available.x)
    } else {
        consumed[0].reverseAxis().coerceAtLeast(available.x)
    }

    val offsetY = if (available.y >= 0) {
        consumed[1].reverseAxis().coerceAtMost(available.y)
    } else {
        consumed[1].reverseAxis().coerceAtLeast(available.y)
    }

    return Offset(offsetX, offsetY)
}

private fun NestedScrollSource.toViewType(): Int = when (this) {
    NestedScrollSource.Drag -> TYPE_TOUCH
    else -> TYPE_NON_TOUCH
}

// TODO (levima) Maybe use a more accurate threshold?
private const val ScrollingAxesThreshold = 0.5f

/**
 * Make an assumption that the scrolling axes is determined by a threshold of 0.5 on either
 * direction.
 */
private val Offset.scrollAxes: Int
    get() {
        var axes = ViewCompat.SCROLL_AXIS_NONE
        if (x.absoluteValue >= ScrollingAxesThreshold) {
            axes = axes or ViewCompat.SCROLL_AXIS_HORIZONTAL
        }
        if (y.absoluteValue >= ScrollingAxesThreshold) {
            axes = axes or ViewCompat.SCROLL_AXIS_VERTICAL
        }
        return axes
    }

/**
 * Create and [remember] the [NestedScrollConnection] that enables Nested Scroll Interop
 * between a View parent that implements [androidx.core.view.NestedScrollingParent3] and a
 * Compose child. This should be used in conjunction with a
 * [androidx.compose.ui.input.nestedscroll.nestedScroll] modifier. Nested Scroll is enabled by
 * default on the compose side and you can use this connection to enable both nested scroll on the
 * view side and to add glue logic between View and compose.
 *
 * Note that this only covers the use case where a cooperating parent is used. A cooperating parent
 * is one that implements NestedScrollingParent3, a key layout that does that is
 * [androidx.coordinatorlayout.widget.CoordinatorLayout].
 *
 * Learn how to enable nested scroll interop:
 * @sample androidx.compose.ui.samples.ComposeInCooperatingViewNestedScrollInteropSample
 *
 */
@ExperimentalComposeUiApi
@Composable
fun rememberNestedScrollInteropConnection(): NestedScrollConnection {
    val composeView = LocalView.current
    return remember(composeView) {
        NestedScrollInteropConnection(composeView)
    }
}