package org.jetbrains.compose.videoplayer

import androidx.compose.runtime.Composable

@Composable
fun VideoPlayerSwing(url: String, width: Int, height: Int) {
    VideoPlayerSwingImpl(url, width, height)
}

@Composable
fun VideoPlayerCompose(url: String, width: Int, height: Int) {
    VideoPlayerComposeImpl(url, width, height)
}

internal expect fun VideoPlayerSwingImpl(url: String, width: Int, height: Int)

internal expect fun VideoPlayerComposeImpl(url: String, width: Int, height: Int)
