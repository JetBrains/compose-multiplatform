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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightBehavior
import androidx.compose.ui.text.style.LineVerticalAlignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.round

private val HintStyle = TextStyle(fontSize = 14.sp)
private fun Float.format(digits: Int = 2) = "%.${digits}f".format(this)
private val FontSize = 60.sp

@OptIn(ExperimentalTextApi::class)
@Composable
fun TextLineHeightDemo() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
            .background(TextMetricColors.Default.background)
    ) {
        var lineHeightSp = remember { mutableStateOf(60f) }
        var lineHeightEm = remember { mutableStateOf(1f) }
        var lineHeightEnabled = remember { mutableStateOf(false) }
        val lineHeightBehaviorEnabled = remember { mutableStateOf(false) }
        var lineVerticalAlignment = remember {
            mutableStateOf(LineVerticalAlignment.Proportional)
        }
        val trimFirstLineTop = remember { mutableStateOf(false) }
        val trimLastLineBottom = remember { mutableStateOf(false) }
        val includeFontPadding = remember { mutableStateOf(false) }
        val applyMaxLines = remember { mutableStateOf(false) }
        val ellipsize = remember { mutableStateOf(false) }
        val useSizedSpan = remember { mutableStateOf(false) }
        val singleLine = remember { mutableStateOf(false) }
        val useTallScript = remember { mutableStateOf(false) }

        Column(Modifier.padding(16.dp)) {
            LineHeightConfiguration(lineHeightSp, lineHeightEm, lineHeightEnabled)
            StringConfiguration(useSizedSpan, singleLine, useTallScript)
            FontPaddingAndMaxLinesConfiguration(includeFontPadding, applyMaxLines, ellipsize)
            LineHeightBehaviorConfiguration(
                lineHeightBehaviorEnabled,
                trimFirstLineTop,
                trimLastLineBottom,
                lineVerticalAlignment
            )
            Spacer(Modifier.padding(16.dp))
            TextWithLineHeight(
                lineHeightEnabled.value,
                lineHeightSp.value,
                lineHeightEm.value,
                if (lineHeightBehaviorEnabled.value) {
                    LineHeightBehavior(
                        alignment = lineVerticalAlignment.value,
                        trimFirstLineTop = trimFirstLineTop.value,
                        trimLastLineBottom = trimLastLineBottom.value
                    )
                } else null,
                includeFontPadding.value,
                applyMaxLines.value,
                ellipsize.value,
                useSizedSpan.value,
                singleLine.value,
                useTallScript.value
            )
        }
    }
}

@Composable
private fun LineHeightConfiguration(
    lineHeightSp: MutableState<Float>,
    lineHeightEm: MutableState<Float>,
    lineHeightEnabled: MutableState<Boolean>
) {
    Column {
        val density = LocalDensity.current
        val lineHeightInPx = with(density) { lineHeightSp.value.sp.toPx() }
        Text(
            "Line height: ${lineHeightSp.value.format()}.sp [$lineHeightInPx px, $density]",
            style = HintStyle
        )
        Row {
            Checkbox(
                checked = lineHeightEnabled.value,
                onCheckedChange = { lineHeightEnabled.value = it }
            )
            SnappingSlider(
                value = lineHeightSp.value,
                onValueChange = {
                    lineHeightSp.value = it
                    lineHeightEm.value = 0f
                    lineHeightEnabled.value = true
                },
                steps = 11,
                valueRange = 0f..120f
            )
        }

        val fontSizeInPx = with(density) { FontSize.toPx() }
        val lineHeightEmInPx = lineHeightEm.value * fontSizeInPx
        Text(
            "Line height: ${lineHeightEm.value.format()}.em [$lineHeightEmInPx px]",
            style = HintStyle
        )
        Row {
            Checkbox(
                checked = lineHeightEnabled.value,
                onCheckedChange = { lineHeightEnabled.value = it }
            )
            SnappingSlider(
                value = lineHeightEm.value,
                onValueChange = {
                    lineHeightEm.value = it
                    lineHeightSp.value = 0f
                    lineHeightEnabled.value = true
                },
                steps = 5,
                valueRange = 0f..3f
            )
        }
    }
}

@Composable
private fun StringConfiguration(
    useSizeSpan: MutableState<Boolean>,
    singleLine: MutableState<Boolean>,
    useTallScript: MutableState<Boolean>
) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useSizeSpan.value,
                onCheckedChange = { useSizeSpan.value = it }
            )
            Text("Size Span", style = HintStyle)
            Checkbox(
                checked = singleLine.value,
                onCheckedChange = { singleLine.value = it }
            )
            Text("Single Line", style = HintStyle)
            Checkbox(
                checked = useTallScript.value,
                onCheckedChange = { useTallScript.value = it }
            )
            Text("Tall script", style = HintStyle)
        }
    }
}

