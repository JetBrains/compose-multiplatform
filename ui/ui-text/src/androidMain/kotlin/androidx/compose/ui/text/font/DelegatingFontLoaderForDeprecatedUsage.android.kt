/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Bridge between subclasses of Font.ResourceLoader and the new FontFamily.Resolver API.
 *
 * To use add as a CompositionLocal replacing the default FontFamily.Resolver:
 *
 * ```
 * LocalFontFamilyResolver provides createFontFamilyResolver(myFontResourceLoader, context)
 * ```
 *
 * This FontFamily.Resolver is capable of loading all fonts that the default FontFamily.Resolver is.
 * In addition, it will delegate all resource fonts to the provided Font.ResourceLoader, preserving
 * the behavior of Compose 1.0.
 *
 * This method will be removed by Compose 2.0, and callers should migrate to using [AndroidFont] to
 * implement the same behavior using font fallback chains.
 *
 * A FontFamily.Resolver created this way will not share caches with other FontFamily.Resolvers.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalTextApi::class)
@Deprecated("This exists to bridge existing Font.ResourceLoader subclasses to be used as a" +
    "FontFamily.ResourceLoader during upgrade.",
    replaceWith = ReplaceWith("createFontFamilyResolver()"),
)
fun createFontFamilyResolver(
    fontResourceLoader: Font.ResourceLoader,
    context: Context
): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        DelegatingFontLoaderForBridgeUsage(fontResourceLoader, context.applicationContext)
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalTextApi::class)
@Deprecated("This exists to bridge existing Font.ResourceLoader APIs, and should be " +
    "removed with them",
    replaceWith = ReplaceWith("createFontFamilyResolver()"),
)
internal actual fun createFontFamilyResolver(
    fontResourceLoader: Font.ResourceLoader
): FontFamily.Resolver {
    return FontFamilyResolverImpl(DelegatingFontLoaderForDeprecatedUsage(fontResourceLoader))
}

/**
 * Allow converting between a Font.ResourceLoader and a FontLoader for deprecated APIs.
 *
 * This class is not able to load all fonts and exists for API interop.
 *
 * If you are experiencing a crash in custom text code, replace [Font.ResourceLoader] with a
 * [FontFamily.Resolver] using [createFontFamilyResolver] (Font.ResourceLoader, Context).
 */
@Suppress("DEPRECATION")
internal class DelegatingFontLoaderForDeprecatedUsage(
    internal val loader: Font.ResourceLoader
) : PlatformFontLoader {

    // never consider these reusable for caching
    override val cacheKey: Any = Any()

    override fun loadBlocking(font: Font): Any = loader.load(font)

    override suspend fun awaitLoad(font: Font): Any = loader.load(font)
}

/**
 * Allow converting between a Font.ResourceLoader and a FontLoader for real usage by apps that
 * subclassed Font.ResourceLoader in Compose 1.0.
 *
 * This loader is capable of performing all font loads as an upgrade bridge.
 */
@Suppress("DEPRECATION")
internal class DelegatingFontLoaderForBridgeUsage(
    internal val loader: Font.ResourceLoader,
    private val context: Context
) : PlatformFontLoader {
    // never consider these reusable for caching
    override val cacheKey: Any = Any()

    override fun loadBlocking(font: Font): Any? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.loadBlocking(context, font)
            else -> loader.load(font)
        }
    }

    override suspend fun awaitLoad(font: Font): Any? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.awaitLoad(context, font)
            else -> loader.load(font)
        }
    }
}