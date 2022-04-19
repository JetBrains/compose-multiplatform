package org.jetbrains.compose.animatedimage

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

private const val DEFAULT_FRAME_DURATION = 50

@Composable
actual fun AnimatedImage(
    loader: AnimatedImageLoader,
    contentDescription: String?,
    contentModifier: Modifier,
    imageModifier: Modifier,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
    placeHolder: @Composable BoxScope.() -> Unit
) {
    val codec = remember(loader) { mutableStateOf<Codec?>(null) }
    val codecValue = codec.value

    LaunchedEffect(loader) {
        val bytes = loader.loadBytes()
        val data = Data.makeFromBytes(bytes)
        codec.value = Codec.makeFromData(data)
    }

    Box(contentModifier) {
        if (codecValue == null) {
            placeHolder()
        } else {
            val transition = rememberInfiniteTransition()
            val frameIndex by transition.animateValue(
                initialValue = 0,
                targetValue = codecValue.frameCount - 1,
                Int.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 0
                        for ((index, frame) in codecValue.framesInfo.withIndex()) {
                            index at durationMillis
                            val frameDuration = calcFrameDuration(frame)

                            durationMillis += frameDuration
                        }
                    }
                )
            )

            val bitmap = remember(codec) { Bitmap().apply { allocPixels(codecValue.imageInfo) } }

            codecValue.readPixels(bitmap, frameIndex)

            Image(
                bitmap = bitmap.asComposeImageBitmap(),
                modifier = imageModifier,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                filterQuality = filterQuality,
            )
        }
    }
}

@Composable
actual fun rememberAnimatedImage(url: String): AnimatedImageLoader {
    return remember {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            NetworkLoader(url)
        } else
            LocalLoader(url)
    }
}

@Composable
fun rememberResourceAnimatedImage(resourcePath: String): AnimatedImageLoader {
    return remember {
        ResourceLoader(resourcePath)
    }
}


fun calcFrameDuration(frame: AnimationFrameInfo): Int {
    var frameDuration = frame.duration

    // If the frame does not contain information about a duration, set a reasonable constant duration
    if (frameDuration == 0) {
        frameDuration = DEFAULT_FRAME_DURATION
    }

    return frameDuration
}