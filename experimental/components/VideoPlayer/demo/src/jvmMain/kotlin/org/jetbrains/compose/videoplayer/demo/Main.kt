package org.jetbrains.compose.videoplayer.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.videoplayer.VideoPlayer
import org.jetbrains.compose.videoplayer.rememberVideoPlayerState
import java.awt.Dimension

fun main() {
    singleWindowApplication(title = "Video Player") {
        // See https://github.com/JetBrains/compose-multiplatform/issues/2285
        window.minimumSize = Dimension(700, 560)
        MaterialTheme {
            App()
        }
    }
}

@Composable
fun App() {
    val state = rememberVideoPlayerState()
    Column {
        val progress by VideoPlayer(
            url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            state = state,
            onFinish = state::stopPlayback,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        Slider(
            value = progress.fraction,
            onValueChange = { state.seek = it },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Timestamp: ${progress.time}", modifier = Modifier.width(140.dp))
            IconButton(onClick = state::toggleResume) {
                Icon(
                    painter = painterResource("${if (state.isResumed) "pause" else "play"}.svg"),
                    contentDescription = "Play/Pause"
                )
            }
            IconButton(onClick = state::toggleFullscreen) {
                Icon(
                    painter = painterResource("${if (state.isFullscreen) "exit" else "enter"}-fullscreen.svg"),
                    contentDescription = "Toggle fullscreen"
                )
            }
            OutlinedTextField(
                value = state.speed.toString(),
                maxLines = 1,
                leadingIcon = {
                    Icon(
                        painter = painterResource("speed.svg"),
                        contentDescription = "Speed",
                        modifier = Modifier.size(36.dp)
                    )
                },
                modifier = Modifier.width(104.dp),
                onValueChange = { state.speed = it.toFloat() }
            )
            Row {
                Icon(painter = painterResource("volume.svg"), contentDescription = "Volume")
                // TODO: Make the slider change volume in logarithmic manner
                //  See https://www.dr-lex.be/info-stuff/volumecontrols.html
                //  and https://ux.stackexchange.com/q/79672/117386
                //  and https://dcordero.me/posts/logarithmic_volume_control.html
                Slider(
                    value = state.volume,
                    onValueChange = { state.volume = it },
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}
