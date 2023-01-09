import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import platform.AppKit.NSApplication
import platform.AppKit.NSApp

fun main() {
    NSApplication.sharedApplication()
    Window("Minesweeper") {
        MaterialTheme {
            Game(
                requestWindowSize = { _, _ ->
                    // TODO(not implemented yet
                }
            )
        }
    }
    NSApp?.run()
}

@Composable
actual fun loadImage(src: String): Painter = loadImageAsColoredRect(src)

actual fun isMobileDevice() = false