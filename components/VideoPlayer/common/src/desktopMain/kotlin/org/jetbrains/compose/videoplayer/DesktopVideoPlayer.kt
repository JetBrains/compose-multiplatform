package org.jetbrains.compose.videoplayer
  
import androidx.compose.runtime.Composable

@Composable
actual fun VideoPlayerImpl(url: String, width: Int, height: Int) {
    println("Video player for $url")
}
