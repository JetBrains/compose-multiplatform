package org.jetbrains.compose.videoplayer

import androidx.compose.runtime.Composable

@Composable
fun VideoPlayer(url: String, width: Int, height: Int) {
    VideoPlayerImpl(url, width, height)
}

internal expect fun VideoPlayerImpl(url: String, width: Int, height: Int)