@Composable
private fun FontPaddingAndMaxLinesConfiguration(
    includeFontPadding: MutableState<Boolean>,
    applyMaxLines: MutableState<Boolean>,
    ellipsize: MutableState<Boolean>,
) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = includeFontPadding.value,
                onCheckedChange = { includeFontPadding.value = it }
            )
            Text("IncludeFontPadding", style = HintStyle)
            Checkbox(
                checked = applyMaxLines.value,
                onCheckedChange = { applyMaxLines.value = it }
            )
            Text("maxLines", style = HintStyle)
            Checkbox(
                checked = ellipsize.value,
                onCheckedChange = { ellipsize.value = it }
            )
            Text("ellipsize", style = HintStyle)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun LineHeightBehaviorConfiguration(
    lineHeightBehaviorEnabled: MutableState<Boolean>,
    trimFirstLineTop: MutableState<Boolean>,
    trimLastLineBottom: MutableState<Boolean>,
    lineVerticalAlignment: MutableState<LineVerticalAlignment>
) {
    Column {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = lineHeightBehaviorEnabled.value,
                    onCheckedChange = { lineHeightBehaviorEnabled.value = it }
                )
                Text("LineHeightBehavior", style = HintStyle)
                Checkbox(
                    checked = trimFirstLineTop.value,
                    onCheckedChange = { trimFirstLineTop.value = it },
                    enabled = lineHeightBehaviorEnabled.value
                )
                Text("TrimFirstLineTop", style = HintStyle)
                Spacer(Modifier.padding(8.dp))
                Checkbox(
                    checked = trimLastLineBottom.value,
                    onCheckedChange = { trimLastLineBottom.value = it },
                    enabled = lineHeightBehaviorEnabled.value
                )
                Text("TrimLastLineBottom", style = HintStyle)
            }
        }

        Column(Modifier.horizontalScroll(rememberScrollState())) {
            LineHeightDistributionOptions(lineVerticalAlignment, lineHeightBehaviorEnabled.value)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun LineHeightDistributionOptions(
    lineVerticalAlignment: MutableState<LineVerticalAlignment>,
    enabled: Boolean
) {
    val options = listOf(
        LineVerticalAlignment.Proportional,
        LineVerticalAlignment.Top,
        LineVerticalAlignment.Center,
        LineVerticalAlignment.Bottom
    )

    Row(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                Modifier
                    .height(56.dp)
                    .selectable(
                        selected = (option == lineVerticalAlignment.value),
                        onClick = { lineVerticalAlignment.value = option },
                        role = Role.RadioButton,
                        enabled = enabled
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == lineVerticalAlignment.value),
                    onClick = null,
                    enabled = enabled
                )
                Text(text = option.toString().split(".")[1], style = HintStyle)
            }
        }
    }
}

@Composable
private fun SnappingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    snap: Boolean = true,
    enabled: Boolean = true
) {
    var lastValue by remember(value) { mutableStateOf(value) }
    val increment = valueRange.endInclusive / (steps + 1).toFloat()
    val snapValue = round(value / increment / 2f) * increment

    Slider(
        modifier = modifier,
        value = lastValue,
        onValueChangeFinished = {
            if (snap) {
                if (lastValue != snapValue) {
                    lastValue = snapValue
                }
            }
        },
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalTextApi::class)
@Composable
private fun TextWithLineHeight(
    lineHeightEnabled: Boolean,
    lineHeightSp: Float,
    lineHeightEm: Float,
    lineHeightBehavior: LineHeightBehavior?,
    includeFontPadding: Boolean,
    applyMaxLines: Boolean,
    ellipsize: Boolean,
    useSizeSpan: Boolean,
    singleLine: Boolean,
    useTallScript: Boolean
) {
    val width = with(LocalDensity.current) { (FontSize.toPx() * 5).toDp() }

    var string = if (singleLine) {
        if (useTallScript) "ဪไ၇ဤန်" else "Abyfhpq"
    } else {
        if (useTallScript) "ဪไ၇ဤနဩဦဤနိမြသကိမ့်ဪไန််" else "ABCDEfgHIjKgpvylzgpvykwi"
    }

    if (applyMaxLines) {
        string = string.repeat(4)
    }

    var text = AnnotatedString(string)
    if (useSizeSpan) {
        text = if (singleLine) {
            buildAnnotatedString {
                append(text)
                addStyle(style = SpanStyle(fontSize = FontSize * 1.5), start = 1, end = 2)
                addStyle(style = SpanStyle(fontSize = FontSize * 1.5), start = 3, end = 4)
            }
        } else {
            buildAnnotatedString {
                append(text)
                addStyle(style = SpanStyle(fontSize = FontSize * 1.5), start = 1, end = 2)
                addStyle(style = SpanStyle(fontSize = FontSize * 1.5), start = 10, end = 12)
                addStyle(style = SpanStyle(fontSize = FontSize * 1.5), start = 18, end = 19)
            }
        }
    }

    val maxLines = if (applyMaxLines) 3 else Int.MAX_VALUE

    val style = TextStyle(
        fontSize = FontSize,
        color = TextMetricColors.Default.text,
        lineHeightBehavior = lineHeightBehavior,
        lineHeight = if (lineHeightEnabled) {
            if (lineHeightSp > 0) lineHeightSp.sp else lineHeightEm.em
        } else {
            TextUnit.Unspecified
        },
        platformStyle = PlatformTextStyle(includeFontPadding = includeFontPadding)
    )

    val overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(width)) {
            TextWithMetrics(
                text = text,
                style = style,
                maxLines = maxLines,
                overflow = overflow,
                softWrap = !singleLine
            )
        }
    }
}
