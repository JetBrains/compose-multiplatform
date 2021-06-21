package org.jetbrains.compose.gifrenderer

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.skija.Bitmap
import org.jetbrains.skija.Codec
import org.jetbrains.skija.Data
import java.net.URL

@Composable
internal actual fun rememberGifRendererImpl(url: String): State<Painter> {
    val codec = remember(url) {
        println("Gif renderer for $url")
        val bytes = URL(url).readBytes()
        val data = Data.makeFromBytes(bytes)
        Codec.makeFromData(data)
    }

    val images = remember(codec) {
        (0 until codec.frameCount).map { frameIndex ->
            val bitmap = Bitmap()
            bitmap.allocPixels(codec.imageInfo)
            codec.readPixels(bitmap, frameIndex)
            BitmapPainter(bitmap.asImageBitmap())
        }
    }

    val transition = rememberInfiniteTransition()
    val frameIndex by transition.animateValue(
        initialValue = 0,
        targetValue = codec.frameCount - 1,
        Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 0
                for ((index, frame) in codec.framesInfo.withIndex()) {
                    index at durationMillis
                    durationMillis += frame.duration
                }
            }
        )
    )

    return remember(images) {
        derivedStateOf { images[frameIndex] }
    }
}
