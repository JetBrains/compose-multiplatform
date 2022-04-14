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

import androidx.compose.ui.text.ExperimentalTextApi

@ExperimentalTextApi
internal actual class PlatformFontFamilyTypefaceAdapter actual constructor() :
    FontFamilyTypefaceAdapter {

    override fun resolve(
        typefaceRequest: TypefaceRequest,
        platformFontLoader: PlatformFontLoader,
        onAsyncCompletion: (TypefaceResult.Immutable) -> Unit,
        createDefaultTypeface: (TypefaceRequest) -> Any
    ): TypefaceResult? {
        if (typefaceRequest.fontFamily is FontListFontFamily) return null
        val skiaFontLoader = (platformFontLoader as SkiaFontLoader)
        val result = skiaFontLoader.loadPlatformTypes(
            typefaceRequest.fontFamily ?: FontFamily.Default,
            typefaceRequest.fontWeight,
            typefaceRequest.fontStyle
        )
        return TypefaceResult.Immutable(result)
    }
}