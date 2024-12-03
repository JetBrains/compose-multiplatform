package benchmarks.complexlazylist.components.refresh

import androidx.compose.runtime.Immutable

internal const val NONE = 0

internal const val TOP = 1

internal const val BOTTOM = 2

@Immutable
internal data class SwipeProgress(
    val location: Int = NONE,
    val offset: Float = 0f,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    val fraction: Float = 0f
)
