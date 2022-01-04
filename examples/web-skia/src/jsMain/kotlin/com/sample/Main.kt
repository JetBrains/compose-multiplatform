package org.jetbrains.compose.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeCanvas
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        renderComposable(rootElementId = "root") {
            var switched by remember { mutableStateOf(false) }
            Canvas({
                attr("width", "300")
                attr("height", "300")
                style {
                    property("outline", "1px solid black")
                }
            }) {
                DomSideEffect { canvas ->
                    ComposeCanvas(canvas).apply {
                        setContent {
                            Column {
                                Button(
                                    modifier = Modifier.padding(16.dp),
                                    onClick = {
                                        println("Button clicked!")
                                    }
                                ) {
                                    Text(if (switched) "🦑 press 🐙" else "Press me!")
                                }
                                Switch(
                                    modifier = Modifier.padding(16.dp),
                                    checked = switched,
                                    onCheckedChange = { switched = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
