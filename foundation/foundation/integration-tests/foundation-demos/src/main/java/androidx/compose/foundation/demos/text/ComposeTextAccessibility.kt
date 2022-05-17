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

import androidx.compose.material.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun TextAccessibilityDemo() {
    Column {
        TagLine("Text to speech with different locales.")
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(localeList = LocaleList("en-us")))
                append("Hello!\n")
                pop()
                pushStyle(SpanStyle(localeList = LocaleList("en-gb")))
                append("Hello!\n")
                pop()
                pushStyle(SpanStyle(localeList = LocaleList("fr")))
                append("Bonjour!\n")
                pop()
                pushStyle(SpanStyle(localeList = LocaleList("tr-TR")))
                append("Merhaba!\n")
                pop()
                pushStyle(SpanStyle(localeList = LocaleList("ja-JP")))
                append("こんにちは!\n")
                pop()
                pushStyle(SpanStyle(localeList = LocaleList("zh")))
                append("你好!")
                pop()
            },
            style = TextStyle(fontSize = fontSize8)
        )

        TagLine("VerbatimTtsAnnotation ")
        Text(
            text = buildAnnotatedString {
                append("This word is read verbatim: ")
                pushTtsAnnotation(VerbatimTtsAnnotation(verbatim = "hello"))
                append("hello\n")
                pop()
                append("This word is read normally: hello")
            },
            style = TextStyle(fontSize = fontSize8)
        )
    }
}