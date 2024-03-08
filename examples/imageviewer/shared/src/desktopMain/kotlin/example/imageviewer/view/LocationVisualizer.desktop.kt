package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import example.imageviewer.model.GpsPosition

@Composable
actual fun LocationVisualizer(
    modifier: Modifier,
    gps: GpsPosition,
    title: String,
    parentScrollEnableState: MutableState<Boolean>
) {
    example.map.MapView(
        modifier,
        userAgent = "ComposeMapViewExample",
        latitude = gps.latitude,
        longitude = gps.longitude,
        startScale = 12_000.0
    )
}
