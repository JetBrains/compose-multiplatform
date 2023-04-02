package com.map

import kotlin.math.roundToInt

/**
 * Картинка в удобном представлении для рисования на конкретной платформе.
 * Требуется чтобы отрисовка на Canvas происходила быстро.
 */
expect class TileImage {
    val cropSize: Int
    val offsetX: Int
    val offsetY: Int
    fun lightweightDuplicate(offsetX: Int, offsetY: Int, cropSize: Int): TileImage
}

/**
 * Вырезать нужный кусочек и сделать scale до исходного размера
 */
fun TileImage.cropAndRestoreSize(x: Int, y: Int, targetSize: Int): TileImage {
    val scale: Float = targetSize.toFloat() / TILE_SIZE
    val newSize = maxOf(1, (cropSize * scale).roundToInt())
    val dx = x * newSize / targetSize
    val dy = y * newSize / targetSize
    val newX = offsetX + dx
    val newY = offsetY + dy
    return lightweightDuplicate(newX % TILE_SIZE, newY % TILE_SIZE, newSize)
}
