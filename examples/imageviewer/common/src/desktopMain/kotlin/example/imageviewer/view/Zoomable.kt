package example.imageviewer.view

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusReference
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusReference
import example.imageviewer.style.Transparent

@Composable
fun Zoomable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    val focusRequester = remember { FocusReference() }

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
        .focusReference(focusRequester)
        .focusModifier()
        .clickable(indication = null) { focusRequester.requestFocus() }
    ) {
        children()
    }
}
