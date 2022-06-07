/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * The interface of the font resource.
 *
 * @see ResourceFont
 */
@Immutable
interface Font {
    /**
     * The weight of the font. The system uses this to match a font to a font request
     * that is given in a [androidx.compose.ui.text.SpanStyle].
     */
    val weight: FontWeight

    /**
     * The style of the font, normal or italic. The system uses this to match a font to a
     * font request that is given in a [androidx.compose.ui.text.SpanStyle].
     */
    val style: FontStyle

    /**
     * Interface used to load a font resource.
     */
    @Deprecated(
        "Replaced with FontFamily.Resolver during the introduction of async fonts, " +
            "all usages should be replaced. Custom subclasses can be converted into a " +
            "FontFamily.Resolver by calling createFontFamilyResolver(myFontFamilyResolver, context)"
    )
    interface ResourceLoader {
        /**
         * Loads resource represented by the [Font] object.
         *
         * @throws Exception if font cannot be loaded
         * @throws IllegalStateException if font cannot be loaded
         * @param font [Font] to be loaded
         * @return platform specific typeface
         */
        @Deprecated(
            "Replaced by FontFamily.Resolver, this method should not be called",
            ReplaceWith("FontFamily.Resolver.resolve(font, )"),
        )
        fun load(font: Font): Any
    }

    /**
     * Loading strategy for this font.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTextApi
    @ExperimentalTextApi
    val loadingStrategy: FontLoadingStrategy

    companion object {
        /**
         * This is the global timeout for fetching an [FontLoadingStrategy.Async] font.
         *
         * This defines the "loading" window for a font. After this timeout, a font load may no
         * longer trigger text reflow and is considered "resolved."
         *
         * Each async font is given separate loading window and goes through these states:
         *
         * ```
         * (initial) -> (loading with timeout) -> (resolved)
         * ```
         *
         * - In the initial state, a fallback typeface is used to display text, which will reflow if
         * the font successfully loads.
         * - In the loading state, the font continues to use the fallback typeface and may cause one
         * text reflow by finishing load. After a successful load it is considered resolved and will
         * not cause another text reflow.
         * - If the font fails to load by the timeout, the failure is permanent, and the font will
         * never attempt to load again. Failure never causes text reflow.
         *
         * After a font is in resolved, it will never cause text reflow unless it is evicted from
         * the font cache and re-enters initial.
         *
         * This timeout is not configurable, and timers are maintained globally.
         */
        @ExperimentalTextApi
        internal const val MaximumAsyncTimeout = 15_000L
    }
}

/**
 * Interface used to load a font resource into a platform-specific typeface.
 */
internal interface PlatformFontLoader {
    /**
     * Loads the resource represented by the [Font] in a blocking manner for use in the current
     * frame.
     *
     * This method may safely throw if a font fails to load, or return null.
     *
     * This method will be called on a UI-critical thread, however the font has been determined
     * to be critical to the current frame display and blocking for file system reads is
     * permitted.
     *
     * @throws Exception subclass may optionally be thrown if font cannot be loaded
     * @param font [Font] to be loaded
     * @return platform specific typeface, or null if not available
     */
    fun loadBlocking(font: Font): Any?

    /**
     * Loads resource represented by the [Font] object in a non-blocking manner which causes
     * text reflow when the font resolves.
     *
     * This method may safely throw if the font cannot be loaded, or return null.
     *
     * This method will be called on a UI-critical thread and should not block the thread beyond
     * loading local fonts from disk. Loading fonts from sources slower than the local file
     * system such as a network access should not block the calling thread.
     *
     * @throws Exception subclass may optionally be thrown if font cannot be loaded
     * @param font [Font] to be loaded
     * @return platform specific typeface, or null if not available
     */
    suspend fun awaitLoad(font: Font): Any?

    /**
     * If this loader returns different results for the same [Font] than the platform default
     * loader return a non-null object that uniquely identifies this loader for caching. This
     * cache key will be retained in global maps, and should ensure that it does not create a
     * memory leak.
     *
     * Loaders that return the same results for all fonts as the platform default may return
     * null.
     *
     * This cache key ensures that [FontFamily.Resolver] can lookup cache
     * results per-loader.
     */
    val cacheKey: Any?
}

/**
 * Defines a font to be used while rendering text with resource ID.
 *
 * @sample androidx.compose.ui.text.samples.CustomFontFamilySample
 *
 * @param resId The resource ID of the font file in font resources. i.e. "R.font.myfont".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.TextStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.TextStyle].
 * @param loadingStrategy Load strategy for this font
 *
 * @see FontFamily
 */
@OptIn(ExperimentalTextApi::class)
class ResourceFont internal constructor(
    val resId: Int,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
    loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Async
) : Font {

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET", "CanBePrimaryConstructorProperty")
    @get:ExperimentalTextApi
    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = loadingStrategy

    fun copy(
        resId: Int = this.resId,
        weight: FontWeight = this.weight,
        style: FontStyle = this.style
    ): ResourceFont = copy(resId, weight, style, loadingStrategy = loadingStrategy)

    @ExperimentalTextApi
    fun copy(
        resId: Int = this.resId,
        weight: FontWeight = this.weight,
        style: FontStyle = this.style,
        loadingStrategy: FontLoadingStrategy = this.loadingStrategy
    ): ResourceFont {
        return ResourceFont(
            resId = resId,
            weight = weight,
            style = style,
            loadingStrategy = loadingStrategy
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceFont) return false
        if (resId != other.resId) return false
        if (weight != other.weight) return false
        if (style != other.style) return false
        if (loadingStrategy != other.loadingStrategy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = resId
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + loadingStrategy.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResourceFont(resId=$resId, weight=$weight, style=$style, " +
            "loadingStrategy=$loadingStrategy)"
    }
}

/**
 * Creates a Font with using resource ID.
 *
 * By default, this will load fonts using [FontLoadingStrategy.Blocking], which blocks the first
 * frame they are used until the font is loaded. This is the correct behavior for small fonts
 * available locally.
 *
 * @param resId The resource ID of the font file in font resources. i.e. "R.font.myfont".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * Fonts made with this factory are local fonts, and will block the first frame for loading. To
 * allow async font loading use [Font(resId, weight, style, isLocal)][Font]
 *
 * @see FontFamily
 */
// TODO(b/219783755): Remove this when safe after Compose 1.3
@Deprecated(
    "Maintained for binary compatibility until Compose 1.3.",
    replaceWith = ReplaceWith(
        "Font(resId, weight, style)"
    ),
    DeprecationLevel.HIDDEN
)
@Stable
fun Font(
    resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = ResourceFont(resId, weight, style, loadingStrategy = FontLoadingStrategy.Blocking)

/**
 * Creates a Font with using resource ID.
 *
 * Allows control over [FontLoadingStrategy] strategy. You may supply
 * [FontLoadingStrategy.Blocking], or [FontLoadingStrategy.OptionalLocal] for fonts that are
 * expected on the first frame.
 *
 * [FontLoadingStrategy.Async], will load the font in the background and cause text reflow when
 * loading completes. Fonts loaded from a remote source via resources should use
 * [FontLoadingStrategy.Async].
 *
 * @param resId The resource ID of the font file in font resources. i.e. "R.font.myfont".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param loadingStrategy Load strategy for this font, may be async for async resource fonts
 *
 * @see FontFamily
 */
@Stable
fun Font(
    resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking
): Font = ResourceFont(resId, weight, style, loadingStrategy)

/**
 * Create a [FontFamily] from this single [Font].
 */
@Stable
fun Font.toFontFamily() = FontFamily(this)
