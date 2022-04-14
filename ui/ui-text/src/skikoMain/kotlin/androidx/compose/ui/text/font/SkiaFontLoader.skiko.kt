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

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.compose.ui.text.platform.FontCache
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.platform.PlatformFont
import org.jetbrains.skia.paragraph.FontCollection

internal class SkiaFontLoader(
    private val fontCache: FontCache = FontCache()
) : PlatformFontLoader {

    val fontCollection: FontCollection
        get() = fontCache.fonts

    @OptIn(ExperimentalTextApi::class)
    override fun loadBlocking(font: Font): FontLoadResult? {
        if (font !is PlatformFont) {
            if (font.loadingStrategy != OptionalLocal) {
                throw IllegalArgumentException("Unsupported font type: $font")
            }
            return null
        }

        return when (font.loadingStrategy) {
            Blocking -> fontCache.load(font)
            OptionalLocal -> kotlin.runCatching { fontCache.load(font) }.getOrNull()
            Async -> throw UnsupportedOperationException("Unsupported Async font load path")
            else -> throw IllegalArgumentException(
                "Unknown loading type ${font.loadingStrategy}"
            )
        }
    }

    internal fun loadPlatformTypes(
        fontFamily: FontFamily,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal
    ): FontLoadResult = fontCache.loadPlatformTypes(fontFamily, fontWeight, fontStyle)

    override suspend fun awaitLoad(font: Font): FontLoadResult? {
        // TODO: This should actually do async loading, but for now desktop only supports local
        //  fonts which are allowed to block during loading.

        // When desktop is extended to allow async font resource declarations, this needs updated.
        return loadBlocking(font)
    }

    override val cacheKey: Any = fontCache // results are valid for all shared caches
}