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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.caches.LruCache
import androidx.compose.ui.text.caches.SimpleArrayMap
import androidx.compose.ui.text.fastDistinctBy
import androidx.compose.ui.text.fastFilter
import androidx.compose.ui.text.platform.createSynchronizedObject
import androidx.compose.ui.text.platform.synchronized
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

@ExperimentalTextApi
internal class FontListFontFamilyTypefaceAdapter(
    private val asyncTypefaceCache: AsyncTypefaceCache = AsyncTypefaceCache(),
    injectedContext: CoroutineContext = EmptyCoroutineContext
) : FontFamilyTypefaceAdapter {

    private var asyncLoadScope: CoroutineScope = CoroutineScope(
        // order is important, we prefer our handler but allow injected to overwrite
        DropExceptionHandler + injectedContext + SupervisorJob(injectedContext[Job])
    )

    suspend fun preload(
        family: FontFamily,
        resourceLoader: PlatformFontLoader
    ) {
        if (family !is FontListFontFamily) return

        val allFonts = family.fonts
        // only preload styles that can be satisfied by async fonts
        val asyncStyles = family.fonts
            .fastFilter { it.loadingStrategy == FontLoadingStrategy.Async }
            .fastMap { it.weight to it.style }
            .fastDistinctBy { it }

        val asyncLoads: MutableList<Font> = mutableListOf()

        asyncStyles.fastForEach { (fontWeight, fontStyle) ->
            val matched = fontMatcher.matchFont(allFonts, fontWeight, fontStyle)
            val typeRequest = TypefaceRequest(
                family,
                fontWeight,
                fontStyle,
                FontSynthesis.All,
                resourceLoader.cacheKey
            )
            // this may be satisfied by non-async font still, which is OK as they'll be cached for
            // immediate lookup by caller
            //
            // only do the permanent cache for results provided via async fonts
            val (asyncFontsToLoad, _) = matched.firstImmediatelyAvailable(
                typeRequest,
                asyncTypefaceCache,
                resourceLoader,
                createDefaultTypeface = { } // unused, no fallback necessary
            )
            if (asyncFontsToLoad != null) {
                asyncLoads.add(asyncFontsToLoad.first())
            }
        }

        return coroutineScope {
            asyncLoads
                .fastDistinctBy { it }
                .fastMap { font ->
                    async {
                        asyncTypefaceCache.runCached(font, resourceLoader, true) {
                            try {
                                withTimeout(Font.MaximumAsyncTimeout) {
                                    resourceLoader.awaitLoad(font)
                                }
                            } catch (cause: Exception) {
                                throw IllegalStateException("Unable to load font $font", cause)
                            } ?: throw IllegalStateException("Unable to load font $font")
                        }
                    }
                }.joinAll()
        }
    }

    override fun resolve(
        typefaceRequest: TypefaceRequest,
        platformFontLoader: PlatformFontLoader,
        onAsyncCompletion: ((TypefaceResult.Immutable) -> Unit),
        createDefaultTypeface: (TypefaceRequest) -> Any
    ): TypefaceResult? {
        if (typefaceRequest.fontFamily !is FontListFontFamily) return null
        val matched = fontMatcher.matchFont(
            typefaceRequest.fontFamily.fonts,
            typefaceRequest.fontWeight,
            typefaceRequest.fontStyle
        )
        val (asyncFontsToLoad, synthesizedTypeface) = matched.firstImmediatelyAvailable(
            typefaceRequest,
            asyncTypefaceCache,
            platformFontLoader,
            createDefaultTypeface
        )
        if (asyncFontsToLoad == null) return TypefaceResult.Immutable(synthesizedTypeface)
        val asyncLoader = AsyncFontListLoader(
            fontList = asyncFontsToLoad,
            initialType = synthesizedTypeface,
            typefaceRequest = typefaceRequest,
            asyncTypefaceCache = asyncTypefaceCache,
            onCompletion = onAsyncCompletion,
            platformFontLoader = platformFontLoader
        )

        // Always launch on whatever scope was set prior to this call, and continue until the load
        // completes.
        // Launch is undispatched, allowing immediate results to complete this frame if they're
        // already loaded or can be loaded in a blocking manner (e.g. from disk).
        asyncLoadScope.launch(start = CoroutineStart.UNDISPATCHED) { asyncLoader.load() }
        return TypefaceResult.Async(asyncLoader)
    }

    companion object {
        val fontMatcher = FontMatcher()
        val DropExceptionHandler = CoroutineExceptionHandler { _, _ ->
            // expected to happen when font load fails during async fallback
            // safe to ignore (or log)
        }
    }
}

