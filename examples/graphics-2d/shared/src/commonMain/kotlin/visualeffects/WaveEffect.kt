package visualeffects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WaveEffectGrid() {
    var mouseX by remember { mutableStateOf(0) }
    var mouseY by remember { mutableStateOf(0) }
    var centerX by remember { mutableStateOf(1200) }
    var centerY by remember { mutableStateOf(900) }
    var vX by remember { mutableStateOf(0) }
    var vY by remember { mutableStateOf(0) }
    var timeElapsedNanos by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            var previousTimeNanos = withFrameNanos { it }
            withFrameNanos {
                val deltaTimeNanos = it - previousTimeNanos
                timeElapsedNanos += deltaTimeNanos
                previousTimeNanos = it

                if (State.entered) {
                    centerX = (centerX + vX * deltaTimeNanos / 1000000000).toInt()
                    if (centerX < -100) centerX = -100
                    if (centerX > 2600) centerX = 2600
                    vX =
                        (vX * (1 - deltaTimeNanos.toDouble() / 500000000) + 10 * (mouseX - centerX) * deltaTimeNanos / 1000000000).toInt()
                    centerY = (centerY + vY * deltaTimeNanos / 1000000000).toInt()
                    if (centerY < -100) centerY = -100
                    if (centerY > 1800) centerY = 1800
                    vY =
                        (vY * (1 - deltaTimeNanos.toDouble() / 500000000) + 5 * (mouseY - centerY) * deltaTimeNanos / 1000000000).toInt()

                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(5.dp).shadow(3.dp, RoundedCornerShape(20.dp))
            .onPointerEvent(PointerEventKind.Move) {
                mouseX = x
                mouseY = y
            }
            .onPointerEvent(PointerEventKind.In) {
                State.entered = true
            }
            .onPointerEvent(PointerEventKind.Out) {
                State.entered = false
            }
        ,
        color = Color(0, 0, 0),
        shape = RoundedCornerShape(20.dp)
    ) {
            Box(Modifier.fillMaxSize()) {
                var x = 10 // initial position
                var y = 10 // initial position
                val shift = 25
                var evenRow = false
                val pointerOffsetX = (centerX / 2)
                val pointerOffsety = (centerY / 2)
                while (y < 790) {
                    x = if (evenRow) 10 + shift else 10
                    while (x < 1190) {
                        val size: Int = size(x, y, pointerOffsetX, pointerOffsety)
                        val color = boxColor(x, y, timeElapsedNanos, pointerOffsetX, pointerOffsety)
                        Dot(size, Modifier.offset(x.dp, y.dp), color, timeElapsedNanos)
                        x += shift * 2
                    }
                    y += shift
                    evenRow = !evenRow
                }
                HighPanel(pointerOffsetX, pointerOffsety)
            }

    }
}

@Composable
fun HighPanel(mouseX: Int, mouseY: Int) {
    Text(
        "Compose",
        Modifier.offset(270.dp, 600.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 270, 700)),
        color = colorMouse(mouseX, mouseY, 270, 700),
        fontWeight = FontWeight.Bold
    )
    Text(
        "Multiplatform",
        Modifier.offset(350.dp, 700.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 550, 800)),
        color = colorMouse(mouseX, mouseY, 550, 800),
        fontWeight = FontWeight.Bold
    )
    Text(
        "1.0",
        Modifier.offset(850.dp, 700.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 800, 800)),
        color = colorMouse(mouseX, mouseY, 800, 800),
        fontWeight = FontWeight.Bold
    )
}

private fun alpha(mouseX: Int, mouseY: Int, x: Int, y: Int): Float {
    var d = distance(mouseX, mouseY, x, y)
    if (d > 450) return 0.0f
    d = d / 450 - 0.1
    return (1 - d * d).toFloat()
}

private fun colorMouse(mouseX: Int, mouseY: Int, x: Int, y: Int): Color {
    val d = distance(mouseX, mouseY, x, y) / 450
    val color1 = Color(0x6B, 0x57, 0xFF)
    val color2 = Color(0xFE, 0x28, 0x57)
    val color3 = Color(0xFD, 0xB6, 0x0D)
    val color4 = Color(0xFC, 0xF8, 0x4A)
    if (d > 1) return color1
    if (d > 0.66) return balancedColor(3 * d - 2, color1, color2)
    if (d > 0.33) return balancedColor(3 * d - 1, color2, color3)
    return balancedColor(3 * d, color3, color4)
}

private fun balancedColor(d: Double, color1: Color, color2: Color): Color {
    if (d > 1) return color1
    if (d < 0) return color2
    val red = ((color1.red * d + color2.red * (1 - d)) * 255).toInt()
    val green = ((color1.green * d + color2.green * (1 - d)) * 255).toInt()
    val blue = ((color1.blue * d + color2.blue * (1 - d)) * 255).toInt()
    return Color(red, green, blue)
}


private fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2).toDouble())
}

@Composable
fun Dot(size: Int, modifier: Modifier, color: Color, time: Long) {
    Box(
        modifier.rotate(time.toFloat() / (15 * 10000000)).clip(RoundedCornerShape((3 + size / 20).dp))
            .size(width = size.dp, height = size.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(color)) {
        }
    }
}

private fun size(x: Int, y: Int, mouseX: Int, mouseY: Int): Int {
    val addSize = 3
    var result = 5
    if (y > 550 && x < 550) return result
    if (y > 650 && x < 900) return result
    val distance2 = sqrt((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY).toDouble()) / 200
    val scale: Double = (if (distance2 < 1) {
        addSize * (1 - distance2)
    } else 0.toDouble())
    result += (if (State.entered) round(7.5 * scale).toInt() else 0)
    return result
}

private fun boxColor(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Color {
    if (!State.entered) return Color.White

    val color1 = Color(0x6B, 0x57, 0xFF)
    val color2 = Color(0xFE, 0x28, 0x57)
    val color3 = Color(0xFC, 0xF8, 0x4A)

    val distance = sqrt(((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY)).toDouble())
    val fade = exp(-1 * distance * distance / 150000)

    var c1 = sin(12 * distance / 450 - (time.toDouble() / (5 * 100000000)))
    if (c1 < 0) c1 = 0.0
    var c2 = sin(2 + 12 * distance / 450 - (time.toDouble() / (5 * 100000000)))
    if (c2 < 0) c2 = 0.0
    var c3 = sin(4 + 12 * distance / 450 - (time.toDouble() / (5 * 100000000)))
    if (c3 < 0) c3 = 0.0
    var color = Color.White

    if (c1 <= 0) {
        val d = c2 / (c2 + c3)
        color = balancedColor(d, color2, color3)
    } else if (c2 <= 0) {
        val d = c3 / (c1 + c3)
        color = balancedColor(d, color3, color1)
    } else if (c3 <= 0) {
        val d = c1 / (c1 + c2)
        color = balancedColor(d, color1, color2)
    }

    return balancedColor(fade, color, Color.White)
}

internal class State {
    companion object {
        var entered by mutableStateOf(false)
    }
}
