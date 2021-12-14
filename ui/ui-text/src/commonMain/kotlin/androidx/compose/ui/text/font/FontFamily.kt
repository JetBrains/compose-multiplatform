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
import androidx.compose.runtime.State
import androidx.compose.ui.text.ExperimentalTextApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The primary typography interface for Compose applications.
 *
 * // TODO(seanmcq): Comment here about how it all works
 * @see FontListFontFamily
 * @see GenericFontFamily
 * @see FontFamily.Resolver
 */
@Immutable
sealed class FontFamily(canLoadSynchronously: Boolean) {

    /**
     * Main interface for resolving [FontFamily] into a platform-specific typeface for use in
     * Compose-based applications.
     *
     * Fonts are loaded via [Resolver.resolve] from a FontFamily and a type request, and return a
     * platform-specific typeface.
     *
     * Fonts may be preloaded by calling [Resolver.preload] to avoid text reflow when async fonts
     * load.
     */
    @ExperimentalTextApi
    sealed interface Resolver {
        /**
         * Preloading resolves and caches all fonts reachable in a [FontFamily].
         *
         * Fonts are consider reachable if they are included in the fallback chain for any call to
         * [resolve].
         *
         * This method will suspend until:
         *
         * 1. All [FontLoad.Async] fonts that are reachable have completed loading, or failed to
         * load
         * 2. All reachable fonts in the fallback chain have been loaded and inserted into the
         * cache
         *
         * After returning, all fonts with [FontLoad.Async] and [FontLoad.OptionalLocal] will be
         * permanently cached. In contrast to [resolve] this method will throw when a reachable
         * [FontLoad.Async] font fails to resolve.
         *
         * All fonts with [FontLoad.Blocking] will be cached with normal eviction rules.
         *
         * @throws IllegalStateException if any reachable font fails to load
         * @param fontFamily the family to resolve all fonts from
         * @param resourceLoader to load resolved fonts, typically pass LocalFontLoader.current
         */
        suspend fun preload(
            fontFamily: FontFamily,
            resourceLoader: Font.ResourceLoader
        )

        /**
         * Resolves a typeface using any appropriate logic for the [FontFamily].
         *
         * [FontListFontFamily] will always resolve using fallback chains and load using
         * [Font.ResourceLoader].
         *
         * Platform specific [FontFamily] will resolve according to platform behavior, as documented
         * for each [FontFamily].
         *
         * @param resourceLoader to load resolved fonts into typefaces
         * @param fontFamily family to resolve
         * @param fontWeight desired font weight
         * @param fontStyle desired font style
         * @param fontSynthesis configuration for font synthesis
         * @throws IllegalStateException if the FontFamily cannot resolve a to a typeface
         * @return platform-specific Typeface such as [android.graphics.Typeface]
         */
        fun resolve(
            resourceLoader: Font.ResourceLoader,
            fontFamily: FontFamily? = null,
            fontWeight: FontWeight = FontWeight.Normal,
            fontStyle: FontStyle = FontStyle.Normal,
            fontSynthesis: FontSynthesis = FontSynthesis.All
        ): State<Any>

        /**
         * Set the coroutine context used for loading async fonts.
         *
         * Any current fonts loading will continue in the prior context. New requests will run in
         * the new context.
         *
         * Any [kotlinx.coroutines.CoroutineExceptionHandler] provided will be called with
         * exceptions related to fallback font loading. These exceptions are not fatal, and indicate
         * that font fallback continued to the next font load.
         *
         * 1. Any [kotlinx.coroutines.Job] will be removed
         * 2. If no [kotlinx.coroutines.CoroutineExceptionHandler] is provided, a default
         * implementation will be added that ignores all exceptions.
         *
         * @param context to run load requests on
         */
        fun setAsyncLoadContext(context: CoroutineContext = EmptyCoroutineContext)
    }
    companion object {
        /**
         * The platform default font.
         */
        val Default: SystemFontFamily = DefaultFontFamily()

        /**
         * Font family with low contrast and plain stroke endings.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilySansSerifSample
         *
         * See [CSS sans-serif](https://www.w3.org/TR/css-fonts-3/#sans-serif)
         */
        val SansSerif = GenericFontFamily("sans-serif")

        /**
         * The formal text style for scripts.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilySerifSample
         *
         * See [CSS serif](https://www.w3.org/TR/css-fonts-3/#serif)
         */
        val Serif = GenericFontFamily("serif")

        /**
         * Font family where glyphs have the same fixed width.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilyMonospaceSample
         *
         * See [CSS monospace](https://www.w3.org/TR/css-fonts-3/#monospace)
         */
        val Monospace = GenericFontFamily("monospace")

        /**
         * Cursive, hand-written like font family.
         *
         * If the device doesn't support this font family, the system will fallback to the
         * default font.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilyCursiveSample
         *
         * See [CSS cursive](https://www.w3.org/TR/css-fonts-3/#cursive)
         */
        val Cursive = GenericFontFamily("cursive")
    }

