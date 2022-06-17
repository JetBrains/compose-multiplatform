/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Preview
@Composable
fun TextFieldCursorBlinkingDemo() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        BasicText("Focus on any of the text fields below to observe cursor behavior.")
        BasicText("All fields are not editable, with a fixed selection position")
        Item("Default cursor") {
            DefaultCursor()
        }
        Item("Color cursor") {
            ColorCursor()
        }
        Item("Color changing cursor") {
            RainbowCursor()
        }
        Item("Gradient Cursor") {
            GradientCursor()
        }
        Item("Cursors don't blink when typing (fake typing)") {
            TypingCursorNeverBlinks()
        }
        Item("Changing selection shows cursor") {
            ChangingSelectionShowsCursor()
        }
    }
}

@Composable
private fun Item(title: String, content: @Composable () -> Unit) {
    Column {
        BasicText(title, style = TextStyle.Default.copy(
            color = Color(0xFFAAAAAA),
            fontSize = 20.sp
        ))
        content()
    }
}

@Composable
private fun DefaultCursor() {
    val textFieldValue = TextFieldValue(
        text = "Normal blink",
        selection = TextRange(3)
    )
    BasicTextField(value = textFieldValue, onValueChange = {})
}

@Composable
private fun ColorCursor() {
    val textFieldValue = TextFieldValue(
        text = "Red cursor",
        selection = TextRange(3)
    )
    BasicTextField(
        value = textFieldValue,
        onValueChange = {},
        cursorBrush = SolidColor(Color.Red)
    )
}

private val Red = Color(0xffE13C56)
private val Orange = Color(0xffE16D3C)
private val Yellow = Color(0xffE0AE04)
private val Green = Color(0xff78AA04)
private val Blue = Color(0xff4A7DCF)
private val Purple = Color(0xff7B4397)
private val Rainbow = listOf(Orange, Yellow, Green, Blue, Purple, Red)

@Composable
private fun RainbowCursor() {
    val textFieldValue = TextFieldValue(
        text = "Rainbow cursor",
        selection = TextRange(3)
    )

    val color = remember { Animatable(Red) }
    var shouldAnimate by remember { mutableStateOf(false) }
    LaunchedEffect(shouldAnimate) {
        while (shouldAnimate) {
            Rainbow.forEach {
                color.animateTo(it, TweenSpec(1_800))
            }
        }
    }
    BasicTextField(
        value = textFieldValue,
        onValueChange = {},
        cursorBrush = SolidColor(color.value),
        modifier = Modifier.onFocusChanged { shouldAnimate = it.isFocused }
    )
}

@Composable
private fun GradientCursor() {
    val textFieldValue = TextFieldValue(
        text = "Gradient cursor",
        selection = TextRange(3)
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = {},
        cursorBrush = Brush.verticalGradient(colors = Rainbow),
    )
}

@Composable
fun TypingCursorNeverBlinks() {
    var text by remember { mutableStateOf("") }
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(animate) {
        while (animate) {
            text = ""
            listOf("Lorem ", "ipsum ", "was ", "here.").forEach { word ->
                text += word
                delay(500)
            }
        }
    }
    val textFieldValue = TextFieldValue(
        text = text,
        selection = TextRange(text.length),
    )
    BasicTextField(
        value = textFieldValue,
        onValueChange = {},
        modifier = Modifier.onFocusChanged { animate = it.isFocused }
    )
}

@Composable
@Preview
fun ChangingSelectionShowsCursor() {
    val text = "Some longer text that takes a while to cursor through"
    var selection by remember { mutableStateOf(TextRange(0)) }
    LaunchedEffect(text) {
        while (true) {
            selection = TextRange((selection.start + 1) % text.length)
            delay(500)
        }
    }
    val textFieldValue = TextFieldValue(
        text = text,
        selection = selection
    )
    Column {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {},
            textStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace)
        )
    }
}