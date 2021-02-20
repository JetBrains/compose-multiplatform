package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable

@ExperimentalSplitPaneApi
enum class SplitterHandleAlignment {
    BEFORE,
    ABOVE,
    AFTER
}

@OptIn(ExperimentalSplitPaneApi::class)
internal data class Splitter(
    val measuredPart: @Composable () -> Unit,
    val handlePart: @Composable () -> Unit = measuredPart,
    val alignment: SplitterHandleAlignment = SplitterHandleAlignment.ABOVE
)
