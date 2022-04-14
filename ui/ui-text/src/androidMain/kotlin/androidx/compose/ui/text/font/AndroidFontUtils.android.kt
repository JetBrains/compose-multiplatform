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