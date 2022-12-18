package example.imageviewer.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import example.imageviewer.style.Transparent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Zoomable(
    scaleHandler: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    val focusRequester = FocusRequester()

    Surface(
        color = Transparent,
        modifier = modifier.onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.I -> {
                        scaleHandler.onScale(1.2f)
                    } 
                    Key.O -> {
                        scaleHandler.onScale(0.8f)
                    }
                    Key.R -> {
                        scaleHandler.reset()
                    }
                }
            }
            false
        }
        .focusRequester(focusRequester)
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { scaleHandler.reset() }) {
                focusRequester.requestFocus()
            }
        }
    ) {
        children()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

class ScaleHandler(
    private val scaleState: MutableState<Float>,
    private val maxFactor: Float = 5f,
    private val minFactor: Float = 1f
) {
    fun reset() {
        if (scaleState.value > minFactor) {
            scaleState.value = minFactor
        }
    }

    fun onScale(scaleFactor: Float): Float {
        scaleState.value = scaleState.value + scaleFactor - 1f

        if (maxFactor < scaleState.value) {
            scaleState.value = maxFactor
        }
        if (minFactor > scaleState.value) {
            scaleState.value = minFactor
        }
        return scaleFactor
    }
}
