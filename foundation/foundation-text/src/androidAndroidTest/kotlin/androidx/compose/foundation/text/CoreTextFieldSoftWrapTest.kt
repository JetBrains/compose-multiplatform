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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldSoftWrapTest {

    private val fontFamily = ResourceFont(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textField_softWrapFalse_returnsSizeForMaxIntrinsicWidth() {
        val density = Density(density = 1f, fontScale = 1f)
        val fontSize = 100.sp
        val composableWidth = 50.dp
        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
        val string = "a".repeat(20)

        var textLayout: TextLayoutResult? = null
        var width: Int? = null

        rule.setContent {
            Providers(DensityAmbient provides density) {
                CoreTextField(
                    value = TextFieldValue(string),
                    onValueChange = {},
                    textStyle = textStyle,
                    softWrap = false,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier.width(composableWidth)
                        .onGloballyPositioned {
                            width = it.size.width
                        }
                )
            }
        }

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(width).isNotNull()
            assertThat(width).isEqualTo(composableWidth.toIntPx())
            assertThat(textLayout?.lineCount).isEqualTo(1)
        }
    }

    @Test
    fun textField_softWrapTrue_respectsTheGivenMaxWidth() {
        val density = Density(density = 1f, fontScale = 1f)
        val fontSize = 100.sp
        val composableWidth = 100.dp
        val textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
        val string = "a".repeat(20)

        var textLayout: TextLayoutResult? = null
        var width: Int? = null

        rule.setContent {
            Providers(DensityAmbient provides density) {
                CoreTextField(
                    value = TextFieldValue(string),
                    onValueChange = {},
                    textStyle = textStyle,
                    softWrap = true,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier.width(composableWidth)
                        .onGloballyPositioned {
                            width = it.size.width
                        }
                )
            }
        }

        with(density) {
            assertThat(textLayout).isNotNull()
            assertThat(width).isNotNull()
            assertThat(width).isEqualTo(composableWidth.toIntPx())
            // each character has the same width as composable width
            // therefore the string.length is the line count
            assertThat(textLayout?.lineCount).isEqualTo(string.length)
        }
    }
}