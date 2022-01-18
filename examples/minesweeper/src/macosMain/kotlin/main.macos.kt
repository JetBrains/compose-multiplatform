import androidx.compose.runtime.remember
import platform.AppKit.NSApp
import platform.AppKit.NSApplication
import androidx.compose.ui.window.Window

fun main() {
    NSApplication.sharedApplication()
    Window("Falling Balls") {
        Game(requestWindowSize = { w, h -> })
    }
    NSApp?.run()
}
