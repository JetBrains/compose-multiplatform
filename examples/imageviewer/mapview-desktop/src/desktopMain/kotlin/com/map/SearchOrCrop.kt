package com.map

import kotlin.math.max

/**
 * Из кэша найти графику для нужного тайла.
 * Если нужной графики нету, то можно тайли tile с меньшим zoom и вырезать из него кусочек.
 */
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
        val tile2 = Tile(zoom, x, y) // Tile с меньшим zoom
        val img2 = get(tile2)
        if (img2 != null) {
            val deltaZoom = tile.zoom - tile2.zoom
            val i = tile.x - (x shl deltaZoom)
            val j = tile.y - (y shl deltaZoom)
            val size = max(TILE_SIZE ushr deltaZoom, 1)
            val cropImg = img2.cropAndRestoreSize(i * size, j * size, size)
            return cropImg
        }
    }
    return null
}
