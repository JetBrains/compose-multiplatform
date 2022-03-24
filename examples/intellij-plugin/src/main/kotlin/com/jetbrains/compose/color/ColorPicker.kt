package com.jetbrains.compose.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.WindowPosition

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
        Row {
            Canvas(Modifier.size(360.dp, 256.dp).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isPrimaryPressed) {
                            val position = event.changes.first().position
                            currentColor = try {
                                currentColor.toHsv().copy(
                                    hue = position.x.mod(360f),
                                    saturation = position.y / 256
                                ).toRgb()
                            } catch (t:Throwable) {
                                t.printStackTrace()
                                println("exception $t")
                                currentColor
                            }
                        }
                    }
                }
            }) {
                for (x in 0..360) {
                    for (y in 0..255) {
                        val hue = x
                        val saturation = y / 255f
                        drawRect(
                            color = currentColor.toHsv().copy(hue = hue.toFloat(), saturation = saturation).toRgb(),
                            topLeft = Offset(hue.toFloat(), y.toFloat()),
                            size = Size(1f, 1f)
                        )
                    }
                }
            }
            val BAND_WIDTH = 40
            Canvas(Modifier.size(BAND_WIDTH.dp, 256.dp).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isPrimaryPressed) {
                            currentColor = currentColor.toHsv().copy(value = event.changes.first().position.y / 256f).toRgb()
                        }
                    }
                }
            }) {
                for (y in 0..255) {
                    val value = y / 255f
                    drawRect(
                        color = currentColor.toHsv().copy(value = value).toRgb(), topLeft = Offset(0f, y.toFloat()),
                        size = Size(BAND_WIDTH.toFloat(), 1f)
                    )
                }
            }
        }

        Row {
            Text("color hex: ${currentColor.hexStr()}")
        }
        Row {
            Canvas(Modifier.size(100.dp, 100.dp)) {
                drawRect(color = currentColor, size = Size(100f, 100f))
            }
        }
    }
}

fun Color.hexStr() = "0x" + toArgb().toUInt().toString(16)
