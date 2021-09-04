package org.jetbrains.compose.videoplayer.demo

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.videoplayer.VideoPlayer

fun main() {
    Window(
        title = "Video Player",
        size = IntSize(800, 800)
    ) {
        MaterialTheme {
            DesktopTheme {
                VideoPlayer(
                    url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    width = 640,
                    height = 480
                )
            }
        }
    }
}
