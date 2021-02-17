package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.shortcuts
import androidx.compose.ui.input.pointer.pointerInput
import example.imageviewer.style.Transparent
import androidx.compose.runtime.DisposableEffect

@Composable
fun Zoomable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    val focusRequester = FocusRequester()

    Surface(
        color = Transparent,
        modifier = modifier.shortcuts {
            on(Key.I) {
                onScale.onScale(1.2f)
            }
            on(Key.O) {
                onScale.onScale(0.8f)
            }
            on(Key.R) {
                onScale.resetFactor()
            }
        }
        .focusRequester(focusRequester)
        .focusModifier()
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
