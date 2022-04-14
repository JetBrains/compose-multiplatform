/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.TypefaceHelperMethodsApi28
import androidx.compose.ui.text.font.getAndroidTypefaceStyle

/**
 * An implementation of [AndroidTypeface] for [GenericFontFamily]
 */
@Deprecated("This path for preloading loading fonts is not supported.")
internal class AndroidGenericFontFamilyTypeface(
    fontFamily: GenericFontFamily
) : AndroidTypeface {

    override val fontFamily: FontFamily = fontFamily

    override fun getNativeTypeface(
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        synthesis: FontSynthesis
    ): Typeface = buildStyledTypeface(fontWeight, fontStyle)

    // Platform never return null with Typeface.create
    private val nativeTypeface = Typeface.create(fontFamily.name, Typeface.NORMAL)!!

    private fun buildStyledTypeface(fontWeight: FontWeight, fontStyle: FontStyle) =
        if (Build.VERSION.SDK_INT < 28) {
            Typeface.create(
                nativeTypeface,
                getAndroidTypefaceStyle(fontWeight, fontStyle)
            )
        } else {
            TypefaceHelperMethodsApi28.create(
                nativeTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.Italic
            )
        }
}