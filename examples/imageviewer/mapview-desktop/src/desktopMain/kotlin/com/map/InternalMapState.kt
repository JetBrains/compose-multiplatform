package com.map

data class InternalMapState(
    val width: Int = 100, // display width in dp (pixels)
    val height: Int = 100,//display height in dp (pixels)
    val scale: Double = 1.0,
    val topLeft: GeoPt = GeoPt(0.0, 0.0),
)

data class DisplayTileWithImage<T>(
    val displayTile: DisplayTile,
    val image: T?,
    val tile: Tile,
)

data class DisplayTile(
    val size: Int,//Размер на экране
    val x: Int,//координаты на экране
    val y: Int
)

data class DisplayTileAndTile(
    val display: DisplayTile,
    val tile: Tile
)

val InternalMapState.centerGeo get():GeoPt = displayToGeo(Pt(width / 2, height / 2))
fun InternalMapState.copyAndChangeCenter(targetCenter: GeoPt): InternalMapState =
    copy(
        topLeft = topLeft + targetCenter - centerGeo
    ).correctGeoXY()

/**
 * Корректируем координаты, чтобы они не выходили за край карты.
 */
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

fun InternalMapState.correctGeoX(): InternalMapState = copy(topLeft = topLeft.copy(x = topLeft.x.mod(1.0)))

