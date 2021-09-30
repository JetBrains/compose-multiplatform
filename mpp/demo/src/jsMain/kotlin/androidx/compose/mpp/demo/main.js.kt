package androidx.compose.mpp.demo

import androidx.compose.ui.window.Window
import kotlinx.browser.document
import org.jetbrains.skiko.GenericSkikoView
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLCanvasElement

fun main() {
    onWasmReady {
        Window("Compose/JS sample") {
            myContent()
        }
    }
}
