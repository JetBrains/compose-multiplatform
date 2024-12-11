package benchmarks.complexlazylist.components.refresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal class SwipeRefreshNestedScrollConnection(
    private val state: SwipeRefreshState,
    private val coroutineScope: CoroutineScope,
    private val onRefresh: () -> Unit,
    private val onLoadMore: () -> Unit
) : NestedScrollConnection {
    var refreshEnabled: Boolean = false
    var loadMoreEnabled: Boolean = false
    var refreshTrigger: Float = 100f
    var indicatorHeight: Float = 50f

    private var isTop = false
    private var isBottom = false

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset = when {
        !refreshEnabled && !loadMoreEnabled -> Offset.Zero
        state.loadState != NORMAL -> Offset.Zero
        source == NestedScrollSource.Drag -> {
            if (available.y > 0 && isBottom) {
                onScroll(available)
            } else if (available.y < 0 && isTop) {
                onScroll(available)
            } else {
                Offset.Zero
            }
        }
        else -> Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {

        if (!refreshEnabled && !loadMoreEnabled) {
            return Offset.Zero
        }

        else if (state.loadState != NORMAL) {
            return Offset.Zero
        } else if (source == NestedScrollSource.Drag) {
            if (available.y < 0) {
                if (!isBottom) {
                    isBottom = true
                }
                if (isBottom) {
                    return onScroll(available)
                }

            } else if (available.y > 0) {
                if (!isTop) {
                    isTop = true
                }
                if (isTop) {
                    return onScroll(available)
                }
            }
        }
        return Offset.Zero
    }

    private fun onScroll(available: Offset): Offset {
        if (!isBottom && !isTop) {
            return Offset.Zero
        }
        if (available.y > 0 && isTop) {
            state.isSwipeInProgress = true
        } else if (available.y < 0 && isBottom) {
            state.isSwipeInProgress = true
        } else if (state.indicatorOffset.roundToInt() == 0) {
            state.isSwipeInProgress = false
        }

        val newOffset = (available.y + state.indicatorOffset).let {
            if (isTop) it.coerceAtLeast(0.0F) else it.coerceAtMost(0.0F)
        }
        val dragConsumed = newOffset - state.indicatorOffset

        return if (dragConsumed.absoluteValue >= 0.5f) {
            coroutineScope.launch {
                state.dispatchScrollDelta(
                    dragConsumed,
                    if (isTop) TOP else BOTTOM,
                    refreshTrigger,
                )
            }
            // Return the consumed Y
            Offset(x = 0f, y = dragConsumed)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // If we're dragging, not currently refreshing and scrolled
        // past the trigger point, refresh!
        if (state.loadState == NORMAL && abs(state.indicatorOffset) >= indicatorHeight) {
            if (isTop) {
                onRefresh()
            } else if (isBottom) {
                onLoadMore()
            }
        }

        // Reset the drag in progress state
        state.isSwipeInProgress = false

        // Don't consume any velocity, to allow the scrolling layout to fling
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity.Zero.also {
            isTop = false
            isBottom = false
        }
    }
}