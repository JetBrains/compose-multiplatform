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

package androidx.compose.ui.text

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalTextApi::class)
@RunWith(JUnit4::class)
class TextDelegateTest {
    private val density = Density(density = 1f)
    private val resourceLoader = mock<Font.ResourceLoader>()

    @Test
    fun `constructor with default values`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
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
            resourceLoader = resourceLoader
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
            resourceLoader = resourceLoader
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
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.overflow).isEqualTo(overflow)
    }

    @Test(expected = IllegalStateException::class)
    fun `minIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        textDelegate.minIntrinsicWidth
    }

    @Test(expected = IllegalStateException::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        textDelegate.maxIntrinsicWidth
    }

    @Test
    fun resolveTextDirection_null() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                null
            )
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                null
            )
        ).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun resolveTextDirection_Content() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Content
            )
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Content
            )
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun resolveTextDirection_ContentOrLtr() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.ContentOrLtr
            )
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.ContentOrLtr
            )
        ).isEqualTo(TextDirection.ContentOrLtr)
    }

    @Test
    fun resolveTextDirection_ContentOrRtl() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.ContentOrRtl
            )
        ).isEqualTo(TextDirection.ContentOrRtl)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.ContentOrRtl
            )
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun resolveTextDirection_Ltr() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Ltr
            )
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Ltr
            )
        ).isEqualTo(TextDirection.Ltr)
    }

    @Test
    fun resolveTextDirection_Rtl() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Rtl
            )
        ).isEqualTo(TextDirection.Rtl)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Rtl
            )
        ).isEqualTo(TextDirection.Rtl)
    }
}
