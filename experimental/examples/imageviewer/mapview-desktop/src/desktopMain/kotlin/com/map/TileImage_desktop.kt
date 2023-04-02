package com.map

import androidx.compose.ui.graphics.ImageBitmap

actual class TileImage(
    val platformSpecificData: ImageBitmap,
    actual val offsetX: Int = 0,
    actual val offsetY: Int = 0,
    actual val cropSize: Int = TILE_SIZE,
) {
    actual fun lightweightDuplicate(offsetX: Int, offsetY: Int, cropSize: Int): TileImage =
        TileImage(
            platformSpecificData,
            offsetX = offsetX,
            offsetY = offsetY,
            cropSize = cropSize
        )
}
