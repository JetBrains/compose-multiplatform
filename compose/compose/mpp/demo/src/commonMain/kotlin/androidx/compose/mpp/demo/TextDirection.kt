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

package androidx.compose.mpp.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*

val LabelWidth = 150.dp
val LineHeight = 20.dp

@Composable
fun TextDirection() {
    MaterialTheme {
        val state = rememberScrollState()
        Column(Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(state)) {

            val textDirections = sequenceOf(
                TextDirection.Ltr,
                TextDirection.Rtl,
                TextDirection.Content,
                TextDirection.ContentOrLtr,
                TextDirection.ContentOrRtl
            )

            Text("Latin letters (Strong characters)")
            for (textDirection in textDirections) {
                testTextDirection("Hello World", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("Arabic letters (Strong characters)")
            for (textDirection in textDirections) {
                testTextDirection("مرحبا بالعالم", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("Arabic letters EMBEDDING")
            for (textDirection in textDirections) {
                testTextDirection("\u202Bمرحبا بالعالم\u202C Hello World", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("Arabic letters OVERRIDE")
            for (textDirection in textDirections) {
                testTextDirection("\u202Eمرحبا بالعالم\u202C Hello World", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("Arabic letters ISOLATE")
            for (textDirection in textDirections) {
                testTextDirection("\u2067مرحبا بالعالم\u2069 Hello World", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("Weak characters")
            for (textDirection in textDirections) {
                testTextDirection("12345", textDirection)
            }

            Spacer(Modifier.height(LineHeight))
            Text("LayoutDirection fallback")
            testLayoutDirectionFallback("12345", LayoutDirection.Ltr)
            testLayoutDirectionFallback("12345", LayoutDirection.Rtl)

            Spacer(Modifier.height(LineHeight))
            Text("Locale fallback")
            testContentDirectionLocaleFallback("12345", "en")
            testContentDirectionLocaleFallback("12345", "ar")
        }
    }
}

@Composable
fun testTextDirection(text: String, textDirection: TextDirection) {
    Row {
        Text(
            text = textDirection.toString(),
            modifier = Modifier
                .width(LabelWidth)
                .height(LineHeight)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(
                textDirection = textDirection
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(LineHeight)
                .border(1.dp, Color.Black)
        )
    }
}

@Composable
fun testLayoutDirectionFallback(text: String, layoutDirection: LayoutDirection) {
    Row {
        Text(
            text = "Layout: $layoutDirection",
            modifier = Modifier
                .width(LabelWidth)
                .height(LineHeight)
        )
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Text(
                text = text,
                style = MaterialTheme.typography.body1.copy(
                    textDirection = TextDirection.Content
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LineHeight)
                    .border(1.dp, Color.Black)
            )
        }
    }
}

@Composable
fun testContentDirectionLocaleFallback(text: String, locale: String) {
    Row {
        Text(
            text = "Locale: $locale",
            modifier = Modifier
                .width(LabelWidth)
                .height(LineHeight)
        )
        var size by remember { mutableStateOf(IntSize.Zero) }
        Box(Modifier
            .onSizeChanged { size = it }
            .fillMaxWidth()
            .height(LineHeight)
            .border(1.dp, Color.Black)) {
            val paragraph = Paragraph(
                paragraphIntrinsics = ParagraphIntrinsics(
                    text = text,
                    style = MaterialTheme.typography.body1.copy(
                        localeList = LocaleList(Locale(locale)),
                        textDirection = TextDirection.Content
                    ),
                    density = LocalDensity.current,
                    fontFamilyResolver = LocalFontFamilyResolver.current,
                ),
                constraints = Constraints.fixedWidth(size.width)
            )
            Canvas(
                Modifier.size(
                with(LocalDensity.current) { DpSize(size.width.toDp(), size.height.toDp()) }
            )) {
                paragraph.paint(canvas = drawContext.canvas, color = Color.Black)
            }
        }
    }
}
