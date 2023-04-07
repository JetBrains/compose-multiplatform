package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.model.GpsPosition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun LocationVisualizer(modifier: Modifier, gps: GpsPosition, title: String) {
    Image(
        painter = painterResource("dummy_map.png"),
        contentDescription = "Map",
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
