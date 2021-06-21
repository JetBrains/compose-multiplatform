package org.jetbrains.compose.gifrenderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun rememberGifRenderer(url: String): State<ImageBitmap> {
    return rememberGifRendererImpl(url)
}

internal expect fun rememberGifRendererImpl(url: String): State<ImageBitmap>
