package org.jetbrains.compose.gifrenderer.demo

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.gifrenderer.rememberGifRenderer

fun main() {
    val url = "https://raw.githubusercontent.com/JetBrains/skija/ccf303ebcf926e5ef000fc42d1a6b5b7f1e0b2b5/examples/scenes/images/codecs/animated.gif"

    Window(
        title = "Gif Renderer",
        size = IntSize(800, 800)
    ) {
        MaterialTheme {
            DesktopTheme {
                val image by rememberGifRenderer(url)
                Image(image, null, Modifier.fillMaxSize())
            }
        }
    }
}
