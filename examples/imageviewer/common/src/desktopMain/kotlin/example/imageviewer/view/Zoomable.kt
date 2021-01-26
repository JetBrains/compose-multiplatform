package example.imageviewer.view

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.clickable
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.shortcuts
import example.imageviewer.style.Transparent

@Composable
fun Zoomable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

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
        .clickable(interactionState = InteractionState(), indication = null) { focusRequester.requestFocus() }
    ) {
        children()
    }
}
