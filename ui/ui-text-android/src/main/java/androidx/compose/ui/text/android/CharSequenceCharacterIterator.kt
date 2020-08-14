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
package androidx.compose.ui.text.android

import java.text.CharacterIterator

/**
 * An implementation of [java.text.CharacterIterator] that iterates over a given CharSequence.
 *
 * Note: This file is copied from
 * [CharSequenceCharacterIterator.java](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/CharSequenceCharacterIterator.java).
 *
 * @constructor Constructs the iterator given a CharSequence and a range.  The position of the
 * iterator index is set to the beginning of the range.
 * @param charSequence The given char sequence.
 * @param start The index of the beginning of the range.
 * @param end The index of the end of the range.
 */
internal class CharSequenceCharacterIterator(
    private val charSequence: CharSequence,
    private val start: Int,
    private val end: Int
) : CharacterIterator {
    private var index: Int = start

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     *
     * @return the first character in the text, or [java.text.CharacterIterator.DONE] if
     * the text is empty
     * @see getBeginIndex
     */
    override fun first(): Char {
        index = start
        return current()
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     *
     * @return the last character in the text, or [java.text.CharacterIterator.DONE] if the
     * text is empty
     * @see .getEndIndex
     */
    override fun last(): Char {
        return if (start == end) {
            index = end
            CharacterIterator.DONE
        } else {
            index = end - 1
            charSequence[index]
        }
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     *
     * @return the character at the current position or [java.text.CharacterIterator.DONE]
     * if the current
     * position is off the end of the text.
     * @see .getIndex
     */
    override fun current(): Char {
        return if (index == end) CharacterIterator.DONE else charSequence[index]
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of [java.text.CharacterIterator.DONE] is returned.
     *
     * @return the character at the new position or [java.text.CharacterIterator.DONE] if
     * the new
     * position is off the end of the text range.
     */
    override fun next(): Char {
        index++
        return if (index >= end) {
            index = end
            CharacterIterator.DONE
        } else {
            charSequence[index]
        }
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of [java.text.CharacterIterator.DONE] is
     * returned.
     *
     * @return the character at the new position or [java.text.CharacterIterator.DONE] if
     * the current
     * position is equal to getBeginIndex().
     */
    override fun previous(): Char {
        return if (index <= start) {
            CharacterIterator.DONE
        } else {
            index--
            charSequence[index]
        }
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     *
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or
     * [java.text.CharacterIterator.DONE] if the specified
     * position is equal to getEndIndex()
     */
    override fun setIndex(position: Int): Char {
        return if (position in start..end) {
            index = position
            current()
        } else {
            throw IllegalArgumentException("invalid position")
        }
    }

    /**
     * Returns the start index of the text.
     *
     * @return the index at which the text begins.
     */
    override fun getBeginIndex(): Int {
        return start
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     *
     * @return the index after the last character in the text
     */
    override fun getEndIndex(): Int {
        return end
    }

    /**
     * Returns the current index.
     *
     * @return the current index.
     */
    override fun getIndex(): Int {
        return index
    }

    /**
     * Create a copy of this iterator
     *
     * @return A copy of this
     */
    override fun clone(): Any {
        return try {
            @Suppress("ABSTRACT_SUPER_CALL")
            super.clone()
        } catch (e: CloneNotSupportedException) {
            throw InternalError()
        }
    }
}