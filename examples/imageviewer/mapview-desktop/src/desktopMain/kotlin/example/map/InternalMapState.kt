package example.map

data class InternalMapState(
    val width: Int = 100,
    val height: Int = 100,
    val scale: Double = 1.0,
    val topLeft: GeoPoint = GeoPoint(0.0, 0.0),
)

data class DisplayTileWithImage<T>(
    val displayTile: DisplayTile,
    val image: T?,
    val tile: Tile,
)

data class DisplayTile(
    val size: Int,
    val x: Int,
    val y: Int
)

data class DisplayTileAndTile(
    val display: DisplayTile,
    val tile: Tile
)

val InternalMapState.centerGeo get(): GeoPoint = displayToGeo(DisplayPoint(width / 2, height / 2))
fun InternalMapState.copyAndChangeCenter(targetCenter: GeoPoint): InternalMapState =
    copy(
        topLeft = topLeft + targetCenter - centerGeo
    ).correctGeoXY()

fun InternalMapState.correctGeoXY(): InternalMapState =
    correctGeoX().correctGeoY()

fun InternalMapState.correctGeoY(): InternalMapState {
    val minGeoY = 0.0
    val maxGeoY: Double = 1 - 1 / scale
    return if (topLeft.y < minGeoY) {
        copy(topLeft = topLeft.copy(y = minGeoY))
    } else if (topLeft.y > maxGeoY) {
        copy(topLeft = topLeft.copy(y = maxGeoY))
    } else {
        this
    }
}

fun InternalMapState.correctGeoX(): InternalMapState =
    copy(topLeft = topLeft.copy(x = topLeft.x.mod(1.0)))

