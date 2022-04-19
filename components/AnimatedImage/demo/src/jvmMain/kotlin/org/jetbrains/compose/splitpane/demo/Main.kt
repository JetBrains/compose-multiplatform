package org.jetbrains.compose.splitpane.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.animatedimage.AnimatedImage
import org.jetbrains.compose.animatedimage.rememberAnimatedImage
import org.jetbrains.compose.animatedimage.rememberResourceAnimatedImage


fun main() = singleWindowApplication {
    val url = "https://raw.githubusercontent.com/JetBrains/skija/ccf303ebcf926e5ef000fc42d1a6b5b7f1e0b2b5/examples/scenes/images/codecs/animated.gif"
    val resource = "demo.webp"
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text("GIF from network:")
        AnimatedImage(
            contentModifier = Modifier
                .widthIn(min = 100.dp)
                .heightIn(min = 100.dp),
            loader = rememberAnimatedImage(url),
            contentDescription = null,
            placeHolder = {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        )

        Text("WebP resource:")
        AnimatedImage(
            contentModifier = Modifier
                .size(500.dp),
            imageModifier = Modifier.fillMaxSize(),
            loader = rememberResourceAnimatedImage(resource),
            contentDescription = null,
        )
    }
}