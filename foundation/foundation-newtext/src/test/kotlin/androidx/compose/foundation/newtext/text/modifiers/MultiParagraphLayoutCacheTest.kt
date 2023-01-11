/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class MultiParagraphLayoutCacheTest {
    private val density = Density(density = 1f)
    private val fontFamilyResolver = mock<FontFamily.Resolver>()

    @Test(expected = IllegalStateException::class)
    fun whenMinInstrinsicWidth_withoutLayout_throws() {
        val textDelegate = MultiParagraphLayoutCache(
            text = AnnotatedString(""),
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver
        ).also {
            it.density = density
        }

        textDelegate.minIntrinsicWidth
    }

    @Test(expected = IllegalStateException::class)
    fun whenMaxIntrinsicWidth_withoutLayout_throws() {
        val textDelegate = MultiParagraphLayoutCache(
            text = AnnotatedString(""),
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver
        ).also {
            it.density = density
        }

        textDelegate.maxIntrinsicWidth
    }
}