/**
 * Find the first typeface that is immediately available, as well as any async fonts that are higher
 * priority in the fallback chain.
 *
 * If the List<Font> returned is non-null, it should be used for async fallback resolution with the
 * current typeface loaded used as the initial typeface.
 *
 * @param typefaceRequest type to load
 * @param asyncTypefaceCache cache for finding pre-loaded async fonts
 * @param platformFontLoader loader for resolving types from fonts
 * @return (async fonts to resolve for fallback) to (a typeface that can display this frame)
 */
@ExperimentalTextApi
private fun List<Font>.firstImmediatelyAvailable(
    typefaceRequest: TypefaceRequest,
    asyncTypefaceCache: AsyncTypefaceCache,
    platformFontLoader: PlatformFontLoader,
    createDefaultTypeface: (TypefaceRequest) -> Any
): Pair<List<Font>?, Any> {
    var asyncFontsToLoad: MutableList<Font>? = null
    for (idx in indices) {
        val font = get(idx)
        when (font.loadingStrategy) {
            FontLoadingStrategy.Blocking -> {
                val result: Any = asyncTypefaceCache.runCachedBlocking(font, platformFontLoader) {
                    try {
                        platformFontLoader.loadBlocking(font)
                    } catch (cause: Exception) {
                        throw IllegalStateException("Unable to load font $font", cause)
                    }
                } ?: throw IllegalStateException("Unable to load font $font")
                return asyncFontsToLoad to
                    typefaceRequest.fontSynthesis.synthesizeTypeface(
                        result,
                        font,
                        typefaceRequest.fontWeight,
                        typefaceRequest.fontStyle,
                    )
            }
            FontLoadingStrategy.OptionalLocal -> {
                val result = asyncTypefaceCache.runCachedBlocking(font, platformFontLoader) {
                    // optional fonts should not throw, but consider it a failed load if they do
                    kotlin.runCatching { platformFontLoader.loadBlocking(font) }.getOrNull()
                }
                if (result != null) {
                    return asyncFontsToLoad to
                        typefaceRequest.fontSynthesis.synthesizeTypeface(
                            result,
                            font,
                            typefaceRequest.fontWeight,
                            typefaceRequest.fontStyle,
                        )
                }
            }
            FontLoadingStrategy.Async -> {
                val cacheResult = asyncTypefaceCache.get(font, platformFontLoader)
                if (cacheResult == null) {
                    if (asyncFontsToLoad == null) {
                        asyncFontsToLoad = mutableListOf(font)
                    } else {
                        asyncFontsToLoad.add(font)
                    }
                } else if (cacheResult.isPermanentFailure) {
                    continue // ignore permanent failure; this font will never load
                } else if (cacheResult.result != null) {
                    // it's not a permanent failure, use it
                    return asyncFontsToLoad to
                        typefaceRequest.fontSynthesis.synthesizeTypeface(
                            cacheResult.result,
                            font,
                            typefaceRequest.fontWeight,
                            typefaceRequest.fontStyle
                        )
                }
            }
            else -> throw IllegalStateException("Unknown font type $font")
        }
    }
    // none of the passed fonts match, fall back to platform font
    val fallbackTypeface = createDefaultTypeface(typefaceRequest)
    return asyncFontsToLoad to fallbackTypeface
}

@OptIn(ExperimentalTextApi::class)
internal class AsyncFontListLoader constructor(
    private val fontList: List<Font>,
    initialType: Any,
    private val typefaceRequest: TypefaceRequest,
    private val asyncTypefaceCache: AsyncTypefaceCache,
    private val onCompletion: (TypefaceResult.Immutable) -> Unit,
    private val platformFontLoader: PlatformFontLoader
) : State<Any> {
    override var value by mutableStateOf(initialType)
        private set

    internal var cacheable = true

    suspend fun load() {
        try {
            fontList.fastForEach { font ->
                // we never have to resolve Blocking or OptionalLocal to complete async resolution.
                // if the fonts before async are OptionalLocal, they must all be null
                // if the fonts before async are Blocking, this request never happens
                // if the fonts after async are Blocking or OptionalLocal, they are already the
                //     fallback value and do not need resolved again
                // therefore, it is not possible for an async load failure early in the chain to
                //     require a new blocking or optional load to resolve
                if (font.loadingStrategy == FontLoadingStrategy.Async) {
                    val typeface = asyncTypefaceCache.runCached(font, platformFontLoader, false) {
                        font.loadWithTimeoutOrNull()
                    }
                    if (typeface != null) {
                        value = typefaceRequest.fontSynthesis.synthesizeTypeface(
                            typeface,
                            font,
                            typefaceRequest.fontWeight,
                            typefaceRequest.fontStyle
                        )
                        return /* done loading on first successful typeface */
                    } else {
                        // check cancellation and yield the thread before trying the next font
                        yield()
                    }
                }
            }
        } finally {
            // if we walked off the end, then the current value is the final result
            val shouldCache = coroutineContext.isActive
            cacheable = false
            onCompletion.invoke(TypefaceResult.Immutable(value, shouldCache))
        }
    }

    /**
     * Load a font in a timeout context and ensure that no exception is thrown to caller coroutine.
     */
    internal suspend fun Font.loadWithTimeoutOrNull(): Any? {
        return try {
            // case 0: load completes - success (non-null)
            // case 1: we timeout - permanent failure (null)
            withTimeoutOrNull(Font.MaximumAsyncTimeout) {
                platformFontLoader.awaitLoad(this@loadWithTimeoutOrNull)
            }
        } catch (cancel: CancellationException) {
            // case 2: callee cancels - permanent failure (null)
            if (coroutineContext.isActive) null else throw cancel
        } catch (uncaughtFontLoadException: Exception) {
            // case 3: callee throws another exception - permanent failure (null)

            // since we're basically acting as a global event loop here, an exception that makes
            // it to us is "uncaught" and should be loggable

            // inform uncaught exception handler of the font load failure, so apps may log if
            // desired

            // note: this error is not fatal, and we will continue
            coroutineContext[CoroutineExceptionHandler]?.handleException(
                coroutineContext,
                IllegalStateException(
                    "Unable to load font ${this@loadWithTimeoutOrNull}",
                    uncaughtFontLoadException
                )
            )
            null
        }
    }
}

