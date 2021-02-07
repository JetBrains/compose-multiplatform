package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.animation.SingleDirectionMoveState

private const val Horizontal = true

typealias SplitterState = SingleDirectionMoveState

data class MinimalSizes(
    val firstPlaceableMinimalSize: Dp,
    val secondPlaceableMinimalSize: Dp
)

@Composable
fun VerticalSplitPane(
    splitterState: SplitterState,
    modifier: Modifier = Modifier,
    content: SplitPaneContext.() -> Unit
) {
    val bet: Betrayer = Betrayer.apply(content)

    SplitPane(
        modifier,
        !Horizontal,
        splitterState.apply { minValue = bet.firstPlaceableMinimalSize.value },
        MinimalSizes(
            bet.firstPlaceableMinimalSize,
            bet.secondPlaceableMinimalSize
        ),
        bet.firstPlaceableContent,
        bet.secondPlaceableContent,
        { Separator(!Horizontal, splitterState::smoothMoveBy)}
    )
}

@Composable
fun HorizontalSplitPane(
    splitterState: SplitterState,
    modifier: Modifier = Modifier,
    content: SplitPaneContext.() -> Unit
) {
    val bet: Betrayer = Betrayer.apply(content)
    SplitPane(
        modifier,
        Horizontal,
        splitterState.apply { minValue = bet.firstPlaceableMinimalSize.value },
        MinimalSizes(
            bet.firstPlaceableMinimalSize,
            bet.secondPlaceableMinimalSize
        ),
        bet.firstPlaceableContent,
        bet.secondPlaceableContent,
        { Separator(Horizontal, splitterState::smoothMoveBy)}
    )
}

@Composable
expect fun Separator(
    isHorizontal: Boolean,
    consumeMovement: (delta: Float) -> Unit
)

@Composable
expect fun SplitPane(
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = true,
    state: SplitterState,
    minimalSizesConfiguration: MinimalSizes  = MinimalSizes(0.dp, 0.dp),
    first: @Composable ()->Unit,
    second: @Composable ()->Unit,
    separator: @Composable ()->Unit
)