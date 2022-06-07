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

package androidx.compose.foundation.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalFoundationTextApi::class)
@RunWith(JUnit4::class)
class TextDelegateTest {
    private val density = Density(density = 1f)
    private val fontFamilyResolver = mock<FontFamily.Resolver>()

    @Test
    fun `constructor with default values`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        assertThat(textDelegate.maxLines).isEqualTo(Int.MAX_VALUE)
        assertThat(textDelegate.overflow).isEqualTo(TextOverflow.Clip)
    }

    @Test
    fun `constructor with customized text(TextSpan)`() {
        val text = AnnotatedString("Hello")
        val textDelegate = TextDelegate(
            text = text,
            style = TextStyle.Default,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        assertThat(textDelegate.text).isEqualTo(text)
    }

    @Test
    fun `constructor with customized maxLines`() {
        val maxLines = 8

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            maxLines = maxLines,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        assertThat(textDelegate.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor with customized overflow`() {
        val overflow = TextOverflow.Ellipsis

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            overflow = overflow,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        assertThat(textDelegate.overflow).isEqualTo(overflow)
    }

    @Test(expected = IllegalStateException::class)
    fun `minIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        textDelegate.minIntrinsicWidth
    }

    @Test(expected = IllegalStateException::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )

        textDelegate.maxIntrinsicWidth
    }
}
