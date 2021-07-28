package example.imageviewer.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    val focusRequester = FocusRequester()

    Surface(
        color = Transparent,
        modifier = modifier.onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.I -> onScale.onScale(1.2f)
                    Key.O -> onScale.onScale(0.8f)
                    Key.R -> onScale.resetFactor()
                }
            }
            false
        }
        .focusRequester(focusRequester)
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { onScale.resetFactor() }) {
                focusRequester.requestFocus()
            }
        }
    ) {
        children()
    }

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { }
    }
}
