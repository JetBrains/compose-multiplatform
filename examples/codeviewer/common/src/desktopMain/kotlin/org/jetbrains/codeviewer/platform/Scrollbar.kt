package org.jetbrains.codeviewer.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LazyScrollbarAdapter
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Dp

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = androidx.compose.foundation.VerticalScrollbar(
    modifier,
    adapter = rememberScrollbarAdapter(scrollState)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: LazyListState,
    itemCount: Int,
    averageItemSize: Dp
) = androidx.compose.foundation.VerticalScrollbar(
    modifier,
    adapter = rememberScrollbarAdapterFixed(scrollState, itemCount, averageItemSize)
)

// TODO/migrateToMaster should be fixed in androidx-master-dev
@Composable
fun rememberScrollbarAdapterFixed(
    scrollState: LazyListState,
    itemCount: Int,
    averageItemSize: Dp
): LazyScrollbarAdapter {
    val density = DensityAmbient.current
    return remember(density, scrollState, itemCount, averageItemSize) {
        with(density) {
            LazyScrollbarAdapter(scrollState, itemCount, averageItemSize.toPx())
        }
    }
}