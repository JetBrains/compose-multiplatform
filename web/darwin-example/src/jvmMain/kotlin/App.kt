package co.touchlab.ckit.desktop

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize

fun main() {
    Window(title = "Demo", size = IntSize(600, 400)) {
        SomeCode.HelloWorld()
    }
}