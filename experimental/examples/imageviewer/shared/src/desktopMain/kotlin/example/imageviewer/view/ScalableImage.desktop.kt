package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.cropBitmapByBounds
import example.imageviewer.utils.cropImage
import example.imageviewer.utils.getDisplayBounds
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
@Composable
actual fun ScalableImage(image: ImageBitmap, swipeNext: () -> Unit, swipePrevious: () -> Unit) {
    val scaleState = remember { mutableStateOf(1f) }
    val scale = scaleState.value
    val size = LocalWindowSize.current
    val drag = remember { DragHandler() }

    val modifiedImage: ImageBitmap = remember(image, scale, size) {
        org.jetbrains.skia.Image.makeFromEncoded(
            toByteArray(
                cropBitmapByScale(
                    image.toAwtImage(),
                    size,
                    scaleState.value,
                    drag.getAmount()
                )
            )
        ).toComposeImageBitmap()
    }

    val scaleHandler = remember { ScaleHandler(scaleState) }
    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(
            dragHandler = drag,
            modifier = Modifier.fillMaxSize()
        ) {
            Zoomable(
                scaleHandler = scaleHandler,
                modifier = Modifier.fillMaxSize()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            when (it.key) {
                                Key.DirectionLeft -> swipePrevious()
                                Key.DirectionRight -> swipeNext()
                            }
                        }
                        false
                    }
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

fun cropBitmapByScale(
    bitmap: BufferedImage,
    size: DpSize,
    scale: Float,
    offset: Offset,
): BufferedImage {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap, size),
        size,
        scale,
        offset,
    )
    return cropImage(
        bitmap,
        Rectangle(crop.x, crop.y, crop.width - crop.x, crop.height - crop.y)
    )
}
