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

package androidx.compose.ui.text.android.selection

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.BreakIterator
import java.util.Locale

@SmallTest
@RunWith(AndroidJUnit4::class)
class WordIteratorTest {
    @Test(expected = IllegalArgumentException::class)
    fun testConstructor_IndexOutOfBounds_too_big() {
        WordIterator("text", 100, 100, Locale.ENGLISH)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConstructor_IndexOutOfBounds_too_small() {
        WordIterator("text", -100, -100, Locale.ENGLISH)
    }

    @Test
    fun testConstructor_valid_full_text() {
        val text = "text"
        WordIterator(text, 0, text.length, Locale.ENGLISH)
    }

    @Test
    fun testConstructor_valid_beginning() {
        val text = "text"
        WordIterator(text, 0, 0, Locale.ENGLISH)
    }

    @Test
    fun testConstructor_valid_end() {
        val text = "text"
        WordIterator(text, 0, text.length, Locale.ENGLISH)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNextBoundary_out_of_boundary_too_small() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.nextBoundary(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNextBoundary_out_of_boundary_too_big() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.nextBoundary(text.length + 1)
    }

    @Test
    fun testNextBoundary_iterate_through() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        // Start from the beginning.
        var currentOffset = 0
        // The word is "abc".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('c') + 1)
        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('d'))
        // The word is "def".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('f') + 1)
        // The word is "-".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('g'))
        // The word is "ghi".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('i') + 1)
        // The word is ".".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.') + 1)
        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('j'))
        // The word is "jkl".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.length)
        // WordIterator reaches the end.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(BreakIterator.DONE)
    }

    @Test
    fun testNextBoundary_iterate_through_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordIterator = WordIterator(
            text, 0, text.length,
            Locale("he", "IL")
        )
        // Start from the beginning.
        var currentOffset = 0
        // The word is "\u05d0\u05d1\u05d2"("אבג")
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d2') + 1)
        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d3'))
        // The word is "\u05d3\u05d4"("דה")
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('-'))
        // The word is "-".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d5'))
        // The word is "\u05d5\u05d6("וז")
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.'))
        // The word is ".".
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.') + 1)
        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d7'))
        // The word is "\u05d7\u05d8"("חט")
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.length)
        // WordIterator reaches the end.
        currentOffset = wordIterator.nextBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(BreakIterator.DONE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPrevBoundary_out_of_boundary_too_small() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.prevBoundary(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPrevBoundary_out_of_boundary_too_big() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.prevBoundary(text.length + 1)
    }

    @Test
    fun testPrevBoundary_iterate_through() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        // Start from the end.
        var currentOffset = text.length
        // The word is "jkl".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('j'))
        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.') + 1)
        // The word is ".".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('i') + 1)
        // The word is "ghi".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('g'))
        // The word is "-".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('f') + 1)
        // The word is "def".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('d'))
        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('c') + 1)
        // The word is "abc".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('a'))
        // WordIterator reaches the beginning.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(BreakIterator.DONE)
    }

    @Test
    fun testPrevBoundary_iterate_through_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordIterator = WordIterator(
            text, 0, text.length,
            Locale("he", "IL")
        )
        // Start from the end.
        var currentOffset = text.length
        // The word is "\u05d7\u05d8"("חט")
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d7'))
        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.') + 1)
        // The word is '.'.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('.'))
        // The word is "\u05d5\u05d6("וז")
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d5'))
        // The word is "-".
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('-'))
        // The word is "\u05d3\u05d4"("דה")
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d3'))
        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf(' '))
        // The word is "\u05d0\u05d1\u05d2"("אבג")
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(text.indexOf('\u05d0'))
        // WordIterator reaches the beginning.
        currentOffset = wordIterator.prevBoundary(currentOffset)
        assertThat(currentOffset).isEqualTo(BreakIterator.DONE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPrevWordBeginningOnTwoWordsBoundary_out_of_boundary_too_small() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPrevWordBeginningOnTwoWordsBoundary(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPrevWordBeginningOnTwoWordsBoundary_out_of_boundary_too_big() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.length + 1)
    }

    @Test
    fun testGetPrevWordBeginningOnTwoWordsBoundary_Empty_String() {
        val text = ""
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(0))
            .isEqualTo(BreakIterator.DONE)
    }

    @Test
    fun testGetPrevWordBeginningOnTwoWordsBoundary() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('a')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('c')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('d')))
            .isEqualTo(text.indexOf('d'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('f')))
            .isEqualTo(text.indexOf('d'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('-')))
            .isEqualTo(text.indexOf('d'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('g')))
            .isEqualTo(text.indexOf('g'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('.')))
            .isEqualTo(text.indexOf('g'))
    }

    @Test
    fun testGetPrevWordBeginningOnTwoWordsBoundary_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters -- "あアィイ"
        val text = "\u3042\u30A2\u30A3\u30A4"
        val wordIterator = WordIterator(text, 0, text.length, Locale.JAPANESE)
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u3042')))
            .isEqualTo(text.indexOf('\u3042'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u30A2')))
            .isEqualTo(text.indexOf('\u3042'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u30A4')))
            .isEqualTo(text.indexOf('\u30A2'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.length))
            .isEqualTo(text.indexOf('\u30A2'))
    }

    @Test
    fun testGetPrevWordBeginningOnTwoWordsBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('i')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('n')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\'')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('t')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('t') + 1))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('h')))
            .isEqualTo(text.indexOf('h'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetNextWordEndOnTwoWordBoundary_out_of_boundary_too_small() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getNextWordEndOnTwoWordBoundary(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetNextWordEndOnTwoWordBoundary_out_of_boundary_too_big() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getNextWordEndOnTwoWordBoundary(text.length + 1)
    }

    @Test
    fun testGetNextWordEndOnTwoWordBoundary_Empty_String() {
        val text = ""
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(0))
            .isEqualTo(BreakIterator.DONE)
    }

    @Test
    fun testGetNextWordEndOnTwoWordBoundary() {
        val text = "abc def-ghi. jkl"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('a')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('c')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('d')))
            .isEqualTo(text.indexOf('-'))
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('f')))
            .isEqualTo(text.indexOf('-'))
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('-')))
            .isEqualTo(text.indexOf('-'))
    }

    @Test
    fun testGetNextWordEndOnTwoWordBoundary_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters -- "あアィイ"
        val text = "\u3042\u30A2\u30A3\u30A4"
        val wordIterator = WordIterator(text, 0, text.length, Locale.JAPANESE)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u3042')))
            .isEqualTo(text.indexOf('\u3042') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A2')))
            .isEqualTo(text.indexOf('\u30A4') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A4')))
            .isEqualTo(text.indexOf('\u30A4') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A4') + 1))
            .isEqualTo(text.indexOf('\u30A4') + 1)
    }

    @Test
    fun testGetNextWordEndOnTwoWordBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('i')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('n')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\'')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('t')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('h')))
            .isEqualTo(text.indexOf('e') + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationBeginning_out_of_boundary_too_small() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationBeginning(-2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationBeginning_out_of_boundary_too_big() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationBeginning(text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationBeginning_DONE() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationBeginning(BreakIterator.DONE)
    }

    @Test
    fun testGetPunctuationBeginning() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf('a')))
            .isEqualTo(BreakIterator.DONE)
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf('c')))
            .isEqualTo(BreakIterator.DONE)
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf('!')))
            .isEqualTo(text.indexOf('!'))
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf('?') + 1))
            .isEqualTo(text.indexOf('!'))
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf(';')))
            .isEqualTo(text.indexOf(';'))
        assertThat(wordIterator.getPunctuationBeginning(text.indexOf(')')))
            .isEqualTo(text.indexOf(';'))
        assertThat(wordIterator.getPunctuationBeginning(text.length))
            .isEqualTo(text.indexOf(';'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationEnd_out_of_boundary_too_small() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationEnd(-2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationEnd_out_of_boundary_too_big() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationEnd(text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPunctuationEnd_DONE() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        wordIterator.getPunctuationEnd(BreakIterator.DONE)
    }

    @Test
    fun testGetPunctuationEnd() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf('a')))
            .isEqualTo(text.indexOf('?') + 1)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf('?') + 1))
            .isEqualTo(text.indexOf('?') + 1)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf('(')))
            .isEqualTo(text.indexOf('(') + 1)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf('(') + 2))
            .isEqualTo(text.indexOf(')') + 1)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf(')') + 1))
            .isEqualTo(text.indexOf(')') + 1)
        assertThat(wordIterator.getPunctuationEnd(text.indexOf('d')))
            .isEqualTo(BreakIterator.DONE)
        assertThat(wordIterator.getPunctuationEnd(text.length))
            .isEqualTo(BreakIterator.DONE)
    }

    @Test
    fun testIsAfterPunctuation() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.isAfterPunctuation(text.indexOf('a'))).isFalse()
        assertThat(wordIterator.isAfterPunctuation(text.indexOf('!'))).isFalse()
        assertThat(wordIterator.isAfterPunctuation(text.indexOf('?'))).isTrue()
        assertThat(wordIterator.isAfterPunctuation(text.indexOf('?') + 1)).isTrue()
        assertThat(wordIterator.isAfterPunctuation(text.indexOf('d'))).isFalse()
        assertThat(wordIterator.isAfterPunctuation(BreakIterator.DONE)).isFalse()
        assertThat(wordIterator.isAfterPunctuation(text.length + 1)).isFalse()
    }

    @Test
    fun testIsOnPunctuation() {
        val text = "abc!? (^^;) def"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        assertThat(wordIterator.isOnPunctuation(text.indexOf('a'))).isFalse()
        assertThat(wordIterator.isOnPunctuation(text.indexOf('!'))).isTrue()
        assertThat(wordIterator.isOnPunctuation(text.indexOf('?'))).isTrue()
        assertThat(wordIterator.isOnPunctuation(text.indexOf('?') + 1)).isFalse()
        assertThat(wordIterator.isOnPunctuation(text.indexOf(')'))).isTrue()
        assertThat(wordIterator.isOnPunctuation(text.indexOf(')') + 1)).isFalse()
        assertThat(wordIterator.isOnPunctuation(text.indexOf('d'))).isFalse()
        assertThat(wordIterator.isOnPunctuation(BreakIterator.DONE)).isFalse()
        assertThat(wordIterator.isOnPunctuation(text.length)).isFalse()
        assertThat(wordIterator.isOnPunctuation(text.length + 1)).isFalse()
    }

    @Test
    fun testOneWord() {
        val text = "zen"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsWord(wordIterator, 0, 3)
    }

    @Test
    fun testSpacesOnly() {
        val text = " "
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsNotWord(wordIterator, 0, 1)
    }

    @Test
    fun testCommaWithSpace() {
        val text = ", "
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsNotWord(wordIterator, 0, 2)
    }

    @Test
    fun testSymbols() {
        val text = ":-)"
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsNotWord(wordIterator, 0, 3)
    }

    @Test
    fun testBeginningEnd1() {
        val text = "Well hello,   there! "
        //         0123456789012345678901
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsWord(wordIterator, 0, 4)
        verifyIsWord(wordIterator, 5, 10)
        verifyIsNotWord(wordIterator, 11, 13)
        verifyIsWord(wordIterator, 14, 19)
        verifyIsNotWord(wordIterator, 20, 21)
    }

    @Test
    fun testBeginningEnd2() {
        val text = "  Another - sentence"
        //         012345678901234567890
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsNotWord(wordIterator, 0, 1)
        verifyIsWord(wordIterator, 2, 9)
        verifyIsNotWord(wordIterator, 10, 11)
        verifyIsWord(wordIterator, 12, 20)
    }

    @Test
    fun testBeginningEnd3() {
        val text = "This is \u0644\u0627 tested" // Lama-aleph
        //         012345678     9     01234567
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsWord(wordIterator, 0, 4)
        verifyIsWord(wordIterator, 5, 7)
        verifyIsWord(wordIterator, 8, 10)
        verifyIsWord(wordIterator, 11, 17)
    }

    @Test
    fun testSurrogate() {
        val gothicBairkan = "\uD800\uDF31"
        val text = "one " + gothicBairkan + "xxx word"
        //         0123    45         678901234
        val wordIterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        verifyIsWord(wordIterator, 0, 3)
        verifyIsWordWithSurrogate(wordIterator, 4, 9, 5)
        verifyIsWord(wordIterator, 10, 14)
    }

    private fun verifyIsWordWithSurrogate(
        wordIterator: WordIterator,
        beginning: Int,
        end: Int,
        surrogateIndex: Int
    ) {
        for (i in beginning..end) {
            if (i == surrogateIndex) continue
            assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(i))
                .isEqualTo(beginning)
            assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(i)).isEqualTo(end)
        }
    }

    private fun verifyIsWord(wordIterator: WordIterator, beginning: Int, end: Int) {
        verifyIsWordWithSurrogate(wordIterator, beginning, end, -1)
    }

    private fun verifyIsNotWord(wordIterator: WordIterator, beginning: Int, end: Int) {
        for (i in beginning..end) {
            assertThat(wordIterator.getPrevWordBeginningOnTwoWordsBoundary(i))
                .isEqualTo(BreakIterator.DONE)
            assertThat(wordIterator.getNextWordEndOnTwoWordBoundary(i))
                .isEqualTo(BreakIterator.DONE)
        }
    }
}