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

import androidx.compose.ui.text.platform.FontCache
import androidx.compose.ui.text.ExperimentalTextApi
import kotlin.coroutines.CoroutineContext

/**
 * Create a new fontFamilyResolver for use outside of composition context
 *
 * Example usages:
 * - Before starting compose to preload fonts
 * - Creating Paragraph objects on background thread
 *
 * Usages inside of Composition should use LocalFontFamilyResolver.current
 */
@OptIn(ExperimentalTextApi::class)
fun createFontFamilyResolver(): FontFamily.Resolver {
    return FontFamilyResolverImpl(SkiaFontLoader())
}

/**
 * Create a new fontFamilyResolver for use outside of composition context with a coroutine context.
 *
 * Example usages:
 * - Before starting compose to preload fonts
 * - Creating Paragraph objects on background thread
 * - Configuring LocalFontFamilyResolver with a different CoroutineScope
 *
 * Usages inside of Composition should use LocalFontFamilyResolver.current
 *
 * Any [kotlinx.coroutines.CoroutineExceptionHandler] provided will be called with
 * exceptions related to fallback font loading. These exceptions are not fatal, and indicate
 * that font fallback continued to the next font load.
 *
 * If no [kotlinx.coroutines.CoroutineExceptionHandler] is provided, a default implementation will
 * be added that ignores all exceptions.
 *
 * @param coroutineContext context to launch async requests in during resolution.
 */
@ExperimentalTextApi
fun createFontFamilyResolver(
    coroutineContext: CoroutineContext
): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        SkiaFontLoader(),
        PlatformResolveInterceptor.Default,
        GlobalTypefaceRequestCache,
        FontListFontFamilyTypefaceAdapter(
            GlobalAsyncTypefaceCache,
            coroutineContext
        )
    )
}
/**
 * For bridging between FontLoader and FontFamily.ResourceLoader. Can remove with FontLoader.
 */
@OptIn(ExperimentalTextApi::class)
internal fun createFontFamilyResolver(fontCache: FontCache): FontFamily.Resolver {
    return FontFamilyResolverImpl(SkiaFontLoader(fontCache))
}