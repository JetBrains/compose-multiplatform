package example.map

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.roundToInt

class TileImage(
    val platformSpecificData: ImageBitmap,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val cropSize: Int = TILE_SIZE,
) {
    fun lightweightDuplicate(offsetX: Int, offsetY: Int, cropSize: Int): TileImage =
        TileImage(
            platformSpecificData,
            offsetX = offsetX,
            offsetY = offsetY,
            cropSize = cropSize
        )
}

fun TileImage.cropAndRestoreSize(x: Int, y: Int, targetSize: Int): TileImage {
    val scale: Float = targetSize.toFloat() / TILE_SIZE
    val newSize = maxOf(1, (cropSize * scale).roundToInt())
    val dx = x * newSize / targetSize
    val dy = y * newSize / targetSize
    val newX = offsetX + dx
    val newY = offsetY + dy
    return lightweightDuplicate(newX % TILE_SIZE, newY % TILE_SIZE, newSize)
}
