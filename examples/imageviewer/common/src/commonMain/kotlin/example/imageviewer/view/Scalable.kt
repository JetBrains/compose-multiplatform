package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
            detectTapGestures(onDoubleTap = { onScale.resetFactor() })
            detectTransformGestures { _, _, zoom, _ ->
                onScale.onScale(zoom)
            }
        },
    ) {
        children()
    }
}

class ScaleHandler(private val maxFactor: Float = 5f, private val minFactor: Float = 1f) {
    val factor = mutableStateOf(1f)

    fun resetFactor() {
        if (factor.value > minFactor)
            factor.value = minFactor
    }

    fun onScale(scaleFactor: Float): Float {
        factor.value += scaleFactor - 1f

        if (maxFactor < factor.value) factor.value = maxFactor
        if (minFactor > factor.value) factor.value = minFactor

        return scaleFactor
    }
}