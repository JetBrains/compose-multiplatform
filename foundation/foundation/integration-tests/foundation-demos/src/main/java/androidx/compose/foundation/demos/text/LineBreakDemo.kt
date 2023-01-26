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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val lineBreakOptions = listOf(
    "Simple" to LineBreak.Simple,
    "Paragraph" to LineBreak.Paragraph,
    "Heading" to LineBreak.Heading,
    "Custom" to LineBreak(
        strategy = LineBreak.Strategy.Balanced,
        strictness = LineBreak.Strictness.Strict,
        wordBreak = LineBreak.WordBreak.Default
    )
)

private val demoText = "This is an example text\n今日は自由が丘で焼き鳥を食べます。"
private val presetNameStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)

@Composable
fun TextLineBreakDemo() {
    val selectedFontSize = remember { mutableStateOf(16f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Font size: ${selectedFontSize.value}")
        Slider(
            value = selectedFontSize.value,
            onValueChange = { value -> selectedFontSize.value = value },
            valueRange = 8f..48f
        )

        Row(Modifier.fillMaxWidth()) {
            val textModifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 5.dp)
                .border(1.dp, Color.Gray)

            lineBreakOptions.forEach { (presetName, preset) ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(presetNameStyle) {
                            append(presetName)
                            append(":\n")
                        }
                        append(demoText)
                    },
                    style = TextStyle(
                        lineBreak = preset,
                        fontSize = selectedFontSize.value.sp
                    ),
                    modifier = textModifier.weight(1f)
                )
            }
        }
    }
}