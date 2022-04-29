package org.jetbrains.compose.animatedimage.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.animatedimage.Blank
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadAnimatedImage
import org.jetbrains.compose.resources.LoadState
import org.jetbrains.compose.resources.load
import org.jetbrains.compose.resources.loadOrNull

private val url =
    "https://raw.githubusercontent.com/JetBrains/skija/ccf303ebcf926e5ef000fc42d1a6b5b7f1e0b2b5/examples/scenes/images/codecs/animated.gif"

fun main() = singleWindowApplication {
    Column {
        // Load an image async
        // use "load { loadResourceAnimatedImage(url) }" for resources
        when (val animatedImage = load { loadAnimatedImage(url) }) {
            is LoadState.Success -> Image(
                bitmap = animatedImage.value.animate(),
                contentDescription = null,
            )
            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> Text("Error!")
        }

        Image(
            loadOrNull { loadAnimatedImage(url) }?.animate() ?: ImageBitmap.Blank,
            contentDescription = null,
            Modifier.size(100.dp)
        )
    }
}