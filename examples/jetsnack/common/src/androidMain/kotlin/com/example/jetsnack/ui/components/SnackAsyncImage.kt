package com.example.jetsnack.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.common.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val imagesCache = mutableMapOf<String, ImageBitmap>()

@SuppressLint("UnusedContentLambdaTargetStateParameter")
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
                    Res.readBytes(imageUrl).toAndroidBitmap().asImageBitmap().also {
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

fun ByteArray.toAndroidBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, size)
}