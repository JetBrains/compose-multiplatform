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

package androidx.compose.foundation.text.selection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextFieldSelectionDelegateTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fontFamily = TEST_FONT_FAMILY
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    @OptIn(ExperimentalTextApi::class)
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun getTextFieldSelection_long_press_select_word_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = 2,
            rawEndOffset = 2,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(range.start).isEqualTo(0)
        assertThat(range.end).isEqualTo("hello".length)
    }

    @Test
    fun getTextFieldSelection_long_press_select_word_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = 5,
            rawEndOffset = 5,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(range.start).isEqualTo(text.indexOf("\u05D3"))
        assertThat(range.end).isEqualTo(text.indexOf("\u05D5") + 1)
    }

    @Test
    fun getTextFieldSelection_long_press_drag_handle_not_cross_select_word() {
        val text = "hello world"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val rawStartOffset = text.indexOf('e')
        val rawEndOffset = text.indexOf('r')

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = rawStartOffset,
            rawEndOffset = rawEndOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(range.start).isEqualTo(0)
        assertThat(range.end).isEqualTo(text.length)
    }

    @Test
    fun getTextFieldSelection_long_press_drag_handle_cross_select_word() {
        val text = "hello world"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val rawStartOffset = text.indexOf('r')
        val rawEndOffset = text.indexOf('e')

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = rawStartOffset,
            rawEndOffset = rawEndOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(range.start).isEqualTo(text.length)
        assertThat(range.end).isEqualTo(0)
    }

    @Test
    fun getTextFieldSelection_drag_select_range_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo wor" is selected.
        val startOffset = text.indexOf("l")
        val endOffset = text.indexOf("r") + 1

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.None
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_drag_select_range_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "\u05D1\u05D2 \u05D3" is selected.
        val startOffset = text.indexOf("\u05D1")
        val endOffset = text.indexOf("\u05D3") + 1

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Character
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_drag_select_range_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
        val text = textLtr + textRtl
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo"+"\u05D0\u05D1\u05D2" is selected
        val startOffset = text.indexOf("l")
        val endOffset = text.indexOf("\u05D2") + 1

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Character
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_drag_handles_crossed_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo wor" is selected.
        val startOffset = text.indexOf("r") + 1
        val endOffset = text.indexOf("l")

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Character
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_drag_handles_crossed_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "\u05D1\u05D2 \u05D3" is selected.
        val startOffset = text.indexOf("\u05D3") + 1
        val endOffset = text.indexOf("\u05D1")

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Character
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_drag_handles_crossed_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
        val text = textLtr + textRtl
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo"+"\u05D0\u05D1\u05D2" is selected
        val startOffset = text.indexOf("\u05D2") + 1
        val endOffset = text.indexOf("l")

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Character,
        )

        // Assert.
        assertThat(range.start).isEqualTo(startOffset)
        assertThat(range.end).isEqualTo(endOffset)
    }

    @Test
    fun getTextFieldSelection_empty_string() {
        val text = ""
        val fontSize = 20.sp

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // Act.
        val range = getTextFieldSelection(
            textLayoutResult = textLayoutResult,
            rawStartOffset = 0,
            rawEndOffset = 0,
            previousSelection = null,
            isStartHandle = true,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(range.start).isEqualTo(0)
        assertThat(range.end).isEqualTo(0)
    }

    @OptIn(InternalFoundationTextApi::class)
    private fun simpleTextLayout(
        text: String = "",
        fontSize: TextUnit = TextUnit.Unspecified,
        density: Density
    ): TextLayoutResult {
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        return TextDelegate(
            text = annotatedString,
            style = TextStyle(),
            density = density,
            fontFamilyResolver = fontFamilyResolver
        ).layout(Constraints(), LayoutDirection.Ltr)
    }
}
