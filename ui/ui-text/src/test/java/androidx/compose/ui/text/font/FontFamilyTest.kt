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

@RunWith(JUnit4::class)
@OptIn(ExperimentalTextApi::class)
class FontFamilyTest {

    private val resourceId1 = 1
    private val resourceId2 = 2

    @Test(expected = IllegalStateException::class)
    fun `cannot be instantiated with empty font list`() {
        FontFamily(listOf())
    }

    @Test
    fun `two equal family declarations are equal`() {
        val fontFamily = FontFamily(
            Font(
                resId = resourceId1,
                weight = FontWeight.W900,
                style = FontStyle.Italic
            )
        )

        val otherFontFamily = FontFamily(
            Font(
                resId = resourceId1,
                weight = FontWeight.W900,
                style = FontStyle.Italic
            )
        )

        assertThat(fontFamily).isEqualTo(otherFontFamily)
    }

    @Test
    fun `two non equal family declarations are not equal`() {
        val fontFamily = FontFamily(
            Font(
                resId = resourceId1,
                weight = FontWeight.W900,
                style = FontStyle.Italic
            )
        )

        val otherFontFamily = FontFamily(
            Font(
                resId = resourceId1,
                weight = FontWeight.W800,
                style = FontStyle.Italic
            )
        )

        assertThat(fontFamily).isNotEqualTo(otherFontFamily)
    }

    @Test
    fun `can add fallback font at same weight and style`() {
        FontFamily(
            Font(
                resId = resourceId1,
                weight = FontWeight.W900,
                style = FontStyle.Italic
            ),
            Font(
                resId = resourceId1,
                weight = FontWeight.W900,
                style = FontStyle.Italic
            )
        )
    }
}