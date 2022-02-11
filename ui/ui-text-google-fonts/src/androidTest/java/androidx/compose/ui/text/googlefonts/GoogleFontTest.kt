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

package androidx.compose.ui.text.googlefonts

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@SmallTest
class GoogleFontTest {

    @Test
    fun GoogleFont_create_ComposeFont() {
        val font = GoogleFont("Test font")
        assertThat(font).isInstanceOf(Font::class.java)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun GoogleFont_is_AsyncFont() {
        val font = GoogleFont("Test font")
        assertThat(font.loadingStrategy).isEqualTo(FontLoadingStrategy.Async)
    }

    @Test
    fun GoogleFont_default_W400() {
        val font = GoogleFont("Test")
        assertThat(font.weight).isEqualTo(FontWeight.W400)
    }

    @Test
    fun GoogleFont_default_isNormal() {
        val font = GoogleFont("Test")
        assertThat(font.style).isEqualTo(FontStyle.Normal)
    }

    @Test
    fun GoogleFont_default_wdth_100f() {
        val font = GoogleFont("Test") as GoogleFontImpl
        assertThat(font.width).isWithin(0.1f).of(100f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun GoogleFont_negativeWidth_throws() {
        GoogleFont("name", width = -1.0f)
    }

    @Test
    fun GoogleFont_default_bestEffort_true() {
        val font = GoogleFont("best effort") as GoogleFontImpl
        assertThat(font.bestEffort).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun GoogleFont_throwsOn_emptyName() {
        GoogleFont("")
    }

    @Test
    fun GoogleFont_keepsUrlEncodingRequiredNames() {
        val expected = "!@#$%^&*(){}'<>PYFGCRL?+|AOEUIDHTNS_:QJKXBMWVZ~~`1234567890[]/=\\-;:,."
        val font = GoogleFont(expected) as GoogleFontImpl
        assertThat(font.name).isEqualTo(expected)
    }
}