package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.cropBitmapByScale

private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun ScalableImage(modifier: Modifier, image: ImageBitmap) {
    val scaleState = remember { mutableStateOf(1f) }
    val dragState = remember { mutableStateOf(Offset.Zero) }
    val focusRequester = FocusRequester()

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    dragState.value += dragAmount
                    change.consume()
                }
            }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val delta = event.changes.getOrNull(0)?.scrollDelta ?: Offset.Zero
                            scaleState.updateZoom(1 + delta.y / 100)
                        }
                    }
                }
            }.onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyUp) {
                    when (it.key) {
                        Key.I, Key.Plus, Key.Equals -> scaleState.updateZoom(1.2f)
                        Key.O, Key.Minus -> scaleState.updateZoom(0.8f)
                        Key.R -> scaleState.value = 1f
                    }
                }
                false
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        Image(
            bitmap = cropBitmapByScale(image, scaleState.value, dragState.value),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
actual fun cropBitmapByScale(image: ImageBitmap, scale: Float, offset: Offset): ImageBitmap {
    val size = LocalWindowSize.current
    return cropBitmapByScale(image.toAwtImage(), size, scale, offset).toComposeImageBitmap()
}
