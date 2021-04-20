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
import androidx.compose.ui.text.style.ResolvedTextDirection
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

        val controller = TextController(state).also {
            it.update(selectionRegistrar)
        }
        gesture = controller.longPressDragObserver
    }

    @Test
    fun longPressDragObserver_onLongPress_calls_notifySelectionInitiated() {
        val position = Offset(100f, 100f)

        gesture.onStart(position)

        verify(selectionRegistrar, times(1)).notifySelectionUpdateStart(
            layoutCoordinates = layoutCoordinates,
            startPosition = position,
            adjustment = SelectionAdjustment.WORD
        )
    }

    @Test
    fun longPressDragObserver_onDragStart_reset_dragTotalDistance() {
        // Setup. Make sure selectionManager.dragTotalDistance is not 0.
        val dragDistance1 = Offset(15f, 10f)
        val beginPosition1 = Offset(30f, 20f)
        val dragDistance2 = Offset(100f, 300f)
        val beginPosition2 = Offset(300f, 200f)
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
                startPosition = beginPosition2,
                endPosition = beginPosition2 + dragDistance2,
                adjustment = SelectionAdjustment.CHARACTER
            )
    }

    @Test
    fun longPressDragObserver_onDrag_calls_notifySelectionDrag() {
        val dragDistance = Offset(15f, 10f)
        val beginPosition = Offset(30f, 20f)
        gesture.onStart(beginPosition)
        selectionRegistrar.subselections = mapOf(selectableId to fakeSelection)

        gesture.onDrag(dragDistance)
        verify(selectionRegistrar, times(1))
            .notifySelectionUpdate(
                layoutCoordinates = layoutCoordinates,
                startPosition = beginPosition,
                endPosition = beginPosition + dragDistance,
                adjustment = SelectionAdjustment.CHARACTER
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