package org.jetbrains.compose.videoplayer
  
import androidx.compose.runtime.Composable
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import javax.swing.JFrame
import uk.co.caprica.vlcj.binding.LibVlc
import uk.co.caprica.vlcj.binding.RuntimeUtil
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

private val vlcHome = "/Applications/VLC.app/Contents/MacOS/lib/"

@Composable
internal actual fun VideoPlayerImpl(url: String, width: Int, height: Int) {
    println("Video player for $url")
    NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcHome)
    System.load("$vlcHome/libvlccore.dylib")
    Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc::class.java)
    val frame = JFrame()
    val mediaPlayerComponent = EmbeddedMediaPlayerComponent()
    frame.contentPane = mediaPlayerComponent
    frame.setLocation(0, 0)
    frame.setSize(300,400)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setVisible(true)
    mediaPlayerComponent.mediaPlayer().media().play("/System/Library/Compositions/Yosemite.mov")
}
