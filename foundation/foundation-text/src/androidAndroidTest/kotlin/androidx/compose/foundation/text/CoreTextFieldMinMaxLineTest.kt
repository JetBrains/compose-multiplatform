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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Providers
import androidx.compose.ui.Modifier
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.asFontFamily
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldMinMaxLineTest {

    private val fontFamily = ResourceFont(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    private val density = Density(density = 1f, fontScale = 1f)

    @get:Rule
    val rule = createComposeRule()

    @Test(expected = IllegalArgumentException::class)
    fun textField_maxLines_should_be_greater_than_zero() {
        rule.setContent {
            Providers(DensityAmbient provides density) {
                CoreTextField(
                    value = TextFieldValue(""),
                    onValueChange = {},
                    maxLines = 0,
                )
            }
        }
    }

    @Test
    fun textField_maxLines_moreThanLineCount_doesNotChangeHeight() {
        val string = "a"
        val fontSize = 10.sp
        val composableWidth = 20.dp // line count will be 1
        val expectedLineCount = 1
        val maxLines = 2 // any number greater than 1
        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)

        val (textLayout, height) = setContentMaxLines(string, textStyle, composableWidth, maxLines)

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(height).isNotNull()
            assertThat(height).isEqualTo(fontSize.toIntPx())
            assertThat(textLayout?.lineCount).isEqualTo(expectedLineCount)
        }
    }

    @Test
    fun textField_maxLines_equalToLineCount_doesNotChangeHeight() {
        val string = "a"
        val fontSize = 10.sp
        val composableWidth = 20.dp // line count will be 1
        val expectedLineCount = 1
        val maxLines = 1 // equal to expectedLineCount
        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)

        val (textLayout, height) = setContentMaxLines(string, textStyle, composableWidth, maxLines)

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(height).isNotNull()
            assertThat(height).isEqualTo(fontSize.toIntPx())
            assertThat(textLayout?.lineCount).isEqualTo(expectedLineCount)
        }
    }

    @Test
    fun textField_maxLines_lessThanLineCount_changesHeight() {
        val density = Density(density = 1f, fontScale = 1f)
        val string = "a".repeat(10)
        val fontSize = 10.sp
        val composableWidth = 20.dp // line count will be 10 * 10 / 20 = 5
        val expectedLineCount = 5
        val maxLines = 1

        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)

        val (textLayout, height) = setContentMaxLines(string, textStyle, composableWidth, maxLines)

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(height).isNotNull()
            assertThat(height).isEqualTo(fontSize.toIntPx())
            assertThat(textLayout?.lineCount).isEqualTo(expectedLineCount)
        }
    }

    @Test
    fun textField_maxLines_withEmptyText() {
        val string = ""
        val fontSize = 10.sp
        val composableWidth = 10.dp
        val maxLines = 5
        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)

        val (textLayout, height) = setContentMaxLines(string, textStyle, composableWidth, maxLines)

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(height).isNotNull()
            assertThat(height).isEqualTo(fontSize.toIntPx())
            assertThat(textLayout?.lineCount).isEqualTo(1)
        }
    }

    private fun setContentMaxLines(
        string: String,
        textStyle: TextStyle,
        width: Dp,
        maxLines: Int = Int.MAX_VALUE
    ): Pair<TextLayoutResult?, Int?> {
        var textLayout: TextLayoutResult? = null
        var height: Int? = null

        rule.setContent {
            Providers(DensityAmbient provides density) {
                CoreTextField(
                    value = TextFieldValue(string),
                    onValueChange = {},
                    textStyle = textStyle,
                    onTextLayout = { textLayout = it },
                    maxLines = maxLines,
                    modifier = Modifier
                        .width(width)
                        .onGloballyPositioned {
                            height = it.size.height
                        }
                )
            }
        }

        return Pair(textLayout, height)
    }
}