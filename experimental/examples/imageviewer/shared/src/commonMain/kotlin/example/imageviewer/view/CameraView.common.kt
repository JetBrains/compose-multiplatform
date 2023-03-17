package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData

@Composable
internal expect fun CameraView(modifier: Modifier, storage: ImageStorage)
