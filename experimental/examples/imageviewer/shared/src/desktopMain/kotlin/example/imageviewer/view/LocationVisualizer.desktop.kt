package example.imageviewer.view

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import example.imageviewer.model.GpsPosition

@Composable
internal actual fun LocationVisualizer(modifier: Modifier, gps: GpsPosition, title: String) {
    com.map.MapViewWithButtons(
        modifier.height(300.dp),
        userAgent = "ComposerMapViewExample",
        latitude = gps.latitude,
        longitude = gps.longitude,
        startScale = 8_000.0
    )
}
