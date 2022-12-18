package example.imageviewer.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import example.imageviewer.style.DarkGray
import example.imageviewer.style.Transparent
import example.imageviewer.utils.adjustImageScale
import example.imageviewer.utils.displayHeight
import example.imageviewer.utils.displayWidth
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
internal actual fun ScalableImage(image: ImageBitmap, swipeNext: () -> Unit, swipePrevious: () -> Unit) {
    val drag = remember { DragHandler() }
    val scaleState: MutableState<Float> = remember { mutableStateOf(1f) }

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(dragHandler = drag, modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Transparent,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scaleState.value = 1f })
                    detectTransformGestures { _, _, zoom, _ ->
                        val maxFactor = 5f
                        val minFactor = 1f
                        scaleState.value = scaleState.value + zoom - 1f
                        if (maxFactor < scaleState.value) {
                            scaleState.value = maxFactor
                        }
                        if (minFactor > scaleState.value) {
                            scaleState.value = minFactor
                        }
                    }
                },
            ) {
                val bitmap = imageByGesture(image, scaleState.value, drag, swipeNext, swipePrevious)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = adjustImageScale(bitmap)
                )
            }
        }
    }
}

@Composable
private fun imageByGesture(
    image: ImageBitmap,
    scale: Float,
    drag: DragHandler,
    swipeNext: () -> Unit,
    swipePrevious: () -> Unit,
): ImageBitmap {
    val bitmap = cropBitmapByScale(image.asAndroidBitmap(), scale, drag)

    if (scale > 1f)
        return bitmap.asImageBitmap()

    if (abs(drag.getDistance().x) > displayWidth() / 10) {
        if (drag.getDistance().x < 0) {
            swipeNext()
        } else {
            swipePrevious()
        }
        drag.cancel()
    }

    return bitmap.asImageBitmap()
}

fun cropBitmapByScale(bitmap: Bitmap, scale: Float, drag: DragHandler): Bitmap {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap),
        scale,
        drag
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
    scaleFactor: Float,
    drag: DragHandler
): Rect {
    if (scaleFactor <= 1f)
        return Rect(0, 0, bitmap.width, bitmap.height)

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width() / scale).roundToInt()
    var boundH = (bounds.height() / scale).roundToInt()

    scale *= displayWidth() / bounds.width().toDouble()

    val offsetX = drag.getAmount().x / scale
    val offsetY = drag.getAmount().y / scale

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
        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0f
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt().toFloat()

    if (topOffset > invisibleH) {
        topOffset = invisibleH.toFloat()
        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
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

