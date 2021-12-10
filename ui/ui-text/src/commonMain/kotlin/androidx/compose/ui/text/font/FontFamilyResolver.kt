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

import androidx.compose.runtime.State
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.caches.LruCache
import androidx.compose.ui.text.platform.createSynchronizedObject
import androidx.compose.ui.text.platform.synchronized
import androidx.compose.ui.util.fastMap
import kotlin.coroutines.CoroutineContext

@ExperimentalTextApi
internal class FontFamilyResolverImpl(
    private val typefaceRequestCache: TypefaceRequestCache = TypefaceRequestCache(),
    private val fontListFontFamilyTypefaceAdapter: FontListFontFamilyTypefaceAdapter =
        FontListFontFamilyTypefaceAdapter(),
    private val platformFamilyTypefaceAdapter: PlatformFontFamilyTypefaceAdapter =
        PlatformFontFamilyTypefaceAdapter()
) : FontFamily.Resolver {
    override suspend fun preload(
        fontFamily: FontFamily,
        resourceLoader: Font.ResourceLoader
    ) {
        // all other types of FontFamily are already preloaded.
        if (fontFamily !is FontListFontFamily) return

        fontListFontFamilyTypefaceAdapter.preload(fontFamily, resourceLoader)

        val typeRequests = fontFamily.fonts.fastMap {
            TypefaceRequest(
                fontFamily,
                it.weight,
                it.style,
                FontSynthesis.All,
                resourceLoader.cacheKey
            )
        }

        typefaceRequestCache.preWarmCache(typeRequests) { typeRequest ->
            @Suppress("MoveLambdaOutsideParentheses")
            fontListFontFamilyTypefaceAdapter.resolve(
                typefaceRequest = typeRequest,
                resourceLoader = resourceLoader,
                onAsyncCompletion = { /* nothing */ }
            ) ?: platformFamilyTypefaceAdapter.resolve(
                typefaceRequest = typeRequest,
                resourceLoader = resourceLoader,
                onAsyncCompletion = { /* nothing */ }
            ) ?: throw IllegalStateException("Could not load font")
        }
    }

    override fun resolve(
        resourceLoader: Font.ResourceLoader,
        fontFamily: FontFamily?,
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        fontSynthesis: FontSynthesis,
    ): State<Any> {
        val typeRequest = TypefaceRequest(
            fontFamily,
            fontWeight,
            fontStyle,
            fontSynthesis,
            resourceLoader.cacheKey
        )
        val result = typefaceRequestCache.runCached(typeRequest) { onAsyncCompletion ->
            fontListFontFamilyTypefaceAdapter.resolve(
                typeRequest,
                resourceLoader,
                onAsyncCompletion
            ) ?: platformFamilyTypefaceAdapter.resolve(
                typeRequest,
                resourceLoader,
                onAsyncCompletion
            ) ?: throw IllegalStateException("Could not load font")
        }
        return result
    }

    override fun setAsyncLoadContext(context: CoroutineContext) {
        fontListFontFamilyTypefaceAdapter.setAsyncLoadContext(context)
    }
}

@ExperimentalTextApi
internal expect class PlatformFontFamilyTypefaceAdapter() : FontFamilyTypefaceAdapter

internal data class TypefaceRequest(
    val fontFamily: FontFamily?,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle,
    val fontSynthesis: FontSynthesis,
    val resourceLoaderCacheKey: String?
)

internal sealed interface TypefaceResult : State<Any> {
    // Immutable results present as State, but don't trigger a read observer
    class Immutable(override val value: Any) : TypefaceResult
    class Async(internal val current: State<Any>) : TypefaceResult, State<Any> by current
}

internal class TypefaceRequestCache {
    internal val lock = createSynchronizedObject()
    // @GuardedBy("lock")
    private val resultCache = LruCache<TypefaceRequest, TypefaceResult>(16)

    fun runCached(
        typefaceRequest: TypefaceRequest,
        resolveTypeface: ((TypefaceResult) -> Unit) -> TypefaceResult
    ): State<Any> {
        synchronized(lock) {
            resultCache.get(typefaceRequest)?.let {
                return it
            }
        }
        // this is not run synchronized2 as it incurs expected file system reads.
        //
        // As a result, it is possible the same FontFamily resolution is started twice if this
        // function is entered concurrently. This is explicitly allowed, to avoid creating a global
        // lock here.
        //
        // This function must ensure that the final result is a valid cache in the presence of
        // multiple entries.
        //
        // Necessary font load de-duping is the responsibility of actual font resolution mechanisms.
        val currentTypefaceResult = try {
            resolveTypeface { finalResult ->
                // may this after runCached returns, or immediately if the typeface is immediately
                // available without dispatch

                // this converts an async (state) result to an immutable (val) result to optimize
                // future lookups
                synchronized(lock) {
                    resultCache.put(typefaceRequest, finalResult)
                }
            }
        } catch (cause: Exception) {
            throw IllegalStateException("Could not load font", cause)
        }
        synchronized(lock) {
            // async result may have completed prior to this block entering, do not overwrite
            // final results
            if (resultCache.get(typefaceRequest) == null) {
                resultCache.put(typefaceRequest, currentTypefaceResult)
            }
        }
        return currentTypefaceResult
    }

    fun preWarmCache(
        typefaceRequests: List<TypefaceRequest>,
        resolveTypeface: (TypefaceRequest) -> TypefaceResult
    ) {
        for (i in typefaceRequests.indices) {
            val typeRequest = typefaceRequests[i]

            val prior = synchronized(lock) { resultCache.get(typeRequest) }
            if (prior != null) continue

            val next = try {
                resolveTypeface(typeRequest)
            } catch (cause: Exception) {
                throw IllegalStateException("Could not load font", cause)
            }

            // only cache immutable, should not reach as FontListFontFamilyTypefaceAdapter already
            // has async fonts in permanent cache
            if (next is TypefaceResult.Async) continue

            synchronized(lock) {
                resultCache.put(typeRequest, next)
            }
        }
    }

    // @VisibleForTesting
    internal fun get(typefaceRequest: TypefaceRequest) = synchronized(lock) {
        resultCache.get(typefaceRequest)
    }

    // @VisibleForTesting
    internal val size: Int
        get() = synchronized(lock) {
            resultCache.size
        }
}