/**
 * A cache for saving async typefaces that have been loaded.
 *
 * This stores the non-synthesized type, as returned by the async loader directly.
 *
 * All async failures are cached permanently, while successful typefaces may be evicted from the
 * cache at a fixed size.
 */
@ExperimentalTextApi
internal class AsyncTypefaceCache {
    @kotlin.jvm.JvmInline
    internal value class AsyncTypefaceResult(val result: Any?) {
        val isPermanentFailure: Boolean
            get() = result == null
    }

    private val PermanentFailure = AsyncTypefaceResult(null)

    internal data class Key(val font: Font, val loaderKey: Any?)

    // 16 is based on the LruCache in TypefaceCompat Android, but no firm logic for this size.
    // After loading, fonts are put into the resultCache to allow reading from a kotlin function
    // context, reducing async fonts overhead cache lookup overhead only while cached
    // @GuardedBy("cacheLock")
    private val resultCache = LruCache<Key, AsyncTypefaceResult>(16)
    // failures and preloads are permanent, so they are stored separately
    // @GuardedBy("cacheLock")
    private val permanentCache = SimpleArrayMap<Key, AsyncTypefaceResult>()

    private val cacheLock = createSynchronizedObject()

    fun put(
        font: Font,
        platformFontLoader: PlatformFontLoader,
        result: Any?,
        forever: Boolean = false
    ) {
        val key = Key(font, platformFontLoader.cacheKey)
        synchronized(cacheLock) {
            when {
                result == null -> { permanentCache.put(key, PermanentFailure) }
                forever -> { permanentCache.put(key, AsyncTypefaceResult(result)) }
                else -> { resultCache.put(key, AsyncTypefaceResult(result)) }
            }
        }
    }

    fun get(font: Font, platformFontLoader: PlatformFontLoader): AsyncTypefaceResult? {
        val key = Key(font, platformFontLoader.cacheKey)
        return synchronized(cacheLock) {
            resultCache.get(key) ?: permanentCache[key]
        }
    }

    suspend fun runCached(
        font: Font,
        platformFontLoader: PlatformFontLoader,
        forever: Boolean,
        block: suspend () -> Any?
    ): Any? {
        val key = Key(font, platformFontLoader.cacheKey)
        synchronized(cacheLock) {
            val priorResult = resultCache.get(key) ?: permanentCache[key]
            if (priorResult != null) {
                return priorResult.result
            }
        }
        return block().also {
            synchronized(cacheLock) {
                when {
                    it == null -> {
                        permanentCache.put(key, PermanentFailure)
                    }
                    forever -> {
                        permanentCache.put(key, AsyncTypefaceResult(it))
                    }
                    else -> {
                        resultCache.put(key, AsyncTypefaceResult(it))
                    }
                }
            }
        }
    }

    inline fun runCachedBlocking(
        font: Font,
        platformFontLoader: PlatformFontLoader,
        block: () -> Any?
    ): Any? {
        synchronized(cacheLock) {
            val key = Key(font, platformFontLoader.cacheKey)
            val priorResult = resultCache.get(key) ?: permanentCache[key]
            if (priorResult != null) {
                return priorResult.result
            }
        }
        return block().also {
            put(font, platformFontLoader, it)
        }
    }
}