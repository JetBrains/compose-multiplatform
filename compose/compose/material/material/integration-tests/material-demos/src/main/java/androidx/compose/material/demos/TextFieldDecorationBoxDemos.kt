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

package androidx.compose.material.demos

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.material.TextFieldDefaults.TextFieldDecorationBox
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun DecorationBoxDemos() {
    LazyColumn(
        modifier = Modifier.wrapContentSize(Alignment.Center).width(280.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Progressing indicator")
            IndicatorLineTextField(IndicatorType.Progress)
        }
        item {
            Text("Animated indicator")
            IndicatorLineTextField(IndicatorType.Animated)
        }
        item {
            Text("Animated gradient indicator")
            IndicatorLineTextField(IndicatorType.Gradient)
        }
        item {
            Text("Dense outlined text field")
            DenseOutlinedTextField()
        }
        item {
            Text("Dense text field with custom horizontal padding")
            DenseTextField()
        }
    }
}

@Composable
private fun DenseOutlinedTextField() {
    // 40.dp height single line Outlined text field
    var text by remember { mutableStateOf("") }
    val singleLine = true
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.height(40.dp).width(TextFieldDefaults.MinWidth),
        singleLine = singleLine,
        interactionSource = interactionSource
    ) { innerTextField ->
        @OptIn(ExperimentalMaterialApi::class)
        OutlinedTextFieldDecorationBox(
            value = text,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            contentPadding = TextFieldDefaults.outlinedTextFieldPadding(
                // make it dense, Modifier.height controls the height in this case
                top = 0.dp, bottom = 0.dp
            )
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DenseTextField() {
    var text by remember { mutableStateOf("") }
    val singleLine = true
    val enabled = true
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .indicatorLine(enabled, false, interactionSource, TextFieldDefaults.textFieldColors())
            .background(
                TextFieldDefaults.textFieldColors().backgroundColor(enabled).value,
                TextFieldDefaults.TextFieldShape
            )
            .width(TextFieldDefaults.MinWidth),
        singleLine = singleLine,
        interactionSource = interactionSource
    ) { innerTextField ->
        TextFieldDecorationBox(
            value = text,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text("Label") },
            contentPadding = TextFieldDefaults.textFieldWithLabelPadding(
                start = 4.dp,
                end = 4.dp,
                bottom = 4.dp // make it dense
            )
        )
    }
}

@Composable
private fun IndicatorLineTextField(type: IndicatorType) {
    var text by remember { mutableStateOf("") }
    val singleLine = true
    val enabled = true
    val interactionSource = remember { MutableInteractionSource() }

    val colors = TextFieldDefaults.textFieldColors()
    val indicator = when (type) {
        IndicatorType.Progress -> Modifier.progressIndicatorLine(text, interactionSource, enabled)
        IndicatorType.Animated -> Modifier.animatedIndicator(
            interactionSource, enabled, false, colors
        )
        IndicatorType.Gradient -> Modifier.animatedGradient(interactionSource, enabled)
    }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = indicator
            .background(colors.backgroundColor(enabled).value, TextFieldDefaults.TextFieldShape)
            .width(TextFieldDefaults.MinWidth),
        singleLine = singleLine,
        interactionSource = interactionSource,
        enabled = enabled
    ) { innerTextField ->
        @OptIn(ExperimentalMaterialApi::class)
        TextFieldDecorationBox(
            value = text,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text("Label") },
            colors = colors
        )
    }
}

private const val ExpectedInputLength = 8
private fun Modifier.progressIndicatorLine(
    text: String,
    interactionSource: InteractionSource,
    enabled: Boolean
): Modifier = composed {
    val animationDuration = 150
    val focused by interactionSource.collectIsFocusedAsState()

    // height of line
    val targetHeight = if (focused) 4.dp else 2.dp
    val animatedHeight = if (enabled)
        animateDpAsState(targetHeight, tween(durationMillis = animationDuration))
    else
        rememberUpdatedState(1.dp)

    // width of line
    val target = (text.length.toFloat() / ExpectedInputLength).coerceAtMost(1.0f)
    val progress = animateFloatAsState(target, tween(durationMillis = animationDuration))

    val unfocusedEmptyColor = MaterialTheme
        .colors
        .onSurface
        .copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
    val progressColor = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)

    drawWithContent {
        drawContent()
        val strokeWidth = animatedHeight.value.value * density
        val y = size.height - strokeWidth / 2
        val x = size.width * progress.value

        drawLine(
            SolidColor(unfocusedEmptyColor),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth
        )

        drawLine(
            SolidColor(progressColor),
            Offset(0f, y),
            Offset(x, y),
            strokeWidth
        )
    }
}

private fun Modifier.animatedIndicator(
    interactionSource: InteractionSource,
    enabled: Boolean,
    isError: Boolean,
    colors: TextFieldColors
): Modifier = composed {
    val animationDuration = 150
    val focused by interactionSource.collectIsFocusedAsState()
    // height of line
    val targetHeight = if (focused) 2.dp else 1.dp
    val animatedHeight = if (enabled) {
        animateDpAsState(targetHeight, tween(animationDuration))
    } else {
        rememberUpdatedState(1.dp)
    }
    // width of line
    val targetFloat = if (focused) 1f else 0f
    val progress = if (enabled) {
        animateFloatAsState(targetFloat, tween(animationDuration))
    } else {
        rememberUpdatedState(1f)
    }

    val color = colors.indicatorColor(enabled, isError, interactionSource)
    drawWithContent {
        drawContent()
        val strokeWidth = animatedHeight.value.value * density
        val y = size.height - strokeWidth / 2
        val deltaX = size.width * (progress.value) / 2
        val offset1 = if (focused) Offset(size.width / 2 - deltaX, y) else Offset(0f, y)
        val offset2 = if (focused) Offset(size.width / 2 + deltaX, y) else Offset(size.width, y)
        drawLine(
            SolidColor(color.value),
            offset1,
            offset2,
            strokeWidth
        )
    }
}

private fun Modifier.animatedGradient(
    interactionSource: InteractionSource,
    enabled: Boolean
): Modifier = composed {
    val animationDuration = 150
    val unfocusedColor =
        MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
    val disabledColor = unfocusedColor.copy(alpha = ContentAlpha.disabled)
    val focused by interactionSource.collectIsFocusedAsState()

    // height of line
    val targetHeight = if (focused) 2.dp else 1.dp
    val animatedHeight = if (enabled) {
        animateDpAsState(targetHeight, tween(animationDuration))
    } else {
        rememberUpdatedState(1.dp)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val brush = when {
        !enabled -> SolidColor(disabledColor)
        !focused -> SolidColor(unfocusedColor)
        else -> {
            val progress = infiniteTransition.animateFloat(
                0.2f,
                0.8f,
                infiniteRepeatable(tween(3_000), RepeatMode.Reverse)
            )
            Brush.horizontalGradient(
                0.0f to Color.Blue,
                progress.value to Color.Cyan,
                1f to Color.Blue
            )
        }
    }

    drawWithContent {
        drawContent()

        val strokeWidth = animatedHeight.value.value * density
        val y = size.height - strokeWidth / 2
        drawLine(
            brush,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth
        )
    }
}

private enum class IndicatorType {
    Progress, Animated, Gradient
}
