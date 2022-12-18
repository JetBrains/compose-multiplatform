package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.cropBitmapByScale
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
actual fun ScalableImage(modifier: Modifier, image: ImageBitmap) {
    val scaleState = remember { mutableStateOf(1f) }
    val scale = scaleState.value
    val size = LocalWindowSize.current
    val drag = remember { DragHandler() }
    val scaleHandler = remember { ScaleHandler(scaleState) }

    val modifiedImage: ImageBitmap = remember(image, scale, size) {
        org.jetbrains.skia.Image.makeFromEncoded(
            toByteArray(
                cropBitmapByScale(
                    image.toAwtImage(),
                    size,
                    scale,
                    drag.getAmount()
                )
            )
        ).toComposeImageBitmap()
    }

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(
            dragHandler = drag,
            modifier = Modifier.fillMaxSize()
        ) {
            ZoomWithKeyboard(
                scaleHandler = scaleHandler,
                modifier = modifier.fillMaxSize()
            ) {
                Image(
                    bitmap = modifiedImage,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

fun toByteArray(bitmap: BufferedImage): ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(bitmap, "png", baos)
    return baos.toByteArray()
}
