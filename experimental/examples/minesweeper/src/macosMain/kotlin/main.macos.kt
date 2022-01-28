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
actual fun loadImage(src: String): Painter {
    // TODO Bundle pics and show images properly
    val color = when (src) {
        "assets/clock.png" -> Color.Blue
        "assets/flag.png" -> Color.Green
        "assets/mine.png" -> Color.Red
        else -> Color.White
    }

    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = color)
        }
    }
}
