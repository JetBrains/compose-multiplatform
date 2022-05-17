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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SystemFontFamilyDemo() {
    LazyColumn {
        item {
            TagLine(tag = "FontFamily.Default")
            SystemFontFamilyDemoItem(FontFamily.Default)
        }
        item {
            TagLine(tag = "FontFamily.SansSerif")
            SystemFontFamilyDemoItem(FontFamily.SansSerif)
        }
        item {
            TagLine(tag = "FontFamily.Serif")
            SystemFontFamilyDemoItem(FontFamily.Serif)
        }
        item {
            TagLine(tag = "FontFamily.Monospace")
            SystemFontFamilyDemoItem(FontFamily.Monospace)
        }
        item {
            TagLine(tag = "FontFamily.Cursive")
            SystemFontFamilyDemoItem(FontFamily.Cursive)
        }
    }
}

@Preview
@Composable
fun SystemFontFamilyDemoItem(fontFamily: FontFamily) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Upright", fontFamily = fontFamily)
            for (weight in 100..1000 step 100) {
                Text(
                    "$weight",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight(weight),
                    fontStyle = FontStyle.Normal
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Italic", fontFamily = fontFamily, fontStyle = FontStyle.Italic)
            for (weight in 100..1000 step 100) {
                Text(
                    "$weight",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight(weight),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
