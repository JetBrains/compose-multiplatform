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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.DrawLayerModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Uptime
import androidx.compose.ui.unit.milliseconds
import androidx.compose.ui.unit.minus
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following PointerInputEvent to PointerInputChangeEvent tests
// 2 down, 2 move, 2 up, converted correctly
// 3 down, 3 move, 3 up, converted correctly
// down, up, down, up, converted correctly
// 2 down, 1 up, same down, both up, converted correctly
// 2 down, 1 up, new down, both up, converted correctly
// new is up, throws exception

// TODO(shepshapard): Write the following hit testing tests
// 2 down, one hits, target receives correct event
// 2 down, one moves in, one out, 2 up, target receives correct event stream
// down, up, receives down and up
// down, move, up, receives all 3
// down, up, then down and misses, target receives down and up
// down, misses, moves in bounds, up, target does not receive event
// down, hits, moves out of bounds, up, target receives all events

// TODO(shepshapard): Write the following offset testing tests
// 3 simultaneous moves, offsets are correct

// TODO(shepshapard): Write the following pointer input dispatch path tests:
// down, move, up, on 2, hits all 5 passes

@SmallTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalLayoutNodeApi::class)
class PointerInputEventProcessorTest {

    private lateinit var root: LayoutNode
    private lateinit var pointerInputEventProcessor: PointerInputEventProcessor
    private val testOwner: TestOwner = spy()

    @Before
    fun setup() {
        root = LayoutNode(0, 0, 500, 500)
        root.attach(testOwner)
        pointerInputEventProcessor = PointerInputEventProcessor(root)
    }

    @Test
    fun process_downMoveUp_convertedCorrectlyAndTraversesAllPassesInCorrectOrder() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val offset = Offset(100f, 200f)
        val offset2 = Offset(300f, 400f)

        val events = arrayOf(
            PointerInputEvent(8712, Uptime.Boot + 3.milliseconds, offset, true),
            PointerInputEvent(8712, Uptime.Boot + 11.milliseconds, offset2, true),
            PointerInputEvent(8712, Uptime.Boot + 13.milliseconds, offset2, false)
        )

