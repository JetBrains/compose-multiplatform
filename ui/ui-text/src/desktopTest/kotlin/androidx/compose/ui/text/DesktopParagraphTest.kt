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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopParagraphTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontLoader = FontLoader()
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyMeasureFont =
        FontFamily(
            Font(
                "font/sample_font.ttf",
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )

    @Test
    fun getBoundingBox_basic() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )

            for (i in 0..text.length - 1) {
                val box = paragraph.getBoundingBox(i)
                Truth.assertThat(box.left).isEqualTo(i * fontSizeInPx)
                Truth.assertThat(box.right).isEqualTo((i + 1) * fontSizeInPx)
                Truth.assertThat(box.top).isZero()
                Truth.assertThat(box.bottom).isEqualTo(fontSizeInPx + 10)
            }
        }
    }

    @Test
    fun getBoundingBox_multicodepoints() {
        with(defaultDensity) {
            val text = "h\uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            Truth.assertThat(paragraph.getBoundingBox(0))
                .isEqualTo(Rect(0f, 0f, fontSizeInPx, 60f))

            Truth.assertThat(paragraph.getBoundingBox(1))
                .isEqualTo(Rect(fontSizeInPx, 0f, fontSizeInPx * 2.5f, 60f))

            Truth.assertThat(paragraph.getBoundingBox(5))
                .isEqualTo(Rect(fontSizeInPx, 0f, fontSizeInPx * 2.5f, 60f))
        }
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        density: Density? = null,
        width: Float = 2000f
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont
            ).merge(style),
            maxLines = maxLines,
            ellipsis = ellipsis,
            width = width,
            density = density ?: defaultDensity,
            resourceLoader = fontLoader
        )
    }
}