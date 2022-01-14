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

import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.Selection.AnchorInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection.Ltr
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectionMagnifierTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun centerIsUnspecified_whenNoSelection() {
        val manager = SelectionManager(SelectionRegistrarImpl())
        val center = calculateSelectionMagnifierCenterAndroid(manager)
        assertThat(center).isEqualTo(Offset.Unspecified)
    }

    @Test
    fun centerIsUnspecified_whenNotDragging() {
        val manager = SelectionManager(SelectionRegistrarImpl())
        manager.selection = Selection(
            start = AnchorInfo(Ltr, 0, 0),
            end = AnchorInfo(Ltr, 1, 0)
        )
        val center = calculateSelectionMagnifierCenterAndroid(manager)
        assertThat(center).isEqualTo(Offset.Unspecified)
    }

    @Test
    fun returnsSelectionBoundingBoxCenterLeft_whenDraggingStartHandle() {
        val registrar = SelectionRegistrarImpl()
        val manager = SelectionManager(registrar)
        val dragOffset = 3
        val boundingBox = Rect(1f, 2f, 3f, 4f)
        val selectionToReturn = Selection(
            start = AnchorInfo(Ltr, dragOffset, 1),
            end = AnchorInfo(Ltr, 6, 2)
        )
        val selectableOffset = Offset(1f, 1f)
        registrar.subscribe(
            FakeSelectable(
                selectableId = 1,
                boundingBox = {
                    require(it == dragOffset)
                    boundingBox
                },
                selectionToReturn = selectionToReturn,
            )
        )
        manager.containerLayoutCoordinates = FakeLayoutCoordinates(selectableOffset)
        manager.selection = selectionToReturn
        manager.updateSelection(
            startHandlePosition = Offset.Zero,
            endHandlePosition = Offset.Zero,
            previousHandlePosition = Offset.Zero,
            isStartHandle = true,
            adjustment = SelectionAdjustment.None
        )

        val center = calculateSelectionMagnifierCenterAndroid(manager)

        assertThat(center).isEqualTo(boundingBox.centerLeft + selectableOffset)
    }

    @Test
    fun returnsSelectionBoundingBoxCenterRight_whenDraggingEndHandle() {
        val registrar = SelectionRegistrarImpl()
        val manager = SelectionManager(registrar)
        val dragOffset = 3
        val boundingBox = Rect(1f, 2f, 3f, 4f)
        val selectionToReturn = Selection(
            start = AnchorInfo(Ltr, 1, 1),
            end = AnchorInfo(Ltr, dragOffset, 2)
        )
        val selectableOffset = Offset(1f, 1f)
        registrar.subscribe(
            FakeSelectable(
                selectableId = 2,
                boundingBox = {
                    require(it == dragOffset - 1)
                    boundingBox
                },
                selectionToReturn = selectionToReturn
            )
        )
        manager.containerLayoutCoordinates = FakeLayoutCoordinates(selectableOffset)
        manager.selection = selectionToReturn
        manager.updateSelection(
            startHandlePosition = Offset.Zero,
            endHandlePosition = Offset.Zero,
            previousHandlePosition = Offset.Zero,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None
        )

        val center = calculateSelectionMagnifierCenterAndroid(manager)

        assertThat(center).isEqualTo(boundingBox.centerRight + selectableOffset)
    }

    @Test
    fun draggingHandleIsUpdated_whenStartHandleDragged() {
        val registrar = SelectionRegistrarImpl()
        val manager = SelectionManager(registrar)
        manager.containerLayoutCoordinates = FakeLayoutCoordinates()
        manager.updateSelection(
            startHandlePosition = Offset.Zero,
            endHandlePosition = Offset.Zero,
            previousHandlePosition = Offset.Zero,
            isStartHandle = true,
            adjustment = SelectionAdjustment.None
        )

        assertThat(manager.draggingHandle).isEqualTo(Handle.SelectionStart)
    }

    @Test
    fun draggingHandleIsUpdated_whenEndHandleDragged() {
        val registrar = SelectionRegistrarImpl()
        val manager = SelectionManager(registrar)
        manager.containerLayoutCoordinates = FakeLayoutCoordinates()
        manager.updateSelection(
            startHandlePosition = Offset.Zero,
            endHandlePosition = Offset.Zero,
            previousHandlePosition = Offset.Zero,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None
        )

        assertThat(manager.draggingHandle).isEqualTo(Handle.SelectionEnd)
    }

    private class FakeSelectable(
        override var selectableId: Long,
        val boundingBox: (Int) -> Rect = { Rect.Zero },
        val selectionToReturn: Selection? = null
    ) : Selectable {
        override fun updateSelection(
            startHandlePosition: Offset,
            endHandlePosition: Offset,
            previousHandlePosition: Offset?,
            isStartHandle: Boolean,
            containerLayoutCoordinates: LayoutCoordinates,
            adjustment: SelectionAdjustment,
            previousSelection: Selection?
        ): Pair<Selection?, Boolean> {
            return Pair(selectionToReturn, false)
        }

        override fun getSelectAllSelection(): Selection = throw UnsupportedOperationException()
        override fun getText(): AnnotatedString = throw UnsupportedOperationException()
        override fun getLayoutCoordinates(): LayoutCoordinates = FakeLayoutCoordinates()
        override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset =
            throw UnsupportedOperationException()

        override fun getBoundingBox(offset: Int): Rect = boundingBox(offset)
        override fun getRangeOfLineContaining(offset: Int): TextRange = TextRange.Zero
    }

    private class FakeLayoutCoordinates(
        private val offset: Offset = Offset.Zero
    ) : LayoutCoordinates {
        override val size: IntSize
            get() = throw UnsupportedOperationException()
        override val providedAlignmentLines: Set<AlignmentLine>
            get() = throw UnsupportedOperationException()
        override val parentLayoutCoordinates: LayoutCoordinates
            get() = throw UnsupportedOperationException()
        override val parentCoordinates: LayoutCoordinates
            get() = throw UnsupportedOperationException()
        override val isAttached: Boolean = true

        override fun windowToLocal(relativeToWindow: Offset): Offset =
            throw UnsupportedOperationException()

        override fun localToWindow(relativeToLocal: Offset): Offset =
            throw UnsupportedOperationException()

        override fun localToRoot(relativeToLocal: Offset): Offset =
            throw UnsupportedOperationException()

        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset
        ): Offset = relativeToSource + offset

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean
        ): Rect = throw UnsupportedOperationException()

        override fun get(alignmentLine: AlignmentLine): Int = throw UnsupportedOperationException()
    }
}
