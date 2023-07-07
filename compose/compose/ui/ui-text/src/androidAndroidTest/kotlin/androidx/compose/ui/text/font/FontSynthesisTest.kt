/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.UncachedFontFamilyResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalTextApi::class)
class FontSynthesisTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resolver = UncachedFontFamilyResolver(context)

    private fun loadFont(font: Font): Pair<Font, Typeface> {
        return font to resolver.resolve(
            font.toFontFamily(),
            font.weight,
            font.style,
            fontSynthesis = FontSynthesis.None
        ).value as Typeface
    }

    @Test
    fun fontSynthesisDefault_synthesizeTheFontToItalicBold() {
        val (font, typeface) = loadFont(FontTestData.FONT_100_REGULAR)

        val synthesized = FontSynthesis.All.synthesizeTypeface(
            typeface,
            font,
            FontWeight.Bold,
            FontStyle.Italic
        ) as Typeface

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is fake bold and fake Italic
        Truth.assertThat(synthesized.isBold).isTrue()
        Truth.assertThat(synthesized.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisStyle_synthesizeTheFontToItalic() {
        val (font, typeface) = loadFont(FontTestData.FONT_100_REGULAR)

        val synthesized = FontSynthesis.Style.synthesizeTypeface(
            typeface,
            font,
            FontWeight.Bold,
            FontStyle.Italic
        ) as Typeface

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake Italic
        Truth.assertThat(synthesized.isBold).isFalse()
        Truth.assertThat(synthesized.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisWeight_synthesizeTheFontToBold() {
        val (font, typeface) = loadFont(FontTestData.FONT_100_REGULAR)

        val synthesized = FontSynthesis.Weight.synthesizeTypeface(
            typeface,
            font,
            FontWeight.Bold,
            FontStyle.Italic
        ) as Typeface

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake bold
        Truth.assertThat(synthesized.isBold).isTrue()
        Truth.assertThat(synthesized.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisStyle_forMatchingItalicDoesNotSynthesize() {
        val (font, typeface) = loadFont(FontTestData.FONT_100_ITALIC)

        val synthesized = FontSynthesis.Style.synthesizeTypeface(
            typeface,
            font,
            FontWeight.W700,
            FontStyle.Italic
        ) as Typeface

        Truth.assertThat(synthesized.isBold).isFalse()
        Truth.assertThat(synthesized.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisAll_doesNotSynthesizeIfFontIsTheSame_beforeApi28() {
        val (font, loaded) = loadFont(FontTestData.FONT_700_ITALIC)

        val typeface = FontSynthesis.All.synthesizeTypeface(
            loaded,
            font,
            FontWeight.W700,
            FontStyle.Italic
        ) as Typeface
        Truth.assertThat(typeface.isItalic).isFalse()

        if (Build.VERSION.SDK_INT < 23) {
            Truth.assertThat(typeface.isBold).isFalse()
        } else if (Build.VERSION.SDK_INT < 28) {
            Truth.assertThat(typeface.isBold).isTrue()
        } else {
            Truth.assertThat(typeface.isBold).isTrue()
            Truth.assertThat(typeface.weight).isEqualTo(700)
        }
    }

    @Test
    fun fontSynthesisNone_doesNotSynthesize() {
        val (font, loaded) = loadFont(FontTestData.FONT_100_REGULAR)

        val typeface = FontSynthesis.None.synthesizeTypeface(
            loaded,
            font,
            FontWeight.Bold,
            FontStyle.Italic
        ) as Typeface

        Truth.assertThat(typeface.isBold).isFalse()
        Truth.assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisWeight_doesNotSynthesizeIfRequestedWeightIsLessThan600() {
        val (font, loaded) = loadFont(FontTestData.FONT_100_REGULAR)

        // Less than 600 is not synthesized
        val typeface500 = FontSynthesis.Weight.synthesizeTypeface(
            loaded,
            font,
            FontWeight.W500,
            FontStyle.Normal
        ) as Typeface

        // 600 or more is synthesized
        val typeface600 = FontSynthesis.Weight.synthesizeTypeface(
            loaded,
            font,
            FontWeight.W600,
            FontStyle.Normal
        ) as Typeface

        Truth.assertThat(typeface500.isBold).isFalse()
        Truth.assertThat(typeface600.isBold).isTrue()
    }
}