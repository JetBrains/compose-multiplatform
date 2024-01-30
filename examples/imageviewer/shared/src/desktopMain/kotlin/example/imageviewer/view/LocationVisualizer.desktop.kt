package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.model.GpsPosition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
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
