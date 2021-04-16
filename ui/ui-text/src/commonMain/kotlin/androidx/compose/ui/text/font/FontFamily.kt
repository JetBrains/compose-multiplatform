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

package androidx.compose.ui.text.font

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.fastDistinctBy

/**
 * The base class of the font families.
 *
 * @see FontListFontFamily
 * @see GenericFontFamily
 */
@Immutable
// TODO Unused parameter canLoadSynchronously
sealed class FontFamily(val canLoadSynchronously: Boolean) {
    companion object {
        /**
         * The platform default font.
         */
        val Default: SystemFontFamily = DefaultFontFamily()

        /**
         * Font family with low contrast and plain stroke endings.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilySansSerifSample
         *
         * See [CSS sans-serif](https://www.w3.org/TR/css-fonts-3/#sans-serif)
         */
        val SansSerif = GenericFontFamily("sans-serif")

        /**
         * The formal text style for scripts.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilySerifSample
         *
         * See [CSS serif](https://www.w3.org/TR/css-fonts-3/#serif)
         */
        val Serif = GenericFontFamily("serif")

        /**
         * Font family where glyphs have the same fixed width.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilyMonospaceSample
         *
         * See [CSS monospace](https://www.w3.org/TR/css-fonts-3/#monospace)
         */
        val Monospace = GenericFontFamily("monospace")

        /**
         * Cursive, hand-written like font family.
         *
         * If the device doesn't support this font family, the system will fallback to the
         * default font.
         *
         * @sample androidx.compose.ui.text.samples.FontFamilyCursiveSample
         *
         * See [CSS cursive](https://www.w3.org/TR/css-fonts-3/#cursive)
         */
        val Cursive = GenericFontFamily("cursive")
    }
}

/**
 * A base class of [FontFamily]s that is created from file sources.
 */
sealed class FileBasedFontFamily : FontFamily(false)

/**
 * A base class of [FontFamily]s installed on the system.
 */
sealed class SystemFontFamily : FontFamily(true)

/**
 * Defines a font family with list of [Font].
 *
 * @sample androidx.compose.ui.text.samples.FontFamilySansSerifSample
 * @sample androidx.compose.ui.text.samples.CustomFontFamilySample
 */
@Immutable
class FontListFontFamily internal constructor(
    val fonts: List<Font>
) : FileBasedFontFamily(), List<Font> by fonts {
    init {
        check(fonts.isNotEmpty()) { "At least one font should be passed to FontFamily" }
        check(fonts.fastDistinctBy { Pair(it.weight, it.style) }.size == fonts.size) {
            "There cannot be two fonts with the same FontWeight and FontStyle in the same " +
                "FontFamily"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontListFontFamily) return false
        if (fonts != other.fonts) return false
        return true
    }

    override fun hashCode(): Int {
        return fonts.hashCode()
    }

    override fun toString(): String {
        return "FontListFontFamily(fonts=$fonts)"
    }
}

/**
 * Defines a font family with an generic font family name.
 *
 * If the platform cannot find the passed generic font family, use the platform default one.
 *
 * @param name a generic font family name, e.g. "serif", "sans-serif"
 * @see FontFamily.SansSerif
 * @see FontFamily.Serif
 * @see FontFamily.Monospace
 * @see FontFamily.Cursive
 */
@Immutable
class GenericFontFamily internal constructor(val name: String) : SystemFontFamily()

/**
 * Defines a default font family.
 */
@Immutable
internal class DefaultFontFamily internal constructor() : SystemFontFamily()

/**
 * Defines a font family that is already loaded Typeface.
 *
 * @param typeface A typeface instance.
 */
class LoadedFontFamily internal constructor(val typeface: Typeface) : FontFamily(true) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoadedFontFamily) return false
        if (typeface != other.typeface) return false
        return true
    }

    override fun hashCode(): Int {
        return typeface.hashCode()
    }

    override fun toString(): String {
        return "LoadedFontFamily(typeface=$typeface)"
    }
}

/**
 * Construct a font family that contains list of custom font files.
 *
 * @param fonts list of font files
 */
@Stable
fun FontFamily(fonts: List<Font>): FontFamily = FontListFontFamily(fonts)

/**
 * Construct a font family that contains list of custom font files.
 *
 * @param fonts list of font files
 */
@Stable
fun FontFamily(vararg fonts: Font): FontFamily = FontListFontFamily(fonts.asList())

/**
 * Construct a font family that contains loaded font family: Typeface.
 *
 * @param typeface A typeface instance.
 */
@Stable
fun FontFamily(typeface: Typeface): FontFamily = LoadedFontFamily(typeface)