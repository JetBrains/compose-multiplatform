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

package androidx.compose.ui.text.font

import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlatformTypefacesTest {
    private fun assertSuffix(suffix: String, forRange: IntRange) {
        for (weight in forRange) {
            val base = "BASENAME[$weight]"
            val name = getWeightSuffixForFallbackFamilyName(base, FontWeight(weight))
            assertThat(name).isEqualTo("$base$suffix")
        }
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight1To199_isThin() {
        assertSuffix("-thin", 1..199)
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight200To399_isLight() {
        assertSuffix("-light", 200..399)
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight400to499_isNormal() {
        assertSuffix("", 400..499)
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight500to599_isMedium() {
        assertSuffix("-medium", 500..599)
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight600to799_isBold() {
        assertSuffix("", 600..799)
    }

    @Test
    fun getWeightSuffixForFallbackFamilyName_weight800to1000_isBlack() {
        assertSuffix("-black", 800..1000)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun assertCreateNamed_returnsPlatformFallbacks_forAllRelevantWeights() {
        val subject = PlatformTypefaces()
        subject.assertCreateNamedIsNotDefault(FontFamily.SansSerif, 1..399)
        subject.assertCreateNamedIsNotDefault(FontFamily.SansSerif, 500..599)
        subject.assertCreateNamedIsNotDefault(FontFamily.SansSerif, 800..1000)
    }

    private fun PlatformTypefaces.assertCreateNamedIsNotDefault(
        fontFamily: GenericFontFamily,
        forRange: IntRange
    ) {
        val boldTypeface = android.graphics.Typeface.create(
            fontFamily.name, android.graphics.Typeface.BOLD
        )
        val normalTypeface = android.graphics.Typeface.create(
            fontFamily.name, android.graphics.Typeface.NORMAL
        )

        for (weight in forRange) {
            val actualTypefaceUpright = createNamed(
                name = fontFamily,
                fontWeight = FontWeight(weight),
                fontStyle = FontStyle.Normal
            )
            assertThat(actualTypefaceUpright).isNotEqualTo(boldTypeface)
            assertThat(actualTypefaceUpright).isNotEqualTo(normalTypeface)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun createNamed_returnsPlatformDefault_aboveApi28() {
        val subject = PlatformTypefaces()
        subject.assertCreateNamedIsDefault(FontFamily.SansSerif, 1..399)
        subject.assertCreateNamedIsDefault(FontFamily.SansSerif, 500..599)
        subject.assertCreateNamedIsDefault(FontFamily.SansSerif, 800..1000)
    }

    @RequiresApi(28)
    private fun PlatformTypefaces.assertCreateNamedIsDefault(
        fontFamily: GenericFontFamily,
        forRange: IntRange
    ) {
        for (weight in forRange) {
            val normalTypeface = android.graphics.Typeface.create(
                android.graphics.Typeface.create(fontFamily.name, android.graphics.Typeface.NORMAL),
                weight,
                false
            )
            val actualTypefaceUpright = createNamed(
                name = fontFamily,
                fontWeight = FontWeight(weight),
                fontStyle = FontStyle.Normal
            )
            assertThat(actualTypefaceUpright).isEqualTo(normalTypeface)
        }
    }

    @Test
    fun assertOptionalOnDeviceFontFamilyByName_returnsPlatformFallback_SansSerif_allWeights() {
        val subject = PlatformTypefaces()
        for (weight in 1..1000) {
            val genericFontFamilyTypeface = subject.createNamed(
                FontFamily.SansSerif,
                FontWeight(weight),
                FontStyle.Normal
            )
            val optionalOnDeviceFontFamilyTypeface = subject.optionalOnDeviceFontFamilyByName(
                FontFamily.SansSerif.name,
                FontWeight(weight),
                FontStyle.Normal
            )
            assertThat(genericFontFamilyTypeface).isEqualTo(optionalOnDeviceFontFamilyTypeface)
        }
    }

    @Test
    fun assertOptionalOnDeviceFontFamilyByName_returnsPlatformFallback_Serif_allWeights() {
        val subject = PlatformTypefaces()
        for (weight in 1..1000) {
            val genericFontFamilyTypeface = subject.createNamed(
                FontFamily.Serif,
                FontWeight(weight),
                FontStyle.Normal
            )
            val optionalOnDeviceFontFamilyTypeface = subject.optionalOnDeviceFontFamilyByName(
                FontFamily.Serif.name,
                FontWeight(weight),
                FontStyle.Normal
            )
            assertThat(genericFontFamilyTypeface).isEqualTo(optionalOnDeviceFontFamilyTypeface)
        }
    }

    @Test
    fun assertOptionalOnDeviceFontFamilyByName_returnsPlatformFallback_Monospace_allWeights() {
        val subject = PlatformTypefaces()
        for (weight in 1..1000) {
            val genericFontFamilyTypeface = subject.createNamed(
                FontFamily.Monospace,
                FontWeight(weight),
                FontStyle.Normal
            )
            val optionalOnDeviceFontFamilyTypeface = subject.optionalOnDeviceFontFamilyByName(
                FontFamily.Monospace.name,
                FontWeight(weight),
                FontStyle.Normal
            )
            assertThat(genericFontFamilyTypeface).isEqualTo(optionalOnDeviceFontFamilyTypeface)
        }
    }

    @Test
    fun assertOptionalOnDeviceFontFamilyByName_returnsPlatformFallback_Cursive_allWeights() {
        val subject = PlatformTypefaces()
        for (weight in 1..1000) {
            val genericFontFamilyTypeface = subject.createNamed(
                FontFamily.Cursive,
                FontWeight(weight),
                FontStyle.Normal
            )
            val optionalOnDeviceFontFamilyTypeface = subject.optionalOnDeviceFontFamilyByName(
                FontFamily.Cursive.name,
                FontWeight(weight),
                FontStyle.Normal
            )
            assertThat(genericFontFamilyTypeface).isEqualTo(optionalOnDeviceFontFamilyTypeface)
        }
    }
}