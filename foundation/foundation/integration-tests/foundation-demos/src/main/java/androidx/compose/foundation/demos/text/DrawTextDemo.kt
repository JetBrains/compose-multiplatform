/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.foundation.demos.text

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlin.system.measureNanoTime
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

@Preview
@Composable
fun DrawTextDemo() {
    LazyColumn {
        item {
            TagLine(tag = "Draw text string")
            DrawTextString()
        }
        item {
            TagLine(tag = "Draw text long string")
            DrawTextLongString()
        }
        item {
            TagLine(tag = "Draw text AnnotatedString")
            DrawTextAnnotatedString()
        }
        item {
            TagLine(tag = "DrawText measure")
            DrawTextMeasure()
        }

        item {
            TagLine(tag = "DrawText and animate color")
            DrawTextAndAnimateColor()
        }
        item {
            TagLine(tag = "DrawText measure and animate color")
            DrawTextMeasureAndAnimateColor()
        }
    }
}

@Composable
fun DrawTextString() {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        drawRect(brush = Brush.linearGradient(RainbowColors))
        val padding = 16.dp.toPx()

        drawText(
            textMeasurer,
            text = "Hello, World!",
            topLeft = Offset(padding, padding),
            style = TextStyle(fontSize = fontSize8)
        )
    }
}

@Composable
fun DrawTextLongString() {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        drawRect(color = Color.Gray)
        val padding = 16.dp.toPx()

        drawText(
            textMeasurer,
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin fringilla " +
                "laoreet aliquam. Aliquam ut nisl aliquet, laoreet tellus quis, sagittis enim. " +
                "Sed sem dolor, tempus blandit purus suscipit, convallis tincidunt purus. Donec " +
                "mattis placerat arcu sed consectetur. Pellentesque eu turpis lacus.",
            topLeft = Offset(padding, padding),
            style = TextStyle(fontSize = fontSize6),
            overflow = TextOverflow.Visible,
            size = IntSize((size.width - 2 * padding).toInt(), (size.height - 2 * padding).toInt())
        )
    }
}

@Composable
fun DrawTextAnnotatedString() {
    val textMeasurer = rememberTextMeasurer()
    val text = remember {
        buildAnnotatedString {
            append("Hello World\n")
            withStyle(
                SpanStyle(brush = Brush.linearGradient(colors = RainbowColors))
            ) {
                append("Hello World")
            }
            append("\nHello World")
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        drawRect(brush = Brush.linearGradient(RainbowColors))
        val padding = 16.dp.toPx()

        translate(padding, padding) {
            drawText(textMeasurer, text, style = TextStyle(fontSize = fontSize6))
        }
    }
}

@Composable
fun DrawTextMeasure() {
    val textMeasurer = rememberTextMeasurer()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                textLayoutResult = textMeasurer.measure(
                    AnnotatedString("Hello, World!"),
                    style = TextStyle(fontSize = fontSize8)
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }) {
        drawRect(brush = Brush.linearGradient(RainbowColors))
        val padding = 16.dp.toPx()

        textLayoutResult?.let { drawText(it, topLeft = Offset(padding, padding)) }
    }
}

@Composable
fun DrawTextAndAnimateColor() {
    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Blue,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
    )

    var skipCache by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer(cacheSize = if (skipCache) 0 else 16)

    val totalMeasurer = remember(skipCache) { AverageDurationMeasurer() }
    val averageTotalDuration by totalMeasurer.averageDurationFlow.collectAsState(0L)

    Column {
        Text("Average total duration: $averageTotalDuration ns")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = skipCache, onCheckedChange = { skipCache = it })
            Text(text = "Skip Cache")
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(100.dp)) {
            drawRect(brush = Brush.linearGradient(RainbowColors))
            val padding = 16.dp.toPx()

            val duration = measureNanoTime {
                drawText(
                    textMeasurer,
                    text = AnnotatedString("Hello, World!"),
                    style = TextStyle(color = color, fontSize = fontSize8),
                    topLeft = Offset(padding, padding)
                )
            }
            totalMeasurer.addMeasure(duration)
        }
    }
}

@Composable
fun DrawTextMeasureAndAnimateColor() {
    val textMeasurer = rememberTextMeasurer()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Blue,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
    )

    var skipCache by remember { mutableStateOf(false) }
    val layoutMeasurer = remember(skipCache) { AverageDurationMeasurer() }
    val drawMeasurer = remember(skipCache) { AverageDurationMeasurer() }

    val averageLayoutDuration by layoutMeasurer.averageDurationFlow.collectAsState(0L)
    val averageDrawDuration by drawMeasurer.averageDurationFlow.collectAsState(0L)

    Column {
        Text("Average layout duration: $averageLayoutDuration ns")
        Text("Average draw duration: $averageDrawDuration ns")
        Text("Average total duration: ${averageLayoutDuration + averageDrawDuration} ns")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = skipCache, onCheckedChange = { skipCache = it })
            Text(text = "Skip Cache")
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val duration = measureNanoTime {
                        textLayoutResult = textMeasurer.measure(
                            AnnotatedString("Hello, World!"),
                            style = TextStyle(fontSize = fontSize8),
                            skipCache = skipCache
                        )
                    }
                    layoutMeasurer.addMeasure(duration)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }) {
            drawRect(brush = Brush.linearGradient(RainbowColors))
            val padding = 16.dp.toPx()

            textLayoutResult?.let {
                val duration = measureNanoTime {
                    drawText(it, topLeft = Offset(padding, padding), color = color)
                }
                drawMeasurer.addMeasure(duration)
            }
        }
    }
}

class AverageDurationMeasurer(private val capacity: Int = 600 /*60 fps * 10 seconds*/) {
    private val values = mutableStateListOf<Long>()

    fun addMeasure(duration: Long) {
        values.add(duration)
        while (values.size > capacity) {
            values.removeFirst()
        }
    }

    val current = derivedStateOf {
        if (values.isEmpty()) 0L else values.average().roundToLong()
    }

    val averageDurationFlow = snapshotFlow { current.value }.withIndex().map { (index, value) ->
        if (index % 60 == 0) value else null
    }.filterNotNull()
}

private val RainbowColors = listOf(
    Color(0xff9c4f96),
    Color(0xffff6355),
    Color(0xfffba949),
    Color(0xfffae442),
    Color(0xff8bd448),
    Color(0xff2aa8f2)
)
