package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import example.imageviewer.model.ScalableState

@Composable
actual fun ZoomControllerView(modifier: Modifier, scalableState: ScalableState) {
    Slider(
        modifier = modifier.fillMaxWidth(0.5f).padding(12.dp),
        value = scalableState.zoom,
        valueRange = scalableState.zoomLimits.start..scalableState.zoomLimits.endInclusive,
        onValueChange = { scalableState.setZoom(it) },
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
    )
}
