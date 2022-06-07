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

import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrarImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(InternalFoundationTextApi::class)
class TextSelectionLongPressDragTest {
    private val selectionRegistrar = spy(SelectionRegistrarImpl())
    private val selectableId = 1L
    private val selectable = mock<Selectable>().also {
        whenever(it.selectableId).thenReturn(selectableId)
    }

    private val fakeSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = selectableId
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectableId = selectableId
        )
    )

    private lateinit var gesture: TextDragObserver
    private lateinit var layoutCoordinates: LayoutCoordinates
    private lateinit var state: TextState

    @Before
    fun setup() {
        selectionRegistrar.subscribe(selectable)

        layoutCoordinates = mock {
            on { isAttached } doReturn true
        }

        state = TextState(mock(), selectableId)
        state.layoutCoordinates = layoutCoordinates
        state.layoutResult = TextLayoutResult(
            TextLayoutInput(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = listOf(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = mock(),
                constraints = Constraints.fixedWidth(100)
            ),
            multiParagraph = mock(),
            size = IntSize(50, 50)
        )

        val controller = TextController(state).also {
            it.update(selectionRegistrar)
        }
        gesture = controller.longPressDragObserver
    }

    @Test
    fun longPressDragObserver_onLongPress_calls_notifySelectionInitiated() {
        val position = Offset(100f, 100f)
        whenever(state.layoutResult?.getOffsetForPosition(position)).thenReturn("Hello".length)

        gesture.onStart(position)

        verify(selectionRegistrar, times(1)).notifySelectionUpdateStart(
            layoutCoordinates = layoutCoordinates,
            startPosition = position,
            adjustment = SelectionAdjustment.Word
        )
    }

    @Test
    fun longPressDragObserver_onLongPress_out_of_boundary_calls_notifySelectionUpdateSelectAll() {
        val position = Offset(100f, 100f)
        whenever(state.layoutResult?.getOffsetForPosition(position))
            .thenReturn("Hello, World".length)

        gesture.onStart(position)

        verify(selectionRegistrar, times(1)).notifySelectionUpdateSelectAll(
            selectableId = selectableId
        )
    }

    @Test
    fun longPressDragObserver_onDragStart_reset_dragTotalDistance() {
        // Setup. Make sure selectionManager.dragTotalDistance is not 0.
        val dragDistance1 = Offset(15f, 10f)
        val beginPosition1 = Offset(30f, 20f)
        val dragDistance2 = Offset(100f, 300f)
        val beginPosition2 = Offset(300f, 200f)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition1))
            .thenReturn("Hello".length)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition1 + dragDistance1))
            .thenReturn("Hello".length)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition2))
            .thenReturn("Hello".length)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition2 + dragDistance2))
            .thenReturn("Hello".length)

        gesture.onStart(beginPosition1)
        gesture.onDrag(dragDistance1)
        // Setup. Cancel selection and reselect.
//        selectionManager.onRelease()
        // Start the new selection
        gesture.onStart(beginPosition2)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)

        // Act. Reset selectionManager.dragTotalDistance to zero.
        gesture.onDrag(dragDistance2)

        // Verify.
        verify(selectionRegistrar, times(1))
            .notifySelectionUpdate(
                layoutCoordinates = layoutCoordinates,
                newPosition = beginPosition2 + dragDistance2,
                previousPosition = beginPosition2,
                adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                isStartHandle = false
            )
    }

    @Test
    fun longPressDragObserver_onDrag_calls_notifySelectionDrag() {
        val dragDistance = Offset(15f, 10f)
        val beginPosition = Offset(30f, 20f)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition))
            .thenReturn("Hello".length)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition + dragDistance))
            .thenReturn("Hello".length)
        gesture.onStart(beginPosition)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)

        gesture.onDrag(dragDistance)
        verify(selectionRegistrar, times(1))
            .notifySelectionUpdate(
                layoutCoordinates = layoutCoordinates,
                newPosition = beginPosition + dragDistance,
                previousPosition = beginPosition,
                adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                isStartHandle = false
            )
    }

    @Test
    fun longPressDragObserver_onDrag_out_of_boundary_not_call_notifySelectionDrag() {
        val dragDistance = Offset(15f, 10f)
        val beginPosition = Offset(30f, 20f)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition))
            .thenReturn("Hello, World".length)
        whenever(state.layoutResult?.getOffsetForPosition(beginPosition + dragDistance))
            .thenReturn("Hello, World".length)
        gesture.onStart(beginPosition)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)

        gesture.onDrag(dragDistance)
        verify(selectionRegistrar, times(0))
            .notifySelectionUpdate(
                layoutCoordinates = layoutCoordinates,
                newPosition = beginPosition + dragDistance,
                previousPosition = beginPosition,
                adjustment = SelectionAdjustment.Character,
                isStartHandle = false
            )
    }

    @Test
    fun longPressDragObserver_onStop_calls_notifySelectionEnd() {
        val beginPosition = Offset(30f, 20f)
        gesture.onStart(beginPosition)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)
        gesture.onStop()

        verify(selectionRegistrar, times(1))
            .notifySelectionUpdateEnd()
    }

    @Test
    fun longPressDragObserver_onCancel_calls_notifySelectionEnd() {
        val beginPosition = Offset(30f, 20f)
        gesture.onStart(beginPosition)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)
        gesture.onCancel()

        verify(selectionRegistrar, times(1))
            .notifySelectionUpdateEnd()
    }
}