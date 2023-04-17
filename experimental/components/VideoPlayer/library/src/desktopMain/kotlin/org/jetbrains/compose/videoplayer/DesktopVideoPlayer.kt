package org.jetbrains.compose.videoplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer
import java.util.*
import javax.swing.JComponent


@Composable
internal actual fun VideoPlayerSwingImpl(url: String, width: Int, height: Int) {
    NativeDiscovery().discover()
    val mediaPlayerComponent = rememberMediaPlayerComponent()
    DisposableEffect(Unit) {
        mediaPlayerComponent.mediaPlayer().media().play(url)
        onDispose {
            mediaPlayerComponent.mediaPlayer().release()
        }
    }
    return SwingPanel(background = Color.Transparent, modifier = Modifier.width(width.dp).height(height.dp), factory = {
        mediaPlayerComponent
    })
}

@Composable
internal actual fun VideoPlayerComposeImpl(url: String, width: Int, height: Int) {
    NativeDiscovery().discover()
    var imageBitmap by remember { mutableStateOf(ImageBitmap(width, height)) }
    Image(modifier = Modifier.width(width.dp).height(height.dp), bitmap = imageBitmap, contentDescription = "Video")
    val mediaPlayerComponent = rememberMediaPlayerComponent()
    DisposableEffect(Unit) {
        var byteArray: ByteArray? = null
        var imageInfo: ImageInfo? = null
        val mediaPlayer = mediaPlayerComponent.mediaPlayer()
        val callbackVideoSurface = CallbackVideoSurface(
            object : BufferFormatCallback {

                override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                    imageInfo = ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
                    return RV32BufferFormat(sourceWidth, sourceHeight)
                }

                override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
                    byteArray = ByteArray(buffers[0].limit())
                }
            },
            object : RenderCallback {

                override fun display(
                    mediaPlayer: MediaPlayer,
                    nativeBuffers: Array<out ByteBuffer>,
                    bufferFormat: BufferFormat
                ) {
                    imageInfo?.let {
                        val byteBuffer = nativeBuffers[0]
                        byteBuffer.get(byteArray)
                        byteBuffer.rewind()
                        imageBitmap = Bitmap().apply {
                            allocPixels(it)
                            installPixels(byteArray)
                        }.asComposeImageBitmap()
                    }
                }
            },
            true,
            VideoSurfaceAdapters.getVideoSurfaceAdapter(),
        )
        mediaPlayer.videoSurface().set(callbackVideoSurface)
        mediaPlayer.media().play(url)
        onDispose {
            mediaPlayer.release()
        }
    }
}

@Composable
fun rememberMediaPlayerComponent(): JComponent {
    return remember {
        // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
        if (isMacOS()) {
            CallbackMediaPlayerComponent()
        } else {
            EmbeddedMediaPlayerComponent()
        }
    }
}

/**
 * To return mediaPlayer from player components.
 * The method names are same, but they don't share the same parent/interface.
 * That's why need this method.
 */
private fun Any.mediaPlayer(): EmbeddedMediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}

private fun isMacOS(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0
}
