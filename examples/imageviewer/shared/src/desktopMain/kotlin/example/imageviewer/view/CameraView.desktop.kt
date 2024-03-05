package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import example.imageviewer.*
import example.imageviewer.icon.IconPhotoCamera
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import imageviewer.shared.generated.resources.Res

@Composable
actual fun CameraView(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
    val randomPicture = remember { resourcePictures.random() }
    var imageBitmap by remember { mutableStateOf(ImageBitmap(1, 1)) }
    LaunchedEffect(randomPicture) {
        imageBitmap = Res.readBytes(randomPicture.resource).toImageBitmap()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = imageBitmap,
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
        val nameAndDescription = createNewPhotoNameAndDescription()
        CircularButton(
            imageVector = IconPhotoCamera,
            modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
        ) {
            onCapture(
                createCameraPictureData(
                    name = nameAndDescription.name,
                    description = nameAndDescription.description,
                    gps = randomPicture.gps
                ),
                DesktopStorableImage(imageBitmap)
            )
        }
    }
}
