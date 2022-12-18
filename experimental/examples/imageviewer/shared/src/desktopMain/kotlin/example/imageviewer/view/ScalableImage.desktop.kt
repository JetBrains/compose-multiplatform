package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
    val drag = remember { DragHandler() }
    val size = LocalWindowSize.current
    val onUpdate = remember {
        {
            if (image != null) {
                content.state.value = content.state.value.copy(
                    mainImage = org.jetbrains.skia.Image.makeFromEncoded(
                        toByteArray(
                            cropBitmapByScale(
                                image.toAwtImage(),
                                size,
                                content.state.value.scale,
                                drag
                            )
                        )
                    ).toComposeImageBitmap()
                )
            }
        }
    }
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
                    bitmap = content.getSelectedImage() ?: resource("empty.png").rememberImageBitmap().orEmpty(),
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

class ScaleHandler(
    private val scaleState: MutableState<Float>,
    private val maxFactor: Float = 5f,
    private val minFactor: Float = 1f
) {
    fun reset() {
        if (scaleState.value > minFactor) {
            scaleState.value = minFactor
        }
    }

    fun onScale(scaleFactor: Float): Float {
        scaleState.value = scaleState.value + scaleFactor - 1f

        if (maxFactor < scaleState.value) {
            scaleState.value = maxFactor
        }
        if (minFactor > scaleState.value) {
            scaleState.value = minFactor
        }
        return scaleFactor
    }
}
