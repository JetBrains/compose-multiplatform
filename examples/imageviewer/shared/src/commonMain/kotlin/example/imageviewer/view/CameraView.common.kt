package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData

@Composable
expect fun CameraView(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
)
