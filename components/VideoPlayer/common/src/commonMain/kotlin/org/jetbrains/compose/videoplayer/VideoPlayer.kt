package org.jetbrains.compose.videoplayer

import androidx.compose.runtime.Composable

@Composable
fun VideoPlayer(url: String, width: Int, height: Int) {
    VideoPlayerImpl(url, width, height)
}

expect fun VideoPlayerImpl(url: String, width: Int, height: Int)
