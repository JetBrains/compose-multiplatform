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

package androidx.compose.ui.text.platform

import android.content.Context
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontMatcher
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.getAndroidTypefaceStyle
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidTypefaceTest {

    val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    @Suppress("DEPRECATION")
    private fun androidTypefaceFromFontFamily(
        context: Context,
        fontFamily: FontFamily
    ): AndroidTypeface {
        return Typeface(context, fontFamily) as AndroidTypeface
    }

    @Test
    fun createDefaultTypeface() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Normal, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotNull()
        assertThat(nativeTypeface.isBold).isFalse()
        assertThat(nativeTypeface.isItalic).isFalse()
        assertThat(nativeTypeface.bitmap()).isEqualToBitmap(
            android.graphics.Typeface.DEFAULT.bitmap()
        )
    }

    @Test
    fun fontWeightItalicCreatesItalicFont() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Italic, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotNull()
        assertThat(nativeTypeface.isBold).isFalse()
        assertThat(nativeTypeface.isItalic).isTrue()
        assertThat(nativeTypeface.bitmap()).isEqualToBitmap(
            android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.ITALIC)
                .bitmap()
        )
    }

    @Test
    fun fontWeightBoldCreatesBoldFont() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Bold, FontStyle.Normal, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotNull()
        assertThat(nativeTypeface.isBold).isTrue()
        assertThat(nativeTypeface.isItalic).isFalse()
        assertThat(nativeTypeface.bitmap()).isEqualToBitmap(
            android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD).bitmap()
        )
    }

    @Test
    fun fontWeightBoldFontStyleItalicCreatesBoldItalicFont() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Bold, FontStyle.Italic, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotNull()
        assertThat(nativeTypeface.isBold).isTrue()
        assertThat(nativeTypeface.isItalic).isTrue()
        assertThat(nativeTypeface.bitmap()).isEqualToBitmap(
            android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD_ITALIC)
                .bitmap()
        )
    }

    @Test
    fun serifAndSansSerifPaintsDifferent() {
        val typefaceSans = androidTypefaceFromFontFamily(context, FontFamily.SansSerif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.None)
        val typefaceSerif = androidTypefaceFromFontFamily(context, FontFamily.Serif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.None)

        assertThat(typefaceSans).isNotNull()
        assertThat(typefaceSans).isNotNull()
        assertThat(typefaceSans.bitmap()).isNotEqualToBitmap(typefaceSerif.bitmap())
    }

    // Following test is exactly the same as the one in TypefaceAdapterTest. Migrate once
    // TypefaceAdapterTest is migrated to AndroidTypefaceTest
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
                val typefaceStyle = getAndroidTypefaceStyle(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                if (fontStyle == FontStyle.Normal) {
                    assertThat(typefaceStyle).isEqualTo(android.graphics.Typeface.NORMAL)
                } else {
                    assertThat(typefaceStyle).isEqualTo(android.graphics.Typeface.ITALIC)
                }
            }
        }
    }

    // Following test is exactly the same as the one in TypefaceAdapterTest. Migrate once
    // TypefaceAdapterTest is migrated to AndroidTypefaceTest
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
                val typefaceStyle = getAndroidTypefaceStyle(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                if (fontStyle == FontStyle.Normal) {
                    assertThat(typefaceStyle).isEqualTo(android.graphics.Typeface.BOLD)
                } else {
                    assertThat(typefaceStyle).isEqualTo(android.graphics.Typeface.BOLD_ITALIC)
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
                val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
                    .getNativeTypeface(fontWeight, fontStyle, FontSynthesis.None)

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
                val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
                    .getNativeTypeface(fontWeight, fontStyle, FontSynthesis.None)

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
                val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
                    .getNativeTypeface(fontWeight, fontStyle, FontSynthesis.None)

                assertThat(typeface).isNotNull()
                assertThat(typeface.weight).isEqualTo(fontWeight.weight)
                assertThat(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            }
        }
    }

    @Test
    fun customSingleFont() {
        val defaultTypeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.None)

        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.None)

        assertThat(typeface).isNotNull()
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
        assertThat(typeface.isItalic).isFalse()
        assertThat(typeface.isBold).isFalse()
    }

    @Test
    fun customSingleFontBoldItalic() {
        val defaultTypeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.None)

        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.All)

        assertThat(typeface).isNotNull()
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
        assertThat(typeface.isItalic).isTrue()
        assertThat(typeface.isBold).isTrue()
    }

    @Test
    @MediumTest
    fun customSinglefontFamilyExactMatch() {
        val fontFamily = FontFamily(
            FontTestData.FONT_100_REGULAR,
            FontTestData.FONT_100_ITALIC,
            FontTestData.FONT_200_REGULAR,
            FontTestData.FONT_200_ITALIC,
            FontTestData.FONT_300_REGULAR,
            FontTestData.FONT_300_ITALIC,
            FontTestData.FONT_400_REGULAR,
            FontTestData.FONT_400_ITALIC,
            FontTestData.FONT_500_REGULAR,
            FontTestData.FONT_500_ITALIC,
            FontTestData.FONT_600_REGULAR,
            FontTestData.FONT_600_ITALIC,
            FontTestData.FONT_700_REGULAR,
            FontTestData.FONT_700_ITALIC,
            FontTestData.FONT_800_REGULAR,
            FontTestData.FONT_800_ITALIC,
            FontTestData.FONT_900_REGULAR,
            FontTestData.FONT_900_ITALIC
        )

        for (fontWeight in FontWeight.values) {
            for (fontStyle in FontStyle.values()) {
                val typeface = androidTypefaceFromFontFamily(context, fontFamily)
                    .getNativeTypeface(fontWeight, fontStyle, FontSynthesis.None)

                assertThat(typeface).isNotNull()
                assertThat(typeface).isTypefaceOf(fontWeight = fontWeight, fontStyle = fontStyle)
            }
        }
    }

    @Test
    @MediumTest
    @Suppress("DEPRECATION")
    fun androidFontListTypefaceForCustomFont() {
        // customSinglefontFamilyExactMatch tests all the possible outcomes that FontMatcher
        // might return. Therefore for the best effort matching we just make sure that FontMatcher
        // is called.
        val fontWeight = FontWeight.W300
        val fontStyle = FontStyle.Italic
        val fontFamily = FontFamily(
            FontTestData.FONT_200_ITALIC,
            FontTestData.FONT_200_ITALIC_FALLBACK,
            FontTestData.FONT_200_REGULAR
        ) as FontListFontFamily

        val typeface = AndroidFontListTypeface(
            context = context,
            fontFamily = fontFamily,
            necessaryStyles = null,
            fontMatcher = FontMatcher()
        ).getNativeTypeface(fontWeight, fontStyle, FontSynthesis.None)

        /* Match will find 200 weight font, synthesis disabled */
        assertThat(typeface).isTypefaceOf(fontWeight = FontWeight.W200, fontStyle = fontStyle)
    }

    @Test(expected = IllegalStateException::class)
    @MediumTest
    fun noEagerFonts_throws() {
        val asyncFont = object : Font {
            override val weight: FontWeight = FontWeight.W100
            override val style: FontStyle = FontStyle.Italic
            @ExperimentalTextApi
            override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Async
        }

        val fontFamily = FontFamily(
            asyncFont
        ) as FontListFontFamily

        AndroidFontListTypeface(
            context = context,
            fontFamily = fontFamily,
            necessaryStyles = null
        )
    }

    @Test
    @MediumTest
    fun eagerAndAsyncFont_alwaysChoosesEagerFont_evenIfAsyncIsBetterMatch() {
        val asyncFont = object : Font {
            override val weight: FontWeight = FontWeight.W800
            override val style: FontStyle = FontStyle.Italic
            @ExperimentalTextApi
            override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Async
        }

        val fontFamily = FontFamily(
            asyncFont,
            FontTestData.FONT_100_REGULAR
        ) as FontListFontFamily

        // (100, Normal, Blocking) matches for (800, Italic, Blocking)
        val typeface = AndroidFontListTypeface(
            context = context,
            fontFamily = fontFamily,
            necessaryStyles = null
        ).getNativeTypeface(FontWeight.W800, FontStyle.Italic, FontSynthesis.None)
        assertThat(typeface).isTypefaceOf(FontWeight.W100, FontStyle.Normal)
    }

    @Test
    fun resultsAreCached_defaultTypeface() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Normal, FontSynthesis.None
        )

        // getting typeface with same parameters should hit the cache
        // therefore return the same typeface
        val otherNativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Normal, FontSynthesis.None
        )

        assertThat(nativeTypeface).isSameInstanceAs(otherNativeTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontWeight() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)
        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Normal, FontSynthesis.None
        )

        // getting typeface with different parameters should not hit the cache
        // therefore return some other typeface
        val otherNativeTypeface = typeface.getNativeTypeface(
            FontWeight.Bold, FontStyle.Normal, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotSameInstanceAs(otherNativeTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontStyle() {
        val typeface = androidTypefaceFromFontFamily(context, FontFamily.Default)

        val nativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Normal, FontSynthesis.None
        )
        val otherNativeTypeface = typeface.getNativeTypeface(
            FontWeight.Normal, FontStyle.Italic, FontSynthesis.None
        )

        assertThat(nativeTypeface).isNotSameInstanceAs(otherNativeTypeface)
    }

    @Test
    fun resultsAreCached_withCustomTypeface() {
        val fontFamily = FontFamily.SansSerif
        val fontWeight = FontWeight.Normal
        val fontStyle = FontStyle.Italic

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
        val nativeTypeface = typeface.getNativeTypeface(
            fontWeight, fontStyle, FontSynthesis.None
        )
        val otherNativeTypeface = typeface.getNativeTypeface(
            fontWeight, fontStyle, FontSynthesis.None
        )

        assertThat(nativeTypeface).isSameInstanceAs(otherNativeTypeface)
    }

    @Test
    fun cacheCanHoldTwoResults() {
        val serifTypeface = androidTypefaceFromFontFamily(context, FontFamily.Serif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.All)
        val otherSerifTypeface = androidTypefaceFromFontFamily(context, FontFamily.Serif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.All)
        val sansTypeface = androidTypefaceFromFontFamily(context, FontFamily.SansSerif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.All)
        val otherSansTypeface = androidTypefaceFromFontFamily(context, FontFamily.SansSerif)
            .getNativeTypeface(FontWeight.Normal, FontStyle.Normal, FontSynthesis.All)

        assertThat(serifTypeface).isSameInstanceAs(otherSerifTypeface)
        assertThat(sansTypeface).isSameInstanceAs(otherSansTypeface)
        assertThat(sansTypeface).isNotSameInstanceAs(serifTypeface)
    }

    @Test(expected = IllegalStateException::class)
    @OptIn(ExperimentalTextApi::class)
    fun throwsExceptionIfFontIsNotIncludedInTheApp() {
        val fontFamily = FontFamily(Font(-1))
        androidTypefaceFromFontFamily(context, fontFamily)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsExceptionIfFontIsNotReadable() {
        val fontFamily = FontFamily(FontTestData.FONT_INVALID)
        androidTypefaceFromFontFamily(context, fontFamily)
    }

    @Test
    fun fontSynthesisDefault_synthesizeTheFontToItalicBold() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.All)

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is fake bold and fake Italic
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisStyle_synthesizeTheFontToItalic() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.Style)

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake Italic
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun fontSynthesisWeight_synthesizeTheFontToBold() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.Weight)

        // since 100 regular is not bold and not italic, passing FontWeight.bold and
        // FontStyle.Italic should create a Typeface that is only fake bold
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisStyle_forMatchingItalicDoesNotSynthesize() {
        val fontFamily = FontTestData.FONT_100_ITALIC.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.W700, FontStyle.Italic, FontSynthesis.Style)

        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisAll_doesNotSynthesizeIfFontIsTheSame_beforeApi28() {
        val fontFamily = FontTestData.FONT_700_ITALIC.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.W700, FontStyle.Italic, FontSynthesis.All)
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
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.None)

        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun fontSynthesisWeight_doesNotSynthesizeIfRequestedWeightIsLessThan600() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        // Less than 600 is not synthesized
        val typeface500 = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.W500, FontStyle.Normal, FontSynthesis.Weight)

        // 600 or more is synthesized
        val typeface600 = androidTypefaceFromFontFamily(context, fontFamily)
            .getNativeTypeface(FontWeight.W600, FontStyle.Normal, FontSynthesis.Weight)

        assertThat(typeface500.isBold).isFalse()
        assertThat(typeface600.isBold).isTrue()
    }

    @Test
    fun typefaceWrapper_returnsExactSameInstance() {
        val typeface = Typeface(android.graphics.Typeface.MONOSPACE) as AndroidTypefaceWrapper
        assertThat(
            typeface.getNativeTypeface(
                FontWeight.Light,
                FontStyle.Italic,
                FontSynthesis.None
            )
        ).isEqualTo(android.graphics.Typeface.MONOSPACE)
    }
}

internal fun anyFontStyle(): FontStyle {
    return Mockito.argThat { arg: Any ->
        arg is Int || arg is FontStyle
    } as FontStyle? ?: FontStyle.Normal
}