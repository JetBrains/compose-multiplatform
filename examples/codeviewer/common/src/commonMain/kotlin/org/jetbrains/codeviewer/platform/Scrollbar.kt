package org.jetbrains.codeviewer.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
expect fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
)

@Composable
expect fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: LazyListState
)