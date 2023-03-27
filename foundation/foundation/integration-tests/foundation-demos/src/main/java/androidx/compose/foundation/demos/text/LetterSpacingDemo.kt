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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val text = "The quick brown fox jumps over the lazy dog"
@Preview
@Composable
fun LetterSpacingDemo() {
    Column(
        Modifier.padding(horizontal = 16.dp)
    ) {
        var letterSpacing: Float by remember { mutableStateOf(0.0f) }
        var fontSize: Float by remember { mutableStateOf(12f) }

        Text("LetterSpacing: ${letterSpacing.toString().take(4)}.sp")
        Slider(
            value = letterSpacing,
            onValueChange = { letterSpacing = it },
            valueRange = -100f..100f,
        )
        Text("fontSize: ${fontSize.toString().take(4)}.sp")
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 5f..100f
        )
        AnnotatedText(letterSpacing, fontSize)
    }
}

@Composable
fun AnnotatedText(letterSpacing: Float, fontSize: Float) {
    var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Text(text,
            modifier = Modifier
                .fillMaxWidth(0.5f) /* only half the screen, to allow negative em */
                .drawTextMetrics(textLayoutResult, null),
            style = LocalTextStyle.current.copy(
                letterSpacing = letterSpacing.sp,
                fontSize = fontSize.sp),
            onTextLayout = { textLayoutResult = it }
        )
    }
}
