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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.samples.TextStyleBrushSample
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TextBrushDemo() {
    LazyColumn {
        item {
            TagLine(tag = "Sample")
            TextStyleBrushSample()
        }
        item {
            TagLine(tag = "Brush")
            BrushDemo()
        }
        item {
            TagLine(tag = "Brush Emojis")
            BrushGraphicalEmoji()
        }
        item {
            TagLine(tag = "SingleLine Span Brush")
            SingleLineSpanBrush()
        }
        item {
            TagLine(tag = "MultiLine Span Brush")
            MultiLineSpanBrush()
        }
        item {
            TagLine(tag = "MultiParagraph Brush")
            MultiParagraphBrush()
        }
        item {
            TagLine(tag = "Animated Brush")
            AnimatedBrush()
        }
        item {
            TagLine(tag = "Shadow and Brush")
            ShadowAndBrush()
        }
        item {
            TagLine(tag = "TextField")
            TextFieldBrush()
        }
    }
}

@Composable
fun BrushDemo() {
    Text(
        "Brush is awesome\nBrush is awesome\nBrush is awesome",
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = RainbowColors,
                tileMode = TileMode.Mirror
            ),
            fontSize = 30.sp
        )
    )
}

@Composable
fun BrushGraphicalEmoji() {
    Text(
        "\uD83D\uDEF3\uD83D\uDD2E\uD83E\uDDED\uD83E\uDD5D\uD83E\uDD8C\uD83D\uDE0D",
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = RainbowColors,
                tileMode = TileMode.Mirror
            )
        ),
        fontSize = 30.sp
    )
}

@Composable
fun SingleLineSpanBrush() {
    val infiniteTransition = rememberInfiniteTransition()
    val start by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        buildAnnotatedString {
            append("Brush is awesome\n")
            withStyle(
                SpanStyle(
                    brush = Brush.linearGradient(
                        colors = RainbowColors,
                        start = Offset(start, 0f),
                        tileMode = TileMode.Mirror
                    )
                )
            ) {
                append("Brush is awesome")
            }
            append("\nBrush is awesome")
        },
        fontSize = 30.sp,
    )
}

@Composable
fun MultiLineSpanBrush() {
    Text(
        buildAnnotatedString {
            append("Brush is aweso")
            withStyle(
                SpanStyle(
                    brush = Brush.linearGradient(
                        colors = RainbowColors,
                        tileMode = TileMode.Mirror
                    )
                )
            ) {
                append("me\nBrush is awesome\nCo")
            }
            append("mpose is awesome")
        },
        fontSize = 30.sp,
    )
}

@Composable
fun MultiParagraphBrush() {
    Text(
        buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Right)) {
                append(loremIpsum(wordCount = 29))
            }

            withStyle(ParagraphStyle(textAlign = TextAlign.Left)) {
                append(loremIpsum(wordCount = 29))
            }
        },
        style = TextStyle(
            brush = Brush.radialGradient(
                *RainbowStops.zip(RainbowColors).toTypedArray(),
                radius = 600f,
                tileMode = TileMode.Mirror
            )
        ),
        fontSize = 30.sp
    )
}

@Composable
fun AnimatedBrush() {
    val infiniteTransition = rememberInfiniteTransition()
    val radius by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        text = loremIpsum(wordCount = 29),
        style = TextStyle(
            brush = Brush.radialGradient(
                *RainbowStops.zip(RainbowColors).toTypedArray(),
                radius = radius,
                tileMode = TileMode.Mirror
            ),
            fontSize = 30.sp
        )
    )
}

@Composable
fun ShadowAndBrush() {
    Text(
        "Brush is awesome",
        style = TextStyle(
            shadow = Shadow(
                offset = Offset(8f, 8f),
                blurRadius = 4f,
                color = Color.Black
            ),
            brush = Brush.linearGradient(
                colors = RainbowColors,
                tileMode = TileMode.Mirror
            )
        ),
        fontSize = 42.sp
    )
}

@Composable
fun TextFieldBrush() {
    var text by remember { mutableStateOf("Brush is awesome") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            brush = Brush.linearGradient(
                colors = RainbowColors,
                tileMode = TileMode.Mirror
            ),
            fontSize = 30.sp
        )
    )
}

private val RainbowColors = listOf(
    Color(0xff9c4f96),
    Color(0xffff6355),
    Color(0xfffba949),
    Color(0xfffae442),
    Color(0xff8bd448),
    Color(0xff2aa8f2)
)
private val RainbowStops = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
