package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData

@Composable
internal actual fun CameraView(modifier: Modifier, onCapture: (picture: PictureData.Camera, image: PlatformStorableImage)->Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            text = "Camera is not available on Desktop for now.",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
