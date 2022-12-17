package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import example.imageviewer.model.ContentStateData
import example.imageviewer.style.Transparent

@Composable
fun Scalable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    Surface(
        color = Transparent,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { onScale.reset() })
            detectTransformGestures { _, _, zoom, _ ->
                onScale.onScale(zoom)
            }
        },
    ) {
        children()
    }
}

class ScaleHandler(
    private val state: MutableState<ContentStateData>,
    private val maxFactor: Float = 5f,
    private val minFactor: Float = 1f
) {
    fun reset() {
        if (state.value.scale > minFactor) {
            state.value = state.value.copy(
                scale = minFactor
            )
        }
    }

    fun onScale(scaleFactor: Float): Float {
        state.value = state.value.copy(
            scale = state.value.scale + scaleFactor - 1f
        )

        if (maxFactor < state.value.scale) {
            state.value = state.value.copy(
                scale = maxFactor
            )
        }
        if (minFactor > state.value.scale) {
            state.value = state.value.copy(
                scale = minFactor
            )
        }
        return scaleFactor
    }
}
