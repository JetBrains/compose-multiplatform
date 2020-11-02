package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.gesture.RawScaleObserver
import androidx.compose.ui.gesture.doubleTapGestureFilter
import androidx.compose.ui.gesture.rawScaleGestureFilter
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.material.Surface
import example.imageviewer.style.Transparent
import androidx.compose.runtime.mutableStateOf

@Composable
fun Scalable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    Surface(
        color = Transparent,
        modifier = modifier.rawScaleGestureFilter(
            scaleObserver = onScale,
            canStartScaling = { true }
        ).doubleTapGestureFilter(onDoubleTap = { onScale.resetFactor() }),
    ) {
        children()
    }
}

class ScaleHandler(private val maxFactor: Float = 5f, private val minFactor: Float = 1f) :
    RawScaleObserver {
    val factor = mutableStateOf(1f)

    fun resetFactor() {
        if (factor.value > minFactor)
            factor.value = minFactor
    }

    override fun onScale(scaleFactor: Float): Float {
        factor.value += scaleFactor - 1f

        if (maxFactor < factor.value) factor.value = maxFactor
        if (minFactor > factor.value) factor.value = minFactor

        return scaleFactor
    }
}