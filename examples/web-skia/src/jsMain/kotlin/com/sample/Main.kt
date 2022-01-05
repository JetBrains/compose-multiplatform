package org.jetbrains.compose.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.skiko.skiko
import org.jetbrains.skiko.wasm.onWasmReady

@Composable
fun SomeCanvas() {
    Canvas({
        attr("width", "300")
        attr("height", "300")
        style {
            property("outline", "1px solid black")
            property("margin", "0 auto")
        }
    }) {
        skiko {
            val radius = 100f
            val animateFloat = remember { Animatable(0f) }
            LaunchedEffect(animateFloat) {
                animateFloat.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = androidx.compose.ui.graphics.Color.Black,
                    startAngle = 0f,
                    sweepAngle = 360f * animateFloat.value,
                    useCenter = false,
                    topLeft = Offset(size.width / 4, size.height / 4),
                    size = Size(
                        radius * 2,
                        radius * 2
                    ),
                    style = Stroke(2.0f)
                )
            }
        }
    }
}

@Composable
fun App() {
    Table {
        Tr {
            Td {
                SomeCanvas()
            }
            Td {
                SomeCanvas()
            }
            Td {
                SomeCanvas()
            }
        }
    }
}

fun main() {
    onWasmReady {
        renderComposable(rootElementId = "root") {
            App()
        }
    }
}
