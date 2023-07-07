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

/**
 *  Defines whether the font is [Italic] or [Normal].
 *
 *  @see Font
 *  @see FontFamily
 */
// TODO(b/205312869) This constructor should not be public as it leads to FontStyle([cursor]) in AS
@kotlin.jvm.JvmInline
value class FontStyle(val value: Int) {

    override fun toString(): String {
        return when (this) {
            Normal -> "Normal"
            Italic -> "Italic"
            else -> "Invalid"
        }
    }

    companion object {
        /** Use the upright glyphs */
        val Normal = FontStyle(0)

        /** Use glyphs designed for slanting */
        val Italic = FontStyle(1)

        /** Returns a list of possible values of [FontStyle]. */
        fun values(): List<FontStyle> = listOf(Normal, Italic)
    }
}
