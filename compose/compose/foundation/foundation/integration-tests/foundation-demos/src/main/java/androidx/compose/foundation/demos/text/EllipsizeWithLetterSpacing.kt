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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val fontSize = 21.sp
private val positiveTextIndent = TextIndent(fontSize, fontSize)
private val negativeTextIndent = TextIndent(-fontSize, -fontSize)

@Preview
@Composable
fun EllipsizeWithLetterSpacing() {
    SelectionContainer() {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            for (textIndent in arrayOf(
                TextIndent.None,
                positiveTextIndent,
                negativeTextIndent
            )) {
                for (text in arrayOf(
                    displayText,
                    displayTextArabic,
                    displayTextBidi
                )) {
                    for (textAlign in arrayOf(
                        TextAlign.Start,
                        TextAlign.End,
                        TextAlign.Center,
                        TextAlign.Justify
                    )) {
                        for (maxLines in arrayOf(1, 3)) {
                            SecondTagLine(
                                "align=$textAlign, lines=$maxLines, " +
                                    "indent=${textIndent.toLabel()}"
                            )
                            TextWithEllipsizeAndLetterSpacing(
                                text = text,
                                maxLines = maxLines,
                                textAlign = textAlign,
                                textIndent = textIndent
                            )
                            Spacer(Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextWithEllipsizeAndLetterSpacing(
    text: String,
    maxLines: Int,
    textAlign: TextAlign,
    textIndent: TextIndent
) {
    BasicText(
        modifier = Modifier.background(Color.LightGray).requiredWidth(200.dp),
        text = text.repeat(100),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = fontSize,
            textAlign = textAlign,
            letterSpacing = fontSize / 3,
            textDirection = TextDirection.Content,
            textIndent = textIndent
        )
    )
}

private fun TextIndent.toLabel() = when (this) {
    TextIndent.None -> "None"
    positiveTextIndent -> "Positive"
    negativeTextIndent -> "Negative"
    else -> toString()
}
