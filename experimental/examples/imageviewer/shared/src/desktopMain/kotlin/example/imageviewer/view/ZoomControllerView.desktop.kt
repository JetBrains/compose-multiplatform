package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import example.imageviewer.model.MAX_SCALE
import example.imageviewer.model.MIN_SCALE
import example.imageviewer.model.ScalableState

@Composable
internal actual fun ZoomControllerView(modifier: Modifier, scalableState: ScalableState) {
    Slider(
        modifier = modifier.fillMaxWidth(0.5f).padding(12.dp),
        value = scalableState.scale,
        valueRange = MIN_SCALE..MAX_SCALE,
        onValueChange = { scalableState.setScale(it) },
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
    )
}
