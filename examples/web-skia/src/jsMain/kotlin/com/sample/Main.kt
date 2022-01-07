package org.jetbrains.compose.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.skiko.skiko
import org.jetbrains.compose.web.svg.Circle
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.skiko.wasm.onWasmReady

fun Color.Companion.random(): Color {
    return Color((0..255).random(), (0..255).random(), (0..255).random())
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun SomeSvg() {
    Svg(viewBox = "0 0 400 400", attrs = {
        attr("width", "400")
        attr("height", "400")
    }) {
        val radius = remember { Animatable(10f) }
        LaunchedEffect(radius) {
            radius.animateTo(
                targetValue = 200f,
                animationSpec = infiniteRepeatable(
                    tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        Circle(200f, 200f, radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.95f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.9f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.85f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.8f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.75f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.7f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.65f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.6f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.55f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.5f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.45f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.4f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.35f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.3f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.25f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.2f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.15f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
        Circle(200f, 200f, 0.1f * radius.value, attrs = {
            attr("fill", "rgb(${(0..255).random()}, ${(0..255).random()}, ${(0..255).random()})")
        })
    }

}

@Composable
fun SomeCanvas() {
    Canvas({
        attr("width", "400")
        attr("height", "400")
        style {
//            property("outline", "1px solid black")
//            property("margin", "0 auto")
        }
    }) {
        skiko {
            val radius = remember { Animatable(10f) }
            LaunchedEffect(radius) {
                radius.animateTo(
                    targetValue = 200f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .size(400.dp)
                    .fillMaxSize()
            ) {
                drawCircle(color = Color.random(), radius = radius.value)
                drawCircle(color = Color.random(), radius = 0.95f * radius.value)
                drawCircle(color = Color.random(), radius = 0.9f * radius.value)
                drawCircle(color = Color.random(), radius = 0.85f * radius.value)
                drawCircle(color = Color.random(), radius = 0.8f * radius.value)
                drawCircle(color = Color.random(), radius = 0.75f * radius.value)
                drawCircle(color = Color.random(), radius = 0.7f * radius.value)
                drawCircle(color = Color.random(), radius = 0.65f * radius.value)
                drawCircle(color = Color.random(), radius = 0.6f * radius.value)
                drawCircle(color = Color.random(), radius = 0.55f * radius.value)
                drawCircle(color = Color.random(), radius = 0.5f * radius.value)
                drawCircle(color = Color.random(), radius = 0.45f * radius.value)
                drawCircle(color = Color.random(), radius = 0.4f * radius.value)
                drawCircle(color = Color.random(), radius = 0.35f * radius.value)
                drawCircle(color = Color.random(), radius = 0.3f * radius.value)
                drawCircle(color = Color.random(), radius = 0.25f * radius.value)
                drawCircle(color = Color.random(), radius = 0.2f * radius.value)
                drawCircle(color = Color.random(), radius = 0.15f * radius.value)
                drawCircle(color = Color.random(), radius = 0.1f * radius.value)
            }
        }
    }
}

@Composable
fun App() {
    Div({
        style {
            property("width", "100%")
            property("display", "flex")
            justifyContent(JustifyContent.SpaceEvenly)
        }
    }) {
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
    Div({
        style {
            property("width", "100%")
            property("display", "flex")
            justifyContent(JustifyContent.SpaceEvenly)
        }
    }) {
        Span {
            SomeSvg()
        }
        Span {
            SomeSvg()
        }
        Span {
            SomeSvg()
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