    @Suppress("CanBePrimaryConstructorProperty") // for deprecation
    @get:Deprecated(
        message = "Unused property that has no meaning. Do not use.",
        level = DeprecationLevel.ERROR
    )
    val canLoadSynchronously = canLoadSynchronously
}

/**
 * A base class of [FontFamily]s that is created from file sources.
 */
sealed class FileBasedFontFamily : FontFamily(false)

/**
 * A base class of [FontFamily]s installed on the system.
 */
sealed class SystemFontFamily : FontFamily(true)

/**
 * Defines a font family with list of [Font].
 *
 * @sample androidx.compose.ui.text.samples.FontFamilySansSerifSample
 * @sample androidx.compose.ui.text.samples.CustomFontFamilySample
 */
@Immutable
class FontListFontFamily internal constructor(
    fonts: List<Font>
) : FileBasedFontFamily(), List<Font> by fonts {
    init {
        check(fonts.isNotEmpty()) { "At least one font should be passed to FontFamily" }
    }

    /**
     * The fallback list of fonts used for resolving typefaces for this FontFamily.
     */
    val fonts: List<Font> = ArrayList(fonts)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontListFontFamily) return false
        if (fonts != other.fonts) return false
        return true
    }

    override fun hashCode(): Int {
        return fonts.hashCode()
    }

    override fun toString(): String {
        return "FontListFontFamily(fonts=$fonts)"
    }
}

/**
 * Defines a font family with an generic font family name.
 *
 * If the platform cannot find the passed generic font family, use the platform default one.
 *
 * @param name a generic font family name, e.g. "serif", "sans-serif"
 * @see FontFamily.SansSerif
 * @see FontFamily.Serif
 * @see FontFamily.Monospace
 * @see FontFamily.Cursive
 */
@Immutable
class GenericFontFamily internal constructor(val name: String) : SystemFontFamily()

/**
 * Defines a default font family.
 */
@Immutable
internal class DefaultFontFamily internal constructor() : SystemFontFamily()

/**
 * Defines a font family that is already loaded Typeface.
 *
 * @param typeface A typeface instance.
 */
class LoadedFontFamily internal constructor(val typeface: Typeface) : FontFamily(true) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoadedFontFamily) return false
        if (typeface != other.typeface) return false
        return true
    }

    override fun hashCode(): Int {
        return typeface.hashCode()
    }

    override fun toString(): String {
        return "LoadedFontFamily(typeface=$typeface)"
    }
}

/**
 * Construct a font family that contains list of custom font files.
 *
 * @param fonts list of font files
 */
@Stable
fun FontFamily(fonts: List<Font>): FontFamily = FontListFontFamily(fonts)

/**
 * Construct a font family that contains list of custom font files.
 *
 * @param fonts list of font files
 */
@Stable
fun FontFamily(vararg fonts: Font): FontFamily = FontListFontFamily(fonts.asList())

/**
 * Construct a font family that contains loaded font family: Typeface.
 *
 * @param typeface A typeface instance.
 */
@Stable
fun FontFamily(typeface: Typeface): FontFamily = LoadedFontFamily(typeface)
