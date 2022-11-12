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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.properties.Delegates
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldMinMaxLinesTest {
    private val fontSize = 20

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun minLines_smaller_thanInput() {
        displayTextField(
            text = "abc\nabc\nabc",
            minLines = 1
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(3)
            assertThat(height).isEqualTo(fontSize * 3)
        }
    }

    @Test
    fun minLines_greater_thanInput() {
        displayTextField(
            text = "abc",
            minLines = 3
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(1)
            assertThat(height).isEqualTo(fontSize * 3)
        }
    }

    @Test
    fun maxLines_smaller_thanInput() {
        displayTextField(
            text = "abc\nabc\nabc",
            maxLines = 1
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(3)
            assertThat(height).isEqualTo(fontSize)
        }
    }

    @Test
    fun maxLines_greater_thanInput() {
        displayTextField(
            text = "abc",
            maxLines = 3
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(1)
            assertThat(height).isEqualTo(fontSize)
        }
    }

    private fun displayTextField(
        text: String,
        minLines: Int = 1,
        maxLines: Int = Int.MAX_VALUE,
        verify: (Int, TextLayoutResult) -> Unit
    ) {
        var height by Delegates.notNull<Int>()
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                CoreTextField(
                    value = TextFieldValue(text),
                    onValueChange = {},
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .onSizeChanged { height = it.height }
                        .fillMaxWidth(),
                    minLines = minLines,
                    maxLines = maxLines,
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        fontFamily = TEST_FONT_FAMILY,
                        lineHeight = fontSize.sp
                    )
                )
            }
        }

        rule.runOnIdle {
            verify(height, textLayoutResult)
        }
    }
}