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
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
@ExperimentalCoroutinesApi
class FontFamilyResolverImplCancellationTest {
    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var subject: FontFamilyResolverImpl
    private lateinit var scope: TestCoroutineScope
    private lateinit var typefaceRequestCache: TypefaceRequestCache
    private lateinit var asyncTypefaceCache: AsyncTypefaceCache

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontLoader = AndroidFontLoader(context)
    private val fontResolveInterceptor = AndroidFontResolveInterceptor(context)

    @Before
    fun setup() {
        asyncTypefaceCache = AsyncTypefaceCache()
        typefaceRequestCache = TypefaceRequestCache()
        val dispatcher = TestCoroutineDispatcher()
        scope = TestCoroutineScope(dispatcher).also {
            dispatcher.pauseDispatcher()
        }
        val injectedContext = scope.coroutineContext.minusKey(CoroutineExceptionHandler)
        subject = FontFamilyResolverImpl(
            fontLoader,
            fontResolveInterceptor,
            typefaceRequestCache,
            FontListFontFamilyTypefaceAdapter(asyncTypefaceCache, injectedContext))
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    @After
    fun cleanup() {
        try {
            scope.cleanupTestCoroutines()
        } catch (e: AssertionError) {
            // TODO: fix Test finished with active jobs
        }
    }

    @Test
    fun onAsyncLoadCancellation_consideredLoadFailure() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = asyncFont.toFontFamily()
        subject.resolve(fontFamily)

        fun currentCacheItem(): TypefaceResult = typefaceRequestCache.get(
            TypefaceRequest(
                fontFamily,
                FontWeight.Normal,
                FontStyle.Normal,
                FontSynthesis.All,
                fontLoader.cacheKey
            )
        )!!

        scope.runCurrent()
        val beforeCacheEntry = currentCacheItem()
        assertThat(beforeCacheEntry).isAsyncTypeface()
        typefaceLoader.errorOne(asyncFont, CancellationException())
        scope.runCurrent()
        val afterCache = currentCacheItem()
        assertThat(afterCache).isImmutableTypefaceOf(Typeface.DEFAULT)
    }
}