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

package androidx.compose.ui.input

import android.graphics.Matrix
import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.build
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.R
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CursorAnchorInfoBuilderTest {

    private val fontFamilyMeasureFont = Font(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).toFontFamily()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val matrix = Matrix()

    @Test
    fun testSelectionDefault() {
        val textFieldValue = TextFieldValue()
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(0)
    }

    @Test
    fun testSelectionCursor() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(2))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(2)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testSelectionRange() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1, 2))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(1)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testCompositionNone() {
        val textFieldValue = TextFieldValue(composition = null)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testCompositionCoveringAllString() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
    }

    @Test
    fun testCompositionCoveringPortionOfString() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val textFieldValue = TextFieldValue(
            word1 + word2 + word3,
            composition = TextRange(word1.length, (word1 + word2).length)
        )
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(word1.length)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(word2)
    }

    @Test
    fun testResetsBetweenExecutions() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(textFieldValue.composition!!.min)

        val cursorAnchorInfo1 = builder.build(
            TextFieldValue("abcd"),
            getTextLayoutResult(textFieldValue.text),
            matrix
        )

        assertThat(cursorAnchorInfo1.composingText).isNull()
        assertThat(cursorAnchorInfo1.composingTextStart).isEqualTo(-1)
    }

    @Test
    fun testInsertionMarkerCursor() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            textLayoutResult,
            matrix
        )

        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(0)
    }

    @Test
    fun testInsertionMarkerSelectionIsSameWithCursor() {
        val textFieldValue = TextFieldValue(
            "abc",
            selection = TextRange(1, 2)
        )
        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo1 = builder.build(
            textFieldValue,
            textLayoutResult,
            matrix
        )

        val cursorAnchorInfo2 = builder.build(
            textFieldValue.copy(selection = TextRange(1)),
            textLayoutResult,
            matrix
        )

        assertThat(cursorAnchorInfo1.insertionMarkerHorizontal).isEqualTo(
            cursorAnchorInfo2.insertionMarkerHorizontal
        )
        assertThat(cursorAnchorInfo1.insertionMarkerTop).isEqualTo(
            cursorAnchorInfo2.insertionMarkerTop
        )
        assertThat(cursorAnchorInfo1.insertionMarkerBottom).isEqualTo(
            cursorAnchorInfo2.insertionMarkerBottom
        )
        assertThat(cursorAnchorInfo1.insertionMarkerBaseline).isEqualTo(
            cursorAnchorInfo2.insertionMarkerBottom
        )
    }

    @Test
    fun testInsertionMarkerRtl() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textFieldValue = TextFieldValue("\u05D0\u05D1\u05D2", selection = TextRange(0))
        val width = 3 * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(
            textFieldValue.text,
            fontSize = fontSize,
            width = width
        )
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(
            textFieldValue,
            textLayoutResult,
            matrix
        )

        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(width)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(CursorAnchorInfo.FLAG_IS_RTL)
    }

    private fun getTextLayoutResult(
        text: String,
        fontSize: TextUnit = 12.sp,
        width: Float = Float.MAX_VALUE
    ): TextLayoutResult {
        val intWidth = ceil(width).toInt()

        val fontFamilyResolver = createFontFamilyResolver(context)

        val input = TextLayoutInput(
            text = AnnotatedString(text),
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize
            ),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Visible,
            density = defaultDensity,
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = fontFamilyResolver,
            constraints = Constraints(maxWidth = intWidth)
        )

        val paragraph = MultiParagraph(
            annotatedString = input.text,
            style = input.style,
            constraints = Constraints(maxWidth = ceil(width).toInt()),
            density = input.density,
            fontFamilyResolver = fontFamilyResolver
        )

        return TextLayoutResult(
            input, paragraph, IntSize(intWidth, ceil(paragraph.height).toInt())
        )
    }
}