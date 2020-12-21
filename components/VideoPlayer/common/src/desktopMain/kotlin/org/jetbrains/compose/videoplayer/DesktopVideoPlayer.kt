package org.jetbrains.compose.videoplayer
  
import androidx.compose.runtime.Composable
import javax.swing.JFrame
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent


@Composable
internal actual fun VideoPlayerImpl(url: String, width: Int, height: Int) {
    println("Video player for $url")
    /*
    val vlcHome = "/Applications/VLC.app/Contents/MacOS/lib/"
    System.load("$vlcHome/libvlccore.dylib")
    System.load("$vlcHome/libvlc.dylib") */
    val frame = JFrame()
    val mediaPlayerComponent = EmbeddedMediaPlayerComponent()
    frame.contentPane = mediaPlayerComponent
    frame.setLocation(0, 0)
    frame.setSize(width,height)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setVisible(true)
    val ok = mediaPlayerComponent.mediaPlayer().media().play(url)
    println("play gave $ok")
}
