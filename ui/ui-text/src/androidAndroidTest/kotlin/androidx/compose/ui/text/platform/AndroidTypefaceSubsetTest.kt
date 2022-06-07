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

package androidx.compose.ui.text.platform

import android.content.Context
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidTypefaceSubsetTest {

    val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    val fontFamily = FontFamily(
        FontTestData.FONT_100_REGULAR,
        FontTestData.FONT_100_ITALIC,
        FontTestData.FONT_200_REGULAR,
        FontTestData.FONT_200_ITALIC,
        FontTestData.FONT_300_REGULAR,
        FontTestData.FONT_300_ITALIC,
        FontTestData.FONT_400_REGULAR,
        FontTestData.FONT_400_ITALIC,
        FontTestData.FONT_500_REGULAR,
        FontTestData.FONT_500_ITALIC,
        FontTestData.FONT_600_REGULAR,
        FontTestData.FONT_600_ITALIC,
        FontTestData.FONT_700_REGULAR,
        FontTestData.FONT_700_ITALIC,
        FontTestData.FONT_800_REGULAR,
        FontTestData.FONT_800_ITALIC,
        FontTestData.FONT_900_REGULAR,
        FontTestData.FONT_900_ITALIC
    )

    @Suppress("DEPRECATION")
    private fun androidTypefaceFromFontFamily(
        context: Context,
        fontFamily: FontFamily,
        necessaryStyles: List<Pair<FontWeight, FontStyle>>? = null
    ): AndroidTypeface {
        @Suppress("DEPRECATION")
        return Typeface(context, fontFamily, necessaryStyles) as AndroidTypeface
    }

    @Test
    fun subset_load_regular_bold_only_and_query_regular() {
        // Load only Regular and Bold font
        val typeface = androidTypefaceFromFontFamily(
            context,
            fontFamily,
            listOf(
                Pair(FontWeight.W400, FontStyle.Normal),
                Pair(FontWeight.W700, FontStyle.Normal)
            )
        )

        val typefaceFromSubset = typeface.getNativeTypeface(
            FontWeight.Normal,
            FontStyle.Normal,
            FontSynthesis.None
        )

        assertThat(typefaceFromSubset).isTypefaceOf(FontWeight.Normal, FontStyle.Normal)
    }

    @Test
    fun subset_load_regular_bold_only_and_query_bold() {
        // Load only Regular and Bold font
        val typeface = androidTypefaceFromFontFamily(
            context,
            fontFamily,
            listOf(
                Pair(FontWeight.W400, FontStyle.Normal),
                Pair(FontWeight.W700, FontStyle.Normal)
            )
        )

        val typefaceFromSubset = typeface.getNativeTypeface(
            FontWeight.Bold,
            FontStyle.Normal,
            FontSynthesis.None
        )

        assertThat(typefaceFromSubset).isTypefaceOf(FontWeight.Bold, FontStyle.Normal)
    }

    @Test
    fun subset_load_regular_bold_only_and_query_black() {
        // Load only Regular and Bold font
        val typeface = androidTypefaceFromFontFamily(
            context,
            fontFamily,
            listOf(
                Pair(FontWeight.W400, FontStyle.Normal),
                Pair(FontWeight.W700, FontStyle.Normal)
            )
        )

        val typefaceFromSubset = typeface.getNativeTypeface(
            FontWeight.Normal,
            FontStyle.Italic,
            FontSynthesis.None
        )

        // The italic font is not loaded, so querying Italic will return Normal font.
        assertThat(typefaceFromSubset).isTypefaceOf(FontWeight.Normal, FontStyle.Normal)
    }
}