package org.jetbrains.compose.videoplayer.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.videoplayer.VideoPlayer

fun main() {
    singleWindowApplication(title = "Video Player") {
        MaterialTheme {
            App()
        }
    }
}

@Composable
fun App() {
    var seek by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    var volume by remember { mutableStateOf(1f) }
    var isResumed by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    val stopPlayback = remember { { isResumed = false } }
    val toggleResume = remember { { isResumed = !isResumed } }
    val toggleFullscreen = remember { { isFullscreen = !isFullscreen } }
    Column {
        val progress by VideoPlayer(
            url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            seek = seek,
            speed = speed,
            volume = volume,
            isResumed = isResumed,
            isFullscreen = isFullscreen,
            onFinish = stopPlayback,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        Slider(
            value = progress.fraction,
            onValueChange = { seek = it },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Timestamp: ${progress.time}", modifier = Modifier.width(140.dp))
            IconButton(onClick = toggleResume) {
                Icon(
                    painter = painterResource("${if (isResumed) "pause" else "play"}.svg"),
                    contentDescription = "Play/Pause"
                )
            }
            IconButton(onClick = toggleFullscreen) {
                Icon(
                    painter = painterResource("${if (isFullscreen) "exit" else "enter"}-fullscreen.svg"),
                    contentDescription = "Toggle fullscreen"
                )
            }
            OutlinedTextField(
                value = speed.toString(),
                maxLines = 1,
                leadingIcon = {
                    Icon(
                        painter = painterResource("speed.svg"),
                        contentDescription = "Speed",
                        modifier = Modifier.size(36.dp)
                    )
                },
                modifier = Modifier.width(104.dp),
                onValueChange = { speed = it.toFloat() }
            )
            Row {
                Icon(painter = painterResource("volume.svg"), contentDescription = "Volume")
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}
