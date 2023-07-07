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

package androidx.compose.ui.text.matchers

import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.getAndroidTypefaceStyle
import androidx.compose.ui.text.matchers.TypefaceSubject.Companion.DEFINED_CHARACTERS
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

private const val FONT_SIZE = 10f

/**
 * Truth extension for Typeface.
 *
 * Checks if a given [Typeface] has a given [FontWeight] and [FontStyle]. Since [Typeface] does
 * not contain the required information before API 28, it uses the set of specific fonts to infer
 * which type of font was loaded. Check [DEFINED_CHARACTERS] and [FontTestData] for the set of
 * fonts designed for this class.
 *
 * Each font contains [a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r] characters and [wide,narrow] glyph
 * types. Each font file includes one character that is represented with wide glyph, and others
 * with narrow. This is used in the tests in order to differentiate the font that is loaded for
 * a specific weight/style in the FontFamily.
 *
 * The size difference between wide and narrow glyph is 3 (wide = 3 * narrow).
 *
 * - 200 italic has "a" with wide glyph
 * - 200 regular has "b" with wide glyph
 * - ...
 * - 900 italic has "q" with wide glyph
 * - 900 regular has "r" with wide glyph
 */
internal class TypefaceSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Typeface?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<TypefaceSubject?, Typeface?> =
            Factory { failureMetadata, subject -> TypefaceSubject(failureMetadata, subject) }

        internal val DEFINED_CHARACTERS = arrayOf(
            CharacterInfo('a', FontWeight.W100, FontStyle.Italic),
            CharacterInfo('b', FontWeight.W100, FontStyle.Normal),
            CharacterInfo('c', FontWeight.W200, FontStyle.Italic),
            CharacterInfo('d', FontWeight.W200, FontStyle.Normal),
            CharacterInfo('e', FontWeight.W300, FontStyle.Italic),
            CharacterInfo('f', FontWeight.W300, FontStyle.Normal),
            CharacterInfo('g', FontWeight.W400, FontStyle.Italic),
            CharacterInfo('h', FontWeight.W400, FontStyle.Normal),
            CharacterInfo('i', FontWeight.W500, FontStyle.Italic),
            CharacterInfo('j', FontWeight.W500, FontStyle.Normal),
            CharacterInfo('k', FontWeight.W600, FontStyle.Italic),
            CharacterInfo('l', FontWeight.W600, FontStyle.Normal),
            CharacterInfo('m', FontWeight.W700, FontStyle.Italic),
            CharacterInfo('n', FontWeight.W700, FontStyle.Normal),
            CharacterInfo('o', FontWeight.W800, FontStyle.Italic),
            CharacterInfo('p', FontWeight.W800, FontStyle.Normal),
            CharacterInfo('q', FontWeight.W900, FontStyle.Italic),
            CharacterInfo('r', FontWeight.W900, FontStyle.Normal)
        )
    }

    private fun getPaint(typeface: Typeface): TextPaint {
        return TextPaint().apply {
            this.typeface = typeface
            this.textSize = FONT_SIZE
        }
    }

    private fun isSelectedFont(typeface: Typeface, character: Char): Boolean {
        val string = Character.toString(character)
        val measuredWidth = getPaint(typeface).measureText(string)
        // wide glyphs are 3 times the width of narrow glyphs. Therefore for the selected character
        // if the right font is selected the width should be 3 times the font size.
        return java.lang.Float.compare(measuredWidth, FONT_SIZE * 3) == 0
    }

    /**
     * Verifies that [Typeface] object has the given [FontWeight] and [FontStyle].
     *
     * @param [fontWeight] expected [FontWeight]
     * @param [fontStyle] expected [FontStyle]
     */
    fun isTypefaceOf(fontWeight: FontWeight, fontStyle: FontStyle) {
        check("isNotNull()").that(subject).isNotNull()
        val typeface = subject as Typeface
        val charInfo = DEFINED_CHARACTERS.find {
            it.fontWeight == fontWeight && it.fontStyle == fontStyle
        }!!

        val isSelectedFont = isSelectedFont(typeface, charInfo.character)

        if (Build.VERSION.SDK_INT >= 28) {
            check("sameTypeface($isSelectedFont, $fontWeight, $fontStyle)")
                .that(isSelectedFont && typeface.weight == fontWeight.weight).isTrue()
            // cannot check typeface.isItalic == (fontStyle == FontStyle.Italic) since it is for
            // fake italic, and for cases where synthesis is disable this does not give correct
            // signal
        } else {
            check("sameTypeface($isSelectedFont, $fontWeight, $fontStyle)")
                .that(isSelectedFont).isTrue()
        }
    }

    override fun actualCustomStringRepresentation(): String {
        return if (subject != null) {
            val selectedFont = DEFINED_CHARACTERS.find { isSelectedFont(subject, it.character) }
            selectedFont?.toString() + " / " + toString(subject)
        } else {
            super.actualCustomStringRepresentation()
        }
    }

    /**
     * Verifies that [Typeface] object has the given [FontWeight] and [FontStyle].
     *
     * This assertion is best effort prior te API 28, and exact on API 28.
     *
     * @param [fontWeight] expected [FontWeight]
     * @param [fontStyle] expected [FontStyle]
     */
    fun hasWeightAndStyle(fontWeight: FontWeight, fontStyle: FontStyle) {
        check("isNotNull()").that(subject).isNotNull()
        val typeface = subject as Typeface

        val platformTypefaceStyle = getAndroidTypefaceStyle(fontWeight, fontStyle)
        if (Build.VERSION.SDK_INT >= 28) {
            check("weight == ($fontWeight)")
                .that(typeface.weight).isEqualTo(fontWeight.weight)
            check("isItalic == ($fontStyle)")
                .that(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            check("style == ($fontWeight, $fontStyle)")
                .that(typeface.style).isEqualTo(platformTypefaceStyle)
        } else {
            val expectBold = platformTypefaceStyle == Typeface.BOLD ||
                platformTypefaceStyle == Typeface.BOLD_ITALIC
            check("isItalic == ($fontWeight)")
                .that(typeface.isItalic).isEqualTo(fontStyle == FontStyle.Italic)
            check("style == ($fontWeight, $fontStyle)")
                .that(typeface.style).isEqualTo(platformTypefaceStyle)
            check("isBold == ($fontWeight)")
                .that(typeface.isBold).isEqualTo(expectBold)
        }
    }
}

internal class CharacterInfo(
    val character: Char,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle
) {
    override fun toString(): String {
        return toString(fontWeight, fontStyle)
    }
}

internal fun toString(fontWeight: FontWeight, fontStyle: FontStyle): String {
    return "{fontWeight: $fontWeight, fontStyle: $fontStyle}"
}

private fun toString(typeface: Typeface): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        "android.graphics.Typeface(weight=${typeface.weight}, bold=${typeface.isBold} " +
            "italic=${typeface.isItalic}, style=${typeface.style.toTypefaceStyleString()})"
    } else {
        "android.graphics.Typeface(bold=${typeface.isBold} italic=${typeface.isItalic}, " +
            "style=${typeface.style.toTypefaceStyleString()})"
    }
}

private fun Int.toTypefaceStyleString(): String = when (this) {
    Typeface.NORMAL -> "NORMAL"
    Typeface.BOLD -> "BOLD"
    Typeface.BOLD_ITALIC -> "BOLD_ITALIC"
    Typeface.ITALIC -> "ITALIC"
    else -> "Unknown($this)"
}
