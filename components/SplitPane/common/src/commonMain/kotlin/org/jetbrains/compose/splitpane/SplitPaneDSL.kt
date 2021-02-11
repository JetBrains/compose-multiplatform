package org.jetbrains.compose.splitpane

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface SplitPaneScope {

    fun first(
        minSize: Dp = 0.dp,
        content: @Composable () -> Unit
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

internal class SplitPaneScopeImpl : SplitPaneScope {

    private var firstPlaceableMinimalSize: Dp = 0.dp
    private var secondPlaceableMinimalSize: Dp = 0.dp

    internal val minimalSizes: MinimalSizes
        get() = MinimalSizes(firstPlaceableMinimalSize, secondPlaceableMinimalSize)

    internal lateinit var firstPlaceableContent: @Composable () -> Unit
    internal lateinit var secondPlaceableContent: @Composable () -> Unit

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

}

@Composable
fun rememberSplitterState(
    initial: Dp = 0.dp,
    interactionState: InteractionState? = null
): SplitterState {
    return remember {
        SplitterState(
            initial = initial.value,
            interactionState = interactionState
        )
    }
}