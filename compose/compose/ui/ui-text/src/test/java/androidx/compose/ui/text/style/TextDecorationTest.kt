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
package androidx.compose.ui.text.style

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextDecorationTest {

    @Test
    fun `contains with single textDecoration`() {
        val textDecoration = TextDecoration.None
        assertThat(TextDecoration.None in textDecoration).isTrue()
        assertThat(TextDecoration.LineThrough in textDecoration).isFalse()
    }

    @Test
    fun `contains with multiple textDecorations`() {
        val textDecoration = TextDecoration.Underline + TextDecoration.LineThrough
        assertThat(TextDecoration.Underline in textDecoration).isTrue()
        assertThat(TextDecoration.LineThrough in textDecoration).isTrue()
        // since 0 is always included
        assertThat(TextDecoration.None in textDecoration).isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `combine with empty list returns none`() {
        assertThat(TextDecoration.combine(listOf())).isEqualTo(TextDecoration.None)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `combine with single element`() {
        assertThat(TextDecoration.combine(listOf(TextDecoration.Underline)))
            .isEqualTo(TextDecoration.Underline)
    }

    @Test
    fun `toString with single textDecoration`() {
        assertThat(TextDecoration.None.toString()).isEqualTo("TextDecoration.None")
        assertThat(TextDecoration.Underline.toString()).isEqualTo("TextDecoration.Underline")
        assertThat(TextDecoration.LineThrough.toString()).isEqualTo("TextDecoration.LineThrough")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `toString with empty combined`() {
        assertThat(TextDecoration.combine(listOf()).toString()).isEqualTo("TextDecoration.None")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `toString with single combined`() {
        assertThat(TextDecoration.combine(listOf(TextDecoration.LineThrough)).toString())
            .isEqualTo("TextDecoration.LineThrough")
    }

    @Test
    fun `toString with multiple textDecorations`() {
        assertThat(
            (TextDecoration.Underline + TextDecoration.LineThrough).toString()
        ).isEqualTo("TextDecoration[Underline, LineThrough]")
    }
}