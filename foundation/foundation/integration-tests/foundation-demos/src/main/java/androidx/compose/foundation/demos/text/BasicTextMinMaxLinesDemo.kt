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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun BasicTextMinMaxLinesDemo() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TagLine("maxLines == line count")
        TextWithMinMaxLines("Line 1\nLine 2", maxLines = 2)

        TagLine("maxLines > line count")
        TextWithMinMaxLines("abc", maxLines = 3)

        TagLine("maxLines < line count")
        TextWithMinMaxLines("Line 1\nLine 2\nLine 3", maxLines = 2)

        TagLine("maxLines < line count with different line heights")
        TextWithMinMaxLines(
            text = buildAnnotatedString {
                append("Line 1\n")
                withStyle(SpanStyle(fontSize = fontSize8)) {
                    append("Line 2\n")
                }
                append("Line 3")
            },
            maxLines = 2
        )

        TagLine("minLines == line count")
        TextWithMinMaxLines("First line\nSecond line", minLines = 2)

        TagLine("minLines < line count")
        TextWithMinMaxLines("First line\nSecond line\nThird line", minLines = 2)

        TagLine("minLines > line count")
        var sameLineHeightsHasExtraLine by remember { mutableStateOf(false) }
        val extraLine = if (sameLineHeightsHasExtraLine) "\nLine 4" else ""
        TextWithMinMaxLines(
            text = "Line 1\nLine 2\nLine 3$extraLine",
            minLines = 4
        )
        Button(onClick = { sameLineHeightsHasExtraLine = !sameLineHeightsHasExtraLine }) {
            Text(text = "Toggle last line")
        }

        TagLine("minLines > line count with different line heights")
        var diffLineHeightsHasExtraLine by remember { mutableStateOf(false) }
        TextWithMinMaxLines(
            text = buildAnnotatedString {
                append("Line 1\n")
                withStyle(SpanStyle(fontSize = fontSize6)) {
                    append("Line 2\n")
                }
                append("Line 3")
                if (diffLineHeightsHasExtraLine) append("\nLine 4")
            },
            minLines = 4
        )
        Button(onClick = { diffLineHeightsHasExtraLine = !diffLineHeightsHasExtraLine }) {
            Text(text = "Toggle last line")
        }

        TagLine("minLines < maxLines")
        TextWithMinMaxLines(
            "Line 1\nLine 2\nLine 3\nLine 4",
            minLines = 2,
            maxLines = 3
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextWithMinMaxLines(
    text: AnnotatedString,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = Modifier
            .border(1.dp, Color.Gray)
            .padding(2.dp),
        maxLines = maxLines,
        minLines = minLines
    )
}

@Composable
private fun TextWithMinMaxLines(
    text: String = "",
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {
    TextWithMinMaxLines(
        text = AnnotatedString(text),
        minLines = minLines,
        maxLines = maxLines
    )
}