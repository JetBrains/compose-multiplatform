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

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.UncachedFontFamilyResolver
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.platform.bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
@ExperimentalCoroutinesApi
class FontFamilyResolverImplTest {
    private lateinit var scope: TestScope
    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var dispatcher: TestDispatcher
    private lateinit var asyncTypefaceCache: AsyncTypefaceCache
    private lateinit var typefaceCache: TypefaceRequestCache
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val fontLoader = AndroidFontLoader(context)
    // This is the default value that Android uses
    private val accessibilityFontWeightAdjustment = 300
    private lateinit var subject: FontFamilyResolverImpl

    @Before
    fun setup() {
        asyncTypefaceCache = AsyncTypefaceCache()
        typefaceCache = TypefaceRequestCache()
        dispatcher = UnconfinedTestDispatcher()
        scope = TestScope(dispatcher)
        initializeSubject()
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    private fun initializeSubject(
        platformResolveInterceptor: PlatformResolveInterceptor =
            AndroidFontResolveInterceptor(context)
    ) {
        val injectedContext = scope.coroutineContext.minusKey(CoroutineExceptionHandler)

        subject = FontFamilyResolverImpl(
            fontLoader,
            platformResolveInterceptor = platformResolveInterceptor,
            typefaceRequestCache = typefaceCache,
            fontListFontFamilyTypefaceAdapter = FontListFontFamilyTypefaceAdapter(
                asyncTypefaceCache,
                injectedContext
            )
        )
    }

    private fun resolveAsTypeface(
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        fontSynthesis: FontSynthesis = FontSynthesis.All,
    ): Typeface {
        return subject.resolve(
            fontFamily,
            fontWeight,
            fontStyle,
            fontSynthesis
        ).value as Typeface
    }

    @Test
    fun createDefaultTypeface() {
        val typeface: Typeface = resolveAsTypeface()

        assertThat(typeface).hasWeightAndStyle(FontWeight.Normal, FontStyle.Normal)
    }

    @Test
    fun fontWeightItalicCreatesItalicFont() {
        val typeface = resolveAsTypeface(fontStyle = FontStyle.Italic)

        assertThat(typeface).hasWeightAndStyle(FontWeight.Normal, FontStyle.Italic)
    }

    @Test
    fun fontWeightBoldCreatesBoldFont() {
        val typeface = resolveAsTypeface(fontWeight = FontWeight.Bold)

        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Normal)
    }

