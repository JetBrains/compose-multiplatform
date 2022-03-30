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

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.platform.AndroidTypeface

@ExperimentalTextApi
internal actual class PlatformFontFamilyTypefaceAdapter : FontFamilyTypefaceAdapter {

    private val platformTypefaceResolver = PlatformTypefaces()

    override fun resolve(
        typefaceRequest: TypefaceRequest,
        platformFontLoader: PlatformFontLoader,
        onAsyncCompletion: (TypefaceResult.Immutable) -> Unit,
        createDefaultTypeface: (TypefaceRequest) -> Any
    ): TypefaceResult? {
        val result: Typeface = when (typefaceRequest.fontFamily) {
            null, is DefaultFontFamily -> platformTypefaceResolver.createDefault(
                typefaceRequest.fontWeight,
                typefaceRequest.fontStyle
            )
            is GenericFontFamily -> platformTypefaceResolver.createNamed(
                typefaceRequest.fontFamily,
                typefaceRequest.fontWeight,
                typefaceRequest.fontStyle
            )
            is LoadedFontFamily -> {
                (typefaceRequest.fontFamily.typeface as AndroidTypeface).getNativeTypeface(
                    typefaceRequest.fontWeight,
                    typefaceRequest.fontStyle,
                    typefaceRequest.fontSynthesis
                )
            }
            else -> return null // exit to make result non-null
        }
        return TypefaceResult.Immutable(result)
    }
}
