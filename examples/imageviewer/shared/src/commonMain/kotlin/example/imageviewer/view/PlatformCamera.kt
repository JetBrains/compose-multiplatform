package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData

interface PlatformCamera {
    @Composable
    fun Preview(modifier: Modifier)

    fun capture(onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit)
}

sealed interface PlatformCameraState {
    data object Pending : PlatformCameraState
    data class Unavailable(val message: String) : PlatformCameraState
    data class Ready(val camera: PlatformCamera) : PlatformCameraState
}

@Composable
expect fun rememberPlatformCameraState(): PlatformCameraState
