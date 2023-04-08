package com.map

val TILE_SIZE = 256

object Config {
    val OPENSTREET_MAP_LICENSE: String = "https://wiki.openstreetmap.org/wiki/OpenStreetMap_License"
    val OPENSTREET_MAP_POLICY: String = "https://operations.osmfoundation.org/policies/"
    val CLICK_DURATION_MS: Long = 300
    val CLICK_AREA_RADIUS_PX: Int = 7
    val ZOOM_ON_CLICK = 0.8
    val MAX_SCALE_ON_SINGLE_ZOOM_EVENT = 2.0
    val CACHE_DIR_NAME = "map-view-cache"
    val MIN_ZOOM = 0
    val MAX_ZOOM = 22
    val FONT_LEVEL = 2

    fun createTileUrl(zoom: Int, x: Int, y: Int): String {
        return "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    }
}

data class Tile(
    val zoom: Int,
    val x: Int,
    val y: Int
)
