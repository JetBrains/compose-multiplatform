package example.imageviewer.view

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

const val MAX_SCALE = 5f
const val MIN_SCALE = 1f

data class ScalableState(
    val imageSize: IntSize,
    val boxSize: IntSize = IntSize(1, 1),
    val offset: IntOffset = IntOffset.Zero,
    val scale: Float = 1f
)

fun ScalableState.changeOffset(x: Int = offset.x, y: Int = offset.y) = copy(offset = IntOffset(x, y))

fun ScalableState.changeBoxSize(size: IntSize) =
    copy(boxSize = size)
        .updateOffset()

fun ScalableState.setScale(scale: Float) =
    copy(scale = scale)
        .updateOffset()

fun ScalableState.addScale(diff: Float) =
    if (scale + diff > MAX_SCALE) {
        copy(scale = MAX_SCALE)
    } else if (scale + diff < MIN_SCALE) {
        copy(scale = MIN_SCALE)
    } else {
        copy(scale = scale + diff)
    }.updateOffset()

fun ScalableState.addDragAmount(diff: Offset) =
    copy(offset = offset - IntOffset((diff.x + 1).toInt(), (diff.y + 1).toInt()))
        .updateOffset()

val ScalableState.visiblePart
    get() : IntRect {
        val boxRatio = boxSize.width.toFloat() / boxSize.height
        val imageRatio = imageSize.width.toFloat() / imageSize.height.toFloat()

        val size: IntSize =
            if (boxRatio > imageRatio) {
                val height = imageSize.height / scale
                val targetWidth = height * boxRatio
                IntSize(minOf(imageSize.width, targetWidth.toInt()), height.toInt())
            } else {
                val width = imageSize.width / scale
                val targetHeight = width / boxRatio
                IntSize(width.toInt(), minOf(imageSize.height, targetHeight.toInt()))
            }

        return IntRect(offset = offset, size = size)
    }

private fun ScalableState.updateOffset(): ScalableState {
    var result = this
    if (offset.x + visiblePart.width > imageSize.width) {
        result = result.changeOffset(x = imageSize.width - visiblePart.width)
    }
    if (offset.y + visiblePart.height > imageSize.height) {
        result = result.changeOffset(y = imageSize.height - visiblePart.height)
    }
    if (offset.x < 0) {
        result = result.changeOffset(x = 0)
    }
    if (offset.y < 0) {
        result = result.changeOffset(y = 0)
    }
    return result
}
