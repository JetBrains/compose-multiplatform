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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class SkiaParagraphIntrinsicsTest {

    @Test
    fun testLatinResolveTextDirection() {
        val text = "Hello World"
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }

    @Test
    fun testWeakResolveTextDirection() {
        val text = "12345"
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }

    @Test
    fun testLocaleFallbackResolveTextDirection() {
        val text = "12345"
        val ltrLocale = LocaleList(Locale("en"))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, null, ltrLocale))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr, ltrLocale))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl, ltrLocale))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Content, ltrLocale))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrLtr, ltrLocale))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl, ltrLocale))

        val rtlLocale = LocaleList(Locale("ar"))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, null, rtlLocale))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr, rtlLocale))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl, rtlLocale))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Content, rtlLocale))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrLtr, rtlLocale))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl, rtlLocale))
    }

    @Test
    fun testArabicResolveTextDirection() {
        val text = "مرحبا بالعالم"
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }

    @Test
    fun testArabicEmbeddingResolveTextDirection() {
        val text = "\u202Bمرحبا بالعالم\u202C Hello World"
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }

    @Test
    fun testArabicOverrideResolveTextDirection() {
        val text = "\u202Eمرحبا بالعالم\u202C Hello World"
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }

    @Test
    fun testArabicIsolateResolveTextDirection() {
        val text = "\u2067مرحبا بالعالم\u2069 Hello World"
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, null))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Ltr))
        assertEquals(ResolvedTextDirection.Rtl, resolveTextDirection(text, TextDirection.Rtl))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.Content))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrLtr))
        assertEquals(ResolvedTextDirection.Ltr, resolveTextDirection(text, TextDirection.ContentOrRtl))
    }
}