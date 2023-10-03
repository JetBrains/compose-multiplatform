import androidx.compose.ui.window.Window
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("Graphics2D") {
        Graphics2D()
    }
    NSApp?.run()
}
