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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.LongPressDragObserver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.selection.Selectable
import androidx.compose.ui.selection.Selection
import androidx.compose.ui.selection.SelectionRegistrar
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(InternalTextApi::class)
class TextSelectionLongPressDragTest {
    private val selectionRegistrar = mock<SelectionRegistrar>()
    private val selectable = mock<Selectable>()

    private val startSelectable = mock<Selectable>()
    private val endSelectable = mock<Selectable>()

    private val fakeInitialSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = startSelectable
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectable = endSelectable
        )
    )

    private val fakeResultSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectable = endSelectable
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = startSelectable
        )
    )

    private lateinit var gesture: LongPressDragObserver
    private lateinit var layoutCoordinates: LayoutCoordinates
    private lateinit var state: TextState

    @Before
    fun setup() {
        selectionRegistrar.subscribe(selectable)

        layoutCoordinates = mock<LayoutCoordinates> {
            on { isAttached } doReturn true
        }

        state = TextState(mock<TextDelegate>())
        state.selectionRange = null
        state.layoutCoordinates = layoutCoordinates

        gesture = longPressDragObserver(
            state = state,
            selectionRegistrar = selectionRegistrar
        )
    }

    @Test
    fun longPressDragObserver_onLongPress_calls_getSelection_change_selection() {
        val position = Offset(100f, 100f)

        gesture.onLongPress(position)

        verify(selectionRegistrar, times(1)).onUpdateSelection(
            layoutCoordinates = layoutCoordinates,
            startPosition = position,
            endPosition = position
        )
    }

    @Test
    fun longPressDragObserver_onDragStart_reset_dragTotalDistance() {
        // Setup. Make sure selectionManager.dragTotalDistance is not 0.
        val dragDistance1 = Offset(15f, 10f)
        val beginPosition1 = Offset(30f, 20f)
        val dragDistance2 = Offset(100f, 300f)
        val beginPosition2 = Offset(300f, 200f)
        gesture.onLongPress(beginPosition1)
        gesture.onDragStart()
        gesture.onDrag(dragDistance1)
        // Setup. Cancel selection and reselect.
//        selectionManager.onRelease()
        // Start the new selection
        gesture.onLongPress(beginPosition2)
        state.selectionRange = fakeInitialSelection.toTextRange()

        // Act. Reset selectionManager.dragTotalDistance to zero.
        gesture.onDragStart()
        gesture.onDrag(dragDistance2)

        // Verify.
        verify(selectionRegistrar, times(1))
            .onUpdateSelection(
                layoutCoordinates = layoutCoordinates,
                startPosition = beginPosition2,
                endPosition = beginPosition2 + dragDistance2
            )
    }

    @Test
    fun longPressDragObserver_onDrag_calls_getSelection_change_selection() {
        val dragDistance = Offset(15f, 10f)
        val beginPosition = Offset(30f, 20f)
        gesture.onLongPress(beginPosition)
        state.selectionRange = fakeInitialSelection.toTextRange()
        gesture.onDragStart()

        val result = gesture.onDrag(dragDistance)

        assertThat(result).isEqualTo(dragDistance)
        verify(selectionRegistrar, times(1))
            .onUpdateSelection(
                layoutCoordinates = layoutCoordinates,
                startPosition = beginPosition,
                endPosition = beginPosition + dragDistance
            )
    }
}