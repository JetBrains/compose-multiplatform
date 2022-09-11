package org.jetbrains.compose.videoplayer.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.videoplayer.VideoPlayerCompose
import org.jetbrains.compose.videoplayer.VideoPlayerSwing

fun main() {
    singleWindowApplication(
        title = "Video Player",
        state = WindowState(width = 800.dp, height = 800.dp)
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Swing Video Player",
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                VideoPlayerSwing(
                    url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    width = 480,
                    height = 360
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.background(Color.Black), contentAlignment = Alignment.Center) {
                    VideoPlayerCompose(
                        url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        width = 480,
                        height = 360
                    )
                    Text(
                        text = "Compose Video Player",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
