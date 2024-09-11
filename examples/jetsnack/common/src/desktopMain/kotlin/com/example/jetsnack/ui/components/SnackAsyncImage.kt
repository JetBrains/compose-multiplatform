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
import com.example.common.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.net.URL
import javax.imageio.ImageIO


private val imagesCache = mutableMapOf<String, ImageBitmap>()

@OptIn(ExperimentalAnimationApi::class, ExperimentalResourceApi::class)
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
                    org.jetbrains.skia.Image.makeFromEncoded(Res.readBytes(imageUrl)).toComposeImageBitmap().also {
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
