package benchmarks.complexlazylist.components.refresh

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
internal fun SwipeRefreshLayout(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTriggerDistance: Dp = 120.dp,
    indicationHeight: Dp = 60.dp,
    refreshEnabled: Boolean = true,
    loadMoreEnabled: Boolean = true,
    indicator: @Composable BoxScope.(modifier: Modifier, state: SwipeRefreshState, indicatorHeight: Dp) -> Unit = { m, s, height ->
        LoadingIndicatorDefault(m, s, height)
    },
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val refreshTriggerPx = with(LocalDensity.current) { refreshTriggerDistance.toPx() }
    val indicationHeightPx = with(LocalDensity.current) { indicationHeight.toPx() }

    // Our LaunchedEffect, which animates the indicator to its resting position
    LaunchedEffect(state.isSwipeInProgress) {
        if (!state.isSwipeInProgress) {
            // If there's not a swipe in progress, rest the indicator at 0f
            state.animateOffsetTo(0f)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val updatedOnRefresh = rememberUpdatedState(onRefresh)
    val updatedOnLoadMore = rememberUpdatedState(onLoadMore)

    val nestedScrollConnection = remember(state, coroutineScope) {
        SwipeRefreshNestedScrollConnection(
            state,
            coroutineScope,
            onRefresh = { updatedOnRefresh.value.invoke() },
            onLoadMore = { updatedOnLoadMore.value.invoke() }
        )
    }.apply {
        this.refreshEnabled = refreshEnabled
        this.loadMoreEnabled = loadMoreEnabled
        this.refreshTrigger = refreshTriggerPx
        this.indicatorHeight = indicationHeightPx
    }

    BoxWithConstraints(modifier.nestedScroll(connection = nestedScrollConnection)) {
        if (!state.isSwipeInProgress)
            LaunchedEffect((state.loadState == REFRESHING || state.loadState == LOADING_MORE)) {
                animate(
                    animationSpec = tween(durationMillis = 300),
                    initialValue = state.progress.offset,
                    targetValue = when (state.loadState) {
                        LOADING_MORE -> indicationHeightPx
                        REFRESHING -> indicationHeightPx
                        else -> 0f
                    }
                ) { value, _ ->
                    if (!state.isSwipeInProgress) {
                        state.progress = state.progress.copy(
                            offset = value,
                            fraction = min(1f, value / refreshTriggerPx)
                        )
                    }
                }
            }

        val offsetDp = with(LocalDensity.current) {
            state.progress.offset.toDp()
        }
        content(
            when (state.progress.location) {
                TOP -> Modifier.padding(top = offsetDp)
                BOTTOM -> Modifier.padding(bottom = offsetDp)
                else -> Modifier
            }
        )
        if (state.progress.location != NONE) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(refreshTriggerDistance)
                .graphicsLayer {
                    translationY =
                        if (state.progress.location == LOADING_MORE) constraints.maxHeight - state.progress.offset
                        else state.progress.offset - refreshTriggerPx
                }
            ) {
                indicator(
                    Modifier.align(if (state.progress.location == TOP) Alignment.BottomStart else Alignment.TopStart),
                    state,
                    indicationHeight
                )
            }
        }
    }
}

@Composable
internal fun rememberSwipeRefreshState(state: Int) = remember { SwipeRefreshState(state) }