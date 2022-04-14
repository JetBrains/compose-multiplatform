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

import androidx.compose.ui.text.ExperimentalTextApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

@ExperimentalTextApi
@RunWith(JUnit4::class)
class AsyncFontListLoaderTest {

    @Test
    fun loadWithTimeoutOrNull_returnsOnSuccess() {
        val expected = 3
        val resourceLoader = makeResourceLoader { expected }
        val font = makeFont()
        val subject = makeSubject(resourceLoader)
        val result = runBlocking {
            with(subject) {
                font.loadWithTimeoutOrNull()
            }
        }
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun loadWithTimeoutOrNull_nullsOnException() {
        val expected = null
        val resourceLoader = makeResourceLoader { throw IllegalStateException("Thrown") }
        val font = makeFont()
        val subject = makeSubject(resourceLoader)
        val result = runBlocking {
            with(subject) {
                font.loadWithTimeoutOrNull()
            }
        }
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun loadWithTimeoutOrNull_informsUncaughtExceptionHandler_OnException() {
        val expected = IllegalStateException("Thrown")
        val lock = Object()
        var actualException: Throwable? = null
        val latch = CountDownLatch(1)
        val resourceLoader = makeResourceLoader { throw expected }
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            synchronized(lock) {
                actualException = throwable
            }
            latch.countDown()
        }
        val font = makeFont()
        val subject = makeSubject(resourceLoader)
        val result = runBlocking(exceptionHandler) {
            with(subject) {
                font.loadWithTimeoutOrNull()
            }
        }
        assertThat(result).isNull()
        latch.await(1, TimeUnit.SECONDS)
        synchronized(lock) {
            assertThat(actualException).isInstanceOf(IllegalStateException::class.java)
            assertThat(actualException).hasCauseThat().hasMessageThat().isEqualTo(expected.message)
        }
    }

    @Test
    fun loadWithTimeoutOrNull_nullsOnNull() {
        val expected = null
        val resourceLoader = makeResourceLoader { throw IllegalStateException("Thrown") }
        val font = makeFont()
        val subject = makeSubject(resourceLoader)
        val result = runBlocking {
            with(subject) {
                font.loadWithTimeoutOrNull()
            }
        }
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun loadWithTimeoutOrNull_nullsOnCancellation() {
        val expected = null
        val resourceLoader = makeResourceLoader { coroutineContext.cancel(); 3 }
        val font = makeFont()
        val subject = makeSubject(resourceLoader)
        val result = runBlocking {
            with(subject) {
                font.loadWithTimeoutOrNull()
            }
        }
        assertThat(result).isEqualTo(expected)
    }

    // timeout is tested at integration level, avoiding dupe here for now to keep dependencies light

    private fun makeSubject(platformFontLoader: PlatformFontLoader): AsyncFontListLoader {
        return AsyncFontListLoader(
            emptyList(),
            0,
            mock(TypefaceRequest::class.java),
            AsyncTypefaceCache(),
            onCompletion = { },
            platformFontLoader
        )
    }

    private fun makeFont(): Font {
        return object : Font {
            override val weight: FontWeight = FontWeight.Normal
            override val style: FontStyle = FontStyle.Normal
            @ExperimentalTextApi
            override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Async
        }
    }

    private fun makeResourceLoader(asyncLoad: suspend (Font) -> Any?): PlatformFontLoader {
        return object : PlatformFontLoader {
            override fun loadBlocking(font: Font): Any = TODO("Not called")
            override suspend fun awaitLoad(font: Font): Any? = asyncLoad(font)
            override val cacheKey: String = "androidx.compose.ui.text.font.makeResourceLoader"
        }
    }
}