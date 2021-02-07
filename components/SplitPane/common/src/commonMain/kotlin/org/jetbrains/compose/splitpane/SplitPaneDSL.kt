package org.jetbrains.compose.splitpane

import androidx.compose.animation.asDisposableClock
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface SplitPaneContext {

    fun first(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit = { Box(Modifier.size(50.dp)) }
    )

    fun second(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit = { Box(
            Modifier
                .fillMaxSize()
        )
        }
    )
}

internal interface Betrayer : SplitPaneContext {
    var firstPlaceableMinimalSize: Dp
    var secondPlaceableMinimalSize: Dp
    var firstPlaceableContent: @Composable () -> Unit
    var secondPlaceableContent: @Composable () -> Unit

    companion object : Betrayer {
        override fun first(
            minSize: Dp,
            content: @Composable () -> Unit
        ) {
            firstPlaceableMinimalSize = minSize
            firstPlaceableContent = content
        }

        override fun second(
            minSize: Dp,
            content: @Composable () -> Unit
        ) {
            secondPlaceableMinimalSize = minSize
            secondPlaceableContent = content
        }

        override var firstPlaceableMinimalSize: Dp = 0.dp
        override var secondPlaceableMinimalSize: Dp = 0.dp
        override lateinit var firstPlaceableContent: @Composable () -> Unit
        override lateinit var secondPlaceableContent: @Composable () -> Unit

    }
}

@Composable
fun rememberSplitterState(
    initial: Dp = 0.dp,
    interactionState: InteractionState? = null
): SplitterState {
    val clock = AmbientAnimationClock.current.asDisposableClock()
    return remember {
        SplitterState(
            initial = initial.value,
            interactionState = interactionState,
            animationClock = clock
        )
    }
}