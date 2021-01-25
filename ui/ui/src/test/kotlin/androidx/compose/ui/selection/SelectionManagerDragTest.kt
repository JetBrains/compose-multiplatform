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

package androidx.compose.ui.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class SelectionManagerDragTest {
    private val selectionRegistrar = SelectionRegistrarImpl()
    private val selectable = mock<Selectable>()
    private val selectionManager = SelectionManager(selectionRegistrar)

    private val size = IntSize(500, 600)
    private val globalOffset = Offset(100f, 200f)
    private val windowOffset = Offset(100f, 200f)
    private val childToLocalOffset = Offset(300f, 400f)

    private val containerLayoutCoordinates = spy(
        MockCoordinates(
            size = size,
            globalOffset = globalOffset,
            windowOffset = windowOffset,
            childToLocalOffset = childToLocalOffset,
            isAttached = true
        )
    )

    private val startSelectable = mock<Selectable> {
        on { getHandlePosition(any(), any()) } doAnswer Offset.Zero
    }
    private val endSelectable = mock<Selectable> {
        on { getHandlePosition(any(), any()) } doAnswer Offset.Zero
    }
    private val startLayoutCoordinates = mock<LayoutCoordinates>()
    private val endLayoutCoordinates = mock<LayoutCoordinates>()
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
    private var selection: Selection? = fakeInitialSelection
    private val lambda: (Selection?) -> Unit = { selection = it }
    private val spyLambda = spy(lambda)

    @Before
    fun setup() {
        selectionRegistrar.subscribe(selectable)

        whenever(
            selectable.getSelection(
                startPosition = Offset(any()),
                endPosition = Offset(any()),
                containerLayoutCoordinates = any(),
                longPress = any(),
                previousSelection = any(),
                isStartHandle = any()
            )
        ).thenReturn(fakeResultSelection)

        whenever(startSelectable.getLayoutCoordinates()).thenReturn(startLayoutCoordinates)
        whenever(endSelectable.getLayoutCoordinates()).thenReturn(endLayoutCoordinates)

        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = selection
        selectionManager.hapticFeedBack = mock()
    }

    @Test
    fun handleDragObserver_onStart_startHandle_enable_draggingHandle_get_startHandle_info() {
        selectionManager.handleDragObserver(isStartHandle = true).onStart(Offset.Zero)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = startLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )
        verify(spyLambda, times(0)).invoke(fakeResultSelection)
    }

    @Test
    fun handleDragObserver_onStart_endHandle_enable_draggingHandle_get_endHandle_info() {
        selectionManager.handleDragObserver(isStartHandle = false).onStart(Offset.Zero)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = endLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )
        verify(spyLambda, times(0)).invoke(fakeResultSelection)
    }

    @Test
    fun handleDragObserver_onDrag_startHandle_reuse_endHandle_calls_getSelection_change_selection
    () {
        val startOffset = Offset(30f, 50f)
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = true).onStart(startOffset)

        val result = selectionManager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = endLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )
        verify(selectable, times(1))
            .getSelection(
                startPosition = childToLocalOffset + dragDistance,
                endPosition = childToLocalOffset,
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
                longPress = false,
                isStartHandle = true,
                previousSelection = fakeInitialSelection
            )
        assertThat(selection).isEqualTo(fakeResultSelection)
        verify(spyLambda, times(1)).invoke(fakeResultSelection)
        assertThat(result).isEqualTo(dragDistance)
    }

    @Test
    fun handleDragObserver_onDrag_endHandle_reuse_startHandle_calls_getSelection_change_selection
    () {
        val startOffset = Offset(30f, 50f)
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = false).onStart(startOffset)

        val result = selectionManager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = startLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )
        verify(selectable, times(1))
            .getSelection(
                startPosition = childToLocalOffset,
                endPosition = childToLocalOffset + dragDistance,
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
                longPress = false,
                isStartHandle = false,
                previousSelection = fakeInitialSelection
            )
        assertThat(selection).isEqualTo(fakeResultSelection)
        verify(spyLambda, times(1)).invoke(fakeResultSelection)
        assertThat(result).isEqualTo(dragDistance)
    }

    private fun getAdjustedCoordinates(position: Offset): Offset {
        return Offset(position.x, position.y - 1f)
    }
}
