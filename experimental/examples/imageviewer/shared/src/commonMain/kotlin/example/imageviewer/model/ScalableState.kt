package example.imageviewer.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

class ScalableState(val imageSize: IntSize) {
    var boxSize by mutableStateOf(IntSize(1, 1))
    var offset by mutableStateOf(IntOffset.Zero)
    var scale by mutableStateOf(1f)
}

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

fun ScalableState.changeBoxSize(size: IntSize) {
    boxSize = size
    updateOffsetLimits()
}

fun ScalableState.setScale(scale: Float) {
    this.scale = scale
}

fun ScalableState.addScale(diff: Float) {
    scale = if (scale + diff > MAX_SCALE) {
        MAX_SCALE
    } else if (scale + diff < MIN_SCALE) {
        MIN_SCALE
    } else {
        scale + diff
    }
    updateOffsetLimits()
}

fun ScalableState.addDragAmount(diff: Offset) {
    offset -= IntOffset((diff.x + 1).toInt(), (diff.y + 1).toInt())
    updateOffsetLimits()
}

private fun ScalableState.updateOffsetLimits() {
    if (offset.x + visiblePart.width > imageSize.width) {
        changeOffset(x = imageSize.width - visiblePart.width)
    }
    if (offset.y + visiblePart.height > imageSize.height) {
        changeOffset(y = imageSize.height - visiblePart.height)
    }
    if (offset.x < 0) {
        changeOffset(x = 0)
    }
    if (offset.y < 0) {
        changeOffset(y = 0)
    }
}

private fun ScalableState.changeOffset(x: Int = offset.x, y: Int = offset.y) {
    offset = IntOffset(x, y)
}
