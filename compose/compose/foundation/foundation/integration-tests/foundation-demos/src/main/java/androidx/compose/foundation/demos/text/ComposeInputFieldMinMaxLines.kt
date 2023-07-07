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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun BasicTextFieldMinMaxDemo() {
    LazyColumn {
        item {
            TagLine("empty text, no maxLines")
            TextFieldWithMinMaxLines("", maxLines = Int.MAX_VALUE)
        }
        item {
            TagLine("maxLines == line count")
            TextFieldWithMinMaxLines("abc", maxLines = 1)
        }
        item {
            TagLine("empty text, maxLines > line count")
            TextFieldWithMinMaxLines("", maxLines = 2)
        }
        item {
            TagLine("maxLines > line count")
            TextFieldWithMinMaxLines("abc", maxLines = 3)
        }
        item {
            TagLine("maxLines < line count")
            TextFieldWithMinMaxLines("abc".repeat(20), maxLines = 1)
        }
        item {
            TagLine("empty text, no minLines")
            TextFieldWithMinMaxLines("", minLines = 1)
        }
        item {
            TagLine("minLines == line count")
            TextFieldWithMinMaxLines(createMultilineText(2), minLines = 2)
        }
        item {
            TagLine("empty text, minLines > line count")
            TextFieldWithMinMaxLines("", minLines = 2)
        }
        item {
            TagLine("minLines > line count")
            TextFieldWithMinMaxLines(
                createMultilineText(4),
                minLines = 5
            )
        }
        item {
            TagLine("minLines < line count")
            TextFieldWithMinMaxLines(createMultilineText(3), minLines = 2)
        }
        item {
            TagLine("minLines < maxLines")
            TextFieldWithMinMaxLines(
                createMultilineText(4),
                minLines = 2,
                maxLines = 3
            )
        }
        item {
            TagLine("minLines == maxLines")
            TextFieldWithMinMaxLines(
                createMultilineText(2),
                minLines = 3,
                maxLines = 3
            )
        }
        item {
            TagLine("maxLines=4 with different line heights")
            TextFieldWithMinMaxLines(
                createMultilineText(5),
                maxLines = 4,
                spanStyles = listOf(
                    AnnotatedString.Range(SpanStyle(fontSize = 40.sp), 14, 21)
                )
            )
        }
        item {
            TagLine("minLines=5 with different line heights")
            TextFieldWithMinMaxLines(
                createMultilineText(4),
                minLines = 5,
                spanStyles = listOf(
                    AnnotatedString.Range(SpanStyle(fontSize = 40.sp), 14, 21)
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextFieldWithMinMaxLines(
    str: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>? = null
) {
    val state = rememberSaveable { mutableStateOf(str ?: "abc ".repeat(20)) }

    val visualTransformation: VisualTransformation =
        if (spanStyles == null) {
            VisualTransformation.None
        } else {
            VisualTransformation { annotatedString ->
                TransformedText(
                    AnnotatedString(
                        annotatedString.text,
                        spanStyles = spanStyles
                    ),
                    OffsetMapping.Identity
                )
            }
        }

    BasicTextField(
        modifier = demoTextFieldModifiers.clipToBounds(),
        value = state.value,
        onValueChange = { state.value = it },
        textStyle = TextStyle(fontSize = fontSize8),
        cursorBrush = SolidColor(Color.Red),
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation
    )
}

private fun createMultilineText(lineCount: Int) =
    (1..lineCount).joinToString("\n") { "Line $it" }