package example.map

val TILE_SIZE = 256

object Config {
    /**
     * Link to OpenStreetMap licensee
     */
    val OPENSTREET_MAP_LICENSE: String = "https://wiki.openstreetmap.org/wiki/OpenStreetMap_License"

    /**
     * Link to OpenStreetMap policy
     */
    val OPENSTREET_MAP_POLICY: String = "https://operations.osmfoundation.org/policies/"

    /**
     * Click duration. If duration is bigger, zoom will no happens.
     */
    val CLICK_DURATION_MS: Long = 300

    /**
     * Click area with pointer to map. If pointer drags more, then map moves.
     */
    val CLICK_AREA_RADIUS_PX: Int = 7

    /**
     * Zoom on click to map
     */
    val ZOOM_ON_CLICK = 0.8

    /**
     * Max scale on zoom event (like scroll)
     */
    val MAX_SCALE_ON_SINGLE_ZOOM_EVENT = 2.0

    /**
     * Name of temporary directory
     */
    val CACHE_DIR_NAME = "map-view-cache"

    /**
     * Sensitivity of scroll physics to zoom map
     */
    val SCROLL_SENSITIVITY_DESKTOP = 0.05

    /**
     * Minimal available zoom
     */
    val MIN_ZOOM = 0

    /**
     * Maximum available zoom
     */
    val MAX_ZOOM = 22

    /**
     * How big text should be on map
     */
    val FONT_LEVEL = 2

    fun createTileUrl(tile: Tile): String =
        with(tile) {
            "https://tile.openstreetmap.org/$zoom/$x/$y.png"
        }
}

data class Tile(
    val zoom: Int,
    val x: Int,
    val y: Int
)
