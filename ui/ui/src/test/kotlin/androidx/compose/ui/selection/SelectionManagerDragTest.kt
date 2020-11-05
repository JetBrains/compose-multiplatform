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
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
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
class SelectionManagerDragTest {
    private val selectionRegistrar = SelectionRegistrarImpl()
    private val selectable = mock<Selectable>()
    private val selectionManager = SelectionManager(selectionRegistrar)

    private val childToLocal_result = Offset(300f, 400f)

    private val containerLayoutCoordinates = mock<LayoutCoordinates> {
        on { isAttached } doReturn true
        on { childToLocal(child = any(), childLocal = Offset(any())) } doAnswer
            childToLocal_result
    }

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
            .childToLocal(
                child = startLayoutCoordinates,
                childLocal = getAdjustedCoordinates(Offset.Zero)
            )
        verify(spyLambda, times(0)).invoke(fakeResultSelection)
    }

    @Test
    fun handleDragObserver_onStart_endHandle_enable_draggingHandle_get_endHandle_info() {
        selectionManager.handleDragObserver(isStartHandle = false).onStart(Offset.Zero)

        verify(containerLayoutCoordinates, times(1))
            .childToLocal(
                child = endLayoutCoordinates,
                childLocal = getAdjustedCoordinates(Offset.Zero)
            )
        verify(spyLambda, times(0)).invoke(fakeResultSelection)
    }

    @Test
    fun handleDragObserver_onDrag_startHandle_reuse_endHandle_calls_getSelection_change_selection
    () {
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = true).onStart(Offset.Zero)

        val result = selectionManager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .childToLocal(
                child = endLayoutCoordinates,
                childLocal = getAdjustedCoordinates(Offset.Zero)
            )
        verify(selectable, times(1))
            .getSelection(
                startPosition = childToLocal_result + dragDistance,
                endPosition = childToLocal_result,
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
    fun handleDragObserver_onDrag_endHandle_resue_startHandle_calls_getSelection_change_selection
    () {
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = false).onStart(Offset.Zero)

        val result = selectionManager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .childToLocal(
                child = startLayoutCoordinates,
                childLocal = getAdjustedCoordinates(Offset.Zero)
            )
        verify(selectable, times(1))
            .getSelection(
                startPosition = childToLocal_result,
                endPosition = childToLocal_result + dragDistance,
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
