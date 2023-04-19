package org.jetbrains.compose.videoplayer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.util.*

@Composable
internal actual fun VideoPlayerImpl(url: String, width: Int, height: Int) {
    NativeDiscovery().discover()
    val mediaPlayerComponent = remember {
        // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
        if (isMacOS()) {
            CallbackMediaPlayerComponent()
        } else {
            EmbeddedMediaPlayerComponent()
        }
    }
    SideEffect {
        mediaPlayerComponent.mediaPlayer().media().play(url)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerComponent.mediaPlayer().release()
        }
    }

    return SwingPanel(
        background = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        factory = {
            mediaPlayerComponent
        }
    )
}

/**
 * To return mediaPlayer from player components.
 * The method names are same, but they don't share the same parent/interface.
 * That's why need this method.
 */
private fun Any.mediaPlayer(): MediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}

private fun isMacOS(): Boolean {
    val os = System
        .getProperty("os.name", "generic")
        .lowercase(Locale.ENGLISH)
    return "mac" in os || "darwin" in os
}
