package com.map


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Workaround для склеивания MapViewAndroidDesktop и MapViewBrowser между модулями.
 * Если бы Я настроил Hierarchical Multiplatform sourceSet-ы, то такой workaround бы не потребовался
 */

@Composable
internal fun PlatformMapView(
    modifier: Modifier,
    tiles: List<DisplayTileWithImage<TileImage>>,
    onZoom: (Pt?, Double) -> Unit,
    onClick: (Pt) -> Unit,
    onMove: (Int, Int) -> Unit,
    updateSize: (width: Int, height: Int) -> Unit
) {
    MapViewAndroidDesktop(
        modifier = modifier,
        isInTouchMode = false,
        tiles = tiles,
        onZoom = onZoom,
        onClick = onClick,
        onMove = onMove,
        updateSize = updateSize
    )
}

