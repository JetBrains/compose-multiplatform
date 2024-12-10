package com.example.jetsnack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.common.generated.resources.Res
import kotlinx.coroutines.*
import com.example.jetsnack.model.snacks
import org.jetbrains.compose.resources.ExperimentalResourceApi

val imagesCache = mutableMapOf<String, ImageBitmap>()

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun SnackAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier
) {

    var bitmap: ImageBitmap? by remember { mutableStateOf(null) }

    if (bitmap != null) {
        Image(bitmap!!, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
    }

    LaunchedEffect(imageUrl) {
        if (imagesCache.contains(imageUrl)) {
            bitmap = imagesCache[imageUrl]!!
        } else {
            imagesCache[imageUrl] = org.jetbrains.skia.Image.makeFromEncoded(
                Res.readBytes(imageUrl)
            ).toComposeImageBitmap()
            bitmap = imagesCache[imageUrl]
        }
    }
}
@OptIn(ExperimentalResourceApi::class)
suspend fun CoroutineScope.prepareImagesCache() {
    val jobs = mutableListOf<Job>()
    // We have not many images, so we can prepare and cache them upfront
    snacks.forEach {
        val j = launch {
            imagesCache[it.imageUrl] = org.jetbrains.skia.Image.makeFromEncoded(
                Res.readBytes(it.imageUrl)
            ).toComposeImageBitmap()
        }
        jobs.add(j)
    }
    joinAll(*jobs.toTypedArray())
}