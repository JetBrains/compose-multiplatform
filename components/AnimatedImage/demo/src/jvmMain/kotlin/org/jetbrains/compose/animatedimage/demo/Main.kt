package org.jetbrains.compose.animatedimage.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.animatedimage.*

fun main() = singleWindowApplication {
    val url =
        "https://raw.githubusercontent.com/JetBrains/skija/ccf303ebcf926e5ef000fc42d1a6b5b7f1e0b2b5/examples/scenes/images/codecs/animated.gif"

    // Preloaded image that will be shown when the user clicks on the button
    val preloadedImage = remember { mutableStateOf<AnimatedImage?>(null) }

    var showPreloaded by remember { mutableStateOf(false) }
    val preloadedImageValue = preloadedImage.value

    LaunchedEffect(Unit) {
        // Load an image sync
        preloadedImage.value = loadAnimatedImage(url) // use "loadResourceAnimatedImage" for resources
    }

    // Load an image async
    val asyncAnimatedImage =
        asyncAnimatedImageLoaderState(url)  // use "asyncResourceAnimatedImageLoaderState" for resources

    Column {
        when (asyncAnimatedImage) {
            is AnimatedImageLoaderState.Success -> {
                Image(
                    bitmap = animatedImage(asyncAnimatedImage.animatedImage),
                    contentDescription = null,
                )
            }
            AnimatedImageLoaderState.Loading -> {
                CircularProgressIndicator()
            }
            is AnimatedImageLoaderState.Error -> {
                Text("Error!")
            }
        }

        Button(
            onClick = { showPreloaded = true },
        ) {
            Text("Click to show preloaded")
        }

        if (showPreloaded && preloadedImageValue != null) {
            Image(
                bitmap = animatedImage(preloadedImageValue),
                contentDescription = null,
            )
        }
    }
}