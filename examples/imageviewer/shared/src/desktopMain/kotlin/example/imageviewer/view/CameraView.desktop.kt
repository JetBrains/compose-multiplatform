package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import example.imageviewer.*
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource
import java.util.*

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun CameraView(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
    val randomPicture = remember { resourcePictures.random() }
    val imageBitmap = resource(randomPicture.resource).rememberImageBitmap().orEmpty()
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
        Button(onClick = {
            onCapture(
                createCameraPictureData(
                    name = nameAndDescription.name,
                    description = nameAndDescription.description,
                    gps = randomPicture.gps
                ),
                DesktopStorableImage(imageBitmap)
            )
        }, Modifier.align(Alignment.BottomCenter)) {
            Text(LocalLocalization.current.takePhoto)
        }
    }
}
