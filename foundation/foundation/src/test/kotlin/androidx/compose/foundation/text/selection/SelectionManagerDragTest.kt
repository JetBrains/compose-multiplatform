/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionManagerDragTest {

    private val selectionRegistrar = SelectionRegistrarImpl()
    private val selectableKey = 1L
    private val selectable = FakeSelectable().also { it.selectableId = this.selectableKey }
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

    private val startSelectable = FakeSelectable()
    private val endSelectable = FakeSelectable()
    private val startSelectableKey = 2L
    private val endSelectableKey = 3L
    private val startLayoutCoordinates = mock<LayoutCoordinates>()
    private val endLayoutCoordinates = mock<LayoutCoordinates>()
    private val fakeSubselection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = selectableKey
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectableId = selectableKey
        )
    )
    private val fakeInitialSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = startSelectableKey
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectableId = endSelectableKey
        )
    )
    private val fakeResultSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 5,
            selectableId = endSelectableKey
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = startSelectableKey
        )
    )
    private var selection: Selection? = fakeInitialSelection
    private val lambda: (Selection?) -> Unit = { selection = it }
    private val spyLambda = spy(lambda)

    @Before
    fun setup() {
        startSelectable.clear()
        endSelectable.clear()
        startSelectable.layoutCoordinate = startLayoutCoordinates
        startSelectable.selectableId = startSelectableKey
        endSelectable.layoutCoordinate = endLayoutCoordinates
        endSelectable.selectableId = endSelectableKey

        selectionRegistrar.subscribe(selectable)
        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subselections = mapOf(
            selectableKey to fakeSubselection
        )

        selectable.clear()
        selectable.selectionToReturn = fakeResultSelection

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
    fun handleDragObserver_onDrag_startHandle_reuse_endHandle_calls_getSelection_change() {
        val startOffset = Offset(30f, 50f)
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = true).onStart(startOffset)
        selectionManager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = endLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastStartHandlePosition).isEqualTo(childToLocalOffset + dragDistance)
        assertThat(selectable.lastEndHandlePosition).isEqualTo(childToLocalOffset)
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastAdjustment)
            .isEqualTo(SelectionAdjustment.CharacterWithWordAccelerate)
        assertThat(selectable.lastIsStartHandle).isEqualTo(true)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSubselection)

        assertThat(selection).isEqualTo(fakeResultSelection)
        verify(spyLambda, times(1)).invoke(fakeResultSelection)
    }

    @Test
    fun handleDragObserver_onDrag_endHandle_reuse_startHandle_calls_getSelection_change() {
        val startOffset = Offset(30f, 50f)
        val dragDistance = Offset(100f, 100f)
        selectionManager.handleDragObserver(isStartHandle = false).onStart(startOffset)
        selectionManager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        verify(containerLayoutCoordinates, times(1))
            .localPositionOf(
                sourceCoordinates = startLayoutCoordinates,
                relativeToSource = getAdjustedCoordinates(Offset.Zero)
            )

        assertThat(selectable.getSelectionCalledTimes).isEqualTo(1)
        assertThat(selectable.lastEndHandlePosition).isEqualTo(childToLocalOffset + dragDistance)
        assertThat(selectable.lastStartHandlePosition).isEqualTo(childToLocalOffset)
        assertThat(selectable.lastContainerLayoutCoordinates)
            .isEqualTo(selectionManager.requireContainerCoordinates())
        assertThat(selectable.lastAdjustment)
            .isEqualTo(SelectionAdjustment.CharacterWithWordAccelerate)
        assertThat(selectable.lastIsStartHandle).isEqualTo(false)
        assertThat(selectable.lastPreviousSelection).isEqualTo(fakeSubselection)

        assertThat(selection).isEqualTo(fakeResultSelection)
        verify(spyLambda, times(1)).invoke(fakeResultSelection)
    }

    private fun getAdjustedCoordinates(position: Offset): Offset {
        return Offset(position.x, position.y - 1f)
    }
}

internal class FakeSelectable : Selectable {
    override var selectableId = 0L
    var lastEndHandlePosition: Offset? = null
    var lastStartHandlePosition: Offset? = null
    var lastPreviousHandlePosition: Offset? = null
    var lastContainerLayoutCoordinates: LayoutCoordinates? = null
    var lastAdjustment: SelectionAdjustment? = null
    var lastPreviousSelection: Selection? = null
    var lastIsStartHandle: Boolean? = null
    var getSelectionCalledTimes = 0
    var getTextCalledTimes = 0
    var selectionToReturn: Selection? = null
    var textToReturn: AnnotatedString? = null

    var handlePosition = Offset.Zero
    var boundingBox = Rect.Zero
    var layoutCoordinate: LayoutCoordinates? = null

    private val selectableKey = 1L
    private val fakeSelectAllSelection: Selection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = selectableKey
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 10,
            selectableId = selectableKey
        )
    )

    override fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset?,
        isStartHandle: Boolean,
        containerLayoutCoordinates: LayoutCoordinates,
        adjustment: SelectionAdjustment,
        previousSelection: Selection?
    ): Pair<Selection?, Boolean> {
        getSelectionCalledTimes++
        lastStartHandlePosition = startHandlePosition
        lastEndHandlePosition = endHandlePosition
        lastPreviousHandlePosition = previousHandlePosition
        lastContainerLayoutCoordinates = containerLayoutCoordinates
        lastAdjustment = adjustment
        lastPreviousSelection = previousSelection
        lastIsStartHandle = isStartHandle
        return Pair(selectionToReturn, false)
    }

    override fun getSelectAllSelection(): Selection? {
        return fakeSelectAllSelection
    }

    override fun getText(): AnnotatedString {
        getTextCalledTimes++
        return textToReturn!!
    }

    override fun getLayoutCoordinates(): LayoutCoordinates? {
        return layoutCoordinate
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        return handlePosition
    }

    override fun getBoundingBox(offset: Int): Rect {
        return boundingBox
    }

    override fun getRangeOfLineContaining(offset: Int): TextRange {
        return TextRange.Zero
    }

    fun clear() {
        lastEndHandlePosition = null
        lastStartHandlePosition = null
        lastPreviousHandlePosition = null
        lastContainerLayoutCoordinates = null
        lastAdjustment = null
        lastPreviousSelection = null
        lastIsStartHandle = null
        getSelectionCalledTimes = 0
        getTextCalledTimes = 0
        selectionToReturn = null
        textToReturn = null
    }
}