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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalFoundationTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextDelegateWidthWithLetterSpacingTest {
    private val fontFamily = TEST_FONT_FAMILY

    /**
     * values are exact values for the repro case (on Pixel4, Android 11)
     */
    private val density = Density(3.051f, 1.15f)
    private val letterSpacing = 0.4.sp
    private val lineHeight = 16.sp
    private val fontSize = 12.sp
    private val context = InstrumentationRegistry.getInstrumentation().context
    @OptIn(ExperimentalTextApi::class)
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun letterSpacing_and_lineHeight() {
        assertLineCount(
            TextStyle(letterSpacing = letterSpacing, lineHeight = lineHeight)
        )
    }

    @Test
    fun only_letterSpacing() {
        assertLineCount(TextStyle(letterSpacing = letterSpacing))
    }

    @Test
    fun only_lineHeight() {
        assertLineCount(TextStyle(lineHeight = lineHeight))
    }

    @Test
    fun no_lineHeight_or_letterSpacing() {
        assertLineCount(TextStyle())
    }

    private fun assertLineCount(style: TextStyle) {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "This is a callout message"),
            style = style.copy(
                fontFamily = fontFamily,
                fontSize = fontSize
            ),
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )
        val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)
        assertThat(layoutResult.lineCount).isEqualTo(1)
    }
}
