package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.movable.SplitPaneState
import org.jetbrains.compose.movable.SplitterState

data class MinimalSizes(
    val firstPlaceableMinimalSize: Dp,
    val secondPlaceableMinimalSize: Dp
)

@Composable
fun VerticalSplitPane(
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    modifier: Modifier = Modifier,
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = false, splitPaneState).apply(content)) {
        if (firstPlaceableContent != null && secondPlaceableContent != null) {
            SplitPane(
                modifier,
                isHorizontal = false,
                splitPaneState.splitterState,
                minimalSizes,
                firstPlaceableContent!!,
                secondPlaceableContent!!,
                splitter ?: { Splitter(isHorizontal = false, splitPaneState)}
            )
        } else {
            firstPlaceableContent?.invoke()
            secondPlaceableContent?.invoke()
        }
    }

}

@Composable
fun HorizontalSplitPane(
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    modifier: Modifier = Modifier,
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = true, splitPaneState).apply(content)) {
        if (firstPlaceableContent != null && secondPlaceableContent != null) {
            SplitPane(
                modifier,
                isHorizontal = true,
                splitPaneState.splitterState,
                minimalSizes,
                firstPlaceableContent!!,
                secondPlaceableContent!!,
                splitter ?: { Splitter(isHorizontal = true, splitPaneState)}
            )
        } else {
            firstPlaceableContent?.invoke()
            secondPlaceableContent?.invoke()
        }
    }

}

@Composable
internal expect fun Splitter(
    isHorizontal: Boolean,
    splitPaneState: SplitPaneState
)

@Composable
internal expect fun SplitPane(
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = true,
    state: SplitterState,
    minimalSizesConfiguration: MinimalSizes  = MinimalSizes(0.dp, 0.dp),
    first: @Composable ()->Unit,
    second: @Composable ()->Unit,
    separator: @Composable ()->Unit
)