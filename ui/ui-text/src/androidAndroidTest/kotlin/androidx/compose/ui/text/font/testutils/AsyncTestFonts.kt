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

package androidx.compose.ui.text.font.testutils

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth
import kotlinx.coroutines.CompletableDeferred

@Suppress("MemberVisibilityCanBePrivate") // visible for testing
class AsyncTestTypefaceLoader : AndroidFont.TypefaceLoader {
    private val callbackLock = Object()
    private var loadCallback: ((AndroidFont) -> Unit)? = null
    @Volatile
    private var asyncLoadCallback: ((AndroidFont) -> Unit)? = null
    private val requests =
        mutableMapOf<AsyncFauxFont, MutableList<CompletableDeferred<Typeface?>>>()
    internal val completedAsyncRequests = mutableListOf<AsyncFauxFont>()
    internal val blockingRequests = mutableListOf<BlockingFauxFont>()
    internal val blockingAsyncRequests = mutableListOf<BlockingFauxFont>()
    internal val optionalRequests = mutableListOf<OptionalFauxFont>()
    internal val optionalAsyncRequests = mutableListOf<Font>()

    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        val result = when (font) {
            is OptionalFauxFont -> {
                optionalRequests.add(font)
                font.typeface
            }
            is BlockingFauxFont -> {
                blockingRequests.add(font)
                font.typeface
            }
            else -> error("unsupported load() font")
        }
        loadCallback?.invoke(font)
        return result
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        val result = when (font) {
            is OptionalFauxFont -> {
                optionalAsyncRequests.add(font)
                synchronized(callbackLock) {
                    asyncLoadCallback?.invoke(font)
                }
                font.typeface
            }
            is BlockingFauxFont -> {
                blockingAsyncRequests.add(font)
                synchronized(callbackLock) {
                    asyncLoadCallback?.invoke(font)
                }
                font.typeface
            }
            is AsyncFauxFont -> {
                val deferred = CompletableDeferred<Typeface?>()
                val list = requests.getOrPut(font) { mutableListOf() }
                list.add(deferred)
                synchronized(callbackLock) {
                    asyncLoadCallback?.invoke(font)
                }
                deferred.await()
            }
            else -> null
        }
        return result
    }

    fun completeOne(font: AsyncFauxFont, typeface: Typeface?) {
        Truth.assertThat(requests).containsKey(font)
        val requestList = requests[font]!!
        requestList.removeAt(0).complete(typeface)
        completedAsyncRequests.add(font)
    }

    fun errorOne(font: AsyncFauxFont, error: Throwable) {
        Truth.assertThat(requests).containsKey(font)
        val requestList = requests[font]!!
        requestList.removeAt(0).completeExceptionally(error)
        completedAsyncRequests.add(font)
    }

    fun pendingRequestsFor(font: AsyncFauxFont): List<Font> {
        return requests
            .getOrPut(font) { mutableListOf() }
            .map { font }
    }

    fun completedRequests(): List<Font> {
        return (completedAsyncRequests +
            blockingRequests +
            blockingAsyncRequests +
            optionalRequests +
            optionalAsyncRequests)
    }

    fun pendingRequests(): List<AsyncFauxFont> {
        return requests.keys.filter { requests[it]?.isNotEmpty() ?: false }.toList()
    }

    fun onLoad(function: (AndroidFont) -> Unit) {
        loadCallback = function
    }

    fun onAsyncLoad(function: (AndroidFont) -> Unit) {
        synchronized(callbackLock) {
            asyncLoadCallback = function
        }
    }
}

class AsyncFauxFont(
    typefaceLoader: AsyncTestTypefaceLoader,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
    val name: String = "AsyncFauxFont"
) : AndroidFont(Async, typefaceLoader) {
    override fun toString(): String {
        return "$name[$weight, $style]"
    }
}

class OptionalFauxFont(
    typefaceLoader: AsyncTestTypefaceLoader,
    internal val typeface: Typeface?,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
    val name: String = "OptionalFauxFont"
) : AndroidFont(OptionalLocal, typefaceLoader) {
    override fun toString(): String {
        return "$name[$weight, $style]"
    }
}

class BlockingFauxFont(
    typefaceLoader: AsyncTestTypefaceLoader,
    internal val typeface: Typeface,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
    val name: String = "BlockingFauxFont"
) : AndroidFont(Blocking, typefaceLoader) {
    override fun toString(): String {
        return "$name[$weight, $style]"
    }
}