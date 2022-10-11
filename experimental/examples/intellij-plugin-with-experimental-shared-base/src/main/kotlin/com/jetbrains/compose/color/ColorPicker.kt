/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val VALUE_BAND_RATIO = 0.07f
private val DEFAULT_COLORS =
    listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray, Color.Yellow, Color.Cyan, Color.Magenta)

@Composable
fun ColorPicker(colorState: MutableState<Color>) {
    var currentColor: Color by remember { colorState }
    Column {
        Row {
            DEFAULT_COLORS.forEach {
                Box(Modifier.size(30.dp).background(color = it).clickable {
                    currentColor = it
                })
            }
        }
        Divider(Modifier.size(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Result color:")
            Divider(Modifier.size(2.dp))
            TextField(modifier = Modifier.width(120f.dp), value = currentColor.toHexString(), onValueChange = {})
            Divider(Modifier.size(2.dp))
            val size = 60f
            Box(Modifier.size(size.dp).background(color = currentColor))
        }
        Divider(Modifier.size(2.dp))
        var width by remember { mutableStateOf(300) }
        var height by remember { mutableStateOf(256) }
        val rainbowWidth by derivedStateOf { (width * (1 - VALUE_BAND_RATIO)).toInt() }
        val bandWidth by derivedStateOf { width * VALUE_BAND_RATIO }
        fun calcHue(x: Float) = limit0to1(x / rainbowWidth) * HSV.HUE_MAX_VALUE
        fun calcSaturation(y: Float) = 1 - limit0to1(y / height)
        fun calcValue(y: Float) = 1 - limit0to1(y / height)
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
                val circleX = (currentColor.toHsv().hue / 360) * rainbowWidth
                val circleY = (1 - currentColor.toHsv().saturation) * height
                drawCircle(
                    center = Offset(circleX, circleY),
                    color = Color.Black,
                    radius = 5f,
                    style = Stroke(width = 3f)
                )
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
