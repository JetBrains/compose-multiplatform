package com.example.jetsnack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.*
import com.example.jetsnack.model.snacks

val imagesCache = mutableMapOf<String, ImageBitmap>()

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
            val arrayBuffer = loadImage(imageUrl)
            val skiaImg = org.jetbrains.skia.Image.makeFromEncoded(arrayBuffer.toByteArray())
            imagesCache[imageUrl] = skiaImg.toComposeImageBitmap()
            bitmap = imagesCache[imageUrl]
        }
    }
}
suspend fun CoroutineScope.prepareImagesCache() {
    val jobs = mutableListOf<Job>()
    // We have not many images, so we can prepare and cache them upfront
    snacks.forEach {
        val j = launch {
            val arrayBuffer = loadImage(it.imageUrl)
            val skiaImg = org.jetbrains.skia.Image.makeFromEncoded(arrayBuffer.toByteArray())
            imagesCache[it.imageUrl] = skiaImg.toComposeImageBitmap()
        }
        jobs.add(j)
    }
    joinAll(*jobs.toTypedArray())
}