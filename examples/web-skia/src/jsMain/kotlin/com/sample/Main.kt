package org.jetbrains.compose.sample

import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    renderComposable(rootElementId = "root") {
        Canvas {
            onWasmReady {
                window.alert("HEY!!!!!")
            }
        }
    }
}
