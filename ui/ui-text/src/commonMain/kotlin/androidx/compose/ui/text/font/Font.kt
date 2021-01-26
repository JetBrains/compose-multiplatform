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

/**
 * The interface of the font resource.
 *
 * @see ResourceFont
 */
@Immutable
interface Font {
    /**
     * The weight of the font. The system uses this to match a font to a font request
     * that is given in a [androidx.compose.ui.text.SpanStyle].
     */
    val weight: FontWeight

    /**
     * The style of the font, normal or italic. The system uses this to match a font to a
     * font request that is given in a [androidx.compose.ui.text.SpanStyle].
     */
    val style: FontStyle

    /**
     * Interface used to load a font resource.
     */
    interface ResourceLoader {
        /**
         * Loads resource represented by the [Font] object.
         *
         * @param font [Font] to be loaded
         * @return platform specific font
         */
        fun load(font: Font): Any
    }
}

/**
 * Defines a font to be used while rendering text with resource ID.
 *
 * @sample androidx.compose.ui.text.samples.CustomFontFamilySample
 *
 * @param resId The resource ID of the font file in font resources. i.e. "R.font.myfont".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.TextStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.TextStyle].
 *
 * @see FontFamily
 */
class ResourceFont internal constructor(
    val resId: Int,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : Font {

    fun copy(
        resId: Int = this.resId,
        weight: FontWeight = this.weight,
        style: FontStyle = this.style
    ): ResourceFont {
        return ResourceFont(
            resId = resId,
            weight = weight,
            style = style
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceFont) return false
        if (resId != other.resId) return false
        if (weight != other.weight) return false
        if (style != other.style) return false
        return true
    }

    override fun hashCode(): Int {
        var result = resId
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResourceFont(resId=$resId, weight=$weight, style=$style)"
    }
}

/**
 * Creates a Font with using resource ID.
 *
 * @param resId The resource ID of the font file in font resources. i.e. "R.font.myfont".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
@Stable
fun Font(
    resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = ResourceFont(resId, weight, style)

/**
 * Create a [FontFamily] from this single [font].
 */
@Stable
fun Font.toFontFamily() = FontFamily(this)