        val expectedChanges = arrayOf(
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(
                    Uptime.Boot + 3.milliseconds,
                    offset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    offset2,
                    true
                ),
                previous = PointerInputData(
                    Uptime.Boot + 3.milliseconds,
                    offset,
                    true
                ),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(
                    Uptime.Boot + 13.milliseconds,
                    offset2,
                    false
                ),
                previous = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    offset2,
                    true
                ),
                consumed = ConsumedData()
            )
        )

        // Act

        events.forEach { pointerInputEventProcessor.process(it) }

        // Assert

        // Verify call count
        verify(
            pointerInputFilter,
            times(PointerEventPass.values().size * expectedChanges.size)
        ).onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (expected in expectedChanges) {
                for (pass in PointerEventPass.values()) {
                    verify(pointerInputFilter).onPointerEventMock(
                        eq(pointerEventOf(expected)),
                        eq(pass),
                        any()
                    )
                }
            }
        }
    }

    @Test
    fun process_downHits_targetReceives() {

        // Arrange

        val childOffset = Offset(100f, 200f)
        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val offsets = arrayOf(
            Offset(100f, 200f),
            Offset(300f, 200f),
            Offset(100f, 400f),
            Offset(300f, 400f)
        )

        val events = Array(4) { index ->
            PointerInputEvent(index, Uptime.Boot + 5.milliseconds, offsets[index], true)
        }

        val expectedChanges = Array(4) { index ->
            PointerInputChange(
                id = PointerId(index.toLong()),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offsets[index] - childOffset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(expectedChanges.size)).onPointerEventMock(
            any(),
            eq(PointerEventPass.Initial),
            any()
        )
        // Verify call values
        for (expected in expectedChanges) {
            verify(pointerInputFilter).onPointerEventMock(
                eq(pointerEventOf(expected)),
                eq(PointerEventPass.Initial),
                any()
            )
        }
    }

    @Test
    fun process_downMisses_targetDoesNotReceive() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val offsets = arrayOf(
            Offset(99f, 200f),
            Offset(99f, 400f),
            Offset(100f, 199f),
            Offset(100f, 401f),
            Offset(300f, 199f),
            Offset(300f, 401f),
            Offset(301f, 200f),
            Offset(301f, 400f)
        )

        val events = Array(8) { index ->
            PointerInputEvent(index, Uptime.Boot + 0.milliseconds, offsets[index], true)
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        verify(pointerInputFilter, never()).onPointerEventMock(any(), any(), any())
    }

    @Test
    fun process_downHits3of3_all3PointerNodesReceive() {
        process_partialTreeHits(3)
    }

    @Test
    fun process_downHits2of3_correct2PointerNodesReceive() {
        process_partialTreeHits(2)
    }

    @Test
    fun process_downHits1of3_onlyCorrectPointerNodesReceives() {
        process_partialTreeHits(1)
    }

    private fun process_partialTreeHits(numberOfChildrenHit: Int) {
        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val middlePointerInputFilter = spy(MockPointerInputFilter())
        val parentPointerInputFilter = spy(MockPointerInputFilter())

        val childLayoutNode =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifierImpl2(
                    childPointerInputFilter
                )
            )
        val middleLayoutNode: LayoutNode =
            LayoutNode(
                100, 100, 400, 400,
                PointerInputModifierImpl2(
                    middlePointerInputFilter
                )
            ).apply {
                insertAt(0, childLayoutNode)
            }
        val parentLayoutNode: LayoutNode =
            LayoutNode(
                0, 0, 500, 500,
                PointerInputModifierImpl2(
                    parentPointerInputFilter
                )
            ).apply {
                insertAt(0, middleLayoutNode)
            }
        root.insertAt(0, parentLayoutNode)

        val offset = when (numberOfChildrenHit) {
            3 -> Offset(250f, 250f)
            2 -> Offset(150f, 150f)
            1 -> Offset(50f, 50f)
            else -> throw IllegalStateException()
        }

        val event = PointerInputEvent(0, Uptime.Boot + 5.milliseconds, offset, true)

        // Act

        pointerInputEventProcessor.process(event)

        // Assert

        when (numberOfChildrenHit) {
            3 -> {
                verify(parentPointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
                verify(middlePointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
                verify(childPointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
            }
            2 -> {
                verify(parentPointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
                verify(middlePointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
                verify(childPointerInputFilter, never()).onPointerEventMock(
                    any(),
                    any(),
                    any()
                )
            }
            1 -> {
                verify(parentPointerInputFilter).onPointerEventMock(
                    any(),
                    eq(PointerEventPass.Initial),
                    any()
                )
                verify(middlePointerInputFilter, never()).onPointerEventMock(
                    any(),
                    any(),
                    any()
                )
                verify(childPointerInputFilter, never()).onPointerEventMock(
                    any(),
                    any(),
                    any()
                )
            }
            else -> throw IllegalStateException()
        }
    }

    @Test
    fun process_modifiedChange_isPassedToNext() {

        // Arrange

        val input = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                Offset(100f, 0f),
                true
            ),
            previous = PointerInputData(
                Uptime.Boot + 3.milliseconds,
                Offset(0f, 0f),
                true
            ),
            consumed = ConsumedData(
                positionChange = Offset(0f, 0f)
            )
        )
        val output = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                Offset(100f, 0f),
                true
            ),
            previous = PointerInputData(
                Uptime.Boot + 3.milliseconds,
                Offset(0f, 0f),
                true
            ),
            consumed = ConsumedData(
                positionChange = Offset(13f, 0f)
            )
        )

        val pointerInputFilter =
            spy(
                TestPointerInputFilter { pointerEvent, pass, _ ->
                    if (pointerEvent.changes == listOf(input) &&
                        pass == PointerEventPass.Initial
                    ) {
                        listOf(output)
                    } else {
                        pointerEvent.changes
                    }
                }
            )

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val down = PointerInputEvent(
            0,
            Uptime.Boot + 3.milliseconds,
            Offset(0f, 0f),
            true
        )
        val move = PointerInputEvent(
            0,
            Uptime.Boot + 5.milliseconds,
            Offset(100f, 0f),
            true
        )

        // Act

        pointerInputEventProcessor.process(down)
        reset(pointerInputFilter)
        pointerInputEventProcessor.process(move)

        // Assert

        verify(pointerInputFilter)
            .onPointerEventMock(eq(pointerEventOf(input)), eq(PointerEventPass.Initial), any())
        verify(pointerInputFilter)
            .onPointerEventMock(eq(pointerEventOf(output)), eq(PointerEventPass.Main), any())
    }

    @Test
    fun process_nodesAndAdditionalOffsetIncreasinglyInset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            43, 51,
            99, 99
        )
    }

    @Test
    fun process_nodesAndAdditionalOffsetIncreasinglyOutset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            -43, -51,
            1, 1
        )
    }

    @Test
    fun process_nodesAndAdditionalOffsetNotOffset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0,
            50, 50
        )
    }

    @Suppress("SameParameterValue")
    private fun process_dispatchInfoIsCorrect(
        pX1: Int,
        pY1: Int,
        pX2: Int,
        pY2: Int,
        mX1: Int,
        mY1: Int,
        mX2: Int,
        mY2: Int,
        cX1: Int,
        cY1: Int,
        cX2: Int,
        cY2: Int,
        aOX: Int,
        aOY: Int,
        pointerX: Int,
        pointerY: Int
    ) {

        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val middlePointerInputFilter = spy(MockPointerInputFilter())
        val parentPointerInputFilter = spy(MockPointerInputFilter())

        val childOffset = Offset(cX1.toFloat(), cY1.toFloat())
        val childLayoutNode = LayoutNode(
            cX1, cY1, cX2, cY2,
            PointerInputModifierImpl2(
                childPointerInputFilter
            )
        )
        val middleOffset = Offset(mX1.toFloat(), mY1.toFloat())
        val middleLayoutNode: LayoutNode = LayoutNode(
            mX1, mY1, mX2, mY2,
            PointerInputModifierImpl2(
                middlePointerInputFilter
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }
        val parentLayoutNode: LayoutNode = LayoutNode(
            pX1, pY1, pX2, pY2,
            PointerInputModifierImpl2(
                parentPointerInputFilter
            )
        ).apply {
            insertAt(0, middleLayoutNode)
        }

        testOwner.position = IntOffset(aOX, aOY)

        root.insertAt(0, parentLayoutNode)

        val additionalOffset = IntOffset(aOX, aOY)

        val offset = Offset(pointerX.toFloat(), pointerY.toFloat())

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)

        val pointerInputHandlers = arrayOf(
            parentPointerInputFilter,
            middlePointerInputFilter,
            childPointerInputFilter
        )

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - childOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        )

        val expectedSizes = arrayOf(
            IntSize(pX2 - pX1, pY2 - pY1),
            IntSize(mX2 - mX1, mY2 - mY1),
            IntSize(cX2 - cX1, cY2 - cY1)
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        pointerInputHandlers.forEach {
            verify(it, times(PointerEventPass.values().size)).onPointerEventMock(
                any(),
                any(),
                any()
            )
        }
        // Verify call values
        for (pass in PointerEventPass.values()) {
            for (i in pointerInputHandlers.indices) {
                verify(pointerInputHandlers[i]).onPointerEventMock(
                    pointerEventOf(expectedPointerInputChanges[i]),
                    pass,
                    expectedSizes[i]
                )
            }
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -------------
     *  |     |     |
     *  |  t  |     |
     *  |     |     |
     *  |-----|     |
     *  |           |
     *  |     |-----|
     *  |     |     |
     *  |     |  t  |
     *  |     |     |
     *  -------------
     *
     * Where there is one child in the top right, and one in the bottom left, and 2 down touches,
     * one in the top left and one in the bottom right.
     */
    @Test
    fun process_2DownOn2DifferentPointerNodes_hitAndDispatchInfoAreCorrect() {

        // Arrange

        val childPointerInputFilter1 = spy(MockPointerInputFilter())
        val childPointerInputFilter2 = spy(MockPointerInputFilter())

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 50, 50,
                PointerInputModifierImpl2(
                    childPointerInputFilter1
                )
            )
        val childLayoutNode2 =
            LayoutNode(
                50, 50, 100, 100,
                PointerInputModifierImpl2(
                    childPointerInputFilter2
                )
            )
        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(0, childLayoutNode2)
        }

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)

        val down = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 5.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 5.milliseconds, offset2, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offset1,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offset2 - Offset(50f, 50f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        verify(childPointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1),
                    pointerEventPass,
                    IntSize(50, 50)
                )
            verify(childPointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(50, 50)
                )
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  | t      |    |
     *  |        |    |
     *  |  |-------|  |
     *  |  | t     |  |
     *  |  |       |  |
     *  |  |       |  |
     *  |--|  |-------|
     *  |  |  | t     |
     *  |  |  |       |
     *  |  |  |       |
     *  |  |--|       |
     *  |     |       |
     *  ---------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnOverlappingPointerNodes_hitAndDispatchInfoAreCorrect() {

        val childPointerInputFilter1 = spy(MockPointerInputFilter())
        val childPointerInputFilter2 = spy(MockPointerInputFilter())
        val childPointerInputFilter3 = spy(MockPointerInputFilter())

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 50, 150, 150,
            PointerInputModifierImpl2(
                childPointerInputFilter2
            )
        )
        val childLayoutNode3 = LayoutNode(
            100, 100, 200, 200,
            PointerInputModifierImpl2(
                childPointerInputFilter3
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            insertAt(2, childLayoutNode3)
        }

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)
        val offset3 = Offset(125f, 125f)

        val down = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 5.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 5.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 5.milliseconds, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offset1,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offset2 - Offset(50f, 50f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offset3 - Offset(100f, 100f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        verify(childPointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter3, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1),
                    pointerEventPass,
                    IntSize(100, 100)
                )
            verify(childPointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(100, 100)
                )
            verify(childPointerInputFilter3)
                .onPointerEventMock(
                    pointerEventOf(expectedChange3),
                    pointerEventPass,
                    IntSize(100, 100)
                )
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  |             |
     *  |      t      |
     *  |             |
     *  |  |-------|  |
     *  |  |       |  |
     *  |  |   t   |  |
     *  |  |       |  |
     *  |  |-------|  |
     *  |             |
     *  |      t      |
     *  |             |
     *  ---------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnFloatingPointerNodeV_hitAndDispatchInfoAreCorrect() {

        val childPointerInputFilter1 = spy(MockPointerInputFilter())
        val childPointerInputFilter2 = spy(MockPointerInputFilter())

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 150,
            PointerInputModifierImpl2(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            25, 50, 75, 100,
            PointerInputModifierImpl2(
                childPointerInputFilter2
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
        }

        val offset1 = Offset(50f, 25f)
        val offset2 = Offset(50f, 75f)
        val offset3 = Offset(50f, 125f)

        val down = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 7.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 7.milliseconds, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset1,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset2 - Offset(25f, 50f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset3,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        verify(childPointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntSize(100, 150)
                )
            verify(childPointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(50, 50)
                )
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -----------------
     *  |               |
     *  |   |-------|   |
     *  |   |       |   |
     *  | t |   t   | t |
     *  |   |       |   |
     *  |   |-------|   |
     *  |               |
     *  -----------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnFloatingPointerNodeH_hitAndDispatchInfoAreCorrect() {
        val childPointerInputFilter1 = spy(MockPointerInputFilter())
        val childPointerInputFilter2 = spy(MockPointerInputFilter())

        val childLayoutNode1 = LayoutNode(
            0, 0, 150, 100,
            PointerInputModifierImpl2(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 25, 100, 75,
            PointerInputModifierImpl2(
                childPointerInputFilter2
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
        }

        val offset1 = Offset(25f, 50f)
        val offset2 = Offset(75f, 50f)
        val offset3 = Offset(125f, 50f)

        val down = PointerInputEvent(
            Uptime.Boot + 11.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 11.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 11.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 11.milliseconds, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    offset1,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    offset2 - Offset(50f, 25f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    offset3,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        verify(childPointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntSize(150, 100)
                )
            verify(childPointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(50, 50)
                )
        }
    }

    /**
     * This test creates a layout of this shape:
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are clipped by their parent LayoutNode. 4
     * touches touch just inside the parent LayoutNode and inside the child LayoutNodes. 8
     * touches touch just outside the parent LayoutNode but inside the child LayoutNodes.
     *
     * Because layout node bounds are not used to clip pointer input hit testing, all pointers
     * should hit.
     */
    @Test
    fun process_4DownInClippedAreaOfLnsWithPims_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputFilterTopLeft = spy(MockPointerInputFilter())
        val pointerInputFilterTopRight = spy(MockPointerInputFilter())
        val pointerInputFilterBottomLeft = spy(MockPointerInputFilter())
        val pointerInputFilterBottomRight = spy(MockPointerInputFilter())

        val layoutNodeTopLeft = LayoutNode(
            -1, -1, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilterTopLeft
            )
        )
        val layoutNodeTopRight = LayoutNode(
            2, -1, 4, 1,
            PointerInputModifierImpl2(
                pointerInputFilterTopRight
            )
        )
        val layoutNodeBottomLeft = LayoutNode(
            -1, 2, 1, 4,
            PointerInputModifierImpl2(
                pointerInputFilterBottomLeft
            )
        )
        val layoutNodeBottomRight = LayoutNode(
            2, 2, 4, 4,
            PointerInputModifierImpl2(
                pointerInputFilterBottomRight
            )
        )

        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            insertAt(0, layoutNodeTopLeft)
            insertAt(1, layoutNodeTopRight)
            insertAt(2, layoutNodeBottomLeft)
            insertAt(3, layoutNodeBottomRight)
        }
        root.apply {
            insertAt(0, parentLayoutNode)
        }
        val offsetsTopLeft =
            listOf(
                Offset(0f, 1f),
                Offset(1f, 0f),
                Offset(1f, 1f)
            )

        val offsetsTopRight =
            listOf(
                Offset(3f, 0f),
                Offset(3f, 1f),
                Offset(4f, 1f)
            )

        val offsetsBottomLeft =
            listOf(
                Offset(0f, 3f),
                Offset(1f, 3f),
                Offset(1f, 4f)
            )

        val offsetsBottomRight =
            listOf(
                Offset(3f, 3f),
                Offset(3f, 4f),
                Offset(4f, 3f)
            )

        val allOffsets = offsetsTopLeft + offsetsTopRight + offsetsBottomLeft + offsetsBottomRight

        val pointerInputEvent =
            PointerInputEvent(
                Uptime.Boot + 11.milliseconds,
                (allOffsets.indices).map {
                    PointerInputEventData(it, Uptime.Boot + 11.milliseconds, allOffsets[it], true)
                }
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChangesTopLeft =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        Offset(
                            offsetsTopLeft[it].x,
                            offsetsTopLeft[it].y
                        ),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            }

        val expectedChangesTopRight =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 3),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        Offset(
                            offsetsTopRight[it].x - 3f,
                            offsetsTopRight[it].y
                        ),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            }

        val expectedChangesBottomLeft =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 6),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        Offset(
                            offsetsBottomLeft[it].x,
                            offsetsBottomLeft[it].y - 3f
                        ),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            }

        val expectedChangesBottomRight =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 9),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        Offset(
                            offsetsBottomRight[it].x - 3f,
                            offsetsBottomRight[it].y - 3f
                        ),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            }

        // Verify call values
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(pointerInputFilterTopLeft).onPointerEventMock(
                eq(pointerEventOf(*expectedChangesTopLeft.toTypedArray())),
                eq(pointerEventPass),
                any()
            )
            verify(pointerInputFilterTopRight).onPointerEventMock(
                eq(pointerEventOf(*expectedChangesTopRight.toTypedArray())),
                eq(pointerEventPass),
                any()
            )
            verify(pointerInputFilterBottomLeft).onPointerEventMock(
                eq(pointerEventOf(*expectedChangesBottomLeft.toTypedArray())),
                eq(pointerEventPass),
                any()
            )
            verify(pointerInputFilterBottomRight).onPointerEventMock(
                eq(pointerEventOf(*expectedChangesBottomRight.toTypedArray())),
                eq(pointerEventPass),
                any()
            )
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *   |---|
     *   |tt |
     *   |t  |
     *   |---|t
     *       tt
     *
     *   But where the additional offset suggest something more like this shape.
     *
     *   tt
     *   t|---|
     *    |  t|
     *    | tt|
     *    |---|
     *
     *   Without the additional offset, it would be expected that only the top left 3 pointers would
     *   hit, but with the additional offset, only the bottom right 3 hit.
     */
    @Test
    fun process_rootIsOffset_onlyCorrectPointersHit() {

        // Arrange
        val singlePointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 2, 2,
            PointerInputModifierImpl2(
                singlePointerInputFilter
            )
        )
        root.apply {
            insertAt(0, layoutNode)
        }
        val offsetsThatHit =
            listOf(
                Offset(2f, 2f),
                Offset(2f, 1f),
                Offset(1f, 2f)
            )
        val offsetsThatMiss =
            listOf(
                Offset(0f, 0f),
                Offset(0f, 1f),
                Offset(1f, 0f)
            )
        val allOffsets = offsetsThatHit + offsetsThatMiss
        val pointerInputEvent =
            PointerInputEvent(
                Uptime.Boot + 11.milliseconds,
                (allOffsets.indices).map {
                    PointerInputEventData(it, Uptime.Boot + 11.milliseconds, allOffsets[it], true)
                }
            )
        testOwner.position = IntOffset(1, 1)

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - Offset(1f, 1f),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            }

        // Verify call count
        verify(singlePointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(singlePointerInputFilter).onPointerEventMock(
                eq(pointerEventOf(*expectedChanges.toTypedArray())),
                eq(pointerEventPass),
                any()
            )
        }
    }

    @Test
    fun process_downOn3NestedPointerInputModifiers_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter1 = spy(MockPointerInputFilter())
        val pointerInputFilter2 = spy(MockPointerInputFilter())
        val pointerInputFilter3 = spy(MockPointerInputFilter())

        val modifier = PointerInputModifierImpl2(pointerInputFilter1) then
            PointerInputModifierImpl2(pointerInputFilter2) then
            PointerInputModifierImpl2(pointerInputFilter3)

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        )

        root.apply {
            insertAt(0, layoutNode)
        }

        val offset1 = Offset(50f, 75f)

        val down = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset1 - Offset(25f, 50f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count
        verify(pointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter3, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange),
                    pointerEventPass,
                    IntSize(50, 50)
                )
            verify(pointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange),
                    pointerEventPass,
                    IntSize(50, 50)
                )
            verify(pointerInputFilter3)
                .onPointerEventMock(
                    pointerEventOf(expectedChange),
                    pointerEventPass,
                    IntSize(50, 50)
                )
        }
    }

    @Test
    fun process_downOnDeeplyNestedPointerInputModifier_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter = spy(MockPointerInputFilter())

        val layoutNode1 =
            LayoutNode(
                1, 5, 500, 500,
                PointerInputModifierImpl2(pointerInputFilter)
            )
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            insertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        root.apply {
            insertAt(0, layoutNode4)
        }

        val offset1 = Offset(499f, 499f)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset1 - Offset(1f + 2f + 3f + 4f, 5f + 6f + 7f + 8f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputFilter)
                .onPointerEventMock(
                    pointerEventOf(expectedChange),
                    pointerEventPass,
                    IntSize(499, 495)
                )
        }
    }

    @Test
    fun process_downOnComplexPointerAndLayoutNodePath_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter1 = spy(MockPointerInputFilter())
        val pointerInputFilter2 = spy(MockPointerInputFilter())
        val pointerInputFilter3 = spy(MockPointerInputFilter())
        val pointerInputFilter4 = spy(MockPointerInputFilter())

        val layoutNode1 = LayoutNode(
            1, 6, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter1)
                then PointerInputModifierImpl2(pointerInputFilter2)
        )
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3 =
            LayoutNode(
                3, 8, 500, 500,
                PointerInputModifierImpl2(pointerInputFilter3)
                    then PointerInputModifierImpl2(pointerInputFilter4)
            ).apply {
                insertAt(0, layoutNode2)
            }

        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            insertAt(0, layoutNode4)
        }
        root.apply {
            insertAt(0, layoutNode5)
        }

        val offset1 = Offset(499f, 499f)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 3.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 3.milliseconds, offset1, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 3.milliseconds,
                    offset1 - Offset(
                        1f + 2f + 3f + 4f + 5f,
                        6f + 7f + 8f + 9f + 10f
                    ),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedChange2 =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 3.milliseconds,
                    offset1 - Offset(3f + 4f + 5f, 8f + 9f + 10f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        // Verify call count
        verify(pointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter3, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter4, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        PointerEventPass.values()

        // Verify call values
        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputFilter1)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1),
                    pointerEventPass,
                    IntSize(499, 494)
                )
            verify(pointerInputFilter2)
                .onPointerEventMock(
                    pointerEventOf(expectedChange1),
                    pointerEventPass,
                    IntSize(499, 494)
                )
            verify(pointerInputFilter3)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(497, 492)
                )
            verify(pointerInputFilter4)
                .onPointerEventMock(
                    pointerEventOf(expectedChange2),
                    pointerEventPass,
                    IntSize(497, 492)
                )
        }
    }

    @Test
    fun process_downOnFullyOverlappingPointerInputModifiers_onlyTopPointerInputModifierReceives() {

        val pointerInputFilter1 = spy(MockPointerInputFilter())
        val pointerInputFilter2 = spy(MockPointerInputFilter())

        val layoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                pointerInputFilter2
            )
        )

        root.apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, Offset(50f, 50f), true
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert
        verify(pointerInputFilter2, times(3)).onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter1, never()).onPointerEventMock(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputModifierInLayoutNodeWithNoSize_downNotReceived() {

        val pointerInputFilter1 = spy(MockPointerInputFilter())

        val layoutNode1 = LayoutNode(
            0, 0, 0, 0,
            PointerInputModifierImpl2(pointerInputFilter1)
        )

        root.apply {
            insertAt(0, layoutNode1)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, Offset(0f, 0f), true
        )

        // Act
        pointerInputEventProcessor.process(down)

        // Assert
        verify(pointerInputFilter1, never()).onPointerEventMock(any(), any(), any())
    }

    // Cancel Handlers

    @Test
    fun processCancel_noPointers_doesntCrash() {
        pointerInputEventProcessor.processCancel()
    }

    @Test
    fun processCancel_downThenCancel_pimOnlyReceivesCorrectDownThenCancel() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        root.insertAt(0, layoutNode)

        val pointerInputEvent =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(250f, 250f),
                true
            )

        val expectedChange =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(250f, 250f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedChange)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter).onCancel()
        }
    }

    @Test
    fun processCancel_downDownOnSamePimThenCancel_pimOnlyReceivesCorrectChangesThenCancel() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val pointerInputEvent1 =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(200f, 200f),
                true
            )

        val pointerInputEvent2 =
            PointerInputEvent(
                Uptime.Boot + 10.milliseconds,
                listOf(
                    PointerInputEventData(
                        7,
                        Uptime.Boot + 10.milliseconds,
                        Offset(200f, 200f),
                        true
                    ),
                    PointerInputEventData(
                        9,
                        Uptime.Boot + 10.milliseconds,
                        Offset(300f, 300f),
                        true
                    )
                )
            )

        val expectedChanges1 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    current = PointerInputData(
                        Uptime.Boot + 5.milliseconds,
                        Offset(200f, 200f),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            )

        val expectedChanges2 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    current = PointerInputData(
                        Uptime.Boot + 10.milliseconds,
                        Offset(200f, 200f),
                        true
                    ),
                    previous = PointerInputData(
                        Uptime.Boot + 5.milliseconds,
                        Offset(200f, 200f),
                        true
                    ),
                    consumed = ConsumedData()
                ),
                PointerInputChange(
                    id = PointerId(9),
                    current = PointerInputData(
                        Uptime.Boot + 10.milliseconds,
                        Offset(300f, 300f),
                        true
                    ),
                    previous = PointerInputData(
                        null,
                        null,
                        false
                    ),
                    consumed = ConsumedData()
                )
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent1)
        pointerInputEventProcessor.process(pointerInputEvent2)
        pointerInputEventProcessor.processCancel()

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size * 2))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(*expectedChanges1.toTypedArray())),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(*expectedChanges2.toTypedArray())),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter).onCancel()
        }
    }

    @Test
    fun processCancel_downOn2DifferentPimsThenCancel_pimsOnlyReceiveCorrectDownsThenCancel() {

        // Arrange

        val pointerInputFilter1 = spy(MockPointerInputFilter())
        val layoutNode1 = LayoutNode(
            0, 0, 199, 199,
            PointerInputModifierImpl2(pointerInputFilter1)
        )

        val pointerInputFilter2 = spy(MockPointerInputFilter())
        val layoutNode2 = LayoutNode(
            200, 200, 399, 399,
            PointerInputModifierImpl2(pointerInputFilter2)
        )

        root.insertAt(0, layoutNode1)
        root.insertAt(1, layoutNode2)

        val pointerInputEventData1 =
            PointerInputEventData(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(100f, 100f),
                true
            )

        val pointerInputEventData2 =
            PointerInputEventData(
                9,
                Uptime.Boot + 5.milliseconds,
                Offset(300f, 300f),
                true
            )

        val pointerInputEvent = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(pointerInputEventData1, pointerInputEventData2)
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(100f, 100f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedChange2 =
            PointerInputChange(
                id = PointerId(9),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(100f, 100f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        // Verify call count
        verify(pointerInputFilter1, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())
        verify(pointerInputFilter2, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter1) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter1).onPointerEventMock(
                    eq(pointerEventOf(expectedChange1)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter1).onCancel()
        }
        inOrder(pointerInputFilter2) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter2).onPointerEventMock(
                    eq(pointerEventOf(expectedChange2)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter2).onCancel()
        }
    }

    @Test
    fun processCancel_downMoveCancel_pimOnlyReceivesCorrectDownMoveCancel() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        root.insertAt(0, layoutNode)

        val down =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(200f, 200f),
                true
            )

        val move =
            PointerInputEvent(
                7,
                Uptime.Boot + 10.milliseconds,
                Offset(300f, 300f),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(200f, 200f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedMove =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 10.milliseconds,
                    Offset(300f, 300f),
                    true
                ),
                previous = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(200f, 200f),
                    true
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.process(move)
        pointerInputEventProcessor.processCancel()

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size * 2))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedMove)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter).onCancel()
        }
    }

    @Test
    fun processCancel_downCancelMoveUp_pimOnlyReceivesCorrectDownCancel() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        root.insertAt(0, layoutNode)

        val down =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(200f, 200f),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(200f, 200f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.processCancel()

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter).onCancel()
        }
    }

    @Test
    fun processCancel_downCancelDown_pimOnlyReceivesCorrectDownCancelDown() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.insertAt(0, layoutNode)

        val down1 =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                Offset(200f, 200f),
                true
            )

        val down2 =
            PointerInputEvent(
                7,
                Uptime.Boot + 10.milliseconds,
                Offset(200f, 200f),
                true
            )

        val expectedDown1 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    Offset(200f, 200f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedDown2 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 10.milliseconds,
                    Offset(200f, 200f),
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down1)
        pointerInputEventProcessor.processCancel()
        pointerInputEventProcessor.process(down2)

        // Assert

        // Verify call count
        verify(pointerInputFilter, times(PointerEventPass.values().size * 2))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        inOrder(pointerInputFilter) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedDown1)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputFilter).onCancel()
            for (pass in PointerEventPass.values()) {
                verify(pointerInputFilter).onPointerEventMock(
                    eq(pointerEventOf(expectedDown2)),
                    eq(pass),
                    any()
                )
            }
        }
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = spy(MockPointerInputFilter())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val offset = Offset(50f, 50f)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedUpChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    null,
                    false
                ),
                previous = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset,
                    true
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert

        // Verify call count
        verify(parentPointerInputFilter, times(PointerEventPass.values().size * 2))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        PointerEventPass.values().forEach {
            verify(parentPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedUpChange)), eq(it), any())
        }
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_cancelDispatchedToCorrectPointerInputModifierImpl2() {

        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = spy(MockPointerInputFilter())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, Offset(50f, 50f), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert
        verify(childPointerInputFilter).onCancel()
        verify(parentPointerInputFilter, never()).onCancel()
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                childPointerInputFilter
            )
        )

        val parentPointerInputFilter = spy(MockPointerInputFilter())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                parentPointerInputFilter
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val offset = Offset(50f, 50f)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset,
                    true
                ),
                previous = PointerInputData(
                    null,
                    null,
                    false
                ),
                consumed = ConsumedData()
            )

        val expectedUpChange =
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 11.milliseconds,
                    null,
                    false
                ),
                previous = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset,
                    true
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier
        pointerInputEventProcessor.process(up)

        // Assert

        // Verify call count
        verify(parentPointerInputFilter, times(PointerEventPass.values().size * 2))
            .onPointerEventMock(any(), any(), any())
        verify(childPointerInputFilter, times(PointerEventPass.values().size))
            .onPointerEventMock(any(), any(), any())

        // Verify call values
        PointerEventPass.values().forEach {
            verify(parentPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputFilter)
                .onPointerEventMock(eq(pointerEventOf(expectedUpChange)), eq(it), any())
        }
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_cancelDispatchedToCorrectPim() {

        // Arrange

        val childPointerInputFilter = spy(MockPointerInputFilter())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = spy(MockPointerInputFilter())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, Offset(50f, 50f), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier
        pointerInputEventProcessor.process(up)

        // Assert
        verify(childPointerInputFilter).onCancel()
        verify(parentPointerInputFilter, never()).onCancel()
    }

    @Test
    fun process_downNoPointerInputModifiers_nothingInteractedWithAndNoMovementConsumed() {
        val pointerInputEvent =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, Offset(0f, 0f), true)

        val result: ProcessResult = pointerInputEventProcessor.process(pointerInputEvent)

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false
            )
        )
    }

    @Test
    fun process_downNoPointerInputModifiersHit_nothingInteractedWithAndNoMovementConsumed() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())

        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        root.apply {
            insertAt(0, layoutNode)
        }

        val offsets =
            listOf(
                Offset(-1f, 0f),
                Offset(0f, -1f),
                Offset(1f, 0f),
                Offset(0f, 1f)
            )
        val pointerInputEvent =
            PointerInputEvent(
                Uptime.Boot + 11.milliseconds,
                (offsets.indices).map {
                    PointerInputEventData(it, Uptime.Boot + 11.milliseconds, offsets[it], true)
                }
            )

        // Act

        val result: ProcessResult = pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false
            )
        )
    }

    @Test
    fun process_downPointerInputModifierHit_somethingInteractedWithAndNoMovementConsumed() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        root.apply { insertAt(0, layoutNode) }
        val pointerInputEvent =
            PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(0f, 0f), true)

        // Act

        val result = pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = true,
                anyMovementConsumed = false
            )
        )
    }

    @Test
    fun process_downHitsPifRemovedPointerMoves_nothingInteractedWithAndNoMovementConsumed() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        root.apply { insertAt(0, layoutNode) }
        val down = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(1f, 0f), true)

        // Act

        root.removeAt(0, 1)
        val result = pointerInputEventProcessor.process(move)

        // Assert

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false
            )
        )
    }

    @Test
    fun process_downHitsPointerMovesNothingConsumed_somethingInteractedWithAndNoMovementConsumed() {

        // Arrange

        val pointerInputFilter = spy(MockPointerInputFilter())
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        root.apply { insertAt(0, layoutNode) }
        val down = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(1f, 0f), true)

        // Act

        val result = pointerInputEventProcessor.process(move)

        // Assert

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = true,
                anyMovementConsumed = false
            )
        )
    }

    @Test
    fun process_downHitsPointerMovementConsumed_somethingInteractedWithAndMovementConsumed() {

        // Arrange

        val pointerInputFilter: PointerInputFilter =
            spy(
                TestPointerInputFilter { pointerEvent, pass, _ ->
                    if (pass == PointerEventPass.Initial) {
                        pointerEvent.changes.map { it.consumePositionChange(1f, 0f) }
                    } else {
                        pointerEvent.changes
                    }
                }
            )

        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        root.apply { insertAt(0, layoutNode) }
        val down = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, Offset(1f, 0f), true)

        // Act

        val result = pointerInputEventProcessor.process(move)

        // Assert

        assertThat(result).isEqualTo(
            ProcessResult(
                dispatchedToAPointerInputModifier = true,
                anyMovementConsumed = true
            )
        )
    }

    @Test
    fun processResult_trueTrue_propValuesAreCorrect() {
        val processResult1 = ProcessResult(
            dispatchedToAPointerInputModifier = true,
            anyMovementConsumed = true
        )
        assertThat(processResult1.dispatchedToAPointerInputModifier).isTrue()
        assertThat(processResult1.anyMovementConsumed).isTrue()
    }

    @Test
    fun processResult_trueFalse_propValuesAreCorrect() {
        val processResult1 = ProcessResult(
            dispatchedToAPointerInputModifier = true,
            anyMovementConsumed = false
        )
        assertThat(processResult1.dispatchedToAPointerInputModifier).isTrue()
        assertThat(processResult1.anyMovementConsumed).isFalse()
    }

    @Test
    fun processResult_falseTrue_propValuesAreCorrect() {
        val processResult1 = ProcessResult(
            dispatchedToAPointerInputModifier = false,
            anyMovementConsumed = true
        )
        assertThat(processResult1.dispatchedToAPointerInputModifier).isFalse()
        assertThat(processResult1.anyMovementConsumed).isTrue()
    }

    @Test
    fun processResult_falseFalse_propValuesAreCorrect() {
        val processResult1 = ProcessResult(
            dispatchedToAPointerInputModifier = false,
            anyMovementConsumed = false
        )
        assertThat(processResult1.dispatchedToAPointerInputModifier).isFalse()
        assertThat(processResult1.anyMovementConsumed).isFalse()
    }
}

