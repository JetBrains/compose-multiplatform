package com.map

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

data class MapState(
    val latitude: Double,
    val longitude: Double,
    val scale: Double,
)

/**
 * MapView to display Earth tile maps. API provided by OpenStreetMap.
 *
 * @param modifier to specify size strategy for this composable
 *
 * @param latitude initial Latitude of map center.
 * Available values between [-90.0 (South) .. 90.0 (North)]
 *
 * @param longitude initial Longitude of map center
 * Available values between [-180.0 (Left) .. 180.0 (Right)]
 *
 * @param startScale initial scale
 * (value around 1.0   = entire Earth view),
 * (value around 30.0  = Countries),
 * (value around 150.0 = Cities),
 * (value around 40000.0 = Street's)
 *
 * @param state state for Advanced usage
 * You may to configure your own state and control it.
 *
 * @param onStateChange state change handler for Advanced usage
 * You may override change state behaviour in your app
 *
 * @param onMapViewClick handle click event with point coordinates (latitude, longitude)
 * return true to enable zoom on click
 * return false to disable zoom on click
 */
@Composable
fun MapView(
    modifier: Modifier,
    userAgent: String,
    latitude: Double? = null,
    longitude: Double? = null,
    startScale: Double? = null,
    state: State<MapState> = remember {
        mutableStateOf(MapState(latitude ?: 0.0, longitude ?: 0.0, startScale ?: 1.0))
    },
    onStateChange: (MapState) -> Unit = { (state as? MutableState<MapState>)?.value = it },
    onMapViewClick: (latitude: Double, longitude: Double) -> Boolean = { lat, lon -> true },
) {
    val viewScope = rememberCoroutineScope()
    val ioScope = remember {
        CoroutineScope(SupervisorJob(viewScope.coroutineContext.job) + getDispatcherIO())
    }
    val imageRepository = rememberTilesRepository(userAgent, ioScope)

    var width: Int by remember { mutableStateOf(100) }
    var height: Int by remember { mutableStateOf(100) }
    val internalState: InternalMapState by derivedStateOf {
        val center = createGeoPt(state.value.latitude, state.value.longitude)
        InternalMapState(width, height, state.value.scale)
            .copyAndChangeCenter(center)
    }
    val displayTiles: List<DisplayTileWithImage<TileImage>> by derivedStateOf {
        val calcTiles: List<DisplayTileAndTile> = internalState.calcTiles()
        val tilesToDisplay: MutableList<DisplayTileWithImage<TileImage>> = mutableListOf()
        val tilesToLoad: MutableSet<Tile> = mutableSetOf()
        calcTiles.forEach {
            val cachedImage = inMemoryCache[it.tile]
            if (cachedImage != null) {
                tilesToDisplay.add(DisplayTileWithImage(it.display, cachedImage, it.tile))
            } else {
                tilesToLoad.add(it.tile)
                val croppedImage = inMemoryCache.searchOrCrop(it.tile)
                tilesToDisplay.add(DisplayTileWithImage(it.display, croppedImage, it.tile))
            }
        }
        viewScope.launch {
            tilesToLoad.forEach { tile ->
                try {
                    val image: TileImage = imageRepository.loadContent(tile)
                    inMemoryCache = inMemoryCache + (tile to image)
                } catch (t: Throwable) {
                    println("exception in tiles loading, throwable: $t")
                    // ignore errors. Tile image loaded with retries
                }
            }
        }
        tilesToDisplay
    }

    MapViewDesktop(
        modifier = modifier,
        isInTouchMode = false,
        tiles = displayTiles,
        onZoom = { pt: Pt?, change: Double ->
            onStateChange(internalState.zoom(pt, change).toExternalState())
        },
        onClick = { it: Pt ->
            if (onMapViewClick(
                    internalState.displayToGeo(it).latitude,
                    internalState.displayToGeo(it).longitude
                )
            ) {
                onStateChange(internalState.zoom(it, Config.ZOOM_ON_CLICK).toExternalState())
            }
        },
        onMove = { dx: Int, dy: Int ->
            val topLeft = internalState.topLeft + internalState.displayLengthToGeo(Pt(-dx, -dy))
            onStateChange(internalState.copy(topLeft = topLeft).correctGeoXY().toExternalState())
        },
        updateSize = { w: Int, h: Int ->
            width = w
            height = h
            onStateChange(internalState.copy(width = w, height = h).toExternalState())
        }
    )
}

fun InternalMapState.toExternalState() =
    MapState(
        centerGeo.latitude,
        centerGeo.longitude,
        scale
    )

private var inMemoryCache: Map<Tile, TileImage> by mutableStateOf(mapOf())
