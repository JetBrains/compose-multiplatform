/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.collection.LruCache
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.DefaultFontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontMatcher
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.text.font.ResourceFont

/**
 * Creates a Typeface based on generic font family or a custom [FontFamily].
 *
 * @param fontMatcher [FontMatcher] class to be used to match given [FontWeight] and [FontStyle]
 *                    constraints to select a [Font] from a [FontFamily]
 *
 * @param resourceLoader [Font.ResourceLoader] for Android.
 */
internal open class TypefaceAdapter(
    val fontMatcher: FontMatcher = FontMatcher(),
    val resourceLoader: Font.ResourceLoader
) {
    data class CacheKey(
        val fontFamily: FontFamily? = null,
        val fontWeight: FontWeight,
        val fontStyle: FontStyle,
        val fontSynthesis: FontSynthesis
    )

    companion object {
        // Accept FontWeights at and above 600 to be bold. 600 comes from
        // FontFamily.cpp#computeFakery function in minikin
        private val ANDROID_BOLD = FontWeight.W600

        // TODO multiple TypefaceCache's, would be good to unify
        // 16 is a random number and is not based on any strong logic
        val typefaceCache = LruCache<CacheKey, Typeface>(16)

        fun synthesize(
            typeface: Typeface,
            font: Font,
            fontWeight: FontWeight,
            fontStyle: FontStyle,
            fontSynthesis: FontSynthesis
        ): Typeface {

            val synthesizeWeight = fontSynthesis.isWeightOn &&
                (fontWeight >= ANDROID_BOLD && font.weight < ANDROID_BOLD)

            val synthesizeStyle = fontSynthesis.isStyleOn && fontStyle != font.style

            if (!synthesizeStyle && !synthesizeWeight) return typeface

            return if (Build.VERSION.SDK_INT < 28) {
                val targetStyle = getTypefaceStyle(
                    isBold = synthesizeWeight,
                    isItalic = synthesizeStyle && fontStyle == FontStyle.Italic
                )
                Typeface.create(typeface, targetStyle)
            } else {
                val finalFontWeight = if (synthesizeWeight) {
                    // if we want to synthesize weight, we send the requested fontWeight
                    fontWeight.weight
                } else {
                    // if we do not want to synthesize weight, we keep the loaded font weight
                    font.weight.weight
                }

                val finalFontStyle = if (synthesizeStyle) {
                    // if we want to synthesize style, we send the requested fontStyle
                    fontStyle == FontStyle.Italic
                } else {
                    // if we do not want to synthesize style, we keep the loaded font style
                    font.style == FontStyle.Italic
                }
                TypefaceAdapterHelperMethods.create(typeface, finalFontWeight, finalFontStyle)
            }
        }

        /**
         * Convert given [FontWeight] and [FontStyle] to one of [Typeface.NORMAL], [Typeface.BOLD],
         * [Typeface.ITALIC], [Typeface.BOLD_ITALIC]. This function should be called for API < 28
         * since at those API levels system does not accept [FontWeight].
         */
        fun getTypefaceStyle(fontWeight: FontWeight, fontStyle: FontStyle): Int {
            return getTypefaceStyle(fontWeight >= ANDROID_BOLD, fontStyle == FontStyle.Italic)
        }

        private fun getTypefaceStyle(isBold: Boolean, isItalic: Boolean): Int {
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
    }

    /**
     * Creates a Typeface based on the [fontFamily] and the selection constraints [fontStyle] and
     * [fontWeight].
     *
     * @param fontFamily [FontFamily] that defines the system family or a set of custom fonts
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     */
    open fun create(
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        fontSynthesis: FontSynthesis = FontSynthesis.All
    ): Typeface {
        val cacheKey = CacheKey(fontFamily, fontWeight, fontStyle, fontSynthesis)
        val cachedTypeface = typefaceCache.get(cacheKey)
        if (cachedTypeface != null) return cachedTypeface

        val typeface = when (fontFamily) {
            is FontListFontFamily -> create(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis
            )
            is GenericFontFamily ->
                create(
                    genericFontFamily = fontFamily.name,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
            is DefaultFontFamily, null ->
                create(
                    genericFontFamily = null,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
            is LoadedFontFamily ->
                (fontFamily.typeface as AndroidTypeface).getNativeTypeface(
                    fontWeight, fontStyle, fontSynthesis
                )
        }

        // For system Typeface, on different framework versions Typeface might not be cached,
        // therefore it is safer to cache this result on our code and the cost is minimal.
        typefaceCache.put(cacheKey, typeface)

        return typeface
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
            val targetStyle = getTypefaceStyle(fontWeight, fontStyle)
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

            TypefaceAdapterHelperMethods.create(
                familyTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.Italic
            )
        }
    }

    /**
     * Creates a [Typeface] based on the [fontFamily] the requested [FontWeight], [FontStyle]. If
     * the requested [FontWeight] and [FontStyle] exists in the [FontFamily], the exact match is
     * returned. If it does not, the matching is defined based on CSS Font Matching. See
     * [FontMatcher] for more information.
     *
     * @param fontStyle the font style to create the typeface in
     * @param fontWeight the font weight to create the typeface in
     * @param fontFamily [FontFamily] that contains the list of [Font]s
     * @param fontSynthesis [FontSynthesis] which attributes of the font family to synthesize
     *        custom fonts for if they are not already present in the font family
     */
    private fun create(
        fontStyle: FontStyle = FontStyle.Normal,
        fontWeight: FontWeight = FontWeight.Normal,
        fontFamily: FontListFontFamily,
        fontSynthesis: FontSynthesis = FontSynthesis.All
    ): Typeface {
        val font = fontMatcher.matchFont(fontFamily, fontWeight, fontStyle)

        val typeface = try {
            when (font) {
                is ResourceFont -> resourceLoader.load(font) as Typeface
                is AndroidFont -> font.typeface
                else -> throw IllegalStateException("Unknown font type: $font")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Cannot create Typeface from $font", e)
        }

        val loadedFontIsSameAsRequest = fontWeight == font.weight && fontStyle == font.style
        // if synthesis is not requested or there is an exact match we don't need synthesis
        if (fontSynthesis == FontSynthesis.None || loadedFontIsSameAsRequest) {
            return typeface
        }

        return synthesize(typeface, font, fontWeight, fontStyle, fontSynthesis)
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(28)
internal object TypefaceAdapterHelperMethods {
    @RequiresApi(28)
    @DoNotInline
    fun create(typeface: Typeface, finalFontWeight: Int, finalFontStyle: Boolean) = Typeface
        .create(typeface, finalFontWeight, finalFontStyle)
}