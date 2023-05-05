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

import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Adopted tests from text/text/src/androidTest/java/androidx/compose/ui/text/android/selection/WordBoundaryTest.kt
class SkikoParagraphTest {
    private val fontFamilyResolver = createFontFamilyResolver()
    private val defaultDensity = Density(density = 1f)

    @Test
    fun getWordBoundary_out_of_boundary_too_small() {
        val text = "text"
        val paragraph = simpleParagraph(text)

        assertFailsWith<IllegalArgumentException> {
            paragraph.getWordBoundary(-1)
        }
    }

    @Test
    fun getWordBoundary_out_of_boundary_too_big() {
        val text = "text"
        val paragraph = simpleParagraph(text)

        assertFailsWith<IllegalArgumentException> {
            paragraph.getWordBoundary(text.length + 1)
        }
    }

    @Test
    fun getWordBoundary_length() {
        val text = "text"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(0, text.length),
            paragraph.getWordBoundary(text.length)
        )
    }

    @Test
    fun getWordBoundary_empty_string() {
        val paragraph = simpleParagraph("")

        assertEquals(
            TextRange(0, 0),
            paragraph.getWordBoundary(0)
        )
    }

    @Test
    fun getWordBoundary() {
        val text = "abc def-ghi. jkl"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf('a'))
        )
        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf('c'))
        )
        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf(' '))
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.indexOf('-')),
            paragraph.getWordBoundary(text.indexOf('d'))
        )
        assertEquals(
            TextRange(text.indexOf('g'), text.indexOf('.')),
            paragraph.getWordBoundary(text.indexOf('i'))
        )
        assertEquals(
            TextRange(text.indexOf('j'), text.indexOf('l') + 1),
            paragraph.getWordBoundary(text.indexOf('k'))
        )
    }

    @Test
    fun getWordBoundary_spaces() {
        val text = "ab cd  e"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf('b') + 1),
            paragraph.getWordBoundary(text.indexOf('b') + 1)
        )
        assertEquals(
            TextRange(text.indexOf('c'), text.indexOf('d') + 1),
            paragraph.getWordBoundary(text.indexOf('c'))
        )
        assertEquals(
            TextRange(text.indexOf('d') + 2, text.indexOf('d') + 2),
            paragraph.getWordBoundary(text.indexOf('d') + 2)
        )
    }

    @Test
    fun getWordBoundary_no_break_space() {
        val text = "abc\u00A0def\u202Fghi"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf('c') + 1),
            paragraph.getWordBoundary(text.indexOf('b'))
        )
        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf('c') + 1),
            paragraph.getWordBoundary(text.indexOf('\u00A0'))
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.length),
            paragraph.getWordBoundary(text.indexOf('d'))
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.length),
            paragraph.getWordBoundary(text.indexOf('\u202F'))
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.length),
            paragraph.getWordBoundary(text.length)
        )
    }

    @Test
    fun getWordBoundary_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('\u05d0'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf('\u05d0'))
        )
        assertEquals(
            TextRange(text.indexOf('\u05d0'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf('\u05d2'))
        )
        assertEquals(
            TextRange(text.indexOf('\u05d0'), text.indexOf(' ')),
            paragraph.getWordBoundary(text.indexOf(' '))
        )
        assertEquals(
            TextRange(text.indexOf('\u05d3'), text.indexOf('-')),
            paragraph.getWordBoundary(text.indexOf('\u05d4'))
        )
        /*
        TODO: Port punctuation handling from Android.
        assertEquals(
            TextRange(text.indexOf('\u05d3'), text.indexOf('-') + 1),
            paragraph.getWordBoundary(text.indexOf('-'))
        )
        assertEquals(
            TextRange(text.indexOf('-'), text.indexOf('.')),
            paragraph.getWordBoundary(text.indexOf('\u05d5'))
        )
         */
        assertEquals(
            TextRange(text.indexOf('\u05d5'), text.indexOf('.')),
            paragraph.getWordBoundary(text.indexOf('\u05d6'))
        )
        assertEquals(
            TextRange(text.indexOf('\u05d7'), text.length),
            paragraph.getWordBoundary(text.indexOf('\u05d7'))
        )
    }

    @Test
    fun getWordBoundary_CJK() { // Japanese HIRAGANA letter + KATAKANA letters
        val text = "\u3042\u30A2\u30A3\u30A4"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('\u3042'), text.indexOf('\u3042') + 1),
            paragraph.getWordBoundary(text.indexOf('\u3042'))
        )
        /*
        TODO: figure out why skia's ICU split words this way
        assertEquals(
            TextRange(text.indexOf('\u3042'), text.indexOf('\u30A4') + 1),
            paragraph.getWordBoundary(text.indexOf('\u30A2'))
        )
         */
        assertEquals(
            TextRange(text.indexOf('\u30A2'), text.indexOf('\u30A4') + 1),
            paragraph.getWordBoundary(text.indexOf('\u30A4'))
        )
        /*
        TODO: figure out why skia's ICU split words this way
        assertEquals(
            TextRange(text.indexOf('\u30A2'), text.indexOf('\u30A4') + 1),
            paragraph.getWordBoundary(text.length)
        )
         */
    }

    @Test
    fun getWordBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('i'), text.indexOf('t') + 1),
            paragraph.getWordBoundary(text.indexOf('i'))
        )
        assertEquals(
            TextRange(text.indexOf('i'), text.indexOf('t') + 1),
            paragraph.getWordBoundary(text.indexOf('n'))
        )
        assertEquals(
            TextRange(text.indexOf('i'), text.indexOf('t') + 1),
            paragraph.getWordBoundary(text.indexOf('\''))
        )
        assertEquals(
            TextRange(text.indexOf('i'), text.indexOf('t') + 1),
            paragraph.getWordBoundary(text.indexOf('t'))
        )
        assertEquals(
            TextRange(text.indexOf('i'), text.indexOf('t') + 1),
            paragraph.getWordBoundary(text.indexOf('t') + 1)
        )
        assertEquals(
            TextRange(text.indexOf('h'), text.indexOf('e') + 1),
            paragraph.getWordBoundary(text.indexOf('h'))
        )
    }

    @Test
    @Ignore // TODO: Port punctuation handling from Android.
    fun getWordBoundary_isOnPunctuation() {
        val text = "abc!? (^^;) def"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf('!')),
            paragraph.getWordBoundary(text.indexOf('a'))
        )
        assertEquals(
            TextRange(text.indexOf('a'), text.indexOf('?') + 1),
            paragraph.getWordBoundary(text.indexOf('!'))
        )
        assertEquals(
            TextRange(text.indexOf('!'), text.indexOf('?') + 1),
            paragraph.getWordBoundary(text.indexOf('?') + 1)
        )
        assertEquals(
            TextRange(text.indexOf('('), text.indexOf('(') + 1),
            paragraph.getWordBoundary(text.indexOf('('))
        )
        assertEquals(
            TextRange(text.indexOf('(') + 2, text.indexOf('(') + 2),
            paragraph.getWordBoundary(text.indexOf('(') + 2)
        )
        assertEquals(
            TextRange(text.indexOf(';'), text.indexOf(')') + 1),
            paragraph.getWordBoundary(text.indexOf(';'))
        )
        assertEquals(
            TextRange(text.indexOf(';'), text.indexOf(')') + 1),
            paragraph.getWordBoundary(text.indexOf(')'))
        )
        assertEquals(
            TextRange(text.indexOf(';'), text.indexOf(')') + 1),
            paragraph.getWordBoundary(text.indexOf(')') + 1)
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.length),
            paragraph.getWordBoundary(text.indexOf('d'))
        )
        assertEquals(
            TextRange(text.indexOf('d'), text.length),
            paragraph.getWordBoundary(text.length)
        )
    }

    @Test
    fun getWordBoundary_emoji() {
        // "ab 🧑🏿‍🦰 cd" - example of complex emoji
        //             | (offset=3)      | (offset=6)
        val text = "ab \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0 cd"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(3, 10),
            paragraph.getWordBoundary(6)
        )
    }

    @Test
    fun getWordBoundary_multichar() {
        // "ab 𐐔𐐯𐑅𐐨𐑉𐐯𐐻 cd" - example of multi-char code units
        //             | (offset=3)      | (offset=6)
        val text = "ab \uD801\uDC14\uD801\uDC2F\uD801\uDC45\uD801\uDC28\uD801\uDC49\uD801\uDC2F\uD801\uDC3B cd"
        val paragraph = simpleParagraph(text)

        assertEquals(
            TextRange(3, 17),
            paragraph.getWordBoundary(6)
        )
    }

    private fun simpleParagraph(text: String) = Paragraph(
        text = text,
        style = TextStyle(),
        constraints = Constraints(maxWidth = 1000),
        density = defaultDensity,
        fontFamilyResolver = fontFamilyResolver
    )
}