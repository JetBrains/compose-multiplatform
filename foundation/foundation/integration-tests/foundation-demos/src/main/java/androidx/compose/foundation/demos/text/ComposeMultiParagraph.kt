/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

val lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas fermentum non" +
    " diam sed pretium."

@Preview
@Composable
fun MultiParagraphDemo() {
    LazyColumn {
        item {
            TagLine(tag = "multiple paragraphs basic")
            TextDemoParagraph()
        }
        item {
            TagLine(tag = "multiple paragraphs TextAlign")
            TextDemoParagraphTextAlign()
        }
        item {
            TagLine(tag = "multiple paragraphs line height")
            TextDemoParagraphLineHeight()
        }
        item {
            TagLine(tag = "multiple paragraphs TextIndent")
            TextDemoParagraphIndent()
        }
        item {
            TagLine(tag = "multiple paragraphs TextDirection")
            TextDemoParagraphTextDirection()
        }
    }
}

@Preview
@Composable
fun TextDemoParagraph() {
    val text1 = "paragraph1 paragraph1 paragraph1 paragraph1 paragraph1"
    val text2 = "paragraph2 paragraph2 paragraph2 paragraph2 paragraph2"
    Text(
        text = buildAnnotatedString {
            append(text1)
            withStyle(ParagraphStyle()) {
                append(text2)
            }
        },
        style = TextStyle(fontSize = fontSize6)
    )
}

@Preview
@Composable
fun TextDemoParagraphTextAlign() {
    val annotatedString = buildAnnotatedString {
        TextAlign.values().forEach { textAlign ->
            val str = List(4) { "TextAlign.$textAlign" }.joinToString(" ")
            withStyle(ParagraphStyle(textAlign = textAlign)) {
                append(str)
            }
        }
    }

    Text(text = annotatedString, style = TextStyle(fontSize = fontSize6))
}

@Composable
fun TextDemoParagraphLineHeight() {
    val text1 = "LineHeight=30sp: $lorem"
    val text2 = "LineHeight=40sp: $lorem"
    val text3 = "LineHeight=50sp: $lorem"

    Text(
        text = AnnotatedString(
            text = text1 + text2 + text3,
            spanStyles = listOf(),
            paragraphStyles = listOf(
                AnnotatedString.Range(
                    ParagraphStyle(lineHeight = 30.sp),
                    0,
                    text1.length
                ),
                AnnotatedString.Range(
                    ParagraphStyle(lineHeight = 40.sp),
                    text1.length,
                    text1.length + text2.length
                ),
                AnnotatedString.Range(
                    ParagraphStyle(lineHeight = 50.sp),
                    text1.length + text2.length,
                    text1.length + text2.length + text3.length
                )
            )
        ),
        style = TextStyle(fontSize = fontSize6)
    )
}

@Preview
@Composable
fun TextDemoParagraphIndent() {
    val text1 = "TextIndent firstLine TextIndent firstLine TextIndent firstLine"
    val text2 = "TextIndent restLine TextIndent restLine TextIndent restLine"

    Text(
        text = buildAnnotatedString {
            withStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))) {
                append(text1)
            }
            withStyle(ParagraphStyle(textIndent = TextIndent(restLine = 20.sp))) {
                append(text2)
            }
        },
        style = TextStyle(fontSize = fontSize6)
    )
}

@Composable
fun TextDemoParagraphTextDirection() {
    val ltrText = "Hello World! Hello World! Hello World! Hello World! Hello World!"
    val rtlText = "مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم"
    Text(
        text = buildAnnotatedString {
            withStyle(ParagraphStyle()) {
                append(ltrText)
            }
            withStyle(ParagraphStyle()) {
                append(rtlText)
            }
        },
        style = TextStyle(fontSize = fontSize6)
    )
}
