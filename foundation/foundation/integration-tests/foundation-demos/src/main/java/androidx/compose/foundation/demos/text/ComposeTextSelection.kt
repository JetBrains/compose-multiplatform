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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun TextSelectionDemo() {
    LazyColumn {
        item {
            TagLine(tag = "selection")
            TextDemoSelection()
        }
        item {
            TagLine(tag = "selection with string input")
            TextDemoSelectionWithStringInput()
        }
        item {
            TagLine(tag = "selection in 2D Array Vertical")
            TextDemoSelection2DArrayVertical()
        }
        item {
            TagLine(tag = "enable and disable selection")
            TextDemoSelectionEnableAndDisable()
        }
        item {
            TagLine(tag = "fix crashing of longpress in the blank area")
            SelectionContainer {
                Text(
                    text = "Hello World\nHello",
                    modifier = Modifier.fillMaxWidth()
                        .border(BorderStroke(1.dp, color = Color.Black))
                        .height(80.dp)
                )
            }
        }
        item {
            TagLine(tag = "")
        }
    }
}

@Preview
@Composable
fun TextDemoSelection() {
    val arabicSentence =
        "\nكلمة شين في قاموس المعاني الفوري مجال البحث مصطلحات المعجم الوسيط ،اللغة"
    SelectionContainer {
        Text(
            style = TextStyle(
                color = Color(0xFFFF0000),
                fontSize = fontSize6,
                fontWeight = FontWeight.W200,
                fontStyle = FontStyle.Italic
            ),
            text = buildAnnotatedString {
                append(text = "$displayText   ")
                append(text = "$displayTextArabic   ")
                append(text = "$displayTextChinese   ")

                withStyle(
                    SpanStyle(
                        color = Color(0xFF0000FF),
                        fontSize = fontSize10,
                        fontWeight = FontWeight.W800,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append(displayTextHindi)
                }

                append(text = arabicSentence)

                withStyle(SpanStyle(localeList = LocaleList("zh-CN"))) {
                    append("\n先帝创业未半而中道崩殂，今天下三分，益州疲弊，此诚危急存亡之秋也。")
                }

                withStyle(SpanStyle(localeList = LocaleList("ja-JP"))) {
                    append("\nまず、現在天下が魏・呉・蜀に分れており、そのうち蜀は疲弊していることを指摘する。")
                }
            }
        )
    }
}

@Preview
@Composable
fun TextDemoSelectionWithStringInput() {
    SelectionContainer {
        Text(
            text = "$displayText    $displayTextChinese    $displayTextHindi",
            color = Color(0xFFFF0000),
            fontSize = fontSize6,
            fontWeight = FontWeight.W200,
            fontStyle = FontStyle.Italic
        )
    }
}

@Preview
@Composable
fun TextDemoSelection2DArrayVertical() {
    var text = ""
    for (i in 1..3) {
        text = "$text$displayText" + "\n"
    }

    val colorList = listOf(
        Color(0xFFFF0000),
        Color(0xFF00FF00),
        Color(0xFF0000FF),
        Color(0xFF00FFFF),
        Color(0xFFFF00FF),
        Color(0xFFFFFF00),
        Color(0xFF0000FF),
        Color(0xFF00FF00),
        Color(0xFFFF0000)
    )

    SelectionContainer {
        Column(Modifier.fillMaxHeight()) {
            for (i in 0..2) {
                Row(Modifier.fillMaxWidth()) {
                    for (j in 0..2) {
                        Text(
                            text = text,
                            style = TextStyle(
                                color = colorList[i * 3 + j],
                                fontSize = fontSize6
                            )

                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TextDemoSelectionEnableAndDisable() {
    val textSelectable = "This text is selectable."
    val textNotSelectable = "This text is not selectable."

    SelectionContainer {
        Column(Modifier.fillMaxHeight()) {
            Text(
                text = textSelectable,
                style = TextStyle(fontSize = fontSize8)
            )
            DisableSelection {
                Text(
                    text = textNotSelectable,
                    style = TextStyle(fontSize = fontSize8)
                )
            }
            Text(
                text = textSelectable,
                style = TextStyle(fontSize = fontSize8)
            )
            DisableSelection {
                Text(
                    text = textNotSelectable,
                    style = TextStyle(fontSize = fontSize8)
                )
                Text(
                    text = textNotSelectable,
                    style = TextStyle(fontSize = fontSize8)
                )
            }
            Text(
                text = textSelectable,
                style = TextStyle(fontSize = fontSize8)
            )
        }
    }
}
