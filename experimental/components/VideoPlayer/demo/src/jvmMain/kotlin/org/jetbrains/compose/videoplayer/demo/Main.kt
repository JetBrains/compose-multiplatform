package org.jetbrains.compose.videoplayer.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        Text("Timestamp: ${progress.time}")
        Spacer(Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = toggleResume, Modifier.width(150.dp)) {
                Text(if (isResumed) "Pause" else "Play")
            }
            Button(onClick = toggleFullscreen, Modifier.width(150.dp)) {
                Text("${if (isFullscreen) "Exit" else "Enter"} fullscreen")
            }
            OutlinedTextField(
                value = speed.toString(),
                maxLines = 1,
                leadingIcon = { Text("Speed: ") },
                modifier = Modifier.width(104.dp),
                onValueChange = { speed = it.toFloat() }
            )
            Slider(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier.width(100.dp)
            )
        }
    }
}
