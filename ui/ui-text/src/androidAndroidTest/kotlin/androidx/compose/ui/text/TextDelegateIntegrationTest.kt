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

package androidx.compose.ui.text

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.font.asFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextDelegateIntegrationTest {

    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun minIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

            assertThat(textDelegate.minIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

            assertThat(textDelegate.maxIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun testBackgroundPaint_paint_wrap_multiLines() {
        with(density) {
            // Setup test.
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val text = "HelloHello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )
            val layoutResult = textDelegate.layout(
                Constraints(maxWidth = 120),
                LayoutDirection.Ltr
            )

            val expectedBitmap = layoutResult.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            val selectionPaint = Paint().also { it.color = defaultSelectionColor }
            expectedPaint.color = defaultSelectionColor

            val firstLineLeft = layoutResult.multiParagraph.getLineLeft(0)
            val secondLineLeft = layoutResult.multiParagraph.getLineLeft(1)
            val firstLineRight = layoutResult.multiParagraph.getLineRight(0)
            val secondLineRight = layoutResult.multiParagraph.getLineRight(1)
            expectedCanvas.drawRect(
                Rect(firstLineLeft, 0f, firstLineRight, fontSizeInPx),
                expectedPaint
            )
            expectedCanvas.drawRect(
                Rect(
                    secondLineLeft,
                    fontSizeInPx,
                    secondLineRight,
                    layoutResult.multiParagraph.height
                ),
                expectedPaint
            )

            val actualBitmap = layoutResult.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            // Select all.
            TextDelegate.paintBackground(
                start = 0,
                end = text.length,
                paint = selectionPaint,
                canvas = actualCanvas,
                textLayoutResult = layoutResult
            )

            // Assert.
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color() {
        with(density) {
            // Setup test.
            val selectionStart = 0
            val selectionEnd = 3
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )
            val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)

            val expectedBitmap = layoutResult.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            val selectionPaint = Paint().also { it.color = defaultSelectionColor }
            expectedPaint.color = defaultSelectionColor
            expectedCanvas.drawRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fontSizeInPx * (selectionEnd - selectionStart),
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = layoutResult.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            TextDelegate.paintBackground(
                start = selectionStart,
                end = selectionEnd,
                paint = selectionPaint,
                canvas = actualCanvas,
                textLayoutResult = layoutResult
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color_bidi() {
        with(density) {
            // Setup test.
            val textLTR = "Hello"
            // From right to left: שלום
            val textRTL = "\u05e9\u05dc\u05d5\u05dd"
            val text = textLTR + textRTL
            val selectionLTRStart = 2
            val selectionRTLEnd = 2
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )
            val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)

            val expectedBitmap = layoutResult.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            val selectionPaint = Paint().also { it.color = defaultSelectionColor }
            expectedPaint.color = defaultSelectionColor
            // Select "llo".
            expectedCanvas.drawRect(
                Rect(
                    left = fontSizeInPx * selectionLTRStart,
                    top = 0f,
                    right = textLTR.length * fontSizeInPx,
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            // Select "של"
            expectedCanvas.drawRect(
                Rect(
                    left = (textLTR.length + textRTL.length - selectionRTLEnd) * fontSizeInPx,
                    top = 0f,
                    right = (textLTR.length + textRTL.length) * fontSizeInPx,
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = layoutResult.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            TextDelegate.paintBackground(
                start = selectionLTRStart,
                end = textLTR.length + selectionRTLEnd,
                paint = selectionPaint,
                canvas = actualCanvas,
                textLayoutResult = layoutResult
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_customized_color() {
        with(density) {
            // Setup test.
            val selectionStart = 0
            val selectionEnd = 3
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val selectionColor = Color(0x66AABB33)
            val selectionPaint = Paint().also { it.color = selectionColor }
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )
            val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)

            val expectedBitmap = layoutResult.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            expectedPaint.color = selectionColor
            expectedCanvas.drawRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fontSizeInPx * (selectionEnd - selectionStart),
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = layoutResult.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            TextDelegate.paintBackground(
                start = selectionStart,
                end = selectionEnd,
                paint = selectionPaint,
                canvas = actualCanvas,
                textLayoutResult = layoutResult
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun multiParagraphIntrinsics_isReused() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "abc"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        // create the intrinsics object
        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)
        val multiParagraphIntrinsics = textDelegate.paragraphIntrinsics

        // layout should create the MultiParagraph. The final MultiParagraph is expected to use
        // the previously calculated intrinsics
        val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)
        val layoutIntrinsics = layoutResult.multiParagraph.intrinsics

        // primary assertions to make sure that the objects are not null
        assertThat(layoutIntrinsics.infoList.get(0)).isNotNull()
        assertThat(multiParagraphIntrinsics?.infoList?.get(0)).isNotNull()

        // the intrinsics passed to multi paragraph should be the same instance
        assertThat(layoutIntrinsics).isSameInstanceAs(multiParagraphIntrinsics)
        // the ParagraphIntrinsic in the MultiParagraphIntrinsic should be the same instance
        assertThat(layoutIntrinsics.infoList.get(0))
            .isSameInstanceAs(multiParagraphIntrinsics?.infoList?.get(0))
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.layoutInput.constraints).isEqualTo(constraintsFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.layoutInput.constraints).isEqualTo(constraintsSecondLayout)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.size.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.size.height).isEqualTo(heightSecondLayout)
    }
}

private fun TextLayoutResult.toBitmap() = Bitmap.createBitmap(
    size.width,
    size.height,
    Bitmap.Config.ARGB_8888
)
