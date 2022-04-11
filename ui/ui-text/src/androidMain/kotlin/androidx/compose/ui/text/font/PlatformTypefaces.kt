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

import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * Primary internal interface for resolving typefaces from Android platform
 */
internal interface PlatformTypefaces {
    /**
     * Resolve the system default font
     *
     * @param fontWeight weight to load, if available, will fallback
     * @param fontStyle italic or not, will fallback
     */
    fun createDefault(fontWeight: FontWeight, fontStyle: FontStyle): Typeface

    /**
     * Resolve a system named font. Only supports the names in FontFamily.* definitions.
     *
     * @param name name of a font, as specified in FontFamily.*
     * @param fontWeight weight to load, if available, will fallback
     * @param fontStyle italic or not, will fallback
     */
    fun createNamed(name: GenericFontFamily, fontWeight: FontWeight, fontStyle: FontStyle): Typeface

    /**
     * Find a font by name that does not match Typeface.DEFAULT.
     *
     * This is used to figure out if the platform has fonts like "comic-sans" preloaded.
     *
     * @param familyName font to attempt to load from system caches
     * @param weight weight to load, if available, will fallback
     * @param style italic or not, will fallback
     * @return typeface from system cache if available, or null if the system doesn't know this font
     * name
     */
    fun optionalOnDeviceFontFamilyByName(
        familyName: String,
        weight: FontWeight,
        style: FontStyle
    ): Typeface?
}

internal fun PlatformTypefaces(): PlatformTypefaces {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PlatformTypefacesApi28()
    } else {
        PlatformTypefacesApi()
    }
}

@VisibleForTesting
private class PlatformTypefacesApi : PlatformTypefaces {
    override fun createDefault(fontWeight: FontWeight, fontStyle: FontStyle) =
        createAndroidTypefaceUsingTypefaceStyle(null, fontWeight, fontStyle)

    override fun createNamed(
        name: GenericFontFamily,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): Typeface {
        return loadNamedFromTypefaceCacheOrNull(
            getWeightSuffixForFallbackFamilyName(name.name, fontWeight),
            fontWeight,
            fontStyle
        ) ?: createAndroidTypefaceUsingTypefaceStyle(name.name, fontWeight, fontStyle)
    }

    override fun optionalOnDeviceFontFamilyByName(
        familyName: String,
        weight: FontWeight,
        style: FontStyle
    ): Typeface? {
        // if the developer specified one of the named fonts, behave identically to the
        // GenericFontFamily behavior, return the same as createNamed always
        return when (familyName) {
            FontFamily.SansSerif.name -> createNamed(FontFamily.SansSerif, weight, style)
            FontFamily.Serif.name -> createNamed(FontFamily.Serif, weight, style)
            FontFamily.Monospace.name -> createNamed(FontFamily.Monospace, weight, style)
            FontFamily.Cursive.name -> createNamed(FontFamily.Cursive, weight, style)
            else -> loadNamedFromTypefaceCacheOrNull(familyName, weight, style)
        }
    }

    private fun loadNamedFromTypefaceCacheOrNull(
        familyName: String,
        weight: FontWeight,
        style: FontStyle
    ): Typeface? {
        if (familyName.isEmpty()) return null
        val typeface = createAndroidTypefaceUsingTypefaceStyle(familyName, weight, style)
        return typeface.takeIf {
            // Typeface may lookup missed results via either Typeface.DEFAULT or null, check both
            it != Typeface.create(Typeface.DEFAULT, getAndroidTypefaceStyle(weight, style)) &&
                it != createAndroidTypefaceUsingTypefaceStyle(null, weight, style)
        }
    }

    private fun createAndroidTypefaceUsingTypefaceStyle(
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

        val targetStyle = getAndroidTypefaceStyle(fontWeight, fontStyle)
        return if (genericFontFamily.isNullOrEmpty()) {
            Typeface.defaultFromStyle(targetStyle)
        } else {
            Typeface.create(genericFontFamily, targetStyle)
        }
    }
}

