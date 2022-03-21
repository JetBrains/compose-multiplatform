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

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
class DeviceFontFamilyNameFontTest {

    val context = InstrumentationRegistry.getInstrumentation().context

    @Test(expected = IllegalArgumentException::class)
    fun emptyFamilyName_throwsIllegalArgumentException() {
        Font(DeviceFontFamilyName(""))
    }

    @Test
    fun fontWithAndroidFontFamilyName_isOptional() {
        val font = Font(DeviceFontFamilyName("some name"))
        assertThat(font.loadingStrategy).isEqualTo(FontLoadingStrategy.OptionalLocal)
    }

    @Test
    fun missedFont_resolvesNull() {
        val font = Font(DeviceFontFamilyName(fontNameNotInstalledOnSystem())) as AndroidFont
        val actual = font.typefaceLoader.loadBlocking(context, font)
        assertThat(actual).isNull()
    }

    @Test
    fun missedFont_resolvesNull_allWeightAllStyles() {
        val name = DeviceFontFamilyName(fontNameNotInstalledOnSystem())
        for (style in listOf(FontStyle.Italic, FontStyle.Normal)) {
            for (weight in 100..1000) {
                val font = Font(name, FontWeight(weight), style) as AndroidFont
                val actual = font.typefaceLoader.loadBlocking(context, font)
                assertThat(actual).isNull()
            }
        }
    }

    @Test
    fun cursive_resolvesNonNull() {
        // this family name is defined in aosp fonts.xml, and is generally available
        assumeTrue(Typeface.create("cursive", Typeface.NORMAL)
            != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val name = DeviceFontFamilyName("cursive")
        val font = Font(name) as AndroidFont
        val actual = font.typefaceLoader.loadBlocking(context, font)
        assertThat(actual).isNotNull()
    }

    @Test
    fun cursive_resolvesNonNull_allWeightAllStyles() {
        // this family name is defined in aosp fonts.xml, and is generally available
        assumeTrue(Typeface.create("cursive", Typeface.NORMAL)
            != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val name = DeviceFontFamilyName("cursive")
        for (style in listOf(FontStyle.Italic, FontStyle.Normal)) {
            for (weight in 100..1000) {
                val font = Font(name, FontWeight(weight), style) as AndroidFont
                val actual = font.typefaceLoader.loadBlocking(context, font)
                assertThat(actual).isNotNull()
            }
        }
    }

    private fun fontNameNotInstalledOnSystem(): String {
        var fontName = "This is a font name that is not installed, like actually, and we will " +
            "append random characters until it fails to lookup"
        var index = 0
        // check that it's actually not installed, and append nonsense until wo confirm it misses
        while (Typeface.create(fontName, Typeface.NORMAL)
            != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)) {
            fontName += index++.toString()
        }
        return fontName
    }
}