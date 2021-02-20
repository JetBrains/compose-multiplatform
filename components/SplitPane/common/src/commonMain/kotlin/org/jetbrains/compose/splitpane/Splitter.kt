package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable

@ExperimentalSplitPaneApi
enum class SplitterHandleAlign {
    BEFORE,
    ABOVE,
    AFTER
}

@OptIn(ExperimentalSplitPaneApi::class)
internal data class Splitter(
    val measuredPart: @Composable () -> Unit,
    val handlePart: @Composable () -> Unit = measuredPart,
    val align: SplitterHandleAlign = SplitterHandleAlign.ABOVE
)
