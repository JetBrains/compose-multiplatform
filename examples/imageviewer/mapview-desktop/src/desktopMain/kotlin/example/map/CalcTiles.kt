package example.map

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.roundToInt

fun example.map.InternalMapState.calcTiles(): List<example.map.DisplayTileAndTile> {
    fun geoLengthToDisplay(geoLength: Double): Int {
        return (height * geoLength * scale).toInt()
    }

    val zoom: Int = minOf(
        example.map.Config.MAX_ZOOM,
        maxOf(
            example.map.Config.MIN_ZOOM,
            ceil(log2(geoLengthToDisplay(1.0) / example.map.TILE_SIZE.toDouble())).roundToInt() - example.map.Config.FONT_LEVEL
        )
    )
    val maxTileIndex: Int = example.map.pow2(zoom)
    val tileSize: Int = geoLengthToDisplay(1.0) / maxTileIndex + 1
    val minI = (topLeft.x * maxTileIndex).toInt()
    val minJ = (topLeft.y * maxTileIndex).toInt()

    fun geoXToDisplay(x: Double): Int = geoLengthToDisplay(x - topLeft.x)
    fun geoYToDisplay(y: Double): Int = geoLengthToDisplay(y - topLeft.y)

    val tiles: List<example.map.DisplayTileAndTile> = buildList {
        for (i in minI until Int.MAX_VALUE) {
            val geoX = i.toDouble() / maxTileIndex
            val displayX = geoXToDisplay(geoX)
            if (displayX >= width) {
                break
            }
            for (j in minJ until Int.MAX_VALUE) {
                val geoY = j.toDouble() / maxTileIndex
                val displayY = geoYToDisplay(geoY)
                if (displayY >= height) {
                    break
                }
                val tile = example.map.Tile(zoom, i % maxTileIndex, j % maxTileIndex)
                add(
                    example.map.DisplayTileAndTile(
                        example.map.DisplayTile(tileSize, displayX, displayY),
                        tile
                    )
                )
            }
        }
    }
    return tiles
}
