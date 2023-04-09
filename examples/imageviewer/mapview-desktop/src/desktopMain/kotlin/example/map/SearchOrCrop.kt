package example.map

import kotlin.math.max

fun Map<Tile, TileImage>.searchOrCrop(tile: Tile): TileImage? {
    val img1 = get(tile)
    if (img1 != null) {
        return img1
    }
    var zoom = tile.zoom
    var x = tile.x
    var y = tile.y
    while (zoom > 0) {
        zoom--
        x /= 2
        y /= 2
        val tile2 = Tile(zoom, x, y)
        val img2 = get(tile2)
        if (img2 != null) {
            val deltaZoom = tile.zoom - tile2.zoom
            val i = tile.x - (x shl deltaZoom)
            val j = tile.y - (y shl deltaZoom)
            val size = max(TILE_SIZE ushr deltaZoom, 1)
            return img2.cropAndRestoreSize(i * size, j * size, size)
        }
    }
    return null
}
