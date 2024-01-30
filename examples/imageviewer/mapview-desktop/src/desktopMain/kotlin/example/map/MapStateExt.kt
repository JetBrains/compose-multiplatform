package example.map

fun InternalMapState.geoLengthToDisplay(geoLength: Double): Int {
    return (height * geoLength * scale).toInt()
}

fun InternalMapState.geoXToDisplay(x: Double): Int = geoLengthToDisplay(x - topLeft.x)
fun InternalMapState.geoYToDisplay(y: Double): Int = geoLengthToDisplay(y - topLeft.y)

@Suppress("unused")
fun InternalMapState.geoToDisplay(geoPt: GeoPoint): DisplayPoint =
    DisplayPoint(geoXToDisplay(geoPt.x), geoYToDisplay(geoPt.y))

fun InternalMapState.displayLengthToGeo(displayLength: Int): Double =
    displayLength / (scale * height)

fun InternalMapState.displayLengthToGeo(pt: DisplayPoint): GeoPoint =
    GeoPoint(displayLengthToGeo(pt.x), displayLengthToGeo(pt.y))

fun InternalMapState.displayToGeo(displayPt: DisplayPoint): GeoPoint {
    val x1 = displayLengthToGeo((displayPt.x))
    val y1 = displayLengthToGeo((displayPt.y))
    return topLeft + GeoPoint(x1, y1)
}

@Suppress("unused")
val InternalMapState.minScale get(): Double = 1.0

val InternalMapState.maxScale
    get(): Double =
        (TILE_SIZE.toDouble() / height) * fastPow2ForPositiveInt(Config.MAX_ZOOM)

internal fun fastPow2ForPositiveInt(x: Int): Int {
    if (x < 0) {
        return 0
    }
    return 1 shl x
}

fun InternalMapState.zoom(zoomCenter: DisplayPoint?, change: Double): InternalMapState {
    val state = this
    val pt = zoomCenter ?: DisplayPoint(state.width / 2, state.height / 2)
    var multiply = (1 + change)
    if (multiply < 1 / Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT) {
        multiply = 1 / Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT
    } else if (multiply > Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT) {
        multiply = Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT
    }
    var scale = state.scale * multiply
    scale = scale.coerceIn(state.minScale..state.maxScale)
    val scaledState = state.copy(scale = scale)
    val geoDelta = state.displayToGeo(pt) - scaledState.displayToGeo(pt)
    return scaledState.copy(topLeft = scaledState.topLeft + geoDelta).correctGeoXY()
}
