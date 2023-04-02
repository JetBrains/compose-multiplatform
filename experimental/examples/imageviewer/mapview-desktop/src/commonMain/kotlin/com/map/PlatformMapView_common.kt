package com.map


import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Workaround для склеивания MapViewAndroidDesktop и MapViewBrowser между модулями.
 * Если бы Я настроил Hierarchical Multiplatform sourceSet-ы, то такой workaround бы не потребовался
 */
@Composable
internal expect fun PlatformMapView(
    modifier: DisplayModifier,
    tiles: List<DisplayTileWithImage<TileImage>>,
    onZoom: (Pt?, Double) -> Unit,
    onClick: (Pt) -> Unit,
    onMove: (Int, Int) -> Unit,
    updateSize: (width: Int, height: Int) -> Unit
)
