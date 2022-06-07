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
package androidx.compose.ui.text.font

import androidx.compose.ui.text.ExperimentalTextApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalTextApi
@RunWith(JUnit4::class)
class FontTest {

    private val resourceId = 1

    @Test
    fun `default values`() {
        val font = Font(resId = resourceId)
        assertThat(font.weight).isEqualTo(FontWeight.Normal)
        assertThat(font.style).isEqualTo(FontStyle.Normal)
    }

    @Test
    fun `two equal font declarations are equal`() {
        val font = Font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )

        val otherFont = Font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )

        assertThat(font).isEqualTo(otherFont)
    }

    @Test
    fun `two non equal font declarations are not equal`() {
        val font = Font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )

        val otherFont = Font(
            resId = resourceId,
            weight = FontWeight.W800,
            style = FontStyle.Italic
        )

        assertThat(font).isNotEqualTo(otherFont)
    }

    @Test
    fun `asFontFamilyList returns a FontFamily`() {
        val font = Font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )

        val fontFamily = font.toFontFamily() as FontListFontFamily

        assertThat(fontFamily).isNotNull()
        assertThat(fontFamily).isNotEmpty()
        assertThat(fontFamily[0]).isSameInstanceAs(font)
    }
}