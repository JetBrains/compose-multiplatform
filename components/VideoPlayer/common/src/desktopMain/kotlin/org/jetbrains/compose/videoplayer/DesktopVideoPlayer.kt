package org.jetbrains.compose.videoplayer

import androidx.compose.desktop.SwingPanel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

@Composable
internal actual fun VideoPlayerImpl(url: String, width: Int, height: Int) {
    println("Video player for $url")
    NativeDiscovery().discover()
    // Doesn't work on macOS, see https://github.com/caprica/vlcj/issues/887 for suggestions.
    val mediaPlayerComponent = EmbeddedMediaPlayerComponent()
    SideEffect {
        val ok = mediaPlayerComponent.mediaPlayer().media().play(url)
        println("play gave $ok")
    }
    return SwingPanel(
        background = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        componentBlock = {
            mediaPlayerComponent
        }
    )
}
