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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Allow converting between a Font.ResourceLoader and a FontLoader for deprecated APIs
 */
@Suppress("DEPRECATION")
private class DelegatingFontLoaderForDeprecatedUsage(
    val loader: Font.ResourceLoader
) : FontLoader {

    // never consider these reusable for caching
    override val cacheKey: Any = Any()

    override fun loadBlocking(font: Font): Any = loader.load(font)

    override suspend fun awaitLoad(font: Font): Any = withContext(Dispatchers.IO) {
        loader.load(font)
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalTextApi::class)
@Deprecated("This exists to bridge existing Font.ResourceLoader APIs, and should be " +
    "removed with them",
    replaceWith = ReplaceWith("createFontFamilyResolver()"),
)
internal fun createFontFamilyResolver(
    fontResourceLoader: Font.ResourceLoader
): FontFamily.Resolver {
    return FontFamilyResolverImpl(DelegatingFontLoaderForDeprecatedUsage(fontResourceLoader))
}