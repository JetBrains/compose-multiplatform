/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.FontTestData.Companion.FONT_100_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_100_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_200_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_200_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_300_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_300_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_400_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_400_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_500_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_500_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_600_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_600_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_700_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_700_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_800_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_800_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_900_ITALIC
import androidx.compose.ui.text.FontTestData.Companion.FONT_900_REGULAR
import androidx.compose.ui.text.FontTestData.Companion.FONT_INVALID
import androidx.compose.ui.text.TestFontResourceLoader
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontMatcher
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TypefaceAdapterTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)

    private fun TypefaceAdapter() = TypefaceAdapter(resourceLoader = resourceLoader)

    @Test
    fun createDefaultTypeface() {
        val typeface = TypefaceAdapter().create()

        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontWeightItalicCreatesItalicFont() {
        val typeface = TypefaceAdapter().create(fontStyle = FontStyle.Italic)

        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun fontWeightBoldCreatesBoldFont() {
        val typeface = TypefaceAdapter().create(fontWeight = FontWeight.Bold)

        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontWeightBoldFontStyleItalicCreatesBoldItalicFont() {
        val typeface = TypefaceAdapter().create(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )

        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun serifAndSansSerifPaintsDifferent() {
        val typefaceSans = TypefaceAdapter().create(FontFamily.SansSerif)
        val typefaceSerif = TypefaceAdapter().create(FontFamily.Serif)

        assertThat(typefaceSans).isNotNull()
        assertThat(typefaceSans).isNotNull()
        assertThat(typefaceSans.bitmap()).isNotEqualToBitmap(typefaceSerif.bitmap())
    }

    @Test
    fun getTypefaceStyleSnapToNormalFor100to500() {
        val fontWeights = arrayOf(
            FontWeight.W100,
            FontWeight.W200,
            FontWeight.W300,
            FontWeight.W400,
            FontWeight.W500
        )

        for (fontWeight in fontWeights) {
            for (fontStyle in FontStyle.values()) {
                val typefaceStyle = TypefaceAdapter.getTypefaceStyle(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                if (fontStyle == FontStyle.Normal) {
                    assertThat(typefaceStyle).isEqualTo(Typeface.NORMAL)
                } else {
                    assertThat(typefaceStyle).isEqualTo(Typeface.ITALIC)
                }
            }
        }
    }

    @Test
    fun getTypefaceStyleSnapToBoldFor600to900() {
        val fontWeights = arrayOf(
            FontWeight.W600,
            FontWeight.W700,
            FontWeight.W800,
            FontWeight.W900
        )

        for (fontWeight in fontWeights) {
            for (fontStyle in FontStyle.values()) {
                val typefaceStyle = TypefaceAdapter.getTypefaceStyle(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                if (fontStyle == FontStyle.Normal) {
                    assertThat(typefaceStyle).isEqualTo(Typeface.BOLD)
                } else {
                    assertThat(typefaceStyle).isEqualTo(Typeface.BOLD_ITALIC)
                }
            }
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun fontWeights100To500SnapToNormalBeforeApi28() {
        val fontWeights = arrayOf(
            FontWeight.W100,
            FontWeight.W200,
            FontWeight.W300,
            FontWeight.W400,
            FontWeight.W500
        )

        for (fontWeight in fontWeights) {
            for (fontStyle in FontStyle.values()) {
                val typeface = TypefaceAdapter().create(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typeface).isNotNull()
                assertThat(typeface.isBold).isFalse()
                assertThat(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            }
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun fontWeights600To900SnapToBoldBeforeApi28() {
        val fontWeights = arrayOf(
            FontWeight.W600,
            FontWeight.W700,
            FontWeight.W800,
            FontWeight.W900
        )

        for (fontWeight in fontWeights) {
            for (fontStyle in FontStyle.values()) {
                val typeface = TypefaceAdapter().create(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typeface).isNotNull()
                assertThat(typeface.isBold).isTrue()
                assertThat(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun typefaceCreatedWithCorrectFontWeightAndFontStyle() {
        for (fontWeight in FontWeight.values) {
            for (fontStyle in FontStyle.values()) {
                val typeface = TypefaceAdapter().create(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typeface).isNotNull()
                assertThat(typeface.weight).isEqualTo(fontWeight.weight)
                assertThat(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            }
        }
    }

    @Test
    @MediumTest
    fun customSingleFont() {
        val defaultTypeface = TypefaceAdapter().create()

        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(fontFamily = fontFamily)

        assertThat(typeface).isNotNull()
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
        assertThat(typeface.isItalic).isFalse()
        assertThat(typeface.isBold).isFalse()
    }

    @Test
    @MediumTest
    fun customSingleFontBoldItalic() {
        val defaultTypeface = TypefaceAdapter().create()

        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )

        assertThat(typeface).isNotNull()
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
        assertThat(typeface.isItalic).isTrue()
        assertThat(typeface.isBold).isTrue()
    }

    @Test
    @MediumTest
    fun customSingleFontFamilyExactMatch() {
        val fontFamily = FontFamily(
            FONT_100_REGULAR,
            FONT_100_ITALIC,
            FONT_200_REGULAR,
            FONT_200_ITALIC,
            FONT_300_REGULAR,
            FONT_300_ITALIC,
            FONT_400_REGULAR,
            FONT_400_ITALIC,
            FONT_500_REGULAR,
            FONT_500_ITALIC,
            FONT_600_REGULAR,
            FONT_600_ITALIC,
            FONT_700_REGULAR,
            FONT_700_ITALIC,
            FONT_800_REGULAR,
            FONT_800_ITALIC,
            FONT_900_REGULAR,
            FONT_900_ITALIC
        )

        for (fontWeight in FontWeight.values) {
            for (fontStyle in FontStyle.values()) {
                val typeface = TypefaceAdapter().create(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    fontFamily = fontFamily
                )

                assertThat(typeface).isNotNull()
                assertThat(typeface).isTypefaceOf(fontWeight = fontWeight, fontStyle = fontStyle)
            }
        }
    }

    @Test
    @MediumTest
    fun fontMatcherCalledForCustomFont() {
        // customSinglefontFamilyExactMatch tests all the possible outcomes that FontMatcher
        // might return. Therefore for the best effort matching we just make sure that FontMatcher
        // is called.
        val fontWeight = FontWeight.W300
        val fontStyle = FontStyle.Italic
        val fontFamily = FontFamily(FONT_200_ITALIC) as FontListFontFamily

        val fontMatcher = mock<FontMatcher>()
        whenever(fontMatcher.matchFont(any(), any(), anyFontStyle()))
            .thenReturn(FONT_200_ITALIC)

        TypefaceAdapter(fontMatcher = fontMatcher, resourceLoader = resourceLoader).create(
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily
        )

        verify(fontMatcher, times(1)).matchFont(
            eq(fontFamily),
            eq(fontWeight),
            eqFontStyle(fontStyle)
        )
    }

    @Test
    fun resultsAreCached_defaultTypeface() {
        val typefaceAdapter = TypefaceAdapter()
        val typeface = typefaceAdapter.create()

        // getting typeface with same parameters should hit the cache
        // therefore return the same typeface
        val otherTypeface = typefaceAdapter.create()

        assertThat(typeface).isSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontWeight() {
        val typefaceAdapter = TypefaceAdapter()
        val typeface = typefaceAdapter.create(fontWeight = FontWeight.Normal)

        // getting typeface with different parameters should not hit the cache
        // therefore return some other typeface
        val otherTypeface = typefaceAdapter.create(fontWeight = FontWeight.Bold)

        assertThat(typeface).isNotSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontStyle() {
        val typefaceAdapter = TypefaceAdapter()

        val typeface = typefaceAdapter.create(fontStyle = FontStyle.Normal)
        val otherTypeface = typefaceAdapter.create(fontStyle = FontStyle.Italic)

        assertThat(typeface).isNotSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsAreCached_withCustomTypeface() {
        val fontFamily = FontFamily.SansSerif
        val fontWeight = FontWeight.Normal
        val fontStyle = FontStyle.Italic

        val typefaceAdapter = TypefaceAdapter()
        val typeface = typefaceAdapter.create(fontFamily, fontWeight, fontStyle)
        val otherTypeface = typefaceAdapter.create(fontFamily, fontWeight, fontStyle)

        assertThat(typeface).isSameInstanceAs(otherTypeface)
    }

    @Test
    fun cacheCanHoldTwoResults() {
        val typefaceAdapter = TypefaceAdapter()

        val serifTypeface = typefaceAdapter.create(FontFamily.Serif)
        val otherSerifTypeface = typefaceAdapter.create(FontFamily.Serif)
        val sansTypeface = typefaceAdapter.create(FontFamily.SansSerif)
        val otherSansTypeface = typefaceAdapter.create(FontFamily.SansSerif)

        assertThat(serifTypeface).isSameInstanceAs(otherSerifTypeface)
        assertThat(sansTypeface).isSameInstanceAs(otherSansTypeface)
        assertThat(sansTypeface).isNotSameInstanceAs(serifTypeface)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsExceptionIfFontIsNotIncludedInTheApp() {
        val fontFamily = FontFamily(Font(resId = -1))
        TypefaceAdapter().create(fontFamily)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsExceptionIfFontIsNotReadable() {
        val fontFamily = FontFamily(FONT_INVALID)
        TypefaceAdapter().create(fontFamily)
    }

    @Test
    fun fontSynthesisDefault_synthesizeTheFontToItalicBold() {
        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.All
        )

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is fake bold and fake Italic
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisStyle_synthesizeTheFontToItalic() {
        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Style
        )

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake Italic
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisWeight_synthesizeTheFontToBold() {
        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Weight
        )

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake bold
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisStyle_forMatchingItalicDoesNotSynthesize() {
        val fontFamily = FONT_100_ITALIC.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W700,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Style
        )

        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisAll_doesNotSynthesizeIfFontIsTheSame_beforeApi28() {
        val fontFamily = FONT_700_ITALIC.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W700,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.All
        )
        assertThat(typeface.isItalic).isFalse()

        if (Build.VERSION.SDK_INT < 23) {
            assertThat(typeface.isBold).isFalse()
        } else if (Build.VERSION.SDK_INT < 28) {
            assertThat(typeface.isBold).isTrue()
        } else {
            assertThat(typeface.isBold).isTrue()
            assertThat(typeface.weight).isEqualTo(700)
        }
    }

    @Test
    fun fontSynthesisNone_doesNotSynthesize() {
        val fontFamily = FONT_100_REGULAR.toFontFamily()

        val typeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.None
        )

        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisWeight_doesNotSynthesizeIfRequestedWeightIsLessThan600() {
        val fontFamily = FONT_100_REGULAR.toFontFamily()

        // Less than 600 is not synthesized
        val typeface500 = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W500,
            fontSynthesis = FontSynthesis.Weight
        )
        // 600 or more is synthesized
        val typeface600 = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W600,
            fontSynthesis = FontSynthesis.Weight
        )

        assertThat(typeface500.isBold).isFalse()
        assertThat(typeface600.isBold).isTrue()
    }
}