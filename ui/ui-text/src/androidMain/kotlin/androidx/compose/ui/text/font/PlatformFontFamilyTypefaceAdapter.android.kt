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
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.platform.AndroidTypeface

@ExperimentalTextApi
internal actual class PlatformFontFamilyTypefaceAdapter : FontFamilyTypefaceAdapter {
    override fun resolve(
        typefaceRequest: TypefaceRequest,
        resourceLoader: Font.ResourceLoader,
        onAsyncCompletion: (TypefaceResult.Immutable) -> Unit
    ): TypefaceResult? {
        val result: Typeface = when (typefaceRequest.fontFamily) {
            null, is DefaultFontFamily -> create(
                null,
                typefaceRequest.fontWeight,
                typefaceRequest.fontStyle
            )
            is GenericFontFamily -> create(
                typefaceRequest.fontFamily.name,
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

    /**
     * Creates a Typeface object based on the system installed fonts. [genericFontFamily] is used
     * to define the main family to create the Typeface such as serif, sans-serif.
     *
     * [fontWeight] is used to define the thickness of the Typeface. Before Android 28 font weight
     * cannot be defined therefore this function assumes anything at and above [FontWeight.W600]
     * is bold and any value less than [FontWeight.W600] is normal.
     *
     * @param genericFontFamily generic font family name such as serif, sans-serif
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     */
    private fun create(
        genericFontFamily: String? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal
    ): Typeface {
        if (fontStyle == FontStyle.Normal &&
            fontWeight == FontWeight.Normal &&
            genericFontFamily.isNullOrEmpty()
        ) {
            return Typeface.DEFAULT
        }

        return if (Build.VERSION.SDK_INT < 28) {
            val targetStyle = getAndroidTypefaceStyle(fontWeight, fontStyle)
            if (genericFontFamily.isNullOrEmpty()) {
                Typeface.defaultFromStyle(targetStyle)
            } else {
                Typeface.create(genericFontFamily, targetStyle)
            }
        } else {
            val familyTypeface = if (genericFontFamily == null) {
                Typeface.DEFAULT
            } else {
                Typeface.create(genericFontFamily, Typeface.NORMAL)
            }

            TypefaceHelperMethodsApi28.create(
                familyTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.Italic
            )
        }
    }
}