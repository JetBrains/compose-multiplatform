/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.text.TextRange

/** StringBuilder.appendCodePoint is already defined on JVM so it's called appendCodePointX. */
internal expect fun StringBuilder.appendCodePointX(codePoint: Int): StringBuilder

/**
 * Returns the index of the character break preceding [index].
 */
internal expect fun String.findPrecedingBreak(index: Int): Int

/**
 * Returns the index of the character break following [index]. Returns -1 if there are no more
 * breaks before the end of the string.
 */
internal expect fun String.findFollowingBreak(index: Int): Int

internal fun CharSequence.findParagraphStart(startIndex: Int): Int {
    for (index in startIndex - 1 downTo 1) {
        if (this[index - 1] == '\n') {
            return index
        }
    }
    return 0
}

internal fun CharSequence.findParagraphEnd(startIndex: Int): Int {
    for (index in startIndex + 1 until this.length) {
        if (this[index] == '\n') {
            return index
        }
    }
    return this.length
}

/**
 * Returns the text range of the paragraph at the given character offset.
 *
 * Paragraphs are separated by Line Feed character (\n).
 */
internal fun CharSequence.getParagraphBoundary(index: Int): TextRange {
    return TextRange(findParagraphStart(index), findParagraphEnd(index))
}