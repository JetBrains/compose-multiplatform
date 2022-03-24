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
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal val FontWeight.Companion.AndroidBold
    get() = W600

/**
 * Lookup style used by [android.graphics.Typeface].
 *
 * May return one of:
 * - [Typeface.BOLD_ITALIC]
 * - [Typeface.BOLD]
 * - [Typeface.ITALIC]
 * - [Typeface.NORMAL]
 */
internal fun getAndroidTypefaceStyle(fontWeight: FontWeight, fontStyle: FontStyle): Int {
    val isBold = fontWeight >= FontWeight.AndroidBold
    val isItalic = fontStyle == FontStyle.Italic
    return getAndroidTypefaceStyle(isBold, isItalic)
}

/**
 * Lookup android typeface style without requiring a [FontWeight] or [FontStyle] object.
 *
 * May return one of:
 * - [Typeface.BOLD_ITALIC]
 * - [Typeface.BOLD]
 * - [Typeface.ITALIC]
 * - [Typeface.NORMAL]
 */
internal fun getAndroidTypefaceStyle(isBold: Boolean, isItalic: Boolean): Int {
    return if (isItalic && isBold) {
        Typeface.BOLD_ITALIC
    } else if (isBold) {
        Typeface.BOLD
    } else if (isItalic) {
        Typeface.ITALIC
    } else {
        Typeface.NORMAL
    }
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
internal fun createAndroidTypeface(
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

/**
 * Creates a Typeface using Typface.create(Typeface, ...) with API level branching.
 */
internal fun createAndroidTypeface(
    typeface: Typeface,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal
): Typeface {
    return if (Build.VERSION.SDK_INT < 28) {
        val targetStyle = getAndroidTypefaceStyle(fontWeight, fontStyle)
        Typeface.create(typeface, targetStyle)
    } else {
        TypefaceHelperMethodsApi28.create(
            typeface,
            fontWeight.weight,
            fontStyle == FontStyle.Italic
        )
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(28)
internal object TypefaceHelperMethodsApi28 {
    @RequiresApi(28)
    @DoNotInline
    fun create(typeface: Typeface, finalFontWeight: Int, finalFontStyle: Boolean) =
        Typeface.create(typeface, finalFontWeight, finalFontStyle)
}