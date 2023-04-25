/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

// Adopted copy of compose/ui/ui-text/src/test/java/androidx/compose/ui/text/AnnotatedStringTransformTest.kt
class AnnotatedStringTransformSkikoTest {

    private val spanStyle1 = SpanStyle(fontSize = 8.sp)
    private val spanStyle2 = SpanStyle(fontSize = 16.sp)
    private val spanStyle3 = SpanStyle(fontSize = 24.sp)

    private val paraStyle1 = ParagraphStyle(lineHeight = 10.sp)
    private val paraStyle2 = ParagraphStyle(lineHeight = 20.sp)

    /**
     * Helper function for creating AnnotatedString.Item with TextRange.
     */
    private fun <T> makeRange(style: T, range: TextRange) =
        AnnotatedString.Range(style, range.min, range.max)

    /**
     * Make AnnotatedString.Item with original string with using "(" and ")" characters.
     *
     * For example.
     *   val text = "aaa bbb ccc"
     *
     *   AnnotatedString.Item(STYLE, 4, 8)
     *
     * can be written as
     *
     *   val text = "aaa bbb ccc"
     *
     *   makeRange(STYLE, "aaa (bbb )ccc")
     */
    private fun <T> makeRange(style: T, rangeStr: String): AnnotatedString.Range<T> {
        val start = rangeStr.indexOf('(')
        val end = rangeStr.indexOf(')')

        if (start >= end) throw RuntimeException("Invalid range str: $rangeStr")
        return makeRange(style, TextRange(start, end - 1 /* subtract start marker */))
    }

    @Test
    fun englishUppercasePlaintext() {
        val input = AnnotatedString("aaa bbb ccc")

        val uppercase = input.toUpperCase()

        assertEquals(uppercase.text, input.text.uppercase())
    }

    @Test
    fun englishUppercaseSparse() {
        val input = AnnotatedString(
            "aaa bbb ccc",
            listOf(makeRange(spanStyle1, "aaa (bbb) ccc"))
        )

        val uppercase = input.toUpperCase()

        assertEquals(uppercase.text, input.text.uppercase())
    }

    @Test
    fun englishUppercase() {
        val input = AnnotatedString(
            "aaa bbb ccc",
            listOf(
                makeRange(spanStyle1, "(aaa bbb ccc)"),
                makeRange(spanStyle2, "(aaa )bbb ccc"),
                makeRange(spanStyle3, "aaa (bbb ccc)")
            ),
            listOf(
                makeRange(paraStyle1, "(aaa bbb )ccc"),
                makeRange(paraStyle2, "aaa bbb (ccc)")
            )
        )

        val uppercase = input.toUpperCase()

        assertEquals(uppercase.text, input.text.uppercase())
        assertEquals(uppercase.spanStyles, input.spanStyles)
        assertEquals(uppercase.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun englishLowercase() {
        val input = AnnotatedString(
            "aaa bbb ccc",
            listOf(
                makeRange(spanStyle1, "(aaa bbb ccc)"),
                makeRange(spanStyle2, "(aaa )bbb ccc"),
                makeRange(spanStyle3, "aaa (bbb ccc)")
            ),
            listOf(
                makeRange(paraStyle1, "(aaa bbb )ccc"),
                makeRange(paraStyle2, "aaa bbb (ccc)")
            )
        )

        val lowercase = input.toLowerCase()

        assertEquals(lowercase.text, input.text.lowercase())
        assertEquals(lowercase.spanStyles, input.spanStyles)
        assertEquals(lowercase.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun englishCapitalize() {
        val input = AnnotatedString(
            "aaa bbb ccc",
            listOf(
                makeRange(spanStyle1, "(aaa bbb ccc)"),
                makeRange(spanStyle2, "(aaa )bbb ccc"),
                makeRange(spanStyle3, "aaa (bbb ccc)")
            ),
            listOf(
                makeRange(paraStyle1, "(aaa bbb )ccc"),
                makeRange(paraStyle2, "aaa bbb (ccc)")
            )
        )

        val capitalized = input.capitalize()

        assertEquals(capitalized.text, 
            input.text.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        )
        assertEquals(capitalized.spanStyles, input.spanStyles)
        assertEquals(capitalized.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun englishDecapitalize() {
        val input = AnnotatedString(
            "aaa bbb ccc",
            listOf(
                makeRange(spanStyle1, "(aaa bbb ccc)"),
                makeRange(spanStyle2, "(aaa )bbb ccc"),
                makeRange(spanStyle3, "aaa (bbb ccc)")
            ),
            listOf(
                makeRange(paraStyle1, "(aaa bbb )ccc"),
                makeRange(paraStyle2, "aaa bbb (ccc)")
            )
        )

        val decapitalized = input.decapitalize()

        assertEquals(decapitalized.text,
            input.text.replaceFirstChar { it.lowercase() }
        )
        assertEquals(decapitalized.spanStyles, input.spanStyles)
        assertEquals(decapitalized.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun localeDependentCapitalizeTurkish() {
        val input = AnnotatedString(
            "iii hhh jjj",
            listOf(
                makeRange(spanStyle1, "(iii hhh jjj)"),
                makeRange(spanStyle2, "(iii )hhh jjj"),
                makeRange(spanStyle3, "iii (hhh jjj)")
            ),
            listOf(
                makeRange(paraStyle1, "(iii hhh )jjj"),
                makeRange(paraStyle2, "iii hhh (jjj)")
            )
        )
        val capitalized = input.capitalize(LocaleList("tr"))

        assertEquals(capitalized.text, input.text.capitalize(Locale("tr")))
        assertEquals(capitalized.spanStyles, input.spanStyles)
        assertEquals(capitalized.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun localeDependentDecapitalizeTurkish() {
        val input = AnnotatedString(
            "III HHH JJJ",
            listOf(
                makeRange(spanStyle1, "(III HHH JJJ)"),
                makeRange(spanStyle2, "(III )HHH JJJ"),
                makeRange(spanStyle3, "III (HHH JJJ)")
            ),
            listOf(
                makeRange(paraStyle1, "(III HHH )JJJ"),
                makeRange(paraStyle2, "III HHH (JJJ)")
            )
        )

        val decapitalized = input.decapitalize(LocaleList("tr"))

        assertEquals(decapitalized.text,  input.text.decapitalize(Locale("tr")))
        assertEquals(decapitalized.spanStyles, input.spanStyles)
        assertEquals(decapitalized.paragraphStyles, input.paragraphStyles)
    }

    @Test
    fun localeDependentUppercaseOrLowercaseTurkishUppercase() {
        val input = AnnotatedString(
            "hhh iii jjj",
            listOf(
                makeRange(spanStyle1, "(hhh iii jjj)"),
                makeRange(spanStyle2, "(hhh )iii jjj"),
                makeRange(spanStyle3, "hhh (iii jjj)")
            ),
            listOf(
                makeRange(paraStyle1, "(hhh iii )jjj"),
                makeRange(paraStyle2, "hhh iii (jjj)")
            )
        )

        val uppercase = input.toUpperCase(LocaleList("tr"))

        assertEquals(uppercase.text, input.text.toUpperCase(Locale("tr")))

        val upperI = "i".toUpperCase(Locale("tr"))

        assertEquals(uppercase.spanStyles, 
            listOf(
                makeRange(spanStyle1, "(HHH $upperI$upperI$upperI JJJ)"),
                makeRange(spanStyle2, "(HHH )$upperI$upperI$upperI JJJ"),
                makeRange(spanStyle3, "HHH ($upperI$upperI$upperI JJJ)")
            )
        )
        assertEquals(uppercase.paragraphStyles, 
            listOf(
                makeRange(paraStyle1, "(HHH $upperI$upperI$upperI )JJJ"),
                makeRange(paraStyle2, "HHH $upperI$upperI$upperI (JJJ)")
            )
        )
    }

    @Test
    fun not1by1mappingUppercaseOrLowercaseLithuanianLowercase() {
        val input = AnnotatedString(
            "HHH ÌÌÌ YYY",
            listOf(
                makeRange(spanStyle1, "(HHH ÌÌÌ YYY)"),
                makeRange(spanStyle2, "(HHH )ÌÌÌ YYY"),
                makeRange(spanStyle3, "HHH (ÌÌÌ YYY)")
            ),
            listOf(
                makeRange(paraStyle1, "(HHH ÌÌÌ )YYY"),
                makeRange(paraStyle2, "HHH ÌÌÌ (YYY)")
            )
        )

        val lowercase = input.toLowerCase(LocaleList("lt"))

        assertEquals(lowercase.text, input.text.toLowerCase(Locale("lt")))

        // Usually generate U+0069 U+0307 U+0300
        val lowerIDot = "Ì".toLowerCase(Locale("lt"))
        assertEquals(lowercase.spanStyles, 
            listOf(
                makeRange(spanStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot yyy)"),
                makeRange(spanStyle2, "(hhh )$lowerIDot$lowerIDot$lowerIDot yyy"),
                makeRange(spanStyle3, "hhh ($lowerIDot$lowerIDot$lowerIDot yyy)")
            )
        )
        assertEquals(lowercase.paragraphStyles, 
            listOf(
                makeRange(paraStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot )yyy"),
                makeRange(paraStyle2, "hhh $lowerIDot$lowerIDot$lowerIDot (yyy)")
            )
        )
    }

    @Test
    fun nothingHappensForCjkUppercaseOrLowercaseJapaneseUppercase() {
        val input = AnnotatedString(
            "あああ いいい ううう",
            listOf(
                makeRange(spanStyle1, "(あああ いいい ううう)"),
                makeRange(spanStyle2, "(あああ )いいい ううう"),
                makeRange(spanStyle3, "あああ (いいい ううう)")
            ),
            listOf(
                makeRange(paraStyle1, "(あああ いいい )ううう"),
                makeRange(paraStyle2, "あああ いいい (ううう)")
            )
        )

        val uppercase = input.toUpperCase()

        // No upper case concept in Japanese, so should be the same
        assertEquals(uppercase.text, input.text)
        assertEquals(uppercase.spanStyles, input.spanStyles)
        assertEquals(uppercase.paragraphStyles, input.paragraphStyles)
    }
}