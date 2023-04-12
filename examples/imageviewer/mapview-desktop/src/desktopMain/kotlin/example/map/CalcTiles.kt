package example.map

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.roundToInt

fun InternalMapState.calcTiles(): List<DisplayTileAndTile> {
    fun geoLengthToDisplay(geoLength: Double): Int {
        return (height * geoLength * scale).toInt()
    }

    val zoom: Int = minOf(
        Config.MAX_ZOOM,
        maxOf(
            Config.MIN_ZOOM,
            ceil(log2(geoLengthToDisplay(1.0) / TILE_SIZE.toDouble())).roundToInt() - Config.FONT_LEVEL
        )
    )
    val maxTileIndex: Int = fastPow2ForPositiveInt(zoom)
    val tileSize: Int = geoLengthToDisplay(1.0) / maxTileIndex + 1
    val minCol = (topLeft.x * maxTileIndex).toInt()
    val minRow = (topLeft.y * maxTileIndex).toInt()

    fun geoXToDisplay(x: Double): Int = geoLengthToDisplay(x - topLeft.x)
    fun geoYToDisplay(y: Double): Int = geoLengthToDisplay(y - topLeft.y)

    val tiles: List<DisplayTileAndTile> = buildList {
        for (col in minCol until Int.MAX_VALUE) {
            val geoX = col.toDouble() / maxTileIndex
            val displayX = geoXToDisplay(geoX)
            if (displayX >= width) {
                break
            }
            for (row in minRow until Int.MAX_VALUE) {
                val geoY = row.toDouble() / maxTileIndex
                val displayY = geoYToDisplay(geoY)
                if (displayY >= height) {
                    break
                }
                val tile = Tile(zoom, col % maxTileIndex, row % maxTileIndex)
                add(
                    DisplayTileAndTile(
                        DisplayTile(tileSize, displayX, displayY),
                        tile
                    )
                )
            }
        }
    }
    return tiles
}
