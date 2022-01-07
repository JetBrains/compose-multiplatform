package org.jetbrains.compose.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
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
            val radius = remember { Animatable(70f) }
            LaunchedEffect(radius) {
                radius.animateTo(
                    targetValue = 100f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .size(300.dp)
                    .fillMaxSize()
            ) {
                drawCircle(color = Color(255, 160, 122), radius = radius.value)
                drawCircle(color = Color(230, 230, 250), radius = 0.8f * radius.value)
                drawCircle(color = Color(127, 255, 212), radius = 0.6f * radius.value)
                drawCircle(color = Color(255, 215, 0), radius = 0.4f * radius.value)
                drawCircle(color = androidx.compose.ui.graphics.Color(64, 224, 208), radius = 0.2f * radius.value)
            }
        }
    }
}

@Composable
fun App() {
    Div {
        Span {
            SomeCanvas()
        }
        Span {
            SomeCanvas()
        }
        Span {
            SomeCanvas()
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