    @Test
    fun fontWeightBoldFontStyleItalicCreatesBoldItalicFont() {
        val typeface = resolveAsTypeface(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )
        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Italic)
    }

    @Test
    fun serifAndSansSerifPaintsDifferent() {
        val typefaceSans = resolveAsTypeface(FontFamily.SansSerif)
        val typefaceSerif = resolveAsTypeface(FontFamily.Serif)

        assertThat(typefaceSans).hasWeightAndStyle(FontWeight.Normal, FontStyle.Normal)
        assertThat(typefaceSerif).hasWeightAndStyle(FontWeight.Normal, FontStyle.Normal)
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
                val typefaceStyle = resolveAsTypeface(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
                assertThat(typefaceStyle).hasWeightAndStyle(fontWeight, fontStyle)
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
                val typefaceStyle = resolveAsTypeface(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typefaceStyle).hasWeightAndStyle(fontWeight, fontStyle)
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
                val typeface = resolveAsTypeface(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typeface).hasWeightAndStyle(fontWeight, fontStyle)
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
                val typeface = resolveAsTypeface(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
                assertThat(typeface).hasWeightAndStyle(fontWeight, fontStyle)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun typefaceCreatedWithCorrectFontWeightAndFontStyle() {
        for (fontWeight in FontWeight.values) {
            for (fontStyle in FontStyle.values()) {
                val typeface = resolveAsTypeface(
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )

                assertThat(typeface).hasWeightAndStyle(fontWeight, fontStyle)
            }
        }
    }

    @Test
    @MediumTest
    fun customSingleFont() {
        val defaultTypeface = resolveAsTypeface()

        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(fontFamily = fontFamily)

        assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
    }

    @Test
    @MediumTest
    fun customSingleFontBoldItalic() {
        val defaultTypeface = resolveAsTypeface()

        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Italic)
        assertThat(typeface.bitmap()).isNotEqualToBitmap(defaultTypeface.bitmap())
    }

    @Test
    @MediumTest
    fun customSingleFontFamilyExactMatch() {
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
                val typeface = resolveAsTypeface(
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
    fun resultsAreCached_defaultTypeface() {
        val typeface = resolveAsTypeface()

        // getting typeface with same parameters should hit the cache
        // therefore return the same typeface
        val otherTypeface = resolveAsTypeface()

        assertThat(typeface).isSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontWeight() {
        val typeface = resolveAsTypeface(fontWeight = FontWeight.Normal)

        // getting typeface with different parameters should not hit the cache
        // therefore return some other typeface
        val otherTypeface = resolveAsTypeface(fontWeight = FontWeight.Bold)

        assertThat(typeface).isNotSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsNotSame_forDifferentFontStyle() {

        val typeface = resolveAsTypeface(fontStyle = FontStyle.Normal)
        val otherTypeface = resolveAsTypeface(fontStyle = FontStyle.Italic)

        assertThat(typeface).isNotSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsAreCached_withCustomTypeface() {
        val fontFamily = FontFamily.SansSerif
        val fontWeight = FontWeight.Normal
        val fontStyle = FontStyle.Italic

        val typeface = resolveAsTypeface(fontFamily, fontWeight, fontStyle)
        val otherTypeface = resolveAsTypeface(fontFamily, fontWeight, fontStyle)

        assertThat(typeface).isSameInstanceAs(otherTypeface)
    }

    @Test
    fun cacheCanHoldTwoResults() {
        val serifTypeface = resolveAsTypeface(FontFamily.Serif)
        val otherSerifTypeface = resolveAsTypeface(FontFamily.Serif)
        val sansTypeface = resolveAsTypeface(FontFamily.SansSerif)
        val otherSansTypeface = resolveAsTypeface(FontFamily.SansSerif)

        assertThat(serifTypeface).isSameInstanceAs(otherSerifTypeface)
        assertThat(sansTypeface).isSameInstanceAs(otherSansTypeface)
        assertThat(sansTypeface).isNotSameInstanceAs(serifTypeface)
    }

    @Test
    fun resultsAreCached_forLoadedTypeface() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        val typeface = resolveAsTypeface(fontFamily)
        val otherTypeface = resolveAsTypeface(fontFamily)

        assertThat(typeface).isSameInstanceAs(otherTypeface)
    }

    @Test
    fun resultsAreEvicted_whenCacheOverfills_cacheSize16() {
        val font = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE, FontWeight.W100)
        val font800 = BlockingFauxFont(
            typefaceLoader,
            Typeface.SANS_SERIF,
            FontWeight.W800
        )
        val fontFamily = FontFamily(
            font,
            font800
        )

        subject.resolve(fontFamily, FontWeight.W100)

        for (weight in 801..816) {
            // don't use test resolver for cache busting
            subject.resolve(fontFamily, FontWeight(weight))
        }
        assertThat(typefaceCache.get(
            TypefaceRequest(
                fontFamily,
                FontWeight.W100,
                FontStyle.Normal,
                FontSynthesis.All,
                fontLoader.cacheKey
            ))).isNull()
    }

    @Test
    fun resultsAreNotEvicted_whenCacheOverfills_ifUsedRecently_cacheSize16() {
        val font = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE, FontWeight.W100)
        val font800 = BlockingFauxFont(
            typefaceLoader,
            Typeface.SANS_SERIF,
            FontWeight.W800
        )
        val fontFamily = FontFamily(
            font,
            font800
        )

        subject.resolve(fontFamily, FontWeight.W100)
        for (weight in 801..816) {
            subject.resolve(fontFamily, FontWeight.W100)
            subject.resolve(fontFamily, FontWeight(weight))
        }
        assertThat(typefaceCache.get(
            TypefaceRequest(
                fontFamily,
                FontWeight.W100,
                FontStyle.Normal,
                FontSynthesis.All,
                fontLoader.cacheKey
            ))).isNotNull()
    }

    @Test
    fun changingResourceLoader_doesInvalidateCache() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        val typeface = resolveAsTypeface(fontFamily)
        /* definitely not same instance :) */
        val newFontLoader = object : PlatformFontLoader {
            override fun loadBlocking(font: Font): Any = Typeface.DEFAULT
            override suspend fun awaitLoad(font: Font): Any = Typeface.DEFAULT
            override val cacheKey: String = "Not the default resource loader"
        }
        val otherTypeface = UncachedFontFamilyResolver(
            newFontLoader,
            PlatformResolveInterceptor.Default
        )
            .resolve(fontFamily).value as Typeface

        assertThat(typeface).isNotSameInstanceAs(otherTypeface)
    }

    @Test
    fun changingResourceLoader_toAndroidResourceLoader_doesNotInvalidateCache() {
        var first = true
        val unstableLoader = object : AndroidFont.TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                return if (first) {
                    first = false
                    Typeface.DEFAULT
                } else {
                    Typeface.MONOSPACE
                }
            }

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                TODO("Not yet implemented")
            }
        }
        val fontFamily = FontFamily(
            object : AndroidFont(FontLoadingStrategy.Blocking, unstableLoader) {
                override val weight: FontWeight = FontWeight.Normal
                override val style: FontStyle = FontStyle.Normal
            }
        )
        val firstAndroidResourceLoader = AndroidFontLoader(context)
        val androidResolveInterceptor = AndroidFontResolveInterceptor(context)
        val typeface = FontFamilyResolverImpl(
            fontLoader,
            androidResolveInterceptor,
            typefaceCache,
            FontListFontFamilyTypefaceAdapter(asyncTypefaceCache)
        ).resolve(fontFamily).value as Typeface
        val secondAndroidResourceLoader = AndroidFontLoader(context)
        val otherTypeface = FontFamilyResolverImpl(
            fontLoader,
            androidResolveInterceptor,
            typefaceCache,
            FontListFontFamilyTypefaceAdapter(asyncTypefaceCache)
        ).resolve(fontFamily).value as Typeface

        assertThat(firstAndroidResourceLoader).isNotSameInstanceAs(secondAndroidResourceLoader)
        assertThat(typeface).isSameInstanceAs(otherTypeface)
        assertThat(typeface).isSameInstanceAs(Typeface.DEFAULT)
    }

    @Test(expected = IllegalStateException::class)
    @OptIn(ExperimentalTextApi::class)
    fun throwsExceptionIfFontIsNotIncludedInTheApp() {
        val fontFamily = FontFamily(Font(resId = -1))
        resolveAsTypeface(fontFamily)
    }

    @Test(expected = IllegalStateException::class)
    @OptIn(ExperimentalTextApi::class)
    fun throwsExceptionIfFontIsNotReadable() {
        val fontFamily = FontFamily(FontTestData.FONT_INVALID)
        resolveAsTypeface(fontFamily)
    }

    @Test
    fun fontSynthesisDefault_synthesizeTheFontToItalicBold() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.All
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Italic)
    }

    @Test
    fun fontSynthesisStyle_synthesizeTheFontToItalic() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Style
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Italic)
    }

    @Test
    fun fontSynthesisWeight_synthesizeTheFontToBold() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Weight
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Normal)
    }

    @Test
    fun fontSynthesisStyle_forMatchingItalicDoesNotSynthesize() {
        val fontFamily = FontTestData.FONT_100_ITALIC.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W700,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Style
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
    }

    @Test
    fun fontSynthesisAll_doesNotSynthesizeIfFontIsTheSame_beforeApi28() {
        val fontFamily = FontTestData.FONT_700_ITALIC.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W700,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.All
        )
        val expectedWeight = if (Build.VERSION.SDK_INT < 23) {
            FontWeight.Normal
        } else {
            FontWeight.W700
        }

        assertThat(typeface).hasWeightAndStyle(expectedWeight, FontStyle.Normal)
    }

    @Test
    fun fontSynthesisNone_doesNotSynthesize() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.None
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
    }

    @Test
    fun fontSynthesisWeight_doesNotSynthesizeIfRequestedWeightIsLessThan600() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        // Less than 600 is not synthesized
        val typeface500 = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W500,
            fontSynthesis = FontSynthesis.Weight
        )
        // 600 or more is synthesized
        val typeface600 = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W600,
            fontSynthesis = FontSynthesis.Weight
        )

        assertThat(typeface500).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
        assertThat(typeface600).hasWeightAndStyle(FontWeight.W600, FontStyle.Normal)
    }

    @Test
    fun androidFontResolveInterceptor_affectsTheFontWeight() {
        initializeSubject(AndroidFontResolveInterceptor(accessibilityFontWeightAdjustment))
        val fontFamily = FontFamily(
            FontTestData.FONT_400_REGULAR,
            FontTestData.FONT_500_REGULAR,
            FontTestData.FONT_600_REGULAR,
            FontTestData.FONT_700_REGULAR,
            FontTestData.FONT_800_REGULAR
        )
        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.W400
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.W700, FontStyle.Normal)
    }

    @Test
    fun androidFontResolveInterceptor_doesNotAffectTheFontStyle() {
        initializeSubject(AndroidFontResolveInterceptor(accessibilityFontWeightAdjustment))

        val typeface = resolveAsTypeface(
            fontWeight = FontWeight.W400,
            fontStyle = FontStyle.Italic
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.W700, FontStyle.Italic)
    }

    @Test
    fun platformResolveInterceptor_affectsTheResolvedFontStyle() {
        initializeSubject(
            platformResolveInterceptor = object : PlatformResolveInterceptor {
                override fun interceptFontStyle(fontStyle: FontStyle) = FontStyle.Italic
            }
        )

        val typeface = resolveAsTypeface(
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.Normal, FontStyle.Italic)
    }

    @Test
    fun platformResolveInterceptor_affectsTheResolvedFontSynthesis() {
        initializeSubject(
            platformResolveInterceptor = object : PlatformResolveInterceptor {
                override fun interceptFontSynthesis(fontSynthesis: FontSynthesis) =
                    FontSynthesis.All
            }
        )

        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()

        val typeface = resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.None
        )

        assertThat(typeface).hasWeightAndStyle(FontWeight.Bold, FontStyle.Italic)
    }

    @Test
    fun platformResolveInterceptor_affectsTheResolvedFontFamily() {
        initializeSubject(
            platformResolveInterceptor = object : PlatformResolveInterceptor {
                override fun interceptFontFamily(fontFamily: FontFamily?) =
                    FontTestData.FONT_100_REGULAR.toFontFamily()
            }
        )

        val typeface = resolveAsTypeface(fontFamily = FontFamily.Cursive)

        assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
    }

    @Test
    fun androidResolveInterceptor_affectsAsyncFontResolution_withFallback() {
        initializeSubject(AndroidFontResolveInterceptor(accessibilityFontWeightAdjustment))

        val loader = AsyncTestTypefaceLoader()
        val asyncFauxFontW400 = AsyncFauxFont(loader, FontWeight.W400)
        val asyncFauxFontW700 = AsyncFauxFont(loader, FontWeight.W700)
        val blockingFauxFontW400 = BlockingFauxFont(loader, Typeface.DEFAULT, FontWeight.W400)

        val fontFamily = FontFamily(
            asyncFauxFontW400,
            blockingFauxFontW400,
            asyncFauxFontW700
        )

        val fallbackTypeface = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(fallbackTypeface).hasWeightAndStyle(FontWeight.W700, FontStyle.Normal)

        // loads the W700 async font which should be the matched font
        loader.completeOne(asyncFauxFontW700, Typeface.MONOSPACE)

        val typeface = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(typeface).isSameInstanceAs(Typeface.MONOSPACE)
    }

    @Test
    fun androidResolveInterceptor_affectsAsyncFontResolution_withBlockingFont() {
        initializeSubject(AndroidFontResolveInterceptor(accessibilityFontWeightAdjustment))

        val loader = AsyncTestTypefaceLoader()
        val asyncFauxFontW400 = AsyncFauxFont(loader, FontWeight.W400)
        val asyncFauxFontW700 = AsyncFauxFont(loader, FontWeight.W700)
        val blockingFauxFontW700 = BlockingFauxFont(loader, Typeface.SANS_SERIF, FontWeight.W700)

        val fontFamily = FontFamily(
            asyncFauxFontW400,
            asyncFauxFontW700,
            blockingFauxFontW700
        )

        val blockingTypeface = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(blockingTypeface).isSameInstanceAs(Typeface.SANS_SERIF)

        // loads the W700 async font which should be the matched font
        loader.completeOne(asyncFauxFontW700, Typeface.MONOSPACE)

        val typeface = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(typeface).isSameInstanceAs(Typeface.MONOSPACE)
    }

    @Test
    fun androidResolveInterceptor_choosesOptionalFont_whenWeightMatches() {
        val loader = AsyncTestTypefaceLoader()
        val optionalFauxFontW400 = OptionalFauxFont(loader, Typeface.MONOSPACE, FontWeight.W400)
        val optionalFauxFontW700 = OptionalFauxFont(loader, Typeface.SERIF, FontWeight.W700)
        val blockingFauxFontW700 = BlockingFauxFont(loader, Typeface.SANS_SERIF, FontWeight.W700)

        initializeSubject()

        val fontFamily = FontFamily(
            optionalFauxFontW400,
            optionalFauxFontW700,
            blockingFauxFontW700
        )

        val typefaceNoAdjustment = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(typefaceNoAdjustment).isSameInstanceAs(Typeface.MONOSPACE)

        initializeSubject(AndroidFontResolveInterceptor(accessibilityFontWeightAdjustment))

        val typeface = resolveAsTypeface(fontFamily, FontWeight.W400)
        assertThat(typeface).isSameInstanceAs(Typeface.SERIF)
    }
}