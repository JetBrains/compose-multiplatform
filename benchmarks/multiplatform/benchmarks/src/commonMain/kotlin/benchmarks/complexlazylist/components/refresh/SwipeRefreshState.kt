package benchmarks.complexlazylist.components.refresh

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.min

internal const val NORMAL = 0


internal const val REFRESHING = 1


internal const val LOADING_MORE = 2

@Stable
internal class SwipeRefreshState(
    loadState: Int,
) {
    private val _indicatorOffset = Animatable(0f)
    private val mutatorMutex = MutatorMutex()

    var loadState: Int by mutableStateOf(loadState)

    /**
     * Whether a swipe/drag is currently in progress.
     */
    var isSwipeInProgress: Boolean by mutableStateOf(false)
        internal set

    var progress: SwipeProgress by mutableStateOf(SwipeProgress())
        internal set

    /**
     * The current offset for the indicator, in pixels.
     */
    internal val indicatorOffset: Float get() = _indicatorOffset.value

    internal suspend fun animateOffsetTo(
        offset: Float,
    ) {
        mutatorMutex.mutate {
            _indicatorOffset.animateTo(offset)
        }
    }

    /**
     * Dispatch scroll delta in pixels from touch events.
     */
    internal suspend fun dispatchScrollDelta(
        delta: Float,
        location: Int,
        maxOffsetY: Float,
    ) {
        mutatorMutex.mutate(MutatePriority.UserInput) {
            _indicatorOffset.snapTo((_indicatorOffset.value + delta).toFloat())
            updateProgress(
                location = location,
                maxOffsetY = maxOffsetY,
            )
        }
    }

    private fun updateProgress(
        offsetY: Float = abs(indicatorOffset),
        location: Int,
        maxOffsetY: Float,
    ) {
        val offsetPercent = min(1f, offsetY / maxOffsetY)

        val offset = min(maxOffsetY, offsetY)
        progress = SwipeProgress(location, offset, offsetPercent)
    }
}
