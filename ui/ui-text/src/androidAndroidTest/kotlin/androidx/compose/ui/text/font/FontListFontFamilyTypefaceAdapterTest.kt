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

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
class FontListFontFamilyTypefaceAdapterTest {

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
        scope = TestCoroutineScope(dispatcher).also {
            dispatcher.pauseDispatcher()
        }
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

    private fun FontFamily.toTypefaceRequest(
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        fontSynthesis: FontSynthesis = FontSynthesis.All
    ) = TypefaceRequest(
        this,
        fontWeight,
        fontStyle,
        fontSynthesis,
        fontLoader.cacheKey
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_onlyBlockingFonts_doesNotLoad() {
        val expected = Typeface.MONOSPACE
        val font = BlockingFauxFont(typefaceLoader, expected)
        val fontFamily = font.toFontFamily()
        val result = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { error("Should not call") },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        assertThat(result).isImmutableTypefaceOf(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_blockingAndAsyncFonts_matchesBlocking_doesLoad() {
        val expected = Typeface.MONOSPACE
        val blockingFont = BlockingFauxFont(typefaceLoader, expected)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(blockingFont, asyncFont)
        val result = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { error("Should not call") },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        assertThat(result).isImmutableTypefaceOf(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun doOneAsyncRequest(
        request: TypefaceRequest,
        beforeAsyncLoad: (TypefaceResult) -> Unit,
        doCompleteAsync: (TypefaceResult) -> Unit
    ): Pair<TypefaceResult, Deferred<TypefaceResult>> {
        val result = CompletableDeferred<TypefaceResult>()
        val reply = subject.resolve(
            request,
            fontLoader,
            onAsyncCompletion = { result.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )!!
        beforeAsyncLoad(reply)
        scope.runCurrent()
        doCompleteAsync(reply)
        scope.runCurrent()
        return reply to result
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_blockingAndAsyncFonts_matchesAsync_doesLoadForBoth() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(asyncFont, blockingFont)

        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.MONOSPACE)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
            }
        )
        assertThat(reply).currentAsyncTypefaceValue(Typeface.SERIF)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(Typeface.SERIF)
        }
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont, blockingFont)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_blockingAndAsyncFonts_matchesAsync_onlyLoadsFirstBlockingFallback() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val expectedInitial = Typeface.MONOSPACE
        val expected = Typeface.SANS_SERIF
        val blockingFont = BlockingFauxFont(typefaceLoader, expectedInitial)
        val blockingFont2 = BlockingFauxFont(typefaceLoader, Typeface.SERIF)
        val fontFamily = FontFamily(asyncFont, blockingFont, blockingFont2)
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(expectedInitial)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )
        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont, asyncFont)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_blockingAndAsyncFonts_differentStyles_onlyLoadsAsync() {
        val blockingFont = BlockingFauxFont(
            typefaceLoader,
            Typeface.MONOSPACE,
            weight = FontWeight.W100
        )
        val expected = Typeface.SANS_SERIF
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont, blockingFont)
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                // this hits platform default
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )
        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_asyncFonts_differentStyles_loadsOnlyNeeded() {
        val asyncFont400 = AsyncFauxFont(typefaceLoader)
        val asyncFont100 = AsyncFauxFont(typefaceLoader, weight = FontWeight.W100)
        val expected = Typeface.MONOSPACE
        val fontFamily = FontFamily(asyncFont100, asyncFont400)
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                // this hits platform default
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont400, expected)
            }
        )
        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont400)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_fallbackAsyncFonts_sameStyle_loadsFirst() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_fallbackAsyncFonts_sameStyle_onTimeout_loadsFallback() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                scope.advanceTimeBy(Font.MaximumAsyncTimeout)
                scope.runCurrent()
                typefaceLoader.completeOne(asyncFontFallback, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFontFallback)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_fallbackAsyncFonts_sameStyle_onError_loadsFallback() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                typefaceLoader.errorOne(asyncFont, RuntimeException("FooBared"))
                scope.runCurrent()
                typefaceLoader.completeOne(asyncFontFallback, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            asyncFontFallback
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_fallbackAsyncFonts_sameStyle_yieldsBetweenFontLoads() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val typefaceResult = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = {
                // don't care in this test
            },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        // start first load
        typefaceLoader.errorOne(asyncFont, RuntimeException("Failed to load"))
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        // advance past yield
        scope.runCurrent()
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFontFallback)
        typefaceLoader.completeOne(asyncFontFallback, Typeface.SERIF)
        scope.runCurrent()
        assertThat(typefaceResult).currentAsyncTypefaceValue(Typeface.SERIF)
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            asyncFontFallback
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_optionalAndAsyncFonts_matchesOptional_doesLoad() {
        val expected = Typeface.MONOSPACE
        val optionalFont = OptionalFauxFont(typefaceLoader, expected)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        val result = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { error("Should not call") },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        assertThat(result).isImmutableTypefaceOf(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_optionalAndAsyncFonts_matchesAsync_doesLoadForBoth() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.DEFAULT)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            optionalFont
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_optionalAndAsyncAndBlockingFonts_matchesAsync_doesLoadForAll() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.SERIF)
        val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.SERIF)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            optionalFont,
            blockingFont
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_optionalAndAsyncAndBlockingFonts_matchAsync_validOptional_doesNotLoadBlocking() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.SERIF)
        // this is a weird order, but lets make sure it doesn't break :)
        val fontFamily = FontFamily(asyncFont, optionalFont, blockingFont)
        val expected = Typeface.MONOSPACE
        val (reply, finalResult) = doOneAsyncRequest(
            fontFamily.toTypefaceRequest(),
            beforeAsyncLoad = {
                assertThat(it).currentAsyncTypefaceValue(Typeface.SANS_SERIF)
            },
            doCompleteAsync = {
                typefaceLoader.completeOne(asyncFont, expected)
            }
        )

        assertThat(reply).currentAsyncTypefaceValue(expected)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(expected)
        }
        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(
            asyncFont,
            optionalFont
        )
        scope.advanceUntilIdle()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onResolve_optionalAndAsyncAndBlockingFonts_matchesOptional_doesNotLoadBlockingAsync() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        // this is expected order
        val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
        val result = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { error("Should not call") },
            createDefaultTypeface = { Typeface.DEFAULT }
        )

        assertThat(typefaceLoader.pendingRequests()).isEmpty()
        assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
        assertThat(result).isImmutableTypefaceOf(Typeface.SANS_SERIF)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun onChangeDispatcher_newRequestLaunchesInNewDispatcher() {
        // make another paused dispatcher
        // it's important that this test uses paused dispatchers to allow us control of runtime
        // ordering
        val newDispatcher = TestCoroutineDispatcher().also {
            it.pauseDispatcher()
        }

        subject = FontListFontFamilyTypefaceAdapter(injectedContext = newDispatcher)

        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont)
        val finalResult = CompletableDeferred<TypefaceResult>()

        val result = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { finalResult.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )

        scope.runCurrent()
        newDispatcher.runCurrent()
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)

        // other scope run has no effect
        scope.runCurrent()
        assertThat(result).currentAsyncTypefaceValue(Typeface.DEFAULT)

        // correct scope run completes
        newDispatcher.runCurrent()
        assertThat(finalResult.isActive).isFalse()
        assertThat(result).currentAsyncTypefaceValue(Typeface.SERIF)
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(Typeface.SERIF)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationIsConsideredLoadError() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont)
        val finalResult = CompletableDeferred<TypefaceResult>()

        val firstResult = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { finalResult.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        scope.runCurrent()
        typefaceLoader.errorOne(asyncFont, CancellationException())
        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypeface()
        }
        assertThat(firstResult).currentAsyncTypefaceValue(Typeface.DEFAULT)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationOfFirstRequest_cannotCancelJob() {
        val cancellingLoader = object : AndroidFont.TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                TODO("Not yet implemented")
            }

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                coroutineContext[Job]?.cancel()
                return null
            }
        }
        val asyncFont = object : AndroidFont(FontLoadingStrategy.Async, cancellingLoader) {
            override val weight: FontWeight = FontWeight.Normal
            override val style: FontStyle = FontStyle.Normal
        }

        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFontFallback")
        val finalResult = CompletableDeferred<TypefaceResult>()
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)

        subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { finalResult.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        scope.runCurrent()
        typefaceLoader.completeOne(asyncFontFallback, Typeface.SERIF)

        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(Typeface.SERIF)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationOfFirstLoad_stillDoesSecond_inFallback() {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFontFallback")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val finalResult = CompletableDeferred<TypefaceResult>()

        subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { finalResult.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        scope.runCurrent()
        typefaceLoader.errorOne(asyncFont, CancellationException())

        scope.runCurrent()
        typefaceLoader.completeOne(asyncFontFallback, Typeface.SERIF)

        scope.runBlockingTest {
            assertThat(finalResult.await()).isImmutableTypefaceOf(Typeface.SERIF)
        }
    }

    @Test
    fun dispatchesOnInitialThread_butDispatchesToBackground_whenRunOnRealDispatcher() {
        subject = FontListFontFamilyTypefaceAdapter(cache)
        var beforeThread: Thread? = null
        var afterThread: Thread? = null
        val lock = Object()
        val latch = CountDownLatch(1)

        val typefaceLoader = object : AndroidFont.TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                TODO("Not yet implemented")
            }

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                synchronized(lock) {
                    // should be on the testing thread here
                    beforeThread = Thread.currentThread()
                }
                // yield causes dispatch
                yield()
                synchronized(lock) {
                    // should be on a worker thread here
                    afterThread = Thread.currentThread()
                }
                latch.countDown()
                return Typeface.SERIF
            }
        }
        val font = object : AndroidFont(FontLoadingStrategy.Async, typefaceLoader) {
            override val weight: FontWeight = FontWeight.W400
            override val style: FontStyle = FontStyle.Normal
        }
        val fontFamily = FontFamily(font)
        subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { /* none */ },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        latch.await(1, TimeUnit.SECONDS)
        synchronized(lock) {
            assertThat(beforeThread).isSameInstanceAs(Thread.currentThread())
            assertThat(afterThread).isNotSameInstanceAs(Thread.currentThread())
            assertThat(afterThread).isNotNull()
        }
    }

    @Test
    fun cancellationDoesNotCancelScope_forDefaultScope() {
        subject = FontListFontFamilyTypefaceAdapter(cache)

        requestAndThrowOnRealDispatcher(CancellationException())
        requestAndCompleteOnRealDispatcher()
    }

    @Test
    fun cancellationDoesNotCancelScope_forProvidedScopeWithJob() {
        subject = FontListFontFamilyTypefaceAdapter(cache, Dispatchers.IO + Job())
        requestAndThrowOnRealDispatcher(CancellationException())
        requestAndCompleteOnRealDispatcher()
    }

    @Test
    fun runtimeExceptionDoesNotCancelScope_forDefaultScope() {
        subject = FontListFontFamilyTypefaceAdapter(cache)
        requestAndThrowOnRealDispatcher(RuntimeException("fail the request"))
        requestAndCompleteOnRealDispatcher()
    }

    @Test
    fun runtimeExceptionDoesNotCancelScope_forProvidedScopeWithJob() {
        subject = FontListFontFamilyTypefaceAdapter(cache, Dispatchers.IO + Job())
        requestAndThrowOnRealDispatcher(RuntimeException("fail the request"))
        requestAndCompleteOnRealDispatcher()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun runtimeExceptionOnRealDispatcher_informsExceptionHandler() {
        val exception: CompletableDeferred<Throwable> = CompletableDeferred()
        subject = FontListFontFamilyTypefaceAdapter(
            cache,
            CoroutineExceptionHandler { _, throwable ->
                exception.complete(throwable)
            }
        )
        val cause = RuntimeException("fail the request")
        requestAndThrowOnRealDispatcher(cause)
        runBlocking {
            withTimeout(5_000) { // just so local execution doesn't hang on failure
                val error = exception.await()
                assertThat(error).isInstanceOf(IllegalStateException::class.java)
                assertThat(error).hasCauseThat().isEqualTo(cause)
            }
        }
    }

    @Test
    fun cancellingPassedScope_doesNotCancelFontLoads() {
        val job = Job()
        subject = FontListFontFamilyTypefaceAdapter(cache, Dispatchers.IO + job)
        job.cancel()
        requestAndThrowOnRealDispatcher(RuntimeException("fail the request"))

        // this request should not do any async work due to cancellation, but should also be
        // uncached
        val asyncFont = AsyncFauxFont(typefaceLoader, name = "RequestAndComplete")
        val fontFamily = FontFamily(asyncFont)
        val requestLatch = CountDownLatch(1)
        val result = CompletableDeferred<TypefaceResult?>()
        typefaceLoader.onAsyncLoad { requestLatch.countDown() }
        val asyncRequest: TypefaceResult? = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { result.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        assertThat(asyncRequest!!.cacheable).isFalse()
        assertThat(asyncRequest).currentAsyncTypefaceValue(Typeface.DEFAULT)
        val typefaceResult = runBlocking { result.await() }
        assertThat(typefaceResult).isImmutableTypefaceOf(Typeface.DEFAULT)
        assertThat(typefaceResult!!.cacheable).isFalse()
    }

    private fun requestAndCompleteOnRealDispatcher() {
        val asyncFont = AsyncFauxFont(typefaceLoader, name = "RequestAndComplete")
        val fontFamily = FontFamily(asyncFont)
        val requestLatch = CountDownLatch(1)
        val result = CompletableDeferred<TypefaceResult>()
        typefaceLoader.onAsyncLoad { requestLatch.countDown() }
        val asyncResult = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { result.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        // we're running on a real dispatcher, sync manually
        requestLatch.await()
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        runBlocking {
            assertThat(result.await()).isImmutableTypefaceOf(Typeface.SERIF)
        }
        assertThat(asyncResult).currentAsyncTypefaceValue(Typeface.SERIF)
    }

    private fun requestAndThrowOnRealDispatcher(cause: RuntimeException) {
        val font = AsyncFauxFont(typefaceLoader, name = "RequestAndThrow")
        val fontFamily = FontFamily(font)
        val result = CompletableDeferred<TypefaceResult>()
        val requestLatch = CountDownLatch(1)

        typefaceLoader.onAsyncLoad { requestLatch.countDown() }
        val asyncResult = subject.resolve(
            fontFamily.toTypefaceRequest(),
            fontLoader,
            onAsyncCompletion = { result.complete(it) },
            createDefaultTypeface = { Typeface.DEFAULT }
        )
        // we're running on a real dispatcher, sync manually
        requestLatch.await()
        typefaceLoader.errorOne(font, cause)
        runBlocking {
            assertThat(result.await()).isImmutableTypefaceOf(Typeface.DEFAULT)
        }
        assertThat(asyncResult).currentAsyncTypefaceValue(Typeface.DEFAULT)
    }
}