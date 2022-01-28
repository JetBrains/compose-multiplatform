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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

// This is not a parameterized test due to inline class synthetic arguments
// see: (b/154330441)
@RunWith(AndroidJUnit4::class)
@MediumTest
@OptIn(ExperimentalTextApi::class)
class PlatformFontFamilyTypefaceAdapterTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontLoader = AndroidFontLoader(context)

    @Suppress("MemberVisibilityCanBePrivate")
    val parameters: List<Pair<FontWeight, FontStyle>> = (15 until 1000 step 15)
        .zip(listOf(FontStyle.Italic, FontStyle.Normal))
        .map { (weight, style) -> FontWeight(weight) to style }

    @Test
    fun canLoadNullTypeface() {
        assertLoadForAllWeightsAndStyles(null)
    }

    @Test
    fun canLoadDefaultTypeface() {
        val fontFamily = FontFamily.Default
        assertLoadForAllWeightsAndStyles(fontFamily)
    }

    @Test
    fun canLoadCursiveTypeface() {
        val fontFamily = FontFamily.Cursive
        assertLoadForAllWeightsAndStyles(fontFamily)
    }

    @Test
    fun canLoadMonospaceTypeface() {
        val fontFamily = FontFamily.Monospace
        assertLoadForAllWeightsAndStyles(fontFamily)
    }

    @Test
    fun canLoadSerifTypeface() {
        val fontFamily = FontFamily.Serif
        assertLoadForAllWeightsAndStyles(fontFamily)
    }

    @Test
    fun canLoadSansSerifTypeface() {
        val fontFamily = FontFamily.SansSerif
        assertLoadForAllWeightsAndStyles(fontFamily)
    }

    @Suppress("DEPRECATION")
    @Test
    fun canLoadLoadedFontFamily_noSynthesis() {
        val fontFamily = FontFamily(
            Typeface(context, FontTestData.FONT_100_REGULAR.toFontFamily())
        )
        // this runs through synthesis path
        parameters.forEach { (weight, style) ->
            val subject = PlatformFontFamilyTypefaceAdapter()
            val typeRequest = TypefaceRequest(
                fontFamily,
                weight,
                style,
                FontSynthesis.None,
                null
            )
            val trackingFun = TrackingFun()
            val result = subject.resolve(
                typefaceRequest = typeRequest,
                platformFontLoader = fontLoader,
                onAsyncCompletion = trackingFun,
                createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
            )

            val typeface = (result as TypefaceResult.Immutable).value as Typeface?
            assertThat(typeface).hasWeightAndStyle(FontWeight.W100, FontStyle.Normal)
            trackingFun.assertNeverCalled()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun canLoadLoadedFontFamily_synthesizeStyle() {
        val fontFamily = FontFamily(
            Typeface(context, FontTestData.FONT_100_REGULAR.toFontFamily())
        )
        // this runs through synthesis path
        parameters.forEach { (weight, style) ->
            val subject = PlatformFontFamilyTypefaceAdapter()
            val typeRequest = TypefaceRequest(
                fontFamily,
                weight,
                style,
                FontSynthesis.Style,
                null
            )
            val trackingFun = TrackingFun()
            val result = subject.resolve(
                typefaceRequest = typeRequest,
                platformFontLoader = fontLoader,
                onAsyncCompletion = trackingFun,
                createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
            )

            val typeface = (result as TypefaceResult.Immutable).value as Typeface?
            assertThat(typeface).hasWeightAndStyle(FontWeight.W100, style)
            trackingFun.assertNeverCalled()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun canLoadLoadedFontFamily_weightSynthesis() {
        val fontFamily = FontFamily(
            Typeface(context, FontTestData.FONT_100_REGULAR.toFontFamily())
        )
        // this runs through synthesis path
        parameters.forEach { (weight, style) ->
            val subject = PlatformFontFamilyTypefaceAdapter()
            val typeRequest = TypefaceRequest(
                fontFamily,
                weight,
                style,
                FontSynthesis.Weight,
                null
            )
            val trackingFun = TrackingFun()
            val result = subject.resolve(
                typefaceRequest = typeRequest,
                platformFontLoader = fontLoader,
                onAsyncCompletion = trackingFun,
                createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
            )

            val typeface = (result as TypefaceResult.Immutable).value as Typeface?
            val finalWeight = if (weight >= FontWeight.AndroidBold) {
                weight
            } else {
                FontWeight.W100
            }
            assertThat(typeface).hasWeightAndStyle(finalWeight, FontStyle.Normal)
            trackingFun.assertNeverCalled()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun canLoadLoadedFontFamily_allSynthesis() {
        val fontFamily = FontFamily(
            Typeface(context, FontTestData.FONT_100_REGULAR.toFontFamily())
        )
        // this runs through synthesis path
        parameters.forEach { (weight, style) ->
            val subject = PlatformFontFamilyTypefaceAdapter()
            val typeRequest = TypefaceRequest(
                fontFamily,
                weight,
                style,
                FontSynthesis.All,
                null
            )
            val trackingFun = TrackingFun()
            val result = subject.resolve(
                typefaceRequest = typeRequest,
                platformFontLoader = fontLoader,
                onAsyncCompletion = trackingFun,
                createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
            )

            val typeface = (result as TypefaceResult.Immutable).value as Typeface?
            val finalWeight = if (weight >= FontWeight.AndroidBold) {
                weight
            } else {
                FontWeight.W100
            }
            assertThat(typeface).hasWeightAndStyle(finalWeight, style)
            trackingFun.assertNeverCalled()
        }
    }

    @Test
    fun fontListFontFamily_returnsNull() {
        val subject = PlatformFontFamilyTypefaceAdapter()
        val typeRequest = TypefaceRequest(
            FontTestData.FONT_100_REGULAR.toFontFamily(),
            FontWeight.W100,
            FontStyle.Normal,
            FontSynthesis.All,
            null
        )
        val trackingFun = TrackingFun()
        val result = subject.resolve(
            typefaceRequest = typeRequest,
            platformFontLoader = fontLoader,
            onAsyncCompletion = trackingFun,
            createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
        )

        assertThat(result).isNull()
        trackingFun.assertNeverCalled()
    }

    private fun assertLoadForAllWeightsAndStyles(fontFamily: FontFamily?) {
        parameters.forEach { (weight, style) ->
            val subject = PlatformFontFamilyTypefaceAdapter()
            val typeRequest = TypefaceRequest(
                fontFamily,
                weight,
                style,
                FontSynthesis.All,
                null,
            )
            val trackingFun = TrackingFun()
            val result = subject.resolve(
                typefaceRequest = typeRequest,
                platformFontLoader = fontLoader,
                onAsyncCompletion = trackingFun,
                createDefaultTypeface = { android.graphics.Typeface.DEFAULT }
            )

            val typeface = (result as TypefaceResult.Immutable).value as Typeface?
            assertThat(typeface).hasWeightAndStyle(weight, style)
            trackingFun.assertNeverCalled()
        }
    }

    private class TrackingFun : Function1<TypefaceResult, Unit> {
        private val lock = Object()
        private val _calledWith = mutableListOf<TypefaceResult>()
        val calledWith: List<TypefaceResult>
            get() = synchronized(lock) { _calledWith }

        override operator fun invoke(typefaceResult: TypefaceResult) {
            synchronized(lock) {
                _calledWith.add(typefaceResult)
            }
        }

        fun assertNeverCalled() {
            assertThat(calledWith).isEmpty()
        }
    }
}