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

/**
 * Apply android font-synthesis rules.
 *
 * 1. Fake bold is applied if requestedWeight >= 600 and fontWeight < 600
 * 2. Fake italic is applied if requestedStyle does not match font style
 *
 * Android does not support fake un-bold.
 */
internal actual fun FontSynthesis.synthesizeTypeface(
    typeface: Any,
    font: Font,
    requestedWeight: FontWeight,
    requestedStyle: FontStyle
): Any {
    if (typeface !is Typeface) return typeface

    val synthesizeWeight = isWeightOn && font.weight != requestedWeight &&
        (requestedWeight >= FontWeight.AndroidBold && font.weight < FontWeight.AndroidBold)

    val synthesizeStyle = isStyleOn && requestedStyle != font.style

    if (!synthesizeStyle && !synthesizeWeight) return typeface

    return if (Build.VERSION.SDK_INT < 28) {
        val targetStyle = getAndroidTypefaceStyle(
            isBold = synthesizeWeight,
            isItalic = synthesizeStyle && requestedStyle == FontStyle.Italic
        )
        Typeface.create(typeface, targetStyle)
    } else {
        val finalFontWeight = if (synthesizeWeight) {
            // if we want to synthesize weight, we send the requested fontWeight
            requestedWeight.weight
        } else {
            // if we do not want to synthesize weight, we keep the loaded font weight
            font.weight.weight
        }

        val finalFontStyle = if (synthesizeStyle) {
            // if we want to synthesize style, we send the requested fontStyle
            requestedStyle == FontStyle.Italic
        } else {
            // if we do not want to synthesize style, we keep the loaded font style
            font.style == FontStyle.Italic
        }
        TypefaceHelperMethodsApi28.create(typeface, finalFontWeight, finalFontStyle)
    }
}