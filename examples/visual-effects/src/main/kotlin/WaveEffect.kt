package org.jetbrains.compose.demo.visuals

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlin.math.*

@Composable
fun WaveEffect(onCloseRequest: () -> Unit, showControls: Boolean) {
    var windowState = remember { WindowState(width = 1200.dp, height = 800.dp) }
    Window(onCloseRequest = {}, undecorated = true, transparent = true, state = windowState) {
        Grid()
    }

    if (showControls) {
        Window(
            onCloseRequest = onCloseRequest,
            state = WindowState(width = 200.dp, height = 400.dp, position = WindowPosition(1400.dp, 200.dp))
        ) {
            Column {
                SettingsPanel(State.red, "Red")
                SettingsPanel(State.green, "Green")
                SettingsPanel(State.blue, "Blue")
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun Grid() {
    var mouseX by remember { mutableStateOf(0) }
    var mouseY by remember { mutableStateOf(0) }
    var centerX by remember { mutableStateOf(1200) }
    var centerY by remember { mutableStateOf(900) }
    var vX by remember { mutableStateOf(0) }
    var vY by remember { mutableStateOf(0) }

    var time by remember { mutableStateOf(System.nanoTime()) }
    var prevTime by remember { mutableStateOf(System.nanoTime()) }

    if (State.mouseUsed) {
        centerX = (centerX + vX * (time - prevTime) / 1000000000).toInt()
        if (centerX < -100) centerX = -100
        if (centerX > 2600) centerX = 2600
        vX =
            (vX * (1 - (time - prevTime).toDouble() / 500000000) + 10 * (mouseX - centerX) * (time - prevTime) / 1000000000).toInt()
        centerY = (centerY + vY * (time - prevTime) / 1000000000).toInt()
        if (centerY < -100) centerY = -100
        if (centerY > 1800) centerY = 1800
        vY =
            (vY * (1 - (time - prevTime).toDouble() / 500000000) + 5 * (mouseY - centerY) * (time - prevTime) / 1000000000).toInt()

        prevTime = time
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(5.dp).shadow(3.dp, RoundedCornerShape(20.dp))
            .pointerMoveFilter(onMove = { mouseX = it.x.toInt(); mouseY = it.y.toInt(); false; },
                onEnter = { State.mouseUsed = true; false; }, onExit = { State.mouseUsed = false; false; }),
        color = Color(0, 0, 0),
        shape = RoundedCornerShape(20.dp)
    ) {
            Box(Modifier.fillMaxSize()) {
                var x = 10 // initial position
                var y = 10 // initial position
                val shift = 25
                var evenRow = false
                var pointerOffsetX = (centerX / 2)
                var pointerOffsety = (centerY / 2)
                while (y < 790) {
                    x = if (evenRow) 10 + shift else 10
                    while (x < 1190) {
                        var size: Int = size(x, y, pointerOffsetX, pointerOffsety)
                        var color = boxColor(x, y, time, pointerOffsetX, pointerOffsety)
                        Dot(size, Modifier.offset(x.dp, y.dp), color, time)
                        x += shift * 2
                    }
                    y += shift
                    evenRow = !evenRow
                }
                HighPanel(pointerOffsetX, pointerOffsety)
            }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos {
                    time = it
                }
            }
        }

    }
}

@Composable
fun HighPanel(mouseX: Int, mouseY: Int) {
    Text(
        "Compose",
        androidx.compose.ui.Modifier.offset(270.dp, 600.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 270, 700)),
        color = colorMouse(mouseX, mouseY, 270, 700),
        fontWeight = FontWeight.Bold
    )
    Text(
        "Multiplatform",
        androidx.compose.ui.Modifier.offset(350.dp, 700.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 550, 800)),
        color = colorMouse(mouseX, mouseY, 550, 800),
        fontWeight = FontWeight.Bold
    )
    Text(
        "1.0",
        androidx.compose.ui.Modifier.offset(800.dp, 700.dp).scale(7.0f).alpha(alpha(mouseX, mouseY, 800, 800)),
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
    var d = distance(mouseX, mouseY, x, y) / 450
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
    var distance2 = sqrt((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY).toDouble()) / 200
    val scale: Double = (if (distance2 < 1) {
        addSize * (1 - distance2)
    } else 0.toDouble())
    result += (if (State.mouseUsed) round(7.5 * scale).toInt() else 0)
    return result
}

private fun boxColor(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Color {
    if (!State.mouseUsed) return Color.White

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
        var d = c2 / (c2 + c3)
        color = balancedColor(d, color2, color3)
    } else if (c2 <= 0) {
        var d = c3 / (c1 + c3)
        color = balancedColor(d, color3, color1)
    } else if (c3 <= 0) {
        var d = c1 / (c1 + c2)
        color = balancedColor(d, color1, color2)
    }

    return balancedColor(fade, color, Color.White)
}

internal class ColorSettings {
    var enabled by mutableStateOf(true)
    var waveLength by mutableStateOf(30.0)
    var simple by mutableStateOf(true)
    var period by mutableStateOf(80.0)
}

private class State {
    companion object {
        var red by mutableStateOf(ColorSettings())
        var green by mutableStateOf(ColorSettings())
        var blue by mutableStateOf(ColorSettings())
        var mouseUsed by mutableStateOf(false)
    }
}

@Composable
internal fun SettingsPanel(settings: ColorSettings, name: String) {
    Row {
        Text(name)
        Checkbox(settings.enabled, onCheckedChange = { settings.enabled = it })
        Checkbox(settings.simple, onCheckedChange = { settings.simple = it })
        Slider(
            (settings.waveLength.toFloat() - 10) / 90,
            { settings.waveLength = 10 + 90 * it.toDouble() },
            Modifier.width(100.dp)
        )
        Slider(
            (settings.period.toFloat() - 10) / 90,
            { settings.period = 10 + 90 * it.toDouble() },
            Modifier.width(100.dp)
        )
    }
}

