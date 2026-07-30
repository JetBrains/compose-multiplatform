package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import example.imageviewer.PlatformStorableImage
import example.imageviewer.icon.IconPhotoCamera
import example.imageviewer.model.PictureData

@Composable
fun CameraCaptureFrame(
    modifier: Modifier = Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit,
) {
    val state = rememberPlatformCameraState()
    Box(
        modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            PlatformCameraState.Pending -> CircularProgressIndicator(color = Color.White)
            is PlatformCameraState.Unavailable -> Text(state.message, color = Color.White)
            is PlatformCameraState.Ready -> ReadyCamera(state.camera, onCapture)
        }
    }
}

@Composable
private fun BoxScope.ReadyCamera(
    camera: PlatformCamera,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit,
) {
    var isCapturing by remember { mutableStateOf(false) }

    camera.Preview(Modifier.fillMaxSize())
    CircularButton(
        imageVector = IconPhotoCamera,
        modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
        enabled = !isCapturing,
    ) {
        isCapturing = true
        camera.capture { picture, image ->
            isCapturing = false
            onCapture(picture, image)
        }
    }
    if (isCapturing) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp).align(Alignment.Center),
            color = Color.White.copy(alpha = 0.7f),
            strokeWidth = 8.dp,
        )
    }
}
