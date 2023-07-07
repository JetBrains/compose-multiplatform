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
import kotlin.test.*

class StringTest {

    @Test
    fun emptyStringTransformations() {
        val empty = ""

        assertEquals(empty.toUpperCase(Locale.current), empty)
        assertEquals(empty.toLowerCase(Locale.current), empty)
        assertEquals(empty.capitalize(Locale.current), empty)
        assertEquals(empty.decapitalize(Locale.current), empty)
    }

    @Test
    fun twoCharactersRepresentedAsSingleUnicodeLetter() {
        // https://en.wikipedia.org/wiki/D%C5%BE
        val lowercase = "ǆ" // U+01C6
        val titlecase = "ǅ" // U+01C5
        val uppercase = "Ǆ" // U+01C4
        val serbianLocale = LocaleList("sr")

        assertEquals(lowercase.toUpperCase(serbianLocale), uppercase)
        assertEquals(lowercase.toLowerCase(serbianLocale), lowercase)
        assertEquals(lowercase.capitalize(serbianLocale), titlecase)
        assertEquals(lowercase.decapitalize(serbianLocale), lowercase)

        assertEquals(titlecase.toUpperCase(serbianLocale), uppercase)
        assertEquals(titlecase.toLowerCase(serbianLocale), lowercase)
        assertEquals(titlecase.capitalize(serbianLocale), titlecase)
        assertEquals(titlecase.decapitalize(serbianLocale), lowercase)

        assertEquals(uppercase.toUpperCase(serbianLocale), uppercase)
        assertEquals(uppercase.toLowerCase(serbianLocale), lowercase)
        assertEquals(uppercase.capitalize(serbianLocale), uppercase)
        assertEquals(uppercase.decapitalize(serbianLocale), lowercase)
    }

    @Test
    fun directionality() {
        assertEquals(StrongDirectionType.None, '0'.code.strongDirectionType()) // Number
        assertEquals(StrongDirectionType.Ltr, 'A'.code.strongDirectionType()) // Latin
        assertEquals(StrongDirectionType.Rtl, 'א'.code.strongDirectionType()) // Hebrew
        assertEquals(StrongDirectionType.Rtl, '؈'.code.strongDirectionType()) // Arabic
    }
}