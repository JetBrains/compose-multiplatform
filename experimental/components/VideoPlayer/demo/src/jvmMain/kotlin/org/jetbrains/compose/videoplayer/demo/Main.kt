package org.jetbrains.compose.videoplayer.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.videoplayer.VideoPlayer
import org.jetbrains.compose.videoplayer.rememberVideoPlayerState
import java.awt.Dimension

/**
 * To play a local file, use a URL notation like this:
 * ```kotlin
 * const val VIDEO_URL = "file:///C:/Users/John/Desktop/example.mp4"
 * ```
 * Relative paths like this may also work (relative to subproject directory aka `demo/`):
 * ```kotlin
 * val VIDEO_URL = """file:///${Path("videos/example.mp4")}"""
 * ```
 * To package a video with the app distributable,
 * see [this tutorial](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#adding-files-to-packaged-application)
 * and then use a URL syntax like this:
 * ```kotlin
 * val VIDEO_URL = """file:///${Path(System.getProperty("compose.application.resources.dir")) / "example.mp4"}"""
 * ```
 */
const val VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

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
    /*
     * Could not use a [Box] to overlay the controls on top of the video.
     * See https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Swing_Integration
     * Related issues:
     * https://github.com/JetBrains/compose-multiplatform/issues/1521
     * https://github.com/JetBrains/compose-multiplatform/issues/2926
     */
    Column {
        val progress by VideoPlayer(
            url = VIDEO_URL,
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
            Text("Timestamp: ${progress.timeMillis} ms", modifier = Modifier.width(180.dp))
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
