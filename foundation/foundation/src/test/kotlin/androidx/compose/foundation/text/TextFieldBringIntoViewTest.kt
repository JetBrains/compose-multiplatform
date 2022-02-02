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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyBlocking
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalFoundationTextApi::class, ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextFieldBringIntoViewTest {

    private val delegate: TextDelegate = mock()
    private var layoutCoordinates: LayoutCoordinates = mock()
    private val textLayoutResultProxy: TextLayoutResultProxy = mock()
    private val textLayoutResult: TextLayoutResult = mock()
    private val bringIntoViewRequester: BringIntoViewRequester = mock()

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = offset * 2
        override fun transformedToOriginal(offset: Int): Int = offset / 2
    }

    @Before
    fun setup() {
        whenever(textLayoutResultProxy.value).thenReturn(textLayoutResult)
    }

    @Test
    fun notify_focused_rect() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))

        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                editorState,
                delegate,
                textLayoutResult,
                OffsetMapping.Identity
            )
        }
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }
    }

    @Test
    fun notify_rect_tail() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(12))
        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                editorState,
                delegate,
                textLayoutResult,
                OffsetMapping.Identity
            )
        }
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }
    }

    @Test
    fun check_notify_rect_uses_offset_map() {
        val rect = Rect(0f, 1f, 2f, 3f)
        val point = Offset(5f, 6f)
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1, 3))

        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val input = TextLayoutInput(
            text = AnnotatedString(editorState.text),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )

        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                editorState,
                delegate,
                textLayoutResult,
                skippingOffsetMap
            )
        }
        verify(textLayoutResult).getBoundingBox(6)
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }
    }

    @Test
    fun notify_transformed_text() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )

        val input = TextLayoutInput(
            // In this test case, transform the text into double characters text.
            text = AnnotatedString("HHeelllloo,,  WWoorrlldd"),
            style = TextStyle(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            density = Density(1.0f),
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = mock(),
            constraints = mock()
        )
        whenever(textLayoutResult.layoutInput).thenReturn(input)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset * 2
            override fun transformedToOriginal(offset: Int): Int = offset / 2
        }

        // The beginning of the text.
        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                TextFieldValue(text = "Hello, World", selection = TextRange(0)),
                delegate,
                textLayoutResult,
                offsetMapping
            )
        }
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }

        // The tail of the transformed text.
        reset(bringIntoViewRequester)
        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                TextFieldValue(text = "Hello, World", selection = TextRange(24)),
                delegate,
                textLayoutResult,
                offsetMapping
            )
        }
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }

        // Beyond the tail of the transformed text.
        reset(bringIntoViewRequester)
        runBlocking {
            bringIntoViewRequester.bringSelectionEndIntoView(
                TextFieldValue(text = "Hello, World", selection = TextRange(25)),
                delegate,
                textLayoutResult,
                offsetMapping
            )
        }
        verifyBlocking(bringIntoViewRequester) { bringIntoView(rect) }
    }

    private class MockCoordinates(
        override val size: IntSize = IntSize.Zero,
        val localOffset: Offset = Offset.Zero,
        val globalOffset: Offset = Offset.Zero,
        val rootOffset: Offset = Offset.Zero
    ) : LayoutCoordinates {
        override val providedAlignmentLines: Set<AlignmentLine>
            get() = emptySet()
        override val parentLayoutCoordinates: LayoutCoordinates?
            get() = null
        override val parentCoordinates: LayoutCoordinates?
            get() = null
        override val isAttached: Boolean
            get() = true

        override fun windowToLocal(relativeToWindow: Offset): Offset = localOffset

        override fun localToWindow(relativeToLocal: Offset): Offset = globalOffset

        override fun localToRoot(relativeToLocal: Offset): Offset = rootOffset
        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset
        ): Offset = Offset.Zero

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean
        ): Rect = Rect.Zero

        override fun get(alignmentLine: AlignmentLine): Int = 0
    }
}