package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*

private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScalableImage(image: ImageBitmap) {
    val focusRequester = FocusRequester()
    val imageSize = IntSize(image.width, image.height)
    val state = remember(imageSize) { mutableStateOf(ScalableState(imageSize)) }

    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { coordinates ->
                state.value = state.value.changeBoxSize(coordinates.size)
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Color.DarkGray)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount: Offset ->
                        state.value = state.value.addDragAmount(dragAmount)
                        change.consume()
                    }
                }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val delta = event.changes.getOrNull(0)?.scrollDelta ?: Offset.Zero
                                state.value = state.value.addScale(delta.y / 100)
                            }
                        }
                    }
                }.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        when (it.key) {
                            Key.I, Key.Plus, Key.Equals -> {
                                state.value = state.value.addScale(0.2f)
                            }

                            Key.O, Key.Minus -> {
                                state.value = state.value.addScale(-0.2f)
                            }

                            Key.R -> {
                                state.value = state.value.copy(scale = 1f)
                            }
                        }
                    }
                    false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = BitmapPainter(
                    image,
                    srcOffset = state.value.visiblePart.topLeft,
                    srcSize = state.value.visiblePart.size
                ),
                contentDescription = null
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

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

private val ScalableState.visiblePart
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