abstract class TestOwner : Owner {
    var position: IntOffset? = null

    @ExperimentalLayoutNodeApi
    override val root: LayoutNode
        get() = LayoutNode()

    override fun calculatePosition(): IntOffset {
        return position ?: IntOffset.Zero
    }
}

open class TestPointerInputFilter(
    val pointerInputHandler: PointerInputHandler = { pointerEvent, _, _ -> pointerEvent.changes }
) : PointerInputFilter() {

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ): List<PointerInputChange> = onPointerEventMock(pointerEvent, pass, bounds as Any)

    open fun onPointerEventMock(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: Any
    ): List<PointerInputChange> {
        return pointerInputHandler(pointerEvent, pass, bounds as IntSize)
    }

    override fun onCancel() {}
}

private class PointerInputModifierImpl2(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

@OptIn(ExperimentalLayoutNodeApi::class)
private fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int, modifier: Modifier = Modifier) =
    LayoutNode().apply {
        this.modifier = modifier
        measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks("not supported") {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureScope.MeasureResult =
                measureScope.layout(x2 - x, y2 - y) {}
        }
        attach(mockOwner())
        layoutState = LayoutNode.LayoutState.NeedsRemeasure
        remeasure(Constraints())
        var wrapper: LayoutNodeWrapper? = outerLayoutNodeWrapper
        while (wrapper != null) {
            wrapper.measureResult = innerLayoutNodeWrapper.measureResult
            wrapper = (wrapper as? LayoutNodeWrapper)?.wrapped
        }
        place(x, y)
        detach()
    }

