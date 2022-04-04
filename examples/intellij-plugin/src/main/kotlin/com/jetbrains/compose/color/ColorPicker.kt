package com.jetbrains.compose.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(colorState: MutableState<Color>) {
    Column {
        Row {
            listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray, Color.Yellow, Color.Cyan).forEach {
                val width = 40f
                val height = 40f
                Canvas(Modifier.size(width.dp, height.dp).clickable {
                    colorState.value = it
                }) {
                    drawRect(color = it, size = Size(width, height))
                }
            }
        }
        Divider(Modifier.size(5.dp))
        var currentColor: Color by remember { colorState }

        Divider(Modifier.size(5.dp))
        var width by remember { mutableStateOf(300) }
        var height by remember { mutableStateOf(256) }
        val valueBandRatio = 0.07f
        val rainbowWidth by derivedStateOf { (width * (1 - valueBandRatio)).toInt() }
        val bandWidth by derivedStateOf { width * valueBandRatio }
        fun calcHue(x: Float) = limit0to1(x / width) * HSV.HUE_MAX_VALUE
        fun calcSaturation(y: Float) = 1 - limit0to1(y / height)
        fun calcValue(y: Float) = 1 - limit0to1(y / height)

        Row {
            Text("color hex: ${currentColor.toHexString()}")
        }
        Row {
            Canvas(Modifier.size(100.dp, 100.dp)) {
                drawRect(color = currentColor, size = Size(100f, 100f))
            }
        }
        Row(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                width = size.width
                height = size.height
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isPrimaryPressed) {
                            val position = event.changes.first().position
                            if (position.x < rainbowWidth) {
                                currentColor = try {
                                    currentColor.toHsv().copy(
                                        hue = calcHue(position.x),
                                        saturation = calcSaturation(position.y)
                                    ).toRgb()
                                } catch (t: Throwable) {
                                    t.printStackTrace()
                                    println("exception $t")
                                    currentColor
                                }
                            } else {
                                currentColor =
                                    currentColor.toHsv().copy(
                                        value = calcValue(position.y)
                                    ).toRgb()
                            }
                        }
                    }
                }
            }) {
                for (x in 0..rainbowWidth) {
                    for (y in 0..height) {
                        drawRect(
                            color = currentColor.toHsv().copy(
                                hue = calcHue(x.toFloat()),
                                saturation = calcSaturation(y.toFloat())
                            ).toRgb(),
                            topLeft = Offset(x.toFloat(), y.toFloat()),
                            size = Size(1f, 1f)
                        )
                    }
                }
                val valueBandX = rainbowWidth + 1
                for (y in 0..height) {
                    drawRect(
                        color = currentColor.toHsv().copy(value = calcValue(y.toFloat())).toRgb(),
                        topLeft = Offset(valueBandX.toFloat(), y.toFloat()),
                        size = Size(bandWidth, 1f)
                    )
                }
            }

        }
    }
}

fun Color.toHexString() = "0x" + toArgb().toUInt().toString(16)
fun limit(value: Float, min: Float, max: Float) = minOf(
    maxOf(value, min),
    max
)

fun limit0to1(value: Float) = limit(value = value, 0f, 1f)
