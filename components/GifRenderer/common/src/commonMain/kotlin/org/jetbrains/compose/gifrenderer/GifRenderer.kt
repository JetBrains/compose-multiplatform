package org.jetbrains.compose.gifrenderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun rememberGifRenderer(url: String): State<Painter> {
    return rememberGifRendererImpl(url)
}

internal expect fun rememberGifRendererImpl(url: String): State<Painter>
