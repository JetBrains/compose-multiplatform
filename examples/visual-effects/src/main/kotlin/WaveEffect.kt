// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlin.math.*

@Composable
@Preview
fun App() {
//    var text by remember { mutableStateOf("Hello, World\t!") }
//    Column {
//        Text(text)
//        TextField(text, {text = it})
//    }
    val lol = remember { mutableStateOf("") }
    OutlinedTextField(lol.value, onValueChange = { lol.value = it })
}

class ColorSettings {
    var enabled by mutableStateOf(true)
    var waveLength by mutableStateOf(30.0)
    var simple by mutableStateOf(true)
    var period by mutableStateOf(80.0)
}

class State {
    companion object {
        var red by mutableStateOf(ColorSettings())
        var green by mutableStateOf(ColorSettings())
        var blue by mutableStateOf(ColorSettings())
        var mouseUsed by mutableStateOf(false)
    }
}

fun main() = application {
    content(::exitApplication)
}

@Composable
fun content(onCloseRequest: () -> Unit) {
    var windowState = remember { WindowState(width = 1200.dp, height = 800.dp) }
    Window(onCloseRequest = {}, undecorated = true, transparent = true, state = windowState) {
        Grid()
    }


    Window(
        onCloseRequest = onCloseRequest,
        state = WindowState(width = 200.dp, height = 400.dp, position = WindowPosition(1400.dp, 200.dp))
    ) {
        Column {
            settingPanel(State.red, "Red")
            settingPanel(State.green, "Green")
            settingPanel(State.blue, "Blue")
        }
    }
}


@Composable
fun settingPanel(settings: ColorSettings, name: String) {
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
//        centerY = (centerY + (mouseY - centerY) * (time - prevTime) / 1000000000).toInt()
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
        with(LocalDensity.current) {
            Box(Modifier.fillMaxSize()) {
                var x = 10 //initial position
                var y = 10 //initial position
                val shift = 25
                var evenRow = false
                var pointerOffsetX = (centerX / 2)
                var pointerOffsety = (centerY / 2)
                while (y < 790) {
                    x = if (evenRow) 10 + shift else 10
                    while (x < 1190) {
                        var size: Int = size(x, y, time, pointerOffsetX, pointerOffsety)
//                        var color = color(x, y, time, pointerOffsetX, pointerOffsety)
                        var color = color3(x, y, time, pointerOffsetX, pointerOffsety)
                        dot(size, Modifier.offset(x.dp, y.dp), color, time)
                        x = x + shift * 2
                    }
                    y = y + shift
                    evenRow = !evenRow
                }
                var angle = (time.toDouble() / (6 * 10000000)) % 360
                var alpha = (1.5 + sin(time.toDouble() / (10 * 100000000))) / 2.5
                var scale = 0.5 + ((time.toDouble() / (12 * 10000000)) % 100) / 20
                var scale1 = if (scale - 1 > 0.5) (scale - 1) else scale + 2
                var scale2 = if (scale - 2 > 0.5) (scale - 2) else scale + 1

//                for (i in 0..10) {
//                    Text("Compose",
//                        androidx.compose.ui.Modifier.offset(200.dp, 400.dp).rotate((-angle+9*i).toFloat())
//                            .scale((scale-i.toFloat()/2).toFloat()).alpha((exp(-1*i.toFloat()/2))),
//                        color = Color.White, fontWeight = FontWeight.Bold
//                    )
//                    Text("Multiplatform", androidx.compose.ui.Modifier.offset(600.dp, 200.dp).rotate(((angle-9*i)).toFloat())
//                        .scale((scale1-i.toFloat()/2).toFloat()).alpha((exp(-1*i.toFloat()/2))),
//                        color = Color.White, fontWeight = FontWeight.Bold)
//                    Text("1.0", androidx.compose.ui.Modifier.offset(1000.dp, 500.dp).rotate((angle-180-9*i).toFloat()).
//                        scale((scale2-i.toFloat()/2).toFloat()).alpha(exp(-1*i.toFloat()/2)),
//                        color = Color.White, fontWeight = FontWeight.Bold)
//                }

                highPanel(pointerOffsetX, pointerOffsety)
            }
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
fun highPanel(mouseX: Int, mouseY: Int) {
    var color = Color(0xFE, 0x28, 0x57)
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

fun alpha(mouseX: Int, mouseY: Int, x: Int, y: Int): Float {
    var d = distance(mouseX, mouseY, x, y)
    if (d > 450) return 0.0f
    d = d / 450 - 0.1
    return (1 - d * d).toFloat()
}

fun colorMouse(mouseX: Int, mouseY: Int, x: Int, y: Int): Color {
    var d = distance(mouseX, mouseY, x, y) / 450
    val color1 = Color(0x6B, 0x57, 0xFF)
    val color2 = Color(0xFE, 0x28, 0x57)
    val color3 = Color(0xFD, 0xB6, 0x0D)
    val color4 = Color(0xFC, 0xF8, 0x4A)
    if (d > 1) return color1
    if (d > 0.66) return balancedColor(3 * d - 2, color1, color2)
    if (d > 0.33) return balancedColor(3 * d - 1, color2, color3)
    return balancedColor(3 * d, color3, color4)
//    val red = ((color1.red * d + color2.red * (1-d))*255).toInt()
//    val green = ((color1.green * d + color2.green * (1-d))*255).toInt()
//    val blue = ((color1.blue * d + color2.blue * (1-d))*255).toInt()
//    return Color(red, green, blue)

//    return balancedColor(d, color1, color2)
}

fun balancedColor(d: Double, color1: Color, color2: Color): Color {
    if (d > 1) return color1
    if (d < 0) return color2
    val red = ((color1.red * d + color2.red * (1 - d)) * 255).toInt()
    val green = ((color1.green * d + color2.green * (1 - d)) * 255).toInt()
    val blue = ((color1.blue * d + color2.blue * (1 - d)) * 255).toInt()
    return Color(red, green, blue)
}


fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2).toDouble())
}

