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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight

@Sampled
@Composable
fun FontFamilySansSerifSample() {
    Text(
        text = "Demo Text sans-serif",
        fontFamily = FontFamily.SansSerif
    )
}

@Sampled
@Composable
fun FontFamilySerifSample() {
    Text(
        text = "Demo Text serif",
        fontFamily = FontFamily.Serif
    )
}

@Sampled
@Composable
fun FontFamilyMonospaceSample() {
    Text(
        text = "Demo Text monospace",
        fontFamily = FontFamily.Monospace
    )
}

@Sampled
@Composable
fun FontFamilyCursiveSample() {
    Text(
        text = "Demo Text cursive",
        fontFamily = FontFamily.Cursive
    )
}

@Sampled
@Composable
fun CustomFontFamilySample() {
    val fontFamily = FontFamily(
        Font(
            resId = R.font.my_font_400_regular,
            weight = FontWeight.W400,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.my_font_400_italic,
            weight = FontWeight.W400,
            style = FontStyle.Italic
        )
    )
    Text(text = "Demo Text", fontFamily = fontFamily)
}

@Sampled
@Composable
fun FontFamilySynthesisSample() {
    // The font family contains a single font, with normal weight
    val fontFamily = FontFamily(
        Font(resId = R.font.myfont, weight = FontWeight.Normal)
    )
    // Configuring the Text composable to be bold
    // Using FontSynthesis.Weight to have the system render the font bold my making the glyphs
    // thicker
    Text(
        text = "Demo Text",
        style = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSynthesis = FontSynthesis.Weight
        )
    )
}