@VisibleForTesting
@RequiresApi(28)
private class PlatformTypefacesApi28 : PlatformTypefaces {
    override fun optionalOnDeviceFontFamilyByName(
        familyName: String,
        weight: FontWeight,
        style: FontStyle
    ): Typeface? {
        // if the developer specified one of the named fonts, behave identically to the
        // GenericFontFamily behavior, return the same as createNamed always
        return when (familyName) {
            FontFamily.SansSerif.name -> createNamed(FontFamily.SansSerif, weight, style)
            FontFamily.Serif.name -> createNamed(FontFamily.Serif, weight, style)
            FontFamily.Monospace.name -> createNamed(FontFamily.Monospace, weight, style)
            FontFamily.Cursive.name -> createNamed(FontFamily.Cursive, weight, style)
            else -> loadNamedFromTypefaceCacheOrNull(familyName, weight, style)
        }
    }

    override fun createDefault(fontWeight: FontWeight, fontStyle: FontStyle) =
        createAndroidTypefaceApi28(null, fontWeight, fontStyle)

    /**
     * This always exposes the platform behavior, as a well formed Android OEM will have all weights
     * supported correctly in one FontFamily on Android 28+
     */
    override fun createNamed(
        name: GenericFontFamily,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): Typeface = createAndroidTypefaceApi28(name.name, fontWeight, fontStyle)

    private fun loadNamedFromTypefaceCacheOrNull(
        familyName: String,
        weight: FontWeight,
        style: FontStyle
    ): Typeface? {
        if (familyName.isEmpty()) return null
        val typeface = createAndroidTypefaceApi28(familyName, weight, style)
        val isItalic = style == FontStyle.Italic
        return typeface.takeIf {
            // Typeface may lookup missed results via either Typeface.DEFAULT or null, check both
            it != TypefaceHelperMethodsApi28.create(Typeface.DEFAULT, weight.weight, isItalic) &&
                it != createAndroidTypefaceApi28(null, weight, style)
        }
    }

    private fun createAndroidTypefaceApi28(
        genericFontFamily: String? = null,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): Typeface {
        if (fontStyle == FontStyle.Normal &&
            fontWeight == FontWeight.Normal &&
            genericFontFamily.isNullOrEmpty()
        ) {
            return Typeface.DEFAULT
        }

        val familyTypeface = if (genericFontFamily == null) {
            Typeface.DEFAULT
        } else {
            Typeface.create(genericFontFamily, Typeface.NORMAL)
        }

        return Typeface.create(
            familyTypeface,
            fontWeight.weight,
            fontStyle == FontStyle.Italic
        )
    }
}

/**
 * Convert system family name like "sans-serif" to fallback family names like
 * "sans-serif-medium" for platforms <28.
 */
@VisibleForTesting
internal fun getWeightSuffixForFallbackFamilyName(
    name: String,
    fontWeight: FontWeight
): String {
    // logic for matching comes from FontFamily.cpp#computeMatch(FontStyle, FontStyle)

    // for our purposes, we expect full-coverage from 100-900 for system fonts, and can ignore
    // slant, which makes this calculation (weight / 100)

    // fonts from fonts.xml in aosp define these optional fallback fonts
    // 100 -thin
    // 200 ##### not specified; tie break to 300
    // 300 -light
    // 400 # normal
    // 500 -medium
    // 600 ##### not specified; tie break to 700
    // 700 #bold
    // 900 -black

    // In platform, to break ties, the font appearing last (typically higher weight) wins, so we
    // will map 200->300; 600->700; 800->900 for the missing values
    return when (fontWeight.weight / 100) {
        in 0..1 -> "$name-thin" // 100
        in 2..3 -> "$name-light" // fallback 200, 300
        4 -> name // normal lookup 400
        5 -> "$name-medium" // 500
        in 6..7 -> name // tie break 600 -> 700, 700
        in 8..10 -> "$name-black" // 900 black, fallback 800, no match 1000
        else -> name // can't reach
    }
}