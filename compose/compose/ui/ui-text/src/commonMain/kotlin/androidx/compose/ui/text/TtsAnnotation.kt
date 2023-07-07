/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * An annotation that contains the metadata intended for text-to-speech engine. If the text is
 * being processed by a text-to-speech engine, the engine may use the data in this annotation in
 * addition to or instead of its associated text.
 */
sealed class TtsAnnotation

/**
 * The text associated with this annotation is a series of characters that have to be read
 * verbatim.
 * @param verbatim a string where the characters are read verbatim except whitespace.
 */
class VerbatimTtsAnnotation(
    val verbatim: String
) : TtsAnnotation() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerbatimTtsAnnotation) return false
        if (verbatim != other.verbatim) return false
        return true
    }

    override fun hashCode(): Int {
        return verbatim.hashCode()
    }

    override fun toString(): String {
        return "VerbatimTtsAnnotation(verbatim=$verbatim)"
    }
}