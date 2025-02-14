package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class MinimalSizes(
    val firstPlaceableMinimalSize: Dp,
    val secondPlaceableMinimalSize: Dp
)

/**
 * Pane that places it parts **vertically** from top to bottom and allows to change items' **heights**.
 * The [content] block defines DSL which allow you to configure top ([SplitPaneScope.first]),
 * bottom ([SplitPaneScope.second]).
 *
 * @param modifier the modifier to apply to this layout
 * @param splitPaneState the state object to be used to control or observe the split pane state
 * @param content a block which describes the content. Inside this block you can use methods like
 * [SplitPaneScope.first], [SplitPaneScope.second], to describe parts of split pane.
 */
@ExperimentalSplitPaneApi
@Composable
fun VerticalSplitPane(
    modifier: Modifier = Modifier,
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = false, splitPaneState).apply(content)) {
        SplitPane(
            modifier = modifier,
            isHorizontal = false,
            splitPaneState = splitPaneState,
            minimalSizesConfiguration = minimalSizes,
            first = firstPlaceableContent,
            second = secondPlaceableContent,
            splitter = splitter
        )
    }
}

/**
 * Pane that places it parts **horizontally** from left to right and allows to change items' **width**.
 * The [content] block defines DSL which allow you to configure left ([SplitPaneScope.first]),
 * right ([SplitPaneScope.second]) parts of split pane.
 *
 * @param modifier the modifier to apply to this layout
 * @param splitPaneState the state object to be used to control or observe the split pane state
 * @param content a block which describes the content. Inside this block you can use methods like
 * [SplitPaneScope.first], [SplitPaneScope.second], to describe parts of split pane.
 */
@ExperimentalSplitPaneApi
@Composable
fun HorizontalSplitPane(
    modifier: Modifier = Modifier,
    splitPaneState: SplitPaneState = rememberSplitPaneState(),
    content: SplitPaneScope.() -> Unit
) {
    with(SplitPaneScopeImpl(isHorizontal = true, splitPaneState).apply(content)) {
        SplitPane(
            modifier = modifier,
            isHorizontal = true,
            splitPaneState = splitPaneState,
            minimalSizesConfiguration = minimalSizes,
            first = firstPlaceableContent!!,
            second = secondPlaceableContent!!,
            splitter = splitter
        )
    }

}

/**
 * Internal implementation of default splitter
 *
 * @param isHorizontal describes whether it is a horizontal or vertical split pane
 * @param splitPaneState the state object to be used to control or observe the split pane state
 */
@OptIn(ExperimentalSplitPaneApi::class)
internal expect fun defaultSplitter(
    isHorizontal: Boolean,
    splitPaneState: SplitPaneState
): Splitter

/**
 * Internal implementation of split pane that used in all public composable functions
 *
 * @param modifier the modifier to apply to this layout
 * @param isHorizontal describes is it horizontal of vertical split pane
 * @param splitPaneState the state object to be used to control or observe the split pane state
 * @param minimalSizesConfiguration data class ([MinimalSizes]) that provides minimal size for split pane parts
 * @param first first part of split pane, left or top according to [isHorizontal]
 * @param second second part of split pane, right or bottom according to [isHorizontal]
 * @param splitter separator composable, by default [Splitter] is used
 * */
@Composable
@OptIn(ExperimentalSplitPaneApi::class)
internal expect fun SplitPane(
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = true,
    splitPaneState: SplitPaneState,
    minimalSizesConfiguration: MinimalSizes = MinimalSizes(0.dp, 0.dp),
    first: (@Composable () -> Unit)?,
    second: (@Composable () -> Unit)?,
    splitter: Splitter
)
