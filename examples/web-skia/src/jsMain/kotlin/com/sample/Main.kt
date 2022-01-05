package org.jetbrains.compose.sample

import androidx.compose.material.Text
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.skiko.skiko
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        renderComposable(rootElementId = "root") {
            Canvas({
                attr("width", "300")
                attr("height", "300")
                style {
                    property("outline", "1px solid black")
                }
            }) {
                skiko {
                    Text("Press me!")
                }
            }
        }
    }
}
