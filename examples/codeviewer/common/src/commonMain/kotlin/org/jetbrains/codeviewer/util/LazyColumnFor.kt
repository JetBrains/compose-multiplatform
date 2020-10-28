package org.jetbrains.codeviewer.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LazyColumnFor(
    size: Int,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) = LazyColumnForIndexed(
    UnitList(size),
    modifier,
    state,
    contentPadding,
    horizontalGravity,
) { index, _ ->
    itemContent(index)
}

private class UnitList(override val size: Int) : AbstractList<Unit>() {
    override fun get(index: Int) = Unit
}