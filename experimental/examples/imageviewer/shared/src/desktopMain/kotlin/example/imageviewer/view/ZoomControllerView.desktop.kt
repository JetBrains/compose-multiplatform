package example.imageviewer.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import example.imageviewer.model.MAX_SCALE
import example.imageviewer.model.MIN_SCALE
import example.imageviewer.model.ScalableState
import example.imageviewer.model.setScale

@Composable
internal actual fun BoxScope.ZoomControllerView(scalableState: ScalableState) {
    Slider(
        modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
        value = scalableState.scale,
        valueRange = MIN_SCALE..MAX_SCALE,
        onValueChange = { scalableState.setScale(it) },
    )
}
