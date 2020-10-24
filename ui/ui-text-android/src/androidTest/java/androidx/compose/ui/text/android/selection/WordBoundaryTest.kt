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

import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.BreakIterator
import java.util.Locale

@OptIn(InternalPlatformTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class WordBoundaryTest {
    @Test(expected = IllegalArgumentException::class)
    fun testGetWordStart_out_of_boundary_too_small() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordStart(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetWordStart_out_of_boundary_too_big() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordStart(text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetWordStart_DONE() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordStart(BreakIterator.DONE)
    }

    @Test
    fun testGetWordStart_Empty_String() {
        val wordBoundary = WordBoundary(Locale.ENGLISH, "")
        assertThat(wordBoundary.getWordStart(0)).isEqualTo(0)
    }

    @Test
    fun testGetWordStart() {
        val text = "abc def-ghi. jkl"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordStart(text.indexOf('a')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordBoundary.getWordStart(text.indexOf('c')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordBoundary.getWordStart(text.indexOf(' ')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordBoundary.getWordStart(text.indexOf('d')))
            .isEqualTo(text.indexOf('d'))
        assertThat(wordBoundary.getWordStart(text.indexOf('i')))
            .isEqualTo(text.indexOf('g'))
        assertThat(wordBoundary.getWordStart(text.indexOf('k')))
            .isEqualTo(text.indexOf('j'))
    }

    @Test
    fun testGetWordStart_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordBoundary = WordBoundary(Locale("he", "IL"), text)
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d0')))
            .isEqualTo(text.indexOf('\u05d0'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d2')))
            .isEqualTo(text.indexOf('\u05d0'))
        assertThat(wordBoundary.getWordStart(text.indexOf(' ')))
            .isEqualTo(text.indexOf('\u05d0'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d4')))
            .isEqualTo(text.indexOf('\u05d3'))
        assertThat(wordBoundary.getWordStart(text.indexOf('-')))
            .isEqualTo(text.indexOf('\u05d3'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d5')))
            .isEqualTo(text.indexOf('-'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d6')))
            .isEqualTo(text.indexOf('\u05d5'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d7')))
            .isEqualTo(text.indexOf('\u05d7'))
    }

    @Test
    fun testGetWordStart_CJK() { // Japanese HIRAGANA letter + KATAKANA letters
        val text = "\u3042\u30A2\u30A3\u30A4"
        val wordBoundary = WordBoundary(Locale.JAPANESE, text)
        assertThat(wordBoundary.getWordStart(text.indexOf('\u3042')))
            .isEqualTo(text.indexOf('\u3042'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u30A2')))
            .isEqualTo(text.indexOf('\u3042'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\u30A4')))
            .isEqualTo(text.indexOf('\u30A2'))
        assertThat(wordBoundary.getWordStart(text.length))
            .isEqualTo(text.indexOf('\u30A2'))
    }

    @Test
    fun testGetWordStart_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordStart(text.indexOf('i')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordBoundary.getWordStart(text.indexOf('n')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordBoundary.getWordStart(text.indexOf('\'')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordBoundary.getWordStart(text.indexOf('t')))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordBoundary.getWordStart(text.indexOf('t') + 1))
            .isEqualTo(text.indexOf('i'))
        assertThat(wordBoundary.getWordStart(text.indexOf('h')))
            .isEqualTo(text.indexOf('h'))
    }

    @Test
    fun testGetWordStart_isOnPunctuation() {
        val text = "abc!? (^^;) def"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordStart(text.indexOf('!')))
            .isEqualTo(text.indexOf('a'))
        assertThat(wordBoundary.getWordStart(text.indexOf('?') + 1))
            .isEqualTo(text.indexOf('!'))
        assertThat(wordBoundary.getWordStart(text.indexOf(';')))
            .isEqualTo(text.indexOf(';'))
        assertThat(wordBoundary.getWordStart(text.indexOf(')')))
            .isEqualTo(text.indexOf(';'))
        assertThat(wordBoundary.getWordStart(text.length)).isEqualTo(text.indexOf('d'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetWordEnd_out_of_boundary_too_small() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordEnd(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetWordEnd_out_of_boundary_too_big() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordEnd(text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetWordEnd_DONE() {
        val text = "text"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        wordBoundary.getWordEnd(BreakIterator.DONE)
    }

    @Test
    fun testGetWordEnd_Empty_String() {
        val wordBoundary = WordBoundary(Locale.ENGLISH, "")
        assertThat(wordBoundary.getWordEnd(0)).isEqualTo(0)
    }

    @Test
    fun testGetWordEnd() {
        val text = "abc def-ghi. jkl"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordEnd(text.indexOf('a')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf('c')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf(' ')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf('d')))
            .isEqualTo(text.indexOf('-'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('i')))
            .isEqualTo(text.indexOf('.'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('k')))
            .isEqualTo(text.indexOf('l') + 1)
    }

    @Test
    fun testGetWordEnd_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordBoundary = WordBoundary(Locale("he", "IL"), text)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d0')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d2')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf(' ')))
            .isEqualTo(text.indexOf(' '))
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d4')))
            .isEqualTo(text.indexOf('-'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('-')))
            .isEqualTo(text.indexOf('-') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d5')))
            .isEqualTo(text.indexOf('.'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d6')))
            .isEqualTo(text.indexOf('.'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d7'))).isEqualTo(text.length)
    }

    @Test
    fun testGetWordEnd_CJK() { // Japanese HIRAGANA letter + KATAKANA letters
        val text = "\u3042\u30A2\u30A3\u30A4"
        val wordBoundary = WordBoundary(Locale.JAPANESE, text)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u3042')))
            .isEqualTo(text.indexOf('\u3042') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u30A2')))
            .isEqualTo(text.indexOf('\u30A4') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u30A4')))
            .isEqualTo(text.indexOf('\u30A4') + 1)
        assertThat(wordBoundary.getWordEnd(text.length))
            .isEqualTo(text.indexOf('\u30A4') + 1)
    }

    @Test
    fun testGetWordEnd_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordEnd(text.indexOf('i')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('n')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('\'')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('t')))
            .isEqualTo(text.indexOf('t') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('h')))
            .isEqualTo(text.indexOf('e') + 1)
    }

    @Test
    fun testGetWordEnd_isOnPunctuation() {
        val text = "abc!? (^^;) def"
        val wordBoundary = WordBoundary(Locale.ENGLISH, text)
        assertThat(wordBoundary.getWordEnd(text.indexOf('a')))
            .isEqualTo(text.indexOf('!'))
        assertThat(wordBoundary.getWordEnd(text.indexOf('?') + 1))
            .isEqualTo(text.indexOf('?') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('(')))
            .isEqualTo(text.indexOf('(') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('(') + 2))
            .isEqualTo(text.indexOf('(') + 2)
        assertThat(wordBoundary.getWordEnd(text.indexOf(')') + 1))
            .isEqualTo(text.indexOf(')') + 1)
        assertThat(wordBoundary.getWordEnd(text.indexOf('d'))).isEqualTo(text.length)
        assertThat(wordBoundary.getWordEnd(text.length)).isEqualTo(text.length)
    }
}