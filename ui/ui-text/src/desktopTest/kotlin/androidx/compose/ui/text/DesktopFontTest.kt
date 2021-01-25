/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.text.platform.GenericFontFamiliesMapping
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.Typeface
import com.google.common.truth.Truth
import org.jetbrains.skija.Data
import org.jetbrains.skija.Typeface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopFontTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontLoader = FontLoader()

    private val fontListFontFamily by lazy {
        FontFamily(
            Font(
                "font/sample_font.ttf"
            ),
            Font(
                "font/test_400_italic.ttf",
                style = FontStyle.Italic
            )
        )
    }

    private val loadedTypeface by lazy {
        val bytes = Thread
            .currentThread()
            .contextClassLoader
            .getResourceAsStream("font/sample_font.ttf")!!
            .readAllBytes()
        Typeface.makeFromData(Data.makeFromBytes(bytes))
    }

    private val loadedFontFamily by lazy {
        FontFamily(Typeface(loadedTypeface))
    }

    @Test
    fun ensureRegistered() {
        val fontListAlias =
            "-compose-0db2ec7083b4661dae92b0fc5a9b4ec87df7253027df3b1a4b5abc69a60518aa"
        Truth.assertThat(fontLoader.ensureRegistered(fontListFontFamily))
            .isEqualTo(listOf(fontListAlias))

        Truth.assertThat(fontLoader.ensureRegistered(FontFamily.Cursive))
            .isEqualTo(GenericFontFamiliesMapping[FontFamily.Cursive.name])

        Truth.assertThat(fontLoader.ensureRegistered(FontFamily.Default))
            .isEqualTo(emptyList<String>())

        Truth.assertThat(fontLoader.ensureRegistered(loadedFontFamily))
            .isEqualTo(listOf("Sample Font"))
    }

    @Test
    fun findTypeface() {
        Truth.assertThat(fontLoader.findTypeface(fontListFontFamily)!!.isItalic)
            .isEqualTo(false)

        Truth.assertThat(
            fontLoader.findTypeface(
                fontListFontFamily,
                fontStyle = FontStyle.Italic
            )!!.isItalic
        )
            .isEqualTo(true)

        Truth.assertThat(fontLoader.findTypeface(loadedFontFamily))
            .isEqualTo(loadedTypeface)
    }
}