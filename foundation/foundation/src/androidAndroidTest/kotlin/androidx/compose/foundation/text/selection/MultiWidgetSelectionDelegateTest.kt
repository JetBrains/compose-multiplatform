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

package androidx.compose.foundation.text.selection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class MultiWidgetSelectionDelegateTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fontFamily = TEST_FONT_FAMILY
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    @OptIn(ExperimentalTextApi::class)
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun getHandlePosition_StartHandle_invalid() {
        composeTestRule.setContent {
            val text = "hello world\n"
            val fontSize = 20.sp

            val layoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val layoutCoordinates = mock<LayoutCoordinates>()
            whenever(layoutCoordinates.isAttached).thenReturn(true)

            val selectableId = 1L
            val selectable = MultiWidgetSelectionDelegate(
                selectableId = selectableId,
                coordinatesCallback = { layoutCoordinates },
                layoutResultCallback = { layoutResult }
            )

            val selectableInvalidId = 2L
            val startOffset = text.indexOf('h')
            val endOffset = text.indexOf('o')

            val selection = Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = selectableInvalidId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = selectableInvalidId
                ),
                handlesCrossed = false
            )

            // Act.
            val coordinates = selectable.getHandlePosition(
                selection = selection,
                isStartHandle = true
            )

            // Assert.
            assertThat(coordinates).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun getHandlePosition_EndHandle_invalid() {
        composeTestRule.setContent {
            val text = "hello world\n"
            val fontSize = 20.sp

            val layoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val layoutCoordinates = mock<LayoutCoordinates>()
            whenever(layoutCoordinates.isAttached).thenReturn(true)

            val selectableId = 1L
            val selectable = MultiWidgetSelectionDelegate(
                selectableId = selectableId,
                coordinatesCallback = { layoutCoordinates },
                layoutResultCallback = { layoutResult }
            )

            val selectableInvalidId = 2L
            val startOffset = text.indexOf('h')
            val endOffset = text.indexOf('o')

            val selection = Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = selectableInvalidId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = selectableInvalidId
                ),
                handlesCrossed = false
            )

            // Act.
            val coordinates = selectable.getHandlePosition(
                selection = selection,
                isStartHandle = false
            )

            // Assert.
            assertThat(coordinates).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * startOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('o')
        val endOffset = text.indexOf('h')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * startOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D1')
        val endOffset = text.indexOf('\u05D5')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - startOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D5')
        val endOffset = text.indexOf('\u05D1')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - startOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D0')
        val endOffset = text.indexOf('\u05D2')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D0')
        val endOffset = text.indexOf('H')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (textLtr.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * endOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('o')
        val endOffset = text.indexOf('h')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * endOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D1')
        val endOffset = text.indexOf('\u05D5')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - endOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D5')
        val endOffset = text.indexOf('\u05D1')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - endOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('\u05D0')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (textLtr.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D2')
        val endOffset = text.indexOf('\u05D0')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length)), fontSizeInPx)
        )
    }

    @Test
    fun getText_textLayoutResult_Null_Return_Empty_AnnotatedString() {
        val layoutResult = null

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        assertThat(selectable.getText()).isEqualTo(AnnotatedString(""))
    }

    @Test
    fun getText_textLayoutResult_NotNull_Return_AnnotatedString() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        assertThat(selectable.getText()).isEqualTo(AnnotatedString(text, spanStyle))
    }

    @Test
    fun getBoundingBox_valid() {
        val text = "hello\nworld\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val textOffset = text.indexOf('w')

        // Act.
        val box = selectable.getBoundingBox(textOffset)

        // Assert.
        assertThat(box.left).isZero()
        assertThat(box.right).isEqualTo(fontSizeInPx)
        assertThat(box.top).isEqualTo(fontSizeInPx)
        assertThat(box.bottom).isEqualTo(2 * fontSizeInPx)
    }

    @Test
    fun getBoundingBox_zero_length_text_return_zero_rect() {
        val text = ""
        val fontSize = 20.sp

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(0)

        // Assert.
        assertThat(box).isEqualTo(Rect.Zero)
    }

    @Test
    fun getBoundingBox_negative_offset_should_return_zero_rect() {
        val text = "hello\nworld\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(-2)

        // Assert.
        assertThat(box.left).isZero()
        assertThat(box.right).isEqualTo(fontSizeInPx)
        assertThat(box.top).isZero()
        assertThat(box.bottom).isEqualTo(fontSizeInPx)
    }

    @Test
    fun getBoundingBox_offset_larger_than_range_should_return_largest() {
        val text = "hello\nworld"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            selectableId = 1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(text.indexOf('d') + 5)

        // Assert.
        assertThat(box.left).isEqualTo(4 * fontSizeInPx)
        assertThat(box.right).isEqualTo(5 * fontSizeInPx)
        assertThat(box.top).isEqualTo(fontSizeInPx)
        assertThat(box.bottom).isEqualTo(2 * fontSizeInPx)
    }

    @Test
    fun getRangeOfLineContaining_zeroOffset() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(0)

        // Assert.
        assertThat(lineRange.start).isEqualTo(0)
        assertThat(lineRange.end).isEqualTo(5)
    }

    @Test
    fun getRangeOfLineContaining_secondLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(7)

        // Assert.
        assertThat(lineRange.start).isEqualTo(6)
        assertThat(lineRange.end).isEqualTo(11)
    }

    @Test
    fun getRangeOfLineContaining_negativeOffset_returnsFirstLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(-1)

        // Assert.
        assertThat(lineRange.start).isEqualTo(0)
        assertThat(lineRange.end).isEqualTo(5)
    }

    @Test
    fun getRangeOfLineContaining_offsetPastTextLength_returnsLastLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(Int.MAX_VALUE)

        // Assert.
        assertThat(lineRange.start).isEqualTo(6)
        assertThat(lineRange.end).isEqualTo(11)
    }

    @Test
    fun getRangeOfLineContaining_offsetAtNewline_returnsPreviousLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(5)

        // Assert.
        assertThat(lineRange.start).isEqualTo(0)
        assertThat(lineRange.end).isEqualTo(5)
    }

    @Test
    fun getRangeOfLineContaining_emptyString_returnsEmptyRange() {
        val text = ""

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(5)

        // Assert.
        assertThat(lineRange.start).isEqualTo(0)
        assertThat(lineRange.end).isEqualTo(0)
    }

    @Test
    fun getRangeOfLineContaining_emptyLine_returnsEmptyNonZeroRange() {
        val text = "hello\n\nworld"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(6)

        // Assert.
        assertThat(lineRange.start).isEqualTo(6)
        assertThat(lineRange.end).isEqualTo(6)
    }

    @Test
    fun getTextSelectionInfo_long_press_select_word_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val start = Offset((fontSizeInPx * 2), (fontSizeInPx / 2))
        val end = start

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = end,
            endHandlePosition = start,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo("hello".length)
        }
    }

    @Test
    fun getTextSelectionInfo_long_press_select_word_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val start = Offset((fontSizeInPx * 2), (fontSizeInPx / 2))
        val end = start

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = end,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(text.indexOf("\u05D3"))
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(text.indexOf("\u05D5") + 1)
        }
    }

    @Test
    fun getTextSelectionInfo_long_press_drag_handle_not_cross_select_word() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val rawStartOffset = text.indexOf('e')
        val rawEndOffset = text.indexOf('r')
        val start = Offset((fontSizeInPx * rawStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * rawEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(text.length)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isFalse()
    }

    @Test
    fun getTextSelectionInfo_long_press_drag_handle_cross_select_word() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val rawStartOffset = text.indexOf('r')
        val rawEndOffset = text.indexOf('e')
        val start = Offset((fontSizeInPx * rawStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * rawEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(text.length)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_long_press_select_ltr_drag_down() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // long pressed between "h" and "e", "hello" should be selected
        val start = Offset((fontSizeInPx * 2), (fontSizeInPx / 2))
        val end = start

        // Act.
        val (textSelectionInfo1, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )

        // Drag downwards, after the drag the selection should remain the same.
        val (textSelectionInfo2, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = end + Offset(0f, fontSizeInPx / 4),
            endHandlePosition = start,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word,
            previousSelection = textSelectionInfo1,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo1).isNotNull()

        assertThat(textSelectionInfo1?.start).isNotNull()
        textSelectionInfo1?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }

        assertThat(textSelectionInfo1?.end).isNotNull()
        textSelectionInfo1?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo("hello".length)
        }

        assertThat(textSelectionInfo2).isNotNull()
        assertThat(textSelectionInfo2).isEqualTo(textSelectionInfo1)
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo wor" is selected.
        val startOffset = text.indexOf("l")
        val endOffset = text.indexOf("r") + 1
        val start = Offset((fontSizeInPx * startOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * endOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(endOffset)
        }
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "\u05D1\u05D2 \u05D3" is selected.
        val startOffset = text.indexOf("\u05D1")
        val endOffset = text.indexOf("\u05D3") + 1
        val start = Offset(
            (fontSizeInPx * (text.length - 1 - startOffset)),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * (text.length - 1 - endOffset)),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(endOffset)
        }
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        // "llo"+"\u05D0\u05D1\u05D2" is selected
        val startOffset = text.indexOf("l")
        val endOffset = text.indexOf("\u05D2") + 1
        val start = Offset(
            (fontSizeInPx * startOffset),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * (textLtr.length + text.length - endOffset)),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(endOffset)
        }
    }

    @Test
    fun getTextSelectionInfo_single_widget_handles_crossed_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo wor" is selected.
        val startOffset = text.indexOf("r") + 1
        val endOffset = text.indexOf("l")
        val start = Offset((fontSizeInPx * startOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * endOffset), (fontSizeInPx / 2))
        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(endOffset)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_single_widget_handles_crossed_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "\u05D1\u05D2 \u05D3" is selected.
        val startOffset = text.indexOf("\u05D3") + 1
        val endOffset = text.indexOf("\u05D1")
        val start = Offset(
            (fontSizeInPx * (text.length - 1 - startOffset)),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * (text.length - 1 - endOffset)),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(endOffset)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_single_widget_handles_crossed_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo"+"\u05D0\u05D1\u05D2" is selected
        val startOffset = text.indexOf("\u05D2") + 1
        val endOffset = text.indexOf("l")
        val start = Offset(
            (fontSizeInPx * (textLtr.length + text.length - startOffset)),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * endOffset),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(endOffset)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_ltr_drag_endHandle() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo" is selected.
        val oldStartOffset = text.indexOf("l")
        val oldEndOffset = text.indexOf("o") + 1
        val selectableId = 1L
        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // first "l" is selected.
        val start = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isEqualTo(previousSelection.start)

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(oldStartOffset + 1)
        }

        assertThat(textSelectionInfo?.handlesCrossed).isFalse()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_rtl_drag_endHandle() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "\u05D0\u05D1" is selected.
        val oldStartOffset = text.indexOf("\u05D1")
        val oldEndOffset = text.length
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Rtl,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Rtl,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "\u05D1" is selected.
        val start = Offset(
            (fontSizeInPx * (text.length - 1 - oldStartOffset)),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * (text.length - 1 - oldStartOffset)),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isEqualTo(previousSelection.start)

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(it.offset).isEqualTo(oldStartOffset + 1)
        }

        assertThat(textSelectionInfo?.handlesCrossed).isFalse()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_not_crossed() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo" is selected.
        val oldStartOffset = text.indexOf("l")
        val oldEndOffset = text.indexOf("o") + 1
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "o" is selected.
        val start = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo((oldEndOffset - 1))
        }

        assertThat(textSelectionInfo?.end).isEqualTo(previousSelection.end)

        assertThat(textSelectionInfo?.handlesCrossed).isFalse()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_crossed() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo" is selected.
        val oldStartOffset = text.indexOf("o") + 1
        val oldEndOffset = text.indexOf("l")
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // first "l" is selected.
        val start = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo((oldEndOffset + 1))
        }

        assertThat(textSelectionInfo?.end).isEqualTo(previousSelection.end)

        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_not_crossed_bounded() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "e" is selected.
        val oldStartOffset = text.indexOf("e")
        val oldEndOffset = text.indexOf("l")
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "e" should be selected.
        val start = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_crossed_bounded() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "e" is selected.
        val oldStartOffset = text.indexOf("l")
        val oldEndOffset = text.indexOf("e")
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // "e" should be selected.
        val start = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_not_crossed_boundary() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "d" is selected.
        val oldStartOffset = text.length - 1
        val oldEndOffset = text.length
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "d" should be selected.
        val start = Offset(
            (fontSizeInPx * oldEndOffset) - (fontSizeInPx / 2),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * oldEndOffset) - 1,
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_startHandle_crossed_boundary() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "h" is selected.
        val oldStartOffset = text.indexOf("e")
        val oldEndOffset = 0
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // "e" should be selected.
        val start = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldEndOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = true
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_endHandle_crossed() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "llo" is selected.
        val oldStartOffset = text.indexOf("o") + 1
        val oldEndOffset = text.indexOf("l")
        val selectableId = 1L
        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // "o" is selected.
        val start = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isEqualTo(previousSelection.start)

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo((oldStartOffset - 1))
        }

        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_endHandle_not_crossed_bounded() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "e" is selected.
        val oldStartOffset = text.indexOf("e")
        val oldEndOffset = text.indexOf("l")
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "e" should be selected.
        val start = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_endHandle_crossed_bounded() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "e" is selected.
        val oldStartOffset = text.indexOf("l")
        val oldEndOffset = text.indexOf("e")
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // "e" should be selected.
        val start = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))
        val end = Offset((fontSizeInPx * oldStartOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_endHandle_not_crossed_boundary() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "h" is selected.
        val oldStartOffset = 0
        val oldEndOffset = text.indexOf('e')
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        // "h" should be selected.
        val start = Offset(
            (fontSizeInPx * oldStartOffset),
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * oldStartOffset),
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_bound_to_one_character_drag_endHandle_crossed_boundary() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "d" is selected.
        val oldStartOffset = text.length
        val oldEndOffset = text.length - 1
        val selectableId = 1L

        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                offset = oldStartOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                offset = oldEndOffset,
                direction = ResolvedTextDirection.Ltr,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        // "d" should be selected.
        val start = Offset(
            (fontSizeInPx * oldStartOffset) - 1,
            (fontSizeInPx / 2)
        )
        val end = Offset(
            (fontSizeInPx * oldStartOffset) - 1,
            (fontSizeInPx / 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Character,
            previousSelection = previousSelection,
            isStartHandle = false
        )

        // Assert.
        assertThat(textSelectionInfo?.start?.offset).isEqualTo(previousSelection.start.offset)
        assertThat(textSelectionInfo?.end?.offset).isEqualTo(previousSelection.end.offset)
    }

    @Test
    fun getTextSelectionInfo_cross_widget_not_contain_start() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "hello w" is selected.
        val endOffset = text.indexOf("w") + 1
        val start = Offset(-50f, -50f)
        val end = Offset((fontSizeInPx * endOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(endOffset)
        }
    }

    @Test
    fun getTextSelectionInfo_cross_widget_not_contain_end() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "o world" is selected.
        val startOffset = text.indexOf("o")
        val start = Offset((fontSizeInPx * startOffset), (fontSizeInPx / 2))
        val end = Offset(
            (fontSizeInPx * text.length * 2), (fontSizeInPx * 2)
        )

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(text.length)
        }
    }

    @Test
    fun getTextSelectionInfo_cross_widget_not_contain_start_handles_crossed() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "world" is selected.
        val endOffset = text.indexOf("w")
        val start =
            Offset((fontSizeInPx * text.length * 2), (fontSizeInPx * 2))
        val end = Offset((fontSizeInPx * endOffset), (fontSizeInPx / 2))

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(text.length)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(endOffset)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_cross_widget_not_contain_end_handles_crossed() {
        val text = "hello world"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        // "hell" is selected.
        val startOffset = text.indexOf("o")
        val start =
            Offset((fontSizeInPx * startOffset), (fontSizeInPx / 2))
        val end = Offset(-50f, -50f)

        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.None
        )
        // Assert.
        assertThat(textSelectionInfo).isNotNull()

        assertThat(textSelectionInfo?.start).isNotNull()
        textSelectionInfo?.start?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(startOffset)
        }

        assertThat(textSelectionInfo?.end).isNotNull()
        textSelectionInfo?.end?.let {
            assertThat(it.direction).isEqualTo(ResolvedTextDirection.Ltr)
            assertThat(it.offset).isEqualTo(0)
        }
        assertThat(textSelectionInfo?.handlesCrossed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_not_selected() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        val start = Offset(-50f, -50f)
        val end = Offset(-20f, -20f)
        // Act.
        val (textSelectionInfo, _) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = null,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word
        )
        assertThat(textSelectionInfo).isNull()
    }

    @Test
    fun getTextSelectionInfo_handleNotMoved_selectionUpdated_consumed_isTrue() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        val start = Offset(0f, fontSizeInPx / 2)
        val end = Offset(fontSizeInPx * 3, fontSizeInPx / 2)
        // Act.
        // Selection is updated but endHandlePosition is actually the same. Since selection is
        // updated, the movement is consumed.
        val (_, consumed) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = end,
            selectableId = 1,
            adjustment = SelectionAdjustment.Word,
            previousSelection = null,
            isStartHandle = false
        )
        assertThat(consumed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_handleMoved_selectionNotUpdated_consumed_isTrue() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        val start = Offset(0f, fontSizeInPx / 2)
        val end = Offset(fontSizeInPx * 3, fontSizeInPx / 2)
        val previousEnd = Offset(fontSizeInPx * 2, fontSizeInPx / 2)
        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 0,
                selectableId = 1
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 5,
                selectableId = 1
            ),
        )
        // Act.
        // End handle moved from offset 2 to 3. But we are using word based selection, so the
        // selection is still [0, 5). However, since handle moved to a new offset, the movement
        // is consumed.
        val (textSelectionInfo, consumed) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = previousEnd,
            selectableId = 1,
            previousSelection = previousSelection,
            adjustment = SelectionAdjustment.Word,
            isStartHandle = false
        )
        // First check that Selection didn't update.
        assertThat(textSelectionInfo).isEqualTo(previousSelection)
        assertThat(consumed).isTrue()
    }

    @Test
    fun getTextSelectionInfo_handleNotMoved_selectionNotUpdated_consumed_isFalse() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val textLayoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )
        val start = Offset(0f, fontSizeInPx / 2)
        val end = Offset(fontSizeInPx * 2.8f, fontSizeInPx / 2)
        val previousEnd = Offset(fontSizeInPx * 3f, fontSizeInPx / 2)
        val previousSelection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 0,
                selectableId = 1
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 5,
                selectableId = 1
            ),
        )
        // Act.
        // End handle moved, but is still at the offset 3. Selection is not updated either.
        // So the movement is not consumed.
        val (textSelectionInfo, consumed) = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            startHandlePosition = start,
            endHandlePosition = end,
            previousHandlePosition = previousEnd,
            selectableId = 1,
            previousSelection = previousSelection,
            adjustment = SelectionAdjustment.Word,
            isStartHandle = false
        )
        // First check that Selection didn't update.
        assertThat(textSelectionInfo).isEqualTo(previousSelection)
        assertThat(consumed).isFalse()
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