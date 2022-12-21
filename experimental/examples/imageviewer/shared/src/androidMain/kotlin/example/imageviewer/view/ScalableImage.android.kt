package example.imageviewer.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.displayHeight
import example.imageviewer.utils.displayWidth
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f

@Composable
internal actual fun ScalableImage(modifier: Modifier, image: ImageBitmap) {
    val dragState = remember(image) { mutableStateOf(Offset.Zero) }
    val scaleState = remember(image) { mutableStateOf(1f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = DarkGray,
            modifier = modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        dragState.value += pan
                        scaleState.updateZoom(zoom)
                    }
                }.pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scaleState.value = 1f })
                },
        ) {
            Image(
                bitmap = cropBitmapByScale(image, scaleState.value, dragState.value, DpSize(maxWidth, maxHeight)),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun MutableState<Float>.updateZoom(zoom: Float) {
    var newScale = value + zoom - 1f
    if (newScale > MAX_SCALE) {
        newScale = MAX_SCALE
    } else if (newScale < MIN_SCALE) {
        newScale = MIN_SCALE
    }
    value = newScale
}

@Composable
private fun cropBitmapByScale(image: ImageBitmap, scale: Float, offset: Offset, size: DpSize): ImageBitmap =
    cropBitmapByScale(image.asAndroidBitmap(), size, scale, offset).asImageBitmap()

fun cropBitmapByScale(bitmap: Bitmap, size: DpSize, scale: Float, offset: Offset): Bitmap {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap),
        size,
        scale,
        offset
    )
    return Bitmap.createBitmap(
        bitmap,
        crop.left,
        crop.top,
        crop.right - crop.left,
        crop.bottom - crop.top
    )
}

fun cropBitmapByBounds(
    bitmap: Bitmap,
    bounds: Rect,
    size: DpSize,
    scaleFactor: Float,
    offset: Offset,
): Rect {
    if (scaleFactor <= 1f)
        return Rect(0, 0, bitmap.width, bitmap.height)

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width() / scale).roundToInt()
    var boundH = (bounds.height() / scale).roundToInt()

    scale *= displayWidth() / bounds.width().toDouble()

    val offsetX = offset.x / scale
    val offsetY = offset.y / scale

    if (boundW > bitmap.width) {
        boundW = bitmap.width
    }
    if (boundH > bitmap.height) {
        boundH = bitmap.height
    }

    val invisibleW = bitmap.width - boundW
    var leftOffset = (invisibleW / 2.0 - offsetX).roundToInt().toFloat()

    if (leftOffset > invisibleW) {
        leftOffset = invisibleW.toFloat()
//        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
//        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0f
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt().toFloat()

    if (topOffset > invisibleH) {
        topOffset = invisibleH.toFloat()
//        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
//        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
        topOffset = 0f
    }

    return Rect(
        leftOffset.toInt(),
        topOffset.toInt(),
        (leftOffset + boundW).toInt(),
        (topOffset + boundH).toInt()
    )
}

fun getDisplayBounds(bitmap: Bitmap): Rect {

    val boundW: Float = displayWidth().toFloat()
    val boundH: Float = displayHeight().toFloat()

    val ratioX: Float = bitmap.width / boundW
    val ratioY: Float = bitmap.height / boundH
    val ratio: Float = if (ratioX > ratioY) ratioX else ratioY
    val resultW = (boundW * ratio)
    val resultH = (boundH * ratio)

    return Rect(0, 0, resultW.toInt(), resultH.toInt())
}
