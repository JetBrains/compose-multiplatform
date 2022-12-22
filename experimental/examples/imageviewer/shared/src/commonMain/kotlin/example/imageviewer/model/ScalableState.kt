package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

data class ScalableState(
    val imageSize: IntSize,
    val boxSize: IntSize = IntSize(1, 1),
    val offset: IntOffset = IntOffset.Zero,
    val scale: Float = 1f
)

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

fun MutableState<ScalableState>.changeBoxSize(size: IntSize) = modifyState {
    copy(boxSize = size)
        .updateOffsetLimits()
}

fun MutableState<ScalableState>.setScale(scale: Float) = modifyState {
    copy(scale = scale)
        .updateOffsetLimits()
}

fun MutableState<ScalableState>.addScale(diff: Float) = modifyState {
    if (scale + diff > MAX_SCALE) {
        copy(scale = MAX_SCALE)
    } else if (scale + diff < MIN_SCALE) {
        copy(scale = MIN_SCALE)
    } else {
        copy(scale = scale + diff)
    }.updateOffsetLimits()
}

fun MutableState<ScalableState>.addDragAmount(diff: Offset) = modifyState {
    copy(offset = offset - IntOffset((diff.x + 1).toInt(), (diff.y + 1).toInt()))
        .updateOffsetLimits()
}

private fun ScalableState.updateOffsetLimits(): ScalableState {
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

private fun ScalableState.changeOffset(x: Int = offset.x, y: Int = offset.y) = copy(offset = IntOffset(x, y))
