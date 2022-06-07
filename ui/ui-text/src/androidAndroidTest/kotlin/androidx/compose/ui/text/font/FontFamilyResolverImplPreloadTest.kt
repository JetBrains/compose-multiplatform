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

@file:Suppress("DEPRECATION") // b/220884136

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.compose.ui.text.font.testutils.getImmutableResultFor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
@ExperimentalCoroutinesApi
class FontFamilyResolverImplPreloadTest {
    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var scope: TestCoroutineScope
    private lateinit var dispatcher: TestCoroutineDispatcher
    private lateinit var asyncTypefaceCache: AsyncTypefaceCache
    private lateinit var typefaceCache: TypefaceRequestCache
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val fontLoader = AndroidFontLoader(context)
    private lateinit var subject: FontFamilyResolverImpl

    @Before
    fun setup() {
        asyncTypefaceCache = AsyncTypefaceCache()
        typefaceCache = TypefaceRequestCache()
        dispatcher = TestCoroutineDispatcher()
        scope = TestCoroutineScope(dispatcher)
        val injectedContext = scope.coroutineContext.minusKey(CoroutineExceptionHandler)
        subject = FontFamilyResolverImpl(
            fontLoader,
            typefaceRequestCache = typefaceCache,
            fontListFontFamilyTypefaceAdapter = FontListFontFamilyTypefaceAdapter(
                asyncTypefaceCache,
                injectedContext
            )
        )
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    @Test
    fun preload_insertsTypefaceIntoCache() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        scope.runBlockingTest {
            subject.preload(fontFamily)
        }
        assertThat(typefaceCache.size).isEqualTo(1)
        val cacheResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            FontWeight.W100,
            fontLoader = fontLoader
        )
        assertThat(cacheResult).isNotNull()
    }

    @Test
    fun preload_insertsTypefaceIntoCache_onlyForFontWeightAndStyle() {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        scope.runBlockingTest {
            subject.preload(fontFamily)
        }
        assertThat(typefaceCache.size).isEqualTo(1)
        val cacheResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            FontWeight.W200,
            fontLoader = fontLoader
        )
        assertThat(cacheResult).isNull()
    }

    @Test
    fun preload_insertsAllTypefaces_intoCache() {
        val fontFamily = FontFamily(
            FontTestData.FONT_100_REGULAR,
            FontTestData.FONT_200_REGULAR,
            FontTestData.FONT_300_REGULAR,
            FontTestData.FONT_400_REGULAR,
            FontTestData.FONT_500_REGULAR
        )
        scope.runBlockingTest {
            subject.preload(fontFamily)
        }
        assertThat(typefaceCache.size).isEqualTo(5)
        for (weight in 100..500 step 100) {
            val cacheResult = typefaceCache.getImmutableResultFor(
                fontFamily,
                FontWeight(weight),
                fontLoader = fontLoader
            )
            assertThat(cacheResult).isNotNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun preload_resolvesAsyncFonts() {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = font.toFontFamily()
        val preloadResult = scope.async {
            subject.preload(fontFamily)
        }

        assertThat(typefaceLoader.pendingRequestsFor(font)).hasSize(1)
        // at this point, the request is out but font cache hasn't started
        assertThat(typefaceCache.size).isEqualTo(0)

        assertThat(preloadResult.isActive).isTrue()

        typefaceLoader.completeOne(font, Typeface.MONOSPACE)

        scope.runBlockingTest {
            preloadResult.await()
        }

        // at this point, result is back, and preload() has returned, so the main typeface
        // cache contains the result
        assertThat(typefaceCache.size).isEqualTo(1)

        val typefaceResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            fontLoader = fontLoader
        )
        assertThat(typefaceResult).isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun preload_onlyLoadsFirstAsyncFontInChain() {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = FontFamily(
            font,
            fallbackFont
        )
        val preloadResult = scope.async {
            subject.preload(fontFamily)
        }

        typefaceLoader.completeOne(font, Typeface.MONOSPACE)

        scope.runBlockingTest {
            preloadResult.await()
        }

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalStateException::class)
    fun preload_errorsOnTimeout() {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val dispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(dispatcher)

        val fontFamily = FontFamily(
            font,
            fallbackFont
        )
        val deferred = testScope.async { subject.preload(fontFamily) }
        testScope.advanceTimeBy(Font.MaximumAsyncTimeout)
        assertThat(deferred.isCompleted).isTrue()
        testScope.runBlockingTest {
            deferred.await() // actually throw here
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun whenOptionalFontFound_preload_doesNotResolveAsyncFont() {
        val optionalFont = OptionalFauxFont(
            typefaceLoader,
            Typeface.DEFAULT,
            FontWeight.Normal,
            FontStyle.Normal
        )
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val dispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(dispatcher)

        val fontFamily = FontFamily(
            optionalFont,
            fallbackFont
        )
        val preloadResult = testScope.async {
            subject.preload(fontFamily)
        }

        scope.runBlockingTest {
            preloadResult.await()
        }

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            fontLoader = fontLoader
        )
        assertThat(typefaceResult).isSameInstanceAs(Typeface.DEFAULT)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun whenOptionalFontNotFound_preload_doesResolveAsyncFont() {
        val optionalFont = OptionalFauxFont(
            typefaceLoader,
            null,
            FontWeight.Normal,
            FontStyle.Normal
        )
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val dispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(dispatcher)
        val fontFamily = FontFamily(
            optionalFont,
            fallbackFont
        )
        val preloadResult = testScope.async {
            subject.preload(fontFamily)
        }
        testScope.runCurrent() // past yield on optionalFont
        typefaceLoader.completeOne(fallbackFont, Typeface.MONOSPACE)

        scope.runBlockingTest {
            preloadResult.await()
        }

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            fontLoader = fontLoader
        )
        assertThat(typefaceResult).isSameInstanceAs(Typeface.MONOSPACE)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun whenBlockingFont_neverResolvesAsync() {
        val blockingFont = BlockingFauxFont(
            typefaceLoader,
            Typeface.DEFAULT_BOLD,
            FontWeight.Bold,
            FontStyle.Normal
        )
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Bold, FontStyle.Normal)
        val dispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(dispatcher)

        val fontFamily = FontFamily(
            blockingFont,
            fallbackFont
        )
        val preloadResult = testScope.async {
            subject.preload(fontFamily)
        }

        scope.runBlockingTest {
            preloadResult.await()
        }

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult = typefaceCache.getImmutableResultFor(
            fontFamily,
            fontWeight = FontWeight.Bold,
            fontLoader = fontLoader
        )
        assertThat(typefaceResult).isSameInstanceAs(Typeface.DEFAULT_BOLD)
    }

    // other font chain semantics are tested directly, preload is just checking that we don't
    // trigger async work when it's not necessary
}