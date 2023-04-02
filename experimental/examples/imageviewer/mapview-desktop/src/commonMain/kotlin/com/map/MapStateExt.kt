package com.map

fun InternalMapState.geoLengthToDisplay(geoLength: Double): Int {
    return (height * geoLength * scale).toInt()
}

fun InternalMapState.geoXToDisplay(x: Double): Int = geoLengthToDisplay(x - topLeft.x)
fun InternalMapState.geoYToDisplay(y: Double): Int = geoLengthToDisplay(y - topLeft.y)
fun InternalMapState.geoToDisplay(geoPt: GeoPt): Pt = Pt(geoXToDisplay(geoPt.x), geoYToDisplay(geoPt.y))
fun InternalMapState.displayLengthToGeo(displayLength: Int): Double = displayLength / (scale * height)
fun InternalMapState.displayLengthToGeo(pt: Pt): GeoPt = GeoPt(displayLengthToGeo(pt.x), displayLengthToGeo(pt.y))

fun InternalMapState.displayToGeo(displayPt: Pt): GeoPt {
    val x1 = displayLengthToGeo((displayPt.x))
    val y1 = displayLengthToGeo((displayPt.y))
    return topLeft + GeoPt(x1, y1)
}

@Suppress("unused")
val InternalMapState.minScale
    get():Double = 1.0
val InternalMapState.maxScale get():Double = (TILE_SIZE.toDouble() / height) * pow2(Config.MAX_ZOOM)

/**
 * Функция 2^x
 */
fun pow2(x: Int): Int {
    if (x < 0) {
        return 0
    }
    return 1 shl x
}

fun InternalMapState.zoom(zoomCenter:Pt?, change:Double):InternalMapState {
    val state = this
    val pt = zoomCenter ?: Pt(state.width / 2, state.height / 2)
    var multiply = (1 + change)
    if (multiply < 1 / Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT) {
        multiply = 1 / Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT
    } else if (multiply > Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT) {
        multiply = Config.MAX_SCALE_ON_SINGLE_ZOOM_EVENT
    }
    var scale = state.scale * multiply
    if (scale < state.minScale) {
        scale = state.minScale
    }
    if (scale > state.maxScale) {
        scale = state.maxScale
    }
    val scaledState = state.copy(scale = scale)
    val geoDelta = state.displayToGeo(pt) - scaledState.displayToGeo(pt)
    return scaledState.copy(topLeft = scaledState.topLeft + geoDelta).correctGeoXY()
}