@Composable
fun dot(size: Int, modifier: Modifier, color: Color, time: Long) {
    Box(
        modifier.rotate(time.toFloat() / (15 * 10000000)).clip(RoundedCornerShape((3 + size / 20).dp))
            .size(width = size.dp, height = size.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(color)) {
        }
    }
}

fun size(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Int {
    var x0 = (200 + time / 4000000) % 1200
    var y0 = (200 + time / 7000000) % 800
    val addSize = 3
    var result = 5
    if (y > 550 && x < 550) return result
    if (y > 650 && x < 900) return result
    var distance2 = sqrt((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY).toDouble()) / 200
    val scale: Double = (if (distance2 < 1) {
        addSize * (1 - distance2)
    } else 0.toDouble())
    result =
        result /*+ (addSize * (1 + sin(x.toDouble() / 100 - time.toDouble() / (80*10000000)))).toInt()*/ + (if (State.mouseUsed) round(
            7.5 * scale
        ).toInt() else 0)
    return result
}

fun color(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Color { //first pattern
    val distance = sqrt((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY).toDouble())
    val fade = 1.0//if (distance < 200) (1 - distance/200) else 0.0
    var red = getColorComponent(x, y, 600, 400, time, State.red, fade)
    var green = getColorComponent(x, y, 600, 400, time, State.green, fade)
    var blue = getColorComponent(x, y, 600, 400, time, State.blue, fade)
    return Color(red, green, blue, 0xFF)
}

fun getColorComponent(x: Int, y: Int, x0: Int, y0: Int, time: Long, settings: ColorSettings, fade: Double): Int {
    var distance =
        if (!settings.simple) sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0).toDouble()) else (x - x0).toDouble()
    val result =
        if (settings.enabled) round(255 * (1 + cos(distance / settings.waveLength - time.toDouble() / (settings.period * 10000000))) / 2).toInt() else 0
    return (255 - (255 - result) * fade).toInt()
}

fun color2(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Color { //another pattern
    if (!State.mouseUsed) return Color.White
//    if (y > 650) return Color.White
    val distance = sqrt(((x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY)).toDouble())
    val fade = exp(-1 * distance * distance / 150000)
    var red =
        255 - ((255 - ((1 + sin(12 * distance / 450 - (time.toDouble() / (3 * 100000000)))) / 2 * 255).toInt()) * fade).toInt()
    var green =
        255 - ((255 - ((1 + sin(2 + 12 * distance / 450 - (time.toDouble() / (4 * 100000000)))) / 2 * 255).toInt()) * fade).toInt()
    var blue =
        255 - ((255 - ((1 + sin(4 + 12 * distance / 450 - (time.toDouble() / (5 * 100000000)))) / 2 * 255).toInt()) * fade).toInt()
    return Color(red, green, blue, 0xFF)
}

fun color3(x: Int, y: Int, time: Long, mouseX: Int, mouseY: Int): Color { //another pattern
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


