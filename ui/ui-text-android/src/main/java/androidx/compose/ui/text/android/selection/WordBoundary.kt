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
import java.text.BreakIterator
import java.util.Locale

/**
 * Helper class to get word boundary for offset.
 *
 * Returns the start and end of the word at the given offset. Characters not part of a word, such as
 * spaces, symbols, and punctuation, have word breaks on both sides. In such cases, this method will
 * return [offset, offset+1].
 *
 * Word boundaries are defined more precisely in Unicode Standard Annex #29
 * http://www.unicode.org/reports/tr29/#Word_Boundaries
 *
 * Note: The contents of this file is initially copied from
 * [Editor.java](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/Editor.java)
 * @param locale Locale of the input text.
 * @param text The input text to be analyzed.
 * @suppress
 */
@InternalPlatformTextApi
class WordBoundary(
    locale: Locale,
    text: CharSequence
) {
    /**
     * This word iterator is set with text and used to determine word boundaries when a user is
     * selecting text.
     */
    private val wordIterator: WordIterator = WordIterator(text, 0, text.length, locale)

    /**
     * Get the start of the word which the given offset is in.
     *
     * @return the offset of the start of the word.
     */
    fun getWordStart(offset: Int): Int {
        // FIXME - For this and similar methods we're not doing anything to check if there's
        //  a LocaleSpan in the text, this may be something we should try handling or checking for.
        var retOffset = wordIterator.prevBoundary(offset)
        retOffset =
            if (wordIterator.isOnPunctuation(retOffset)) {
                // On punctuation boundary or within group of punctuation, find punctuation start.
                wordIterator.getPunctuationBeginning(offset)
            } else {
                // Not on a punctuation boundary, find the word start.
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(offset)
            }
        return if (retOffset == BreakIterator.DONE) {
            offset
        } else retOffset
    }

    /**
     * Get the end of the word which the given offset is in.
     *
     * @return the offset of the end of the word.
     */
    fun getWordEnd(offset: Int): Int {
        var retOffset = wordIterator.nextBoundary(offset)
        retOffset =
            if (wordIterator.isAfterPunctuation(retOffset)) {
                // On punctuation boundary or within group of punctuation, find punctuation end.
                wordIterator.getPunctuationEnd(offset)
            } else { // Not on a punctuation boundary, find the word end.
                wordIterator.getNextWordEndOnTwoWordBoundary(offset)
            }
        return if (retOffset == BreakIterator.DONE) {
            offset
        } else retOffset
    }
}