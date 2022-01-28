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

/**
 * Used by [FontFamilyResolverImpl] to dispatch requests for either [FontListFontFamily] resolution
 * or delegate all other [FontFamily] requests to [PlatformFontFamilyTypefaceAdapter].
 *
 * - [FontListFontFamily] will be resolved by [FontListFontFamilyTypefaceAdapter]
 * - All other [FontFamily] will be resolved by [PlatformFontFamilyTypefaceAdapter]
 */
internal interface FontFamilyTypefaceAdapter {

    /**
     * Resolve a typefaceRequest to a typeface result.
     *
     * Results are [TypefaceResult.Async] only if a font fallback chain and reflow is required,
     * otherwise return [TypefaceResult.Immutable].
     *
     * Immutable results may be cached forever without any potential for change.
     *
     * Async results must update their caller via writes to the state object, and should ensure that
     * [onAsyncCompletion] is called when the font resolution completes when an immutable final
     * result is available to allow caches to update.
     *
     * @param typefaceRequest unique description of this typeface request
     * @param platformFontLoader font loader used for loading typefaces from [Font] descriptors
     * @param onAsyncCompletion will be called (on an arbitrary thread) when
     * [FontLoadingStrategy.Async] fonts reach their final resolved state, may be called prior to
     * resolve returning
     * @return result of typeface lookup.
     */
    fun resolve(
        typefaceRequest: TypefaceRequest,
        platformFontLoader: PlatformFontLoader,
        onAsyncCompletion: ((TypefaceResult.Immutable) -> Unit),
        createDefaultTypeface: (TypefaceRequest) -> Any
    ): TypefaceResult?
}
