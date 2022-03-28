package com.jetbrains.compose.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun ColorPallet(colorState: MutableState<Color>) {
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
        val width = 300f
        val height = 256f
        Row {
            Canvas(Modifier.size(width.dp, height.dp).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isPrimaryPressed) {
                            val position = event.changes.first().position
                            currentColor = try {
                                currentColor.toHsv().copy(
                                    hue = limit0to1(position.x / width) * HSV.HUE_MAX_VALUE,
                                    saturation = limit0to1(position.y / height)
                                ).toRgb()
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                println("exception $t")
                                currentColor
                            }
                        }
                    }
                }
            }) {
                for (x in 0..width.toInt()) {
                    for (y in 0..height.toInt()) {
                        val hue = (x / width) * HSV.HUE_MAX_VALUE
                        val saturation = y / height
                        drawRect(
                            color = currentColor.toHsv().copy(hue = hue, saturation = saturation).toRgb(),
                            topLeft = Offset(x.toFloat(), y.toFloat()),
                            size = Size(1f, 1f)
                        )
                    }
                }
            }
            val BAND_WIDTH = 40
            Canvas(Modifier.size(BAND_WIDTH.dp, height.dp).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isPrimaryPressed) {
                            val position = event.changes.first().position
                            currentColor =
                                currentColor.toHsv().copy(
                                    value = limit0to1(position.y / height)
                                ).toRgb()
                        }
                    }
                }
            }) {
                for (y in 0..height.toInt()) {
                    val value = y / 255f
                    drawRect(
                        color = currentColor.toHsv().copy(value = value).toRgb(), topLeft = Offset(0f, y.toFloat()),
                        size = Size(BAND_WIDTH.toFloat(), 1f)
                    )
                }
            }
        }

        Row {
            Text("color hex: ${currentColor.toHexString()}")
        }
        Row {
            Canvas(Modifier.size(100.dp, 100.dp)) {
                drawRect(color = currentColor, size = Size(100f, 100f))
            }
        }
    }
}

fun Color.toHexString() = "0x" + toArgb().toUInt().toString(16)
fun limit(value: Float, min: Float, max: Float) = maxOf(value, min).mod(max)
fun limit0to1(value: Float) = limit(value = value, 0f, 1f)
