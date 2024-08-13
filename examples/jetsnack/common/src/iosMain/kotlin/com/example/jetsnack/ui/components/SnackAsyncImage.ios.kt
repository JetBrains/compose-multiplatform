package com.example.jetsnack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val imagesCache = mutableMapOf<String, ImageBitmap>()

@OptIn(ExperimentalAnimationApi::class)
@Composable
actual fun SnackAsyncImage(imageUrl: String, contentDescription: String?, modifier: Modifier) {
    var img: ImageBitmap? by remember(imageUrl) { mutableStateOf(null) }


    AnimatedContent(img, transitionSpec = {
        fadeIn(TweenSpec()) with fadeOut(TweenSpec())
    }) {
        if (img != null) {
            Image(img!!, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
        } else {
            Box(modifier = modifier)
        }
    }

    LaunchedEffect(imageUrl) {
        if (imagesCache.contains(imageUrl)) {
            img = imagesCache[imageUrl]
        } else {
            withContext(Dispatchers.IO) {
                img = try {
                    loadImage(imageUrl).also {
                        imagesCache[imageUrl] = it
                        img = it
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
suspend fun loadImage(url: String): ImageBitmap = suspendCancellableCoroutine { continuation ->
    val nsUrl = NSURL(string = url)
    val task = NSURLSession.sharedSession.dataTaskWithURL(nsUrl) { data, response, error ->
        if (data != null) {
            val byteArray = ByteArray(data.length.toInt()).apply {
                usePinned {
                    memcpy(
                        it.addressOf(0),
                        data.bytes,
                        data.length
                    )
                }
            }

            continuation.resume(Image.makeFromEncoded(byteArray).toComposeImageBitmap())
        } else {
            error?.let {
                continuation.resumeWithException(Exception(it.localizedDescription))
            }
        }
    }

    task.resume()
    continuation.invokeOnCancellation {
        task.cancel()
    }
}