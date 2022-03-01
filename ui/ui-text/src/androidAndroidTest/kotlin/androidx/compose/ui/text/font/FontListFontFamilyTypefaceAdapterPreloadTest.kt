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

@file:Suppress("DEPRECATION") // for deprecated test-coroutines api - b/220884136

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
class FontListFontFamilyTypefaceAdapterPreloadTest {

    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var subject: FontListFontFamilyTypefaceAdapter
    @OptIn(ExperimentalCoroutinesApi::class)
    private lateinit var scope: TestCoroutineScope
    private lateinit var cache: AsyncTypefaceCache

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontLoader = AndroidFontLoader(context)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        cache = AsyncTypefaceCache()
        val dispatcher = TestCoroutineDispatcher()
        scope = TestCoroutineScope(dispatcher)
        val injectedContext = scope.coroutineContext.minusKey(CoroutineExceptionHandler)
        subject = FontListFontFamilyTypefaceAdapter(cache, injectedContext)
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun cleanup() {
        try {
            scope.cleanupTestCoroutines()
        } catch (e: AssertionError) {
            // TODO: fix Test finished with active jobs
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_onlyBlockingFonts_doesNotLoad() {
        val font = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = font.toFontFamily()
        scope.runBlockingTest {
            subject.preload(fontFamily, fontLoader)
            // no styles matched, so no interactions
            assertThat(typefaceLoader.completedRequests()).isEmpty()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_blockingAndAsyncFonts_matchesBlocking_doesLoad() {
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(blockingFont, asyncFont)
        scope.runBlockingTest {
            subject.preload(fontFamily, fontLoader)
            // style matched, but blocking font is higher priority than async font
            assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_blockingAndAsyncFonts_matchesAsync_doesLoadForBoth() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(asyncFont, blockingFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_blockingAndAsyncFonts_matchesAsync_onlyLoadsFirstBlockingFallback() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val blockingFont2 = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(asyncFont, blockingFont, blockingFont2)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_blockingAndAsyncFonts_differentStyles_onlyLoadsAsync() {
        val blockingFont = BlockingFauxFont(
            typefaceLoader,
            Typeface.MONOSPACE,
            weight = FontWeight.W100
        )
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont, blockingFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_asyncFonts_differentStyles_loadsAll() {
        val asyncFont100 = AsyncFauxFont(typefaceLoader, weight = FontWeight.W100)
        val asyncFont400 = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont400, asyncFont100)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont400, asyncFont100)
        typefaceLoader.completeOne(asyncFont400, Typeface.SERIF)
        typefaceLoader.completeOne(asyncFont100, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_fallbackAsyncFonts_sameStyle_loadsFirst() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val job = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)

        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont)
        assertThat(job.isActive).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalStateException::class)
    fun onPreloadFail_timeout_throwsIllegalStateException() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val preloadJob = scope.async {
            subject.preload(fontFamily, fontLoader)
        }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        scope.advanceTimeBy(Font.MaximumAsyncTimeout)
        scope.runBlockingTest {
            preloadJob.await()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalStateException::class)
    fun onPreload_whenFontLoadError_throwsIllegalStateException() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val deferred = scope.async {
            subject.preload(fontFamily, fontLoader)
        }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.errorOne(asyncFont, RuntimeException("Failed to load"))
        scope.runBlockingTest {
            deferred.await()
        }
    }

    class MyFontException : RuntimeException()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalStateException::class)
    fun onPreloadFails_exception_throwsIllegalStateException() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val deferred = scope.async {
            subject.preload(fontFamily, fontLoader)
        }
        typefaceLoader.errorOne(asyncFont, MyFontException())
        scope.runBlockingTest {
            deferred.await() // should throw
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_optionalAndAsyncFonts_matchesOptional_doesLoad() {
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        scope.runBlockingTest {
            subject.preload(fontFamily, fontLoader)
            assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_optionalAndAsyncFonts_matchesAsync_doesLoadForBoth() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            optionalFont
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchesAsync_doesLoadForAll() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            optionalFont,
            blockingFont
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchAsync_validOptional_doesNotLoadBlocking() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        // this is a weird order, but lets make sure it doesn't break :)
        val fontFamily = FontFamily(asyncFont, optionalFont, blockingFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont, optionalFont)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchesOptional_doesNotLoadBlockingAsync() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        // this is expected order
        val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
        val preloadJob = scope.launch {
            subject.preload(fontFamily, fontLoader)
        }

        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
    }
}