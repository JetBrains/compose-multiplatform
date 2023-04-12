package example.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URL

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
 *
 * @param consumeScroll consume scroll events for disable parent scrolling
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
    onMapViewClick: (latitude: Double, longitude: Double) -> Boolean = { _, _ -> true },
    consumeScroll: Boolean = true,
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

    val onZoom = { pt: DisplayPoint?, change: Double ->
        onStateChange(internalState.zoom(pt, change).toExternalState())
    }
    val onClick = { pt: DisplayPoint ->
        val geoPoint = internalState.displayToGeo(pt)
        if (onMapViewClick(geoPoint.latitude, geoPoint.longitude)) {
            onStateChange(internalState.zoom(pt, Config.ZOOM_ON_CLICK).toExternalState())
        }
    }
    val onMove = { dx: Int, dy: Int ->
        val topLeft =
            internalState.topLeft + internalState.displayLengthToGeo(DisplayPoint(-dx, -dy))
        onStateChange(internalState.copy(topLeft = topLeft).correctGeoXY().toExternalState())
    }
    var previousMoveDownPos by remember<MutableState<Offset?>> { mutableStateOf(null) }
    var previousPressTime by remember { mutableStateOf(0L) }
    var previousPressPos by remember<MutableState<Offset?>> { mutableStateOf(null) }
    fun Modifier.applyPointerInput() = pointerInput(Unit) {
        while (true) {
            val event = awaitPointerEventScope {
                awaitPointerEvent()
            }
            val current = event.changes.firstOrNull()?.position
            if (event.type == PointerEventType.Scroll) {
                val scrollY: Float? = event.changes.firstOrNull()?.scrollDelta?.y
                if (scrollY != null && scrollY != 0f) {
                    onZoom(current?.toPt(), -scrollY * Config.SCROLL_SENSITIVITY_DESKTOP)
                }
                if (consumeScroll) {
                    event.changes.forEach {
                        it.consume()
                    }
                }
            }
            when (event.type) {
                PointerEventType.Move -> {
                    if (event.buttons.isPrimaryPressed) {
                        val previous = previousMoveDownPos
                        if (previous != null && current != null) {
                            val dx = (current.x - previous.x).toInt()
                            val dy = (current.y - previous.y).toInt()
                            if (dx != 0 || dy != 0) {
                                onMove(dx, dy)
                            }
                        }
                        previousMoveDownPos = current
                    } else {
                        previousMoveDownPos = null
                    }
                }

                PointerEventType.Press -> {
                    previousPressTime = timeMs()
                    previousPressPos = current
                    previousMoveDownPos = current
                }

                PointerEventType.Release -> {
                    if (timeMs() - previousPressTime < Config.CLICK_DURATION_MS) {
                        val previous = previousPressPos
                        if (current != null && previous != null) {
                            if (current.distanceTo(previous) < Config.CLICK_AREA_RADIUS_PX) {
                                onClick(current.toPt())
                            }
                        }
                    }
                    previousPressTime = timeMs()
                    previousMoveDownPos = null
                }
            }
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize().applyPointerInput()) {
            val p1 = size.width.toInt()
            val p2 = size.height.toInt()
            width = p1
            height = p2
            onStateChange(internalState.copy(width = p1, height = p2).toExternalState())
            clipRect() {
                displayTiles.forEach { (t, img) ->
                    if (img != null) {
                        val size = IntSize(t.size, t.size)
                        val position = IntOffset(t.x, t.y)
                        drawImage(
                            img.extract(),
                            srcOffset = IntOffset(img.offsetX, img.offsetY),
                            srcSize = IntSize(img.cropSize, img.cropSize),
                            dstOffset = position,
                            dstSize = size
                        )
                    }
                }
            }
            drawPath(path = Path().apply<Path> {
                addRect(Rect(0f, 0f, size.width, size.height))
            }, color = Color.Red, style = Stroke(4f))
        }
        Row(Modifier.align(Alignment.BottomCenter)) {
            LinkText("OpenStreetMap license", Config.OPENSTREET_MAP_LICENSE)
            LinkText("Usage policy", Config.OPENSTREET_MAP_POLICY)
        }
    }
}

fun InternalMapState.toExternalState() =
    MapState(
        centerGeo.latitude,
        centerGeo.longitude,
        scale
    )

@Composable
private fun LinkText(text: String, link: String) {
    Text(
        text = text,
        color = Color.Blue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable {
            navigateToUrl(link)
        }
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(5.dp))
            .padding(10.dp)
            .clip(RoundedCornerShape(5.dp))
    )
}

private fun navigateToUrl(url: String) {
    Desktop.getDesktop().browse(URL(url).toURI())
}

private var inMemoryCache: Map<Tile, TileImage> by mutableStateOf(mapOf())
