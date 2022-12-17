package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import example.imageviewer.model.ContentState
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.cropBitmapByBounds
import example.imageviewer.utils.cropImage
import example.imageviewer.utils.getDisplayBounds
import org.jetbrains.skia.Bitmap
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Image(content: ContentState) {
    val drag = remember { DragHandler() }
    val size = LocalWindowSize.current
    val onUpdate = remember { {
        content.state.value = content.state.value.copy(
            mainImage = org.jetbrains.skia.Image.makeFromEncoded(
                toByteArray(
                    cropBitmapByScale(
                        content.state.value.origin!!.toAwtImage(), //todo npe
                        size,
                        content.state.value.scale,
                        drag
                    )
                )
            ).toComposeImageBitmap()
        )
    } }
    val scaleHandler = remember { ScaleHandler(content.state) }
    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(
            onUpdate = onUpdate,
            dragHandler = drag,
            modifier = Modifier.fillMaxSize()
        ) {
            Zoomable(
                onUpdate = onUpdate,
                scaleHandler = scaleHandler,
                modifier = Modifier.fillMaxSize()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    content.swipePrevious()
                                }
                                Key.DirectionRight -> {
                                    content.swipeNext()
                                }
                            }
                        }
                        false
                    }
            ) {
                Image(
                    bitmap = content.getSelectedImage(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


fun ContentState.updateMainImage() {

}

fun toByteArray(bitmap: BufferedImage) : ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(bitmap, "png", baos)
    return baos.toByteArray()
}

fun cropBitmapByScale(
    bitmap: BufferedImage,
    size: DpSize,
    scale: Float,
    drag: DragHandler
): BufferedImage {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap, size),
        size,
        scale,
        drag
    )
    return cropImage(
        bitmap,
        Rectangle(crop.x, crop.y, crop.width - crop.x, crop.height - crop.y)
    )
}
