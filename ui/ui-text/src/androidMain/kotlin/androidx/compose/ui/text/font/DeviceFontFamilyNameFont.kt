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

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Describes a system-installed font that may be present on some Android devices.
 *
 * You should assume this will not resolve on some devices and provide an appropriate fallback font.
 *
 * Family name lookup is device and platform-specific, and different OEMs may install different
 * fonts. All fonts described this way are considered [FontLoadingStrategy.OptionalLocal] and will
 * continue to the next font in the chain if they are not present on a device.
 *
 * Use this method to prefer locally pre-loaded system fonts when they are available. System fonts
 * are always more efficient to load than reading a font file, or downloadable fonts.
 *
 * A system installed font resolution will never trigger text reflow.
 *
 * This descriptor will trust the [weight] and [style] parameters as accurate. However, it is not
 * required that the loaded fonts actually support the requested weight and style and this may
 * trigger platform level font-synthesis of fake bold or fake italic during font resolution.
 *
 * This Font can not describe the system-installed [Typeface.DEFAULT]. All other system-installed
 * fonts are allowed.
 *
 * @param familyName Android system-installed font family name
 * @param weight weight to load
 * @param style style to load
 *
 * @throws IllegalArgumentException if familyName is empty
 */
@ExperimentalTextApi
fun Font(
    familyName: DeviceFontFamilyName,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font {
    return DeviceFontFamilyNameFont(familyName, weight, style)
}

/**
 * An Android system installed font family name as used by [Typeface.create].
 *
 * @see Typeface
 * @param name System fontFamilyName as passed to [Typeface.create]
 * @throws IllegalArgumentException if name is empty
 */
@ExperimentalTextApi
@JvmInline
value class DeviceFontFamilyName(val name: String) {
    init {
        require(name.isNotEmpty()) { "name may not be empty" }
    }
}

@ExperimentalTextApi
private class DeviceFontFamilyNameFont constructor(
    private val familyName: DeviceFontFamilyName,
    override val weight: FontWeight,
    override val style: FontStyle
) : AndroidFont(FontLoadingStrategy.OptionalLocal, NamedFontLoader) {

    val resolvedTypeface: Typeface? = PlatformTypefaces().optionalOnDeviceFontFamilyByName(
        familyName.name,
        weight,
        style
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceFontFamilyNameFont

        if (familyName != other.familyName) return false
        if (weight != other.weight) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = familyName.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "Font(familyName=\"$familyName\", weight=$weight, style=$style)"
    }
}

@ExperimentalTextApi
private object NamedFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        return (font as? DeviceFontFamilyNameFont)?.resolvedTypeface
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        throw UnsupportedOperationException("All preloaded fonts are optional local.")
    }
}