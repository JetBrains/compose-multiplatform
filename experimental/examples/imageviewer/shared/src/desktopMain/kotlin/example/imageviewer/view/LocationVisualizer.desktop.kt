package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun LocationVisualizer(modifier: Modifier) {
    Image(
        painter = painterResource("dummy_map.png"),
        contentDescription = "Map",
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().height(300.dp)
    )
}
