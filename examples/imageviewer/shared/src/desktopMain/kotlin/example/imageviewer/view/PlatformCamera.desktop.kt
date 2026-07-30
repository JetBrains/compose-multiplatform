package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import example.imageviewer.*
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import imageviewer.shared.generated.resources.Res

@Composable
actual fun rememberPlatformCameraState(): PlatformCameraState =
    PlatformCameraState.Ready(rememberDesktopCamera())

@Composable
private fun rememberDesktopCamera(): PlatformCamera {
    val randomPicture = remember { resourcePictures.random() }
    var imageBitmap by remember { mutableStateOf(ImageBitmap(1, 1)) }
    LaunchedEffect(randomPicture) {
        imageBitmap = Res.readBytes(randomPicture.resource).toImageBitmap()
    }
    val nameAndDescription = createNewPhotoNameAndDescription()
    return remember(randomPicture, nameAndDescription) {
        DesktopStubCamera(randomPicture, nameAndDescription) { imageBitmap }
    }
}

private class DesktopStubCamera(
    private val randomPicture: PictureData.Resource,
    private val nameAndDescription: NameAndDescription,
    private val currentImage: () -> ImageBitmap,
) : PlatformCamera {

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.fillMaxSize().background(Color.Black)) {
            Image(
                bitmap = currentImage(),
                contentDescription = "Camera stub",
                Modifier.fillMaxSize()
            )
            Text(
                text = """
                    Camera is not available on Desktop for now.
                    Instead, we will use a random picture.
                """.trimIndent(),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(20.dp)
            )
        }
    }

    override fun capture(onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit) {
        onResult(
            createCameraPictureData(
                name = nameAndDescription.name,
                description = nameAndDescription.description,
                gps = randomPicture.gps
            ),
            DesktopStorableImage(currentImage())
        )
    }
}