@ExperimentalLayoutNodeApi
private fun mockOwner(
    position: IntOffset = IntOffset.Zero,
    targetRoot: LayoutNode = LayoutNode()
): Owner = MockOwner(position, targetRoot)

@ExperimentalLayoutNodeApi
@OptIn(
    ExperimentalFocus::class,
    InternalCoreApi::class
)
private class MockOwner(
    private val position: IntOffset,
    private val targetRoot: LayoutNode
) : Owner {
    override fun calculatePosition(): IntOffset = position
    override fun requestFocus(): Boolean = false

    @ExperimentalKeyInput
    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = false

    override fun pauseModelReadObserveration(block: () -> Unit) {
        block()
    }

    override val root: LayoutNode
        get() = targetRoot
    override val hapticFeedBack: HapticFeedback
        get() = TODO("Not yet implemented")
    override val clipboardManager: ClipboardManager
        get() = TODO("Not yet implemented")
    override val textToolbar: TextToolbar
        get() = TODO("Not yet implemented")
    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")
    override val autofill: Autofill?
        get() = null
    override val density: Density
        get() = Density(1f)
    override val semanticsOwner: SemanticsOwner
        get() = TODO("Not yet implemented")
    override val textInputService: TextInputService
        get() = TODO("Not yet implemented")
    override val focusManager: FocusManager
        get() = TODO("Not yet implemented")
    override val fontLoader: Font.ResourceLoader
        get() = TODO("Not yet implemented")
    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr
    override var showLayoutBounds: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}

    override fun onRequestMeasure(layoutNode: LayoutNode) {
    }

    override fun onRequestRelayout(layoutNode: LayoutNode) {
    }

    override val hasPendingMeasureOrLayout = false

    override fun onAttach(node: LayoutNode) {
    }

    override fun onDetach(node: LayoutNode) {
    }

    override fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit) {
        block()
    }

    override fun <T : OwnerScope> observeReads(
        target: T,
        onChanged: (T) -> Unit,
        block: () -> Unit
    ) {
        block()
    }

    override fun measureAndLayout() {
    }

    override fun createLayer(
        drawLayerModifier: DrawLayerModifier,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        TODO("Not yet implemented")
    }

    override fun onSemanticsChange() {
    }

    override val measureIteration: Long
        get() = 0

    override fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit) {
        block()
    }
}

open class MockPointerInputFilter : PointerInputFilter() {

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ): List<PointerInputChange> {
        return onPointerEventMock(pointerEvent, pass, bounds as Any)
    }

    override fun onCancel() {}

    open fun onPointerEventMock(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: Any
    ): List<PointerInputChange> {
        return pointerEvent.changes
    }
}
