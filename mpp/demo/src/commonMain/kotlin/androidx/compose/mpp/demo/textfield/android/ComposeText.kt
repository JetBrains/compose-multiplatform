/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo.textfield.android

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private const val longText = "This is a very-very long string that wraps into a few lines " +
    "given the width restrictions."
const val displayText = "Text Demo"
const val displayTextChinese = "文本演示"
const val displayTextArabic = "\u0639\u0631\u0636\u0020\u0627\u0644\u0646\u0635"
const val displayTextHindi = "पाठ डेमो"
const val displayTextBidi = "Text \u0639\u0631\u0636"

val fontSize4 = 16.sp
val fontSize6 = 20.sp
val fontSize8 = 25.sp
val fontSize10 = 30.sp

@Composable
fun TagLine(tag: String) {
    Text(
        style = TextStyle(fontSize = fontSize8),
        text = buildAnnotatedString {
            append("\n")
            withStyle(
                style = SpanStyle(
                    color = Color(0xFFAAAAAA),
                    fontSize = fontSize6
                )
            ) {
                append(tag)
            }
        }
    )
}