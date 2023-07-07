/*
 * Copyright 2019 The Android Open Source Project
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

/**
 *  Possible options for font synthesis.
 *
 *  `FontSynthesis` is used to specify whether the system should fake bold or slanted
 *  glyphs when the [FontFamily] used does not contain bold or oblique [Font]s.
 *
 *  If the font family does not include a requested [FontWeight] or [FontStyle], the system
 *  fakes bold or slanted glyphs when the [Weight] or [Style], respectively, or both when [All]
 *  is set. If this is not desired, use [None] to disable font synthesis.
 *
 *  It is possible to fake an increase of [FontWeight] but not a decrease. It is possible to fake
 *  a regular font slanted, but not vice versa.
 *
 *  `FontSynthesis` works the same way as the
 *  [CSS font-synthesis](https://www.w3.org/TR/css-fonts-4/#font-synthesis) property.
 *
 *  @sample androidx.compose.ui.text.samples.FontFamilySynthesisSample
 **/
@kotlin.jvm.JvmInline
value class FontSynthesis internal constructor(internal val value: Int) {

    override fun toString(): String {
        return when (this) {
            None -> "None"
            All -> "All"
            Weight -> "Weight"
            Style -> "Style"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Turns off font synthesis. Neither bold nor slanted faces are synthesized if they don't
         * exist in the [FontFamily]
         */
        val None = FontSynthesis(0)

        /**
         * The system synthesizes both bold and slanted fonts if either of them are not available in
         * the [FontFamily]
         */
        val All = FontSynthesis(1)

        /**
         * Only a bold font is synthesized, if it is not available in the [FontFamily]. Slanted fonts
         * will not be synthesized.
         */
        val Weight = FontSynthesis(2)

        /**
         * Only an slanted font is synthesized, if it is not available in the [FontFamily]. Bold fonts
         * will not be synthesized.
         */
        val Style = FontSynthesis(3)
    }

    internal val isWeightOn: Boolean
        get() = this == All || this == Weight

    internal val isStyleOn: Boolean
        get() = this == All || this == Style
}

/**
 * Perform platform-specific font synthesis such as fake bold or fake italic.
 *
 * Platforms are not required to support synthesis, in which case they should return [typeface].
 *
 * Platforms that support synthesis should check [FontSynthesis.isWeightOn] and
 * [FontSynthesis.isStyleOn] in this method before synthesizing bold or italic, respectively.
 *
 * @param typeface a platform-specific typeface
 * @param font initial font that generated the typeface via loading
 * @param requestedWeight app-requested weight (may be different than the font's weight)
 * @param requestedStyle app-requested style (may be different than the font's style)
 * @return a synthesized typeface, or the passed [typeface] if synthesis is not needed or supported.
 */
internal expect fun FontSynthesis.synthesizeTypeface(
    typeface: Any,
    font: Font,
    requestedWeight: FontWeight,
    requestedStyle: FontStyle
): Any