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

import android.view.InputDevice
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastMaxBy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInputEventProcessorTest {

    private lateinit var pointerInputEventProcessor: PointerInputEventProcessor
    private lateinit var testOwner: TestOwner
    private val positionCalculator = object : PositionCalculator {
        override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

        override fun localToScreen(localPosition: Offset): Offset = localPosition
    }

    @Before
    fun setup() {
        testOwner = TestOwner()
        pointerInputEventProcessor = PointerInputEventProcessor(testOwner.root)
    }

    private fun addToRoot(vararg layoutNodes: LayoutNode) {
        layoutNodes.forEachIndexed { index, node ->
            testOwner.root.insertAt(index, node)
        }
        testOwner.measureAndLayout()
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun pointerTypePassed() {
        val pointerTypes = listOf(
            PointerType.Unknown,
            PointerType.Touch,
            PointerType.Mouse,
            PointerType.Stylus,
            PointerType.Eraser
        )

        // Arrange
        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val offset = Offset(100f, 200f)
        val previousEvents = mutableListOf<PointerInputEventData>()
        val events = pointerTypes.mapIndexed { index, pointerType ->
            previousEvents += PointerInputEventData(
                id = PointerId(index.toLong()),
                uptime = index.toLong(),
                position = Offset(offset.x + index, offset.y + index),
                positionOnScreen = Offset(offset.x + index, offset.y + index),
                down = true,
                type = pointerType
            )
            val data = previousEvents.map {
                it.copy(uptime = index.toLong())
            }
            PointerInputEvent(index.toLong(), data)
        }

        // Act

        events.forEach { pointerInputEventProcessor.process(it) }

        // Assert

        val log = pointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log)
            .hasSize(PointerEventPass.values().size * pointerTypes.size)

        // Verify types of the pointers
        repeat(pointerTypes.size) { eventIndex ->
            PointerEventPass.values().forEachIndexed { passIndex, pass ->
                val item = log[passIndex + (eventIndex * PointerEventPass.values().size)]
                assertThat(item.pass).isEqualTo(pass)

                val changes = item.pointerEvent.changes
                assertThat(changes.size).isEqualTo(eventIndex + 1)

                for (i in 0..eventIndex) {
                    val pointerType = pointerTypes[i]
                    val change = changes[i]
                    assertThat(change.type).isEqualTo(pointerType)
                }
            }
        }
    }

    /**
     * PointerInputEventProcessor doesn't currently support reentrancy and
     * b/233209795 indicates that it is likely causing a crash. This test
     * ensures that if we have reentrancy that we exit without handling
     * the event. This test can be replaced with tests supporting reentrant
     * behavior when reentrancy is supported.
     */
    @Test
    fun noReentrancy() {
        var reentrancyCount = 0
        // Arrange
        val reentrantPointerInputFilter = object : PointerInputFilter() {
            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ) {
                if (pass != PointerEventPass.Initial) {
                    return
                }
                if (reentrancyCount > 1) {
                    // Don't allow infinite recursion. Just enough to break the test.
                    return
                }
                val oldId = pointerEvent.changes.fastMaxBy { it.id.value }!!.id.value.toInt()
                val event = PointerInputEvent(oldId + 1, 14, Offset.Zero, true)
                // force a reentrant call
                val result = pointerInputEventProcessor.process(event)
                assertThat(result.anyMovementConsumed).isFalse()
                assertThat(result.dispatchedToAPointerInputModifier).isFalse()
                pointerEvent.changes.forEach { it.consume() }
                reentrancyCount++
            }

            override fun onCancel() {
            }
        }

        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(reentrantPointerInputFilter)
        )

        addToRoot(layoutNode)

        // Act

        val result =
            pointerInputEventProcessor.process(PointerInputEvent(8712, 3, Offset.Zero, true))

        // Assert

        assertThat(reentrancyCount).isEqualTo(1)

        assertThat(result.anyMovementConsumed).isFalse()
        assertThat(result.dispatchedToAPointerInputModifier).isTrue()
    }

    @Test
    fun process_downMoveUp_convertedCorrectlyAndTraversesAllPassesInCorrectOrder() {

        // Arrange
        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val offset = Offset(100f, 200f)
        val offset2 = Offset(300f, 400f)

        val events = arrayOf(
            PointerInputEvent(8712, 3, offset, true),
            PointerInputEvent(8712, 11, offset2, true),
            PointerInputEvent(8712, 13, offset2, false)
        )

        val down = down(8712, 3, offset.x, offset.y)
        val move = down.moveTo(11, offset2.x, offset2.y)
        val up = move.up(13)

        val expectedChanges = arrayOf(down, move, up)

        // Act

        events.forEach { pointerInputEventProcessor.process(it) }

        // Assert

        val log = pointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log)
            .hasSize(PointerEventPass.values().size * expectedChanges.size)

        // Verify call values
        var count = 0
        expectedChanges.forEach { change ->
            PointerEventPass.values().forEach { pass ->
                val item = log[count]
                PointerEventSubject
                    .assertThat(item.pointerEvent)
                    .isStructurallyEqualTo(pointerEventOf(change))
                assertThat(item.pass).isEqualTo(pass)
                count++
            }
        }
    }

    @Test
    fun process_downHits_targetReceives() {

        // Arrange

        val childOffset = Offset(100f, 200f)
        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val offsets = arrayOf(
            Offset(100f, 200f),
            Offset(300f, 200f),
            Offset(100f, 400f),
            Offset(300f, 400f)
        )

        val events = Array(4) { index ->
            PointerInputEvent(index, 5, offsets[index], true)
        }

        val expectedChanges = Array(4) { index ->
            PointerInputChange(
                id = PointerId(index.toLong()),
                5,
                offsets[index] - childOffset,
                true,
                5,
                offsets[index] - childOffset,
                false,
                isInitiallyConsumed = false
            )
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        val log =
            pointerInputFilter
                .log
                .getOnPointerEventLog()
                .filter { it.pass == PointerEventPass.Initial }

        // Verify call count
        assertThat(log)
            .hasSize(expectedChanges.size)

        // Verify call values
        expectedChanges.forEachIndexed { index, change ->
            val item = log[index]
            PointerEventSubject
                .assertThat(item.pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(change))
        }
    }

    @Test
    fun process_downMisses_targetDoesNotReceive() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

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
            PointerInputEvent(index, 0, offsets[index], true)
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        assertThat(pointerInputFilter.log.getOnPointerEventLog()).hasSize(0)
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

        val log = mutableListOf<LogEntry>()
        val childPointerInputFilter = PointerInputFilterMock(log)
        val middlePointerInputFilter = PointerInputFilterMock(log)
        val parentPointerInputFilter = PointerInputFilterMock(log)

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
        addToRoot(parentLayoutNode)

        val offset = when (numberOfChildrenHit) {
            3 -> Offset(250f, 250f)
            2 -> Offset(150f, 150f)
            1 -> Offset(50f, 50f)
            else -> throw IllegalStateException()
        }

        val event = PointerInputEvent(0, 5, offset, true)

        // Act

        pointerInputEventProcessor.process(event)

        // Assert

        val filteredLog = log.getOnPointerEventLog().filter { it.pass == PointerEventPass.Initial }

        when (numberOfChildrenHit) {
            3 -> {
                assertThat(filteredLog).hasSize(3)
                assertThat(filteredLog[0].pointerInputFilter)
                    .isSameInstanceAs(parentPointerInputFilter)
                assertThat(filteredLog[1].pointerInputFilter)
                    .isSameInstanceAs(middlePointerInputFilter)
                assertThat(filteredLog[2].pointerInputFilter)
                    .isSameInstanceAs(childPointerInputFilter)
            }
            2 -> {
                assertThat(filteredLog).hasSize(2)
                assertThat(filteredLog[0].pointerInputFilter)
                    .isSameInstanceAs(parentPointerInputFilter)
                assertThat(filteredLog[1].pointerInputFilter)
                    .isSameInstanceAs(middlePointerInputFilter)
            }
            1 -> {
                assertThat(filteredLog).hasSize(1)
                assertThat(filteredLog[0].pointerInputFilter)
                    .isSameInstanceAs(parentPointerInputFilter)
            }
            else -> throw IllegalStateException()
        }
    }

    @Test
    fun process_modifiedChange_isPassedToNext() {

        // Arrange

        val expectedInput = PointerInputChange(
            id = PointerId(0),
            5,
            Offset(100f, 0f),
            true,
            3,
            Offset(0f, 0f),
            true,
            isInitiallyConsumed = false
        )
        val expectedOutput = PointerInputChange(
            id = PointerId(0),
            5,
            Offset(100f, 0f),
            true,
            3,
            Offset(0f, 0f),
            true,
            isInitiallyConsumed = true
        )

        val pointerInputFilter = PointerInputFilterMock(
            mutableListOf(),
            pointerEventHandler = { pointerEvent, pass, _ ->
                if (pass == PointerEventPass.Initial) {
                    val change = pointerEvent
                        .changes
                        .first()

                    if (change.positionChanged()) change.consume()
                }
            }
        )

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val down = PointerInputEvent(
            0,
            3,
            Offset(0f, 0f),
            true
        )
        val move = PointerInputEvent(
            0,
            5,
            Offset(100f, 0f),
            true
        )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputFilter.log.clear()
        pointerInputEventProcessor.process(move)

        // Assert

        val log = pointerInputFilter.log.getOnPointerEventLog()

        assertThat(log).hasSize(3)
        PointerInputChangeSubject
            .assertThat(log[0].pointerEvent.changes.first())
            .isStructurallyEqualTo(expectedInput)
        PointerInputChangeSubject
            .assertThat(log[1].pointerEvent.changes.first())
            .isStructurallyEqualTo(expectedOutput)
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

        val log = mutableListOf<LogEntry>()
        val childPointerInputFilter = PointerInputFilterMock(log)
        val middlePointerInputFilter = PointerInputFilterMock(log)
        val parentPointerInputFilter = PointerInputFilterMock(log)

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

        val outerLayoutNode = LayoutNode(
            aOX,
            aOY,
            aOX + parentLayoutNode.width,
            aOY + parentLayoutNode.height
        )

        outerLayoutNode.insertAt(0, parentLayoutNode)
        addToRoot(outerLayoutNode)

        val additionalOffset = IntOffset(aOX, aOY)

        val offset = Offset(pointerX.toFloat(), pointerY.toFloat())

        val down = PointerInputEvent(0, 7, offset, true)

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = PointerId(0),
                7,
                offset - additionalOffset,
                true,
                7,
                offset - additionalOffset,
                false,
                isInitiallyConsumed = false
            ),
            PointerInputChange(
                id = PointerId(0),
                7,
                offset - middleOffset - additionalOffset,
                true,
                7,
                offset - middleOffset - additionalOffset,
                false,
                isInitiallyConsumed = false
            ),
            PointerInputChange(
                id = PointerId(0),
                7,
                offset - middleOffset - childOffset - additionalOffset,
                true,
                7,
                offset - middleOffset - childOffset - additionalOffset,
                false,
                isInitiallyConsumed = false
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

        val filteredLog = log.getOnPointerEventLog()

        // Verify call count
        assertThat(filteredLog).hasSize(PointerEventPass.values().size * 3)

        // Verify call values
        filteredLog.verifyOnPointerEventCall(
            0,
            parentPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[0]),
            PointerEventPass.Initial,
            expectedSizes[0]
        )
        filteredLog.verifyOnPointerEventCall(
            1,
            middlePointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[1]),
            PointerEventPass.Initial,
            expectedSizes[1]
        )
        filteredLog.verifyOnPointerEventCall(
            2,
            childPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[2]),
            PointerEventPass.Initial,
            expectedSizes[2]
        )
        filteredLog.verifyOnPointerEventCall(
            3,
            childPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[2]),
            PointerEventPass.Main,
            expectedSizes[2]
        )
        filteredLog.verifyOnPointerEventCall(
            4,
            middlePointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[1]),
            PointerEventPass.Main,
            expectedSizes[1]
        )
        filteredLog.verifyOnPointerEventCall(
            5,
            parentPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[0]),
            PointerEventPass.Main,
            expectedSizes[0]
        )
        filteredLog.verifyOnPointerEventCall(
            6,
            parentPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[0]),
            PointerEventPass.Final,
            expectedSizes[0]
        )
        filteredLog.verifyOnPointerEventCall(
            7,
            middlePointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[1]),
            PointerEventPass.Final,
            expectedSizes[1]
        )
        filteredLog.verifyOnPointerEventCall(
            8,
            childPointerInputFilter,
            pointerEventOf(expectedPointerInputChanges[2]),
            PointerEventPass.Final,
            expectedSizes[2]
        )
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

        val log = mutableListOf<LogEntry>()
        val childPointerInputFilter1 = PointerInputFilterMock(log)
        val childPointerInputFilter2 = PointerInputFilterMock(log)

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
        addToRoot(childLayoutNode1, childLayoutNode2)

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)

        val down = PointerInputEvent(
            5,
            listOf(
                PointerInputEventData(0, 5, offset1, true),
                PointerInputEventData(1, 5, offset2, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                5,
                offset1,
                true,
                5,
                offset1,
                false,
                isInitiallyConsumed = false
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                5,
                offset2 - Offset(50f, 50f),
                true,
                5,
                offset2 - Offset(50f, 50f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        // Verify call count

        val child1Log =
            log.getOnPointerEventLog().filter { it.pointerInputFilter === childPointerInputFilter1 }
        val child2Log =
            log.getOnPointerEventLog().filter { it.pointerInputFilter === childPointerInputFilter2 }
        assertThat(child1Log).hasSize(PointerEventPass.values().size)
        assertThat(child2Log).hasSize(PointerEventPass.values().size)

        // Verify call values

        val expectedBounds = IntSize(50, 50)

        child1Log.verifyOnPointerEventCall(
            0,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Initial,
            expectedBounds
        )
        child1Log.verifyOnPointerEventCall(
            1,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Main,
            expectedBounds
        )
        child1Log.verifyOnPointerEventCall(
            2,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Final,
            expectedBounds
        )

        child2Log.verifyOnPointerEventCall(
            0,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Initial,
            expectedBounds
        )
        child2Log.verifyOnPointerEventCall(
            1,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Main,
            expectedBounds
        )
        child2Log.verifyOnPointerEventCall(
            2,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Final,
            expectedBounds
        )
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

        val log = mutableListOf<LogEntry>()
        val childPointerInputFilter1 = PointerInputFilterMock(log)
        val childPointerInputFilter2 = PointerInputFilterMock(log)
        val childPointerInputFilter3 = PointerInputFilterMock(log)

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

        addToRoot(childLayoutNode1, childLayoutNode2, childLayoutNode3)

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)
        val offset3 = Offset(125f, 125f)

        val down = PointerInputEvent(
            5,
            listOf(
                PointerInputEventData(0, 5, offset1, true),
                PointerInputEventData(1, 5, offset2, true),
                PointerInputEventData(2, 5, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                5,
                offset1,
                true,
                5,
                offset1,
                false,
                isInitiallyConsumed = false
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                5,
                offset2 - Offset(50f, 50f),
                true,
                5,
                offset2 - Offset(50f, 50f),
                false,
                isInitiallyConsumed = false
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                5,
                offset3 - Offset(100f, 100f),
                true,
                5,
                offset3 - Offset(100f, 100f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        val child1Log =
            log.getOnPointerEventLog().filter { it.pointerInputFilter === childPointerInputFilter1 }
        val child2Log =
            log.getOnPointerEventLog().filter { it.pointerInputFilter === childPointerInputFilter2 }
        val child3Log =
            log.getOnPointerEventLog().filter { it.pointerInputFilter === childPointerInputFilter3 }
        assertThat(child1Log).hasSize(PointerEventPass.values().size)
        assertThat(child2Log).hasSize(PointerEventPass.values().size)
        assertThat(child3Log).hasSize(PointerEventPass.values().size)

        // Verify call values

        val expectedBounds = IntSize(100, 100)

        child1Log.verifyOnPointerEventCall(
            0,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Initial,
            expectedBounds
        )
        child1Log.verifyOnPointerEventCall(
            1,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Main,
            expectedBounds
        )
        child1Log.verifyOnPointerEventCall(
            2,
            null,
            pointerEventOf(expectedChange1),
            PointerEventPass.Final,
            expectedBounds
        )

        child2Log.verifyOnPointerEventCall(
            0,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Initial,
            expectedBounds
        )
        child2Log.verifyOnPointerEventCall(
            1,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Main,
            expectedBounds
        )
        child2Log.verifyOnPointerEventCall(
            2,
            null,
            pointerEventOf(expectedChange2),
            PointerEventPass.Final,
            expectedBounds
        )

        child3Log.verifyOnPointerEventCall(
            0,
            null,
            pointerEventOf(expectedChange3),
            PointerEventPass.Initial,
            expectedBounds
        )
        child3Log.verifyOnPointerEventCall(
            1,
            null,
            pointerEventOf(expectedChange3),
            PointerEventPass.Main,
            expectedBounds
        )
        child3Log.verifyOnPointerEventCall(
            2,
            null,
            pointerEventOf(expectedChange3),
            PointerEventPass.Final,
            expectedBounds
        )
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

        val childPointerInputFilter1 = PointerInputFilterMock()
        val childPointerInputFilter2 = PointerInputFilterMock()

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

        addToRoot(childLayoutNode1, childLayoutNode2)

        val offset1 = Offset(50f, 25f)
        val offset2 = Offset(50f, 75f)
        val offset3 = Offset(50f, 125f)

        val down = PointerInputEvent(
            7,
            listOf(
                PointerInputEventData(0, 7, offset1, true),
                PointerInputEventData(1, 7, offset2, true),
                PointerInputEventData(2, 7, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                7,
                offset1,
                true,
                7,
                offset1,
                false,
                isInitiallyConsumed = false
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                7,
                offset2 - Offset(25f, 50f),
                true,
                7,
                offset2 - Offset(25f, 50f),
                false,
                isInitiallyConsumed = false
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                7,
                offset3,
                true,
                7,
                offset3,
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        val log1 = childPointerInputFilter1.log.getOnPointerEventLog()
        val log2 = childPointerInputFilter2.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log1).hasSize(PointerEventPass.values().size)
        assertThat(log2).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log1.verifyOnPointerEventCall(
                index,
                null,
                pointerEventOf(expectedChange1, expectedChange3),
                pass,
                IntSize(100, 150)
            )
            log2.verifyOnPointerEventCall(
                index,
                null,
                pointerEventOf(expectedChange2),
                pass,
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

        val childPointerInputFilter1 = PointerInputFilterMock()
        val childPointerInputFilter2 = PointerInputFilterMock()

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

        addToRoot(childLayoutNode1, childLayoutNode2)

        val offset1 = Offset(25f, 50f)
        val offset2 = Offset(75f, 50f)
        val offset3 = Offset(125f, 50f)

        val down = PointerInputEvent(
            11,
            listOf(
                PointerInputEventData(0, 11, offset1, true),
                PointerInputEventData(1, 11, offset2, true),
                PointerInputEventData(2, 11, offset3, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                11,
                offset1,
                true,
                11,
                offset1,
                false,
                isInitiallyConsumed = false
            )
        val expectedChange2 =
            PointerInputChange(
                id = PointerId(1),
                11,
                offset2 - Offset(50f, 25f),
                true,
                11,
                offset2 - Offset(50f, 25f),
                false,
                isInitiallyConsumed = false
            )
        val expectedChange3 =
            PointerInputChange(
                id = PointerId(2),
                11,
                offset3,
                true,
                11,
                offset3,
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        val log1 = childPointerInputFilter1.log.getOnPointerEventLog()
        val log2 = childPointerInputFilter2.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log1).hasSize(PointerEventPass.values().size)
        assertThat(log2).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log1.verifyOnPointerEventCall(
                index,
                null,
                pointerEventOf(expectedChange1, expectedChange3),
                pass,
                IntSize(150, 100)
            )
            log2.verifyOnPointerEventCall(
                index,
                null,
                pointerEventOf(expectedChange2),
                pass,
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

        val pointerInputFilterTopLeft = PointerInputFilterMock()
        val pointerInputFilterTopRight = PointerInputFilterMock()
        val pointerInputFilterBottomLeft = PointerInputFilterMock()
        val pointerInputFilterBottomRight = PointerInputFilterMock()

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
        addToRoot(parentLayoutNode)

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
                11,
                (allOffsets.indices).map {
                    PointerInputEventData(it, 11, allOffsets[it], true)
                }
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChangesTopLeft =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    11,
                    Offset(
                        offsetsTopLeft[it].x,
                        offsetsTopLeft[it].y
                    ),
                    true,
                    11,
                    Offset(
                        offsetsTopLeft[it].x,
                        offsetsTopLeft[it].y
                    ),
                    false,
                    isInitiallyConsumed = false
                )
            }

        val expectedChangesTopRight =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 3),
                    11,
                    Offset(
                        offsetsTopRight[it].x - 3f,
                        offsetsTopRight[it].y
                    ),
                    true,
                    11,
                    Offset(
                        offsetsTopRight[it].x - 3f,
                        offsetsTopRight[it].y
                    ),
                    false,
                    isInitiallyConsumed = false
                )
            }

        val expectedChangesBottomLeft =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 6),
                    11,
                    Offset(
                        offsetsBottomLeft[it].x,
                        offsetsBottomLeft[it].y - 3f
                    ),
                    true,
                    11,
                    Offset(
                        offsetsBottomLeft[it].x,
                        offsetsBottomLeft[it].y - 3f
                    ),
                    false,
                    isInitiallyConsumed = false
                )
            }

        val expectedChangesBottomRight =
            (offsetsTopLeft.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong() + 9),
                    11,
                    Offset(
                        offsetsBottomRight[it].x - 3f,
                        offsetsBottomRight[it].y - 3f
                    ),
                    true,
                    11,
                    Offset(
                        offsetsBottomRight[it].x - 3f,
                        offsetsBottomRight[it].y - 3f
                    ),
                    false,
                    isInitiallyConsumed = false
                )
            }

        // Verify call values

        val logTopLeft = pointerInputFilterTopLeft.log.getOnPointerEventLog()
        val logTopRight = pointerInputFilterTopRight.log.getOnPointerEventLog()
        val logBottomLeft = pointerInputFilterBottomLeft.log.getOnPointerEventLog()
        val logBottomRight = pointerInputFilterBottomRight.log.getOnPointerEventLog()

        PointerEventPass.values().forEachIndexed { index, pass ->
            logTopLeft.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChangesTopLeft.toTypedArray()),
                expectedPass = pass
            )
            logTopRight.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChangesTopRight.toTypedArray()),
                expectedPass = pass
            )
            logBottomLeft.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChangesBottomLeft.toTypedArray()),
                expectedPass = pass
            )
            logBottomRight.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChangesBottomRight.toTypedArray()),
                expectedPass = pass
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
        val singlePointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 2, 2,
            PointerInputModifierImpl2(
                singlePointerInputFilter
            )
        )
        val outerLayoutNode = LayoutNode(1, 1, 3, 3)
        outerLayoutNode.insertAt(0, layoutNode)
        addToRoot(outerLayoutNode)
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
                11,
                (allOffsets.indices).map {
                    PointerInputEventData(it, 11, allOffsets[it], true)
                }
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    11,
                    offsetsThatHit[it] - Offset(1f, 1f),
                    true,
                    11,
                    offsetsThatHit[it] - Offset(1f, 1f),
                    false,
                    isInitiallyConsumed = false
                )
            }

        val log = singlePointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChanges.toTypedArray()),
                expectedPass = pass
            )
        }
    }

    @Test
    fun process_downOn3NestedPointerInputModifiers_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter1 = PointerInputFilterMock()
        val pointerInputFilter2 = PointerInputFilterMock()
        val pointerInputFilter3 = PointerInputFilterMock()

        val modifier = PointerInputModifierImpl2(pointerInputFilter1) then
            PointerInputModifierImpl2(pointerInputFilter2) then
            PointerInputModifierImpl2(pointerInputFilter3)

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        )

        addToRoot(layoutNode)

        val offset1 = Offset(50f, 75f)

        val down = PointerInputEvent(
            7,
            listOf(
                PointerInputEventData(0, 7, offset1, true)
            )
        )

        val expectedChange =
            PointerInputChange(
                id = PointerId(0),
                7,
                offset1 - Offset(25f, 50f),
                true,
                7,
                offset1 - Offset(25f, 50f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        val log1 = pointerInputFilter1.log.getOnPointerEventLog()
        val log2 = pointerInputFilter2.log.getOnPointerEventLog()
        val log3 = pointerInputFilter3.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log1).hasSize(PointerEventPass.values().size)
        assertThat(log2).hasSize(PointerEventPass.values().size)
        assertThat(log3).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log1.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange),
                expectedPass = pass,
                expectedBounds = IntSize(50, 50)
            )
            log2.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange),
                expectedPass = pass,
                expectedBounds = IntSize(50, 50)
            )
            log3.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange),
                expectedPass = pass,
                expectedBounds = IntSize(50, 50)
            )
        }
    }

    @Test
    fun process_downOnDeeplyNestedPointerInputModifier_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter = PointerInputFilterMock()

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
        addToRoot(layoutNode4)

        val offset1 = Offset(499f, 499f)

        val downEvent = PointerInputEvent(
            7,
            listOf(
                PointerInputEventData(0, 7, offset1, true)
            )
        )

        val expectedChange =
            PointerInputChange(
                id = PointerId(0),
                7,
                offset1 - Offset(1f + 2f + 3f + 4f, 5f + 6f + 7f + 8f),
                true,
                7,
                offset1 - Offset(1f + 2f + 3f + 4f, 5f + 6f + 7f + 8f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        val log = pointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange),
                expectedPass = pass,
                expectedBounds = IntSize(499, 495)
            )
        }
    }

    @Test
    fun process_downOnComplexPointerAndLayoutNodePath_hitAndDispatchInfoAreCorrect() {

        val pointerInputFilter1 = PointerInputFilterMock()
        val pointerInputFilter2 = PointerInputFilterMock()
        val pointerInputFilter3 = PointerInputFilterMock()
        val pointerInputFilter4 = PointerInputFilterMock()

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
        addToRoot(layoutNode5)

        val offset1 = Offset(499f, 499f)

        val downEvent = PointerInputEvent(
            3,
            listOf(
                PointerInputEventData(0, 3, offset1, true)
            )
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(0),
                3,
                offset1 - Offset(
                    1f + 2f + 3f + 4f + 5f,
                    6f + 7f + 8f + 9f + 10f
                ),
                true,
                3,
                offset1 - Offset(
                    1f + 2f + 3f + 4f + 5f,
                    6f + 7f + 8f + 9f + 10f
                ),
                false,
                isInitiallyConsumed = false
            )

        val expectedChange2 =
            PointerInputChange(
                id = PointerId(0),
                3,
                offset1 - Offset(3f + 4f + 5f, 8f + 9f + 10f),
                true,
                3,
                offset1 - Offset(3f + 4f + 5f, 8f + 9f + 10f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        val log1 = pointerInputFilter1.log.getOnPointerEventLog()
        val log2 = pointerInputFilter2.log.getOnPointerEventLog()
        val log3 = pointerInputFilter3.log.getOnPointerEventLog()
        val log4 = pointerInputFilter4.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log1).hasSize(PointerEventPass.values().size)
        assertThat(log2).hasSize(PointerEventPass.values().size)
        assertThat(log3).hasSize(PointerEventPass.values().size)
        assertThat(log4).hasSize(PointerEventPass.values().size)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log1.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange1),
                expectedPass = pass,
                expectedBounds = IntSize(499, 494)
            )
            log2.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange1),
                expectedPass = pass,
                expectedBounds = IntSize(499, 494)
            )
            log3.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange2),
                expectedPass = pass,
                expectedBounds = IntSize(497, 492)
            )
            log4.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange2),
                expectedPass = pass,
                expectedBounds = IntSize(497, 492)
            )
        }
    }

    @Test
    fun process_downOnFullyOverlappingPointerInputModifiers_onlyTopPointerInputModifierReceives() {

        val pointerInputFilter1 = PointerInputFilterMock()
        val pointerInputFilter2 = PointerInputFilterMock()

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

        addToRoot(layoutNode1, layoutNode2)

        val down = PointerInputEvent(
            1, 0, Offset(50f, 50f), true
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert
        assertThat(pointerInputFilter2.log.getOnPointerEventLog()).hasSize(3)
        assertThat(pointerInputFilter1.log.getOnPointerEventLog()).hasSize(0)
    }

    @Test
    fun process_downOnPointerInputModifierInLayoutNodeWithNoSize_downNotReceived() {

        val pointerInputFilter1 = PointerInputFilterMock()

        val layoutNode1 = LayoutNode(
            0, 0, 0, 0,
            PointerInputModifierImpl2(pointerInputFilter1)
        )

        addToRoot(layoutNode1)

        val down = PointerInputEvent(
            1, 0, Offset(0f, 0f), true
        )

        // Act
        pointerInputEventProcessor.process(down)

        // Assert
        assertThat(pointerInputFilter1.log.getOnPointerEventLog()).hasSize(0)
    }

    // Cancel Handlers

    @Test
    fun processCancel_noPointers_doesntCrash() {
        pointerInputEventProcessor.processCancel()
    }

    @Test
    fun processCancel_downThenCancel_pimOnlyReceivesCorrectDownThenCancel() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        addToRoot(layoutNode)

        val pointerInputEvent =
            PointerInputEvent(
                7,
                5,
                Offset(250f, 250f),
                true
            )

        val expectedChange =
            PointerInputChange(
                id = PointerId(7),
                5,
                Offset(250f, 250f),
                true,
                5,
                Offset(250f, 250f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        val log = pointerInputFilter.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size + 1)

        // Verify call values
        PointerEventPass.values().forEachIndexed { index, pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange),
                expectedPass = pass
            )
        }
        log.verifyOnCancelCall(PointerEventPass.values().size)
    }

    @Test
    fun processCancel_downDownOnSamePimThenCancel_pimOnlyReceivesCorrectChangesThenCancel() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val pointerInputEvent1 =
            PointerInputEvent(
                7,
                5,
                Offset(200f, 200f),
                true
            )

        val pointerInputEvent2 =
            PointerInputEvent(
                10,
                listOf(
                    PointerInputEventData(
                        7,
                        10,
                        Offset(200f, 200f),
                        true
                    ),
                    PointerInputEventData(
                        9,
                        10,
                        Offset(300f, 300f),
                        true
                    )
                )
            )

        val expectedChanges1 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    5,
                    Offset(200f, 200f),
                    true,
                    5,
                    Offset(200f, 200f),
                    false,
                    isInitiallyConsumed = false
                )
            )

        val expectedChanges2 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    10,
                    Offset(200f, 200f),
                    true,
                    5,
                    Offset(200f, 200f),
                    true,
                    isInitiallyConsumed = false
                ),
                PointerInputChange(
                    id = PointerId(9),
                    10,
                    Offset(300f, 300f),
                    true,
                    10,
                    Offset(300f, 300f),
                    false,
                    isInitiallyConsumed = false
                )
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent1)
        pointerInputEventProcessor.process(pointerInputEvent2)
        pointerInputEventProcessor.processCancel()

        // Assert

        val log = pointerInputFilter.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size * 2 + 1)

        // Verify call values
        var index = 0
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChanges1.toTypedArray()),
                expectedPass = pass
            )
            index++
        }
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(*expectedChanges2.toTypedArray()),
                expectedPass = pass
            )
            index++
        }
        log.verifyOnCancelCall(index)
    }

    @Test
    fun processCancel_downOn2DifferentPimsThenCancel_pimsOnlyReceiveCorrectDownsThenCancel() {

        // Arrange

        val pointerInputFilter1 = PointerInputFilterMock()
        val layoutNode1 = LayoutNode(
            0, 0, 199, 199,
            PointerInputModifierImpl2(pointerInputFilter1)
        )

        val pointerInputFilter2 = PointerInputFilterMock()
        val layoutNode2 = LayoutNode(
            200, 200, 399, 399,
            PointerInputModifierImpl2(pointerInputFilter2)
        )

        addToRoot(layoutNode1, layoutNode2)

        val pointerInputEventData1 =
            PointerInputEventData(
                7,
                5,
                Offset(100f, 100f),
                true
            )

        val pointerInputEventData2 =
            PointerInputEventData(
                9,
                5,
                Offset(300f, 300f),
                true
            )

        val pointerInputEvent = PointerInputEvent(
            5,
            listOf(pointerInputEventData1, pointerInputEventData2)
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(7),
                5,
                Offset(100f, 100f),
                true,
                5,
                Offset(100f, 100f),
                false,
                isInitiallyConsumed = false
            )

        val expectedChange2 =
            PointerInputChange(
                id = PointerId(9),
                5,
                Offset(100f, 100f),
                true,
                5,
                Offset(100f, 100f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        val log1 =
            pointerInputFilter1.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }
        val log2 =
            pointerInputFilter2.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log1).hasSize(PointerEventPass.values().size + 1)
        assertThat(log2).hasSize(PointerEventPass.values().size + 1)

        // Verify call values
        var index = 0
        PointerEventPass.values().forEach { pass ->
            log1.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange1),
                expectedPass = pass
            )
            log2.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedChange2),
                expectedPass = pass
            )
            index++
        }
        log1.verifyOnCancelCall(index)
        log2.verifyOnCancelCall(index)
    }

    @Test
    fun processCancel_downMoveCancel_pimOnlyReceivesCorrectDownMoveCancel() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        addToRoot(layoutNode)

        val down =
            PointerInputEvent(
                7,
                5,
                Offset(200f, 200f),
                true
            )

        val move =
            PointerInputEvent(
                7,
                10,
                Offset(300f, 300f),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                5,
                Offset(200f, 200f),
                true,
                5,
                Offset(200f, 200f),
                false,
                isInitiallyConsumed = false
            )

        val expectedMove =
            PointerInputChange(
                id = PointerId(7),
                10,
                Offset(300f, 300f),
                true,
                5,
                Offset(200f, 200f),
                true,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.process(move)
        pointerInputEventProcessor.processCancel()

        // Assert

        val log = pointerInputFilter.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size * 2 + 1)

        // Verify call values
        var index = 0
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedDown),
                expectedPass = pass
            )
            index++
        }
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedMove),
                expectedPass = pass
            )
            index++
        }
        log.verifyOnCancelCall(index)
    }

    @Test
    fun processCancel_downCancelMoveUp_pimOnlyReceivesCorrectDownCancel() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(pointerInputFilter)
        )

        addToRoot(layoutNode)

        val down =
            PointerInputEvent(
                7,
                5,
                Offset(200f, 200f),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                5,
                Offset(200f, 200f),
                true,
                5,
                Offset(200f, 200f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.processCancel()

        // Assert

        val log = pointerInputFilter.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size + 1)

        // Verify call values
        var index = 0
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedDown),
                expectedPass = pass
            )
            index++
        }
        log.verifyOnCancelCall(index)
    }

    @Test
    fun processCancel_downCancelDown_pimOnlyReceivesCorrectDownCancelDown() {

        // Arrange

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val down1 =
            PointerInputEvent(
                7,
                5,
                Offset(200f, 200f),
                true
            )

        val down2 =
            PointerInputEvent(
                7,
                10,
                Offset(200f, 200f),
                true
            )

        val expectedDown1 =
            PointerInputChange(
                id = PointerId(7),
                5,
                Offset(200f, 200f),
                true,
                5,
                Offset(200f, 200f),
                false,
                isInitiallyConsumed = false
            )

        val expectedDown2 =
            PointerInputChange(
                id = PointerId(7),
                10,
                Offset(200f, 200f),
                true,
                10,
                Offset(200f, 200f),
                false,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down1)
        pointerInputEventProcessor.processCancel()
        pointerInputEventProcessor.process(down2)

        // Assert

        val log = pointerInputFilter.log.filter { it is OnPointerEventEntry || it is OnCancelEntry }

        // Verify call count
        assertThat(log).hasSize(PointerEventPass.values().size * 2 + 1)

        // Verify call values
        var index = 0
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedDown1),
                expectedPass = pass
            )
            index++
        }
        log.verifyOnCancelCall(index)
        index++
        PointerEventPass.values().forEach { pass ->
            log.verifyOnPointerEventCall(
                index = index,
                expectedEvent = pointerEventOf(expectedDown2),
                expectedPass = pass
            )
            index++
        }
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputFilter = PointerInputFilterMock()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = PointerInputFilterMock()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        addToRoot(parentLayoutNode)

        val offset = Offset(50f, 50f)

        val down = PointerInputEvent(0, 7, offset, true)
        val up = PointerInputEvent(0, 11, offset, false)

        val expectedDownChange =
            PointerInputChange(
                id = PointerId(0),
                7,
                offset,
                true,
                7,
                offset,
                false,
                isInitiallyConsumed = false
            )

        val expectedUpChange =
            PointerInputChange(
                id = PointerId(0),
                11,
                offset,
                false,
                7,
                offset,
                true,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert

        val parentLog = parentPointerInputFilter.log.getOnPointerEventLog()
        val childLog = childPointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(parentLog).hasSize(PointerEventPass.values().size * 2)
        assertThat(childLog).hasSize(PointerEventPass.values().size)

        // Verify call values

        parentLog.verifyOnPointerEventCall(
            index = 0,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Initial
        )
        parentLog.verifyOnPointerEventCall(
            index = 1,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Main
        )
        parentLog.verifyOnPointerEventCall(
            index = 2,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Final
        )
        parentLog.verifyOnPointerEventCall(
            index = 3,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Initial
        )
        parentLog.verifyOnPointerEventCall(
            index = 4,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Main
        )
        parentLog.verifyOnPointerEventCall(
            index = 5,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Final
        )

        childLog.verifyOnPointerEventCall(
            index = 0,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Initial
        )
        childLog.verifyOnPointerEventCall(
            index = 1,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Main
        )
        childLog.verifyOnPointerEventCall(
            index = 2,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Final
        )
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_cancelDispatchedToCorrectPointerInputModifierImpl2() {

        // Arrange

        val childPointerInputFilter = PointerInputFilterMock()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = PointerInputFilterMock()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        addToRoot(parentLayoutNode)

        val down =
            PointerInputEvent(0, 7, Offset(50f, 50f), true)

        val up = PointerInputEvent(0, 11, Offset(50f, 50f), false)

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert
        assertThat(childPointerInputFilter.log.getOnCancelLog()).hasSize(1)
        assertThat(parentPointerInputFilter.log.getOnCancelLog()).hasSize(0)
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputFilter = PointerInputFilterMock()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                childPointerInputFilter
            )
        )

        val parentPointerInputFilter = PointerInputFilterMock()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(
                parentPointerInputFilter
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        addToRoot(parentLayoutNode)

        val offset = Offset(50f, 50f)

        val down = PointerInputEvent(0, 7, offset, true)
        val up = PointerInputEvent(0, 11, offset, false)

        val expectedDownChange =
            PointerInputChange(
                id = PointerId(0),
                7,
                offset,
                true,
                7,
                offset,
                false,
                isInitiallyConsumed = false
            )

        val expectedUpChange =
            PointerInputChange(
                id = PointerId(0),
                11,
                offset,
                false,
                7,
                offset,
                true,
                isInitiallyConsumed = false
            )

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier
        pointerInputEventProcessor.process(up)

        // Assert

        val parentLog = parentPointerInputFilter.log.getOnPointerEventLog()
        val childLog = childPointerInputFilter.log.getOnPointerEventLog()

        // Verify call count
        assertThat(parentLog).hasSize(PointerEventPass.values().size * 2)
        assertThat(childLog).hasSize(PointerEventPass.values().size)

        // Verify call values

        parentLog.verifyOnPointerEventCall(
            index = 0,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Initial
        )
        parentLog.verifyOnPointerEventCall(
            index = 1,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Main
        )
        parentLog.verifyOnPointerEventCall(
            index = 2,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Final
        )
        parentLog.verifyOnPointerEventCall(
            index = 3,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Initial
        )
        parentLog.verifyOnPointerEventCall(
            index = 4,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Main
        )
        parentLog.verifyOnPointerEventCall(
            index = 5,
            expectedEvent = pointerEventOf(expectedUpChange),
            expectedPass = PointerEventPass.Final
        )

        childLog.verifyOnPointerEventCall(
            index = 0,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Initial
        )
        childLog.verifyOnPointerEventCall(
            index = 1,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Main
        )
        childLog.verifyOnPointerEventCall(
            index = 2,
            expectedEvent = pointerEventOf(expectedDownChange),
            expectedPass = PointerEventPass.Final
        )
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_cancelDispatchedToCorrectPim() {

        // Arrange

        val childPointerInputFilter = PointerInputFilterMock()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(childPointerInputFilter)
        )

        val parentPointerInputFilter = PointerInputFilterMock()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl2(parentPointerInputFilter)
        ).apply {
            insertAt(0, childLayoutNode)
        }

        addToRoot(parentLayoutNode)

        val down =
            PointerInputEvent(0, 7, Offset(50f, 50f), true)

        val up =
            PointerInputEvent(0, 11, Offset(50f, 50f), false)

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier
        pointerInputEventProcessor.process(up)

        // Assert
        assertThat(childPointerInputFilter.log.getOnCancelLog()).hasSize(1)
        assertThat(parentPointerInputFilter.log.getOnCancelLog()).hasSize(0)
    }

    @Test
    fun process_downNoPointerInputModifiers_nothingInteractedWithAndNoMovementConsumed() {
        val pointerInputEvent =
            PointerInputEvent(0, 7, Offset(0f, 0f), true)

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

        val pointerInputFilter = PointerInputFilterMock()

        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )

        addToRoot(layoutNode)

        val offsets =
            listOf(
                Offset(-1f, 0f),
                Offset(0f, -1f),
                Offset(1f, 0f),
                Offset(0f, 1f)
            )
        val pointerInputEvent =
            PointerInputEvent(
                11,
                (offsets.indices).map {
                    PointerInputEventData(it, 11, offsets[it], true)
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

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)
        val pointerInputEvent =
            PointerInputEvent(0, 11, Offset(0f, 0f), true)

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

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)
        val down = PointerInputEvent(0, 11, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, 11, Offset(1f, 0f), true)

        // Act

        testOwner.root.removeAt(0, 1)
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

        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)
        val down = PointerInputEvent(0, 11, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, 11, Offset(1f, 0f), true)

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
            PointerInputFilterMock(
                pointerEventHandler = { pointerEvent, pass, _ ->
                    if (pass == PointerEventPass.Initial) {
                        pointerEvent.changes.forEach {
                            if (it.positionChange() != Offset.Zero) it.consume()
                        }
                    }
                }
            )

        val layoutNode = LayoutNode(
            0, 0, 1, 1,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)
        val down = PointerInputEvent(0, 11, Offset(0f, 0f), true)
        pointerInputEventProcessor.process(down)
        val move = PointerInputEvent(0, 11, Offset(1f, 0f), true)

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

    @Test
    fun buttonsPressed() {
        // Arrange
        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)

        class ButtonValidation(
            vararg pressedValues: Int,
            val primary: Boolean = false,
            val secondary: Boolean = false,
            val tertiary: Boolean = false,
            val back: Boolean = false,
            val forward: Boolean = false,
            val anyPressed: Boolean = true,
        ) {
            val pressedValues = pressedValues
        }

        val buttonCheckerMap = mapOf(
            MotionEvent.BUTTON_PRIMARY to ButtonValidation(0, primary = true),
            MotionEvent.BUTTON_SECONDARY to ButtonValidation(1, secondary = true),
            MotionEvent.BUTTON_TERTIARY to ButtonValidation(2, tertiary = true),
            MotionEvent.BUTTON_STYLUS_PRIMARY to ButtonValidation(0, primary = true),
            MotionEvent.BUTTON_STYLUS_SECONDARY to ButtonValidation(1, secondary = true),
            MotionEvent.BUTTON_BACK to ButtonValidation(3, back = true),
            MotionEvent.BUTTON_FORWARD to ButtonValidation(4, forward = true),
            MotionEvent.BUTTON_PRIMARY or MotionEvent.BUTTON_TERTIARY to
                ButtonValidation(0, 2, primary = true, tertiary = true),
            MotionEvent.BUTTON_BACK or MotionEvent.BUTTON_STYLUS_PRIMARY to
                ButtonValidation(0, 3, primary = true, back = true),
            0 to ButtonValidation(anyPressed = false)
        )

        for (entry in buttonCheckerMap) {
            val buttonState = entry.key
            val validator = entry.value
            val event = PointerInputEvent(
                0,
                listOf(PointerInputEventData(0, 0L, Offset.Zero, true)),
                MotionEvent.obtain(
                    0L,
                    0L,
                    MotionEvent.ACTION_DOWN,
                    1,
                    arrayOf(PointerProperties(1, MotionEvent.TOOL_TYPE_MOUSE)),
                    arrayOf(PointerCoords(0f, 0f)),
                    0,
                    buttonState,
                    0.1f,
                    0.1f,
                    0,
                    0,
                    InputDevice.SOURCE_MOUSE,
                    0
                )
            )
            pointerInputEventProcessor.process(event)

            with((pointerInputFilter.log.last() as OnPointerEventEntry).pointerEvent.buttons) {
                assertThat(isPrimaryPressed).isEqualTo(validator.primary)
                assertThat(isSecondaryPressed).isEqualTo(validator.secondary)
                assertThat(isTertiaryPressed).isEqualTo(validator.tertiary)
                assertThat(isBackPressed).isEqualTo(validator.back)
                assertThat(isForwardPressed).isEqualTo(validator.forward)
                assertThat(areAnyPressed).isEqualTo(validator.anyPressed)
                val firstIndex = validator.pressedValues.firstOrNull() ?: -1
                val lastIndex = validator.pressedValues.lastOrNull() ?: -1
                assertThat(indexOfFirstPressed()).isEqualTo(firstIndex)
                assertThat(indexOfLastPressed()).isEqualTo(lastIndex)
                for (i in 0..10) {
                    assertThat(isPressed(i)).isEqualTo(validator.pressedValues.contains(i))
                }
            }
        }
    }

    @Test
    fun metaState() {
        // Arrange
        val pointerInputFilter = PointerInputFilterMock()
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifierImpl2(
                pointerInputFilter
            )
        )
        addToRoot(layoutNode)

        class MetaValidation(
            val control: Boolean = false,
            val meta: Boolean = false,
            val alt: Boolean = false,
            val shift: Boolean = false,
            val sym: Boolean = false,
            val function: Boolean = false,
            val capsLock: Boolean = false,
            val scrollLock: Boolean = false,
            val numLock: Boolean = false
        )

        val buttonCheckerMap = mapOf(
            AndroidKeyEvent.META_CTRL_ON to MetaValidation(control = true),
            AndroidKeyEvent.META_META_ON to MetaValidation(meta = true),
            AndroidKeyEvent.META_ALT_ON to MetaValidation(alt = true),
            AndroidKeyEvent.META_SYM_ON to MetaValidation(sym = true),
            AndroidKeyEvent.META_SHIFT_ON to MetaValidation(shift = true),
            AndroidKeyEvent.META_FUNCTION_ON to MetaValidation(function = true),
            AndroidKeyEvent.META_CAPS_LOCK_ON to MetaValidation(capsLock = true),
            AndroidKeyEvent.META_SCROLL_LOCK_ON to MetaValidation(scrollLock = true),
            AndroidKeyEvent.META_NUM_LOCK_ON to MetaValidation(numLock = true),
            AndroidKeyEvent.META_CTRL_ON or AndroidKeyEvent.META_SHIFT_ON or
                AndroidKeyEvent.META_NUM_LOCK_ON to
                MetaValidation(control = true, shift = true, numLock = true),
            0 to MetaValidation(),
        )

        for (entry in buttonCheckerMap) {
            val metaState = entry.key
            val validator = entry.value
            val event = PointerInputEvent(
                0,
                listOf(PointerInputEventData(0, 0L, Offset.Zero, true)),
                MotionEvent.obtain(
                    0L,
                    0L,
                    MotionEvent.ACTION_DOWN,
                    1,
                    arrayOf(PointerProperties(1, MotionEvent.TOOL_TYPE_MOUSE)),
                    arrayOf(PointerCoords(0f, 0f)),
                    metaState,
                    0,
                    0.1f,
                    0.1f,
                    0,
                    0,
                    InputDevice.SOURCE_MOUSE,
                    0
                )
            )
            pointerInputEventProcessor.process(event)

            val keyboardModifiers = (pointerInputFilter.log.last() as OnPointerEventEntry)
                .pointerEvent.keyboardModifiers
            with(keyboardModifiers) {
                assertThat(isCtrlPressed).isEqualTo(validator.control)
                assertThat(isMetaPressed).isEqualTo(validator.meta)
                assertThat(isAltPressed).isEqualTo(validator.alt)
                assertThat(isAltGraphPressed).isFalse()
                assertThat(isSymPressed).isEqualTo(validator.sym)
                assertThat(isShiftPressed).isEqualTo(validator.shift)
                assertThat(isFunctionPressed).isEqualTo(validator.function)
                assertThat(isCapsLockOn).isEqualTo(validator.capsLock)
                assertThat(isScrollLockOn).isEqualTo(validator.scrollLock)
                assertThat(isNumLockOn).isEqualTo(validator.numLock)
            }
        }
    }

    private fun PointerInputEventProcessor.process(event: PointerInputEvent) =
        process(event, positionCalculator)
}

private class PointerInputModifierImpl2(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

internal fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int, modifier: Modifier = Modifier) =
    LayoutNode().apply {
        this.modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(x, y)
                }
            }
            .then(modifier)
        measurePolicy = object : LayoutNode.NoIntrinsicsMeasurePolicy("not supported") {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult =
                measureScope.layout(x2 - x, y2 - y) {
                    measurables.forEach { it.measure(constraints).place(0, 0) }
                }
        }
    }

@OptIn(ExperimentalComposeUiApi::class, InternalCoreApi::class)
private class TestOwner : Owner {
    val onEndListeners = mutableListOf<() -> Unit>()
    var position: IntOffset = IntOffset.Zero
    override val root = LayoutNode(0, 0, 500, 500)

    private val delegate = MeasureAndLayoutDelegate(root)

    init {
        root.attach(this)
        delegate.updateRootConstraints(Constraints(maxWidth = 500, maxHeight = 500))
    }

    override fun requestFocus(): Boolean = false
    override val rootForTest: RootForTest
        get() = TODO("Not yet implemented")
    override val hapticFeedBack: HapticFeedback
        get() = TODO("Not yet implemented")
    override val inputModeManager: InputModeManager
        get() = TODO("Not yet implemented")
    override val clipboardManager: ClipboardManager
        get() = TODO("Not yet implemented")
    override val accessibilityManager: AccessibilityManager
        get() = TODO("Not yet implemented")
    override val textToolbar: TextToolbar
        get() = TODO("Not yet implemented")
    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")
    override val autofill: Autofill?
        get() = null
    override val density: Density
        get() = Density(1f)
    override val textInputService: TextInputService
        get() = TODO("Not yet implemented")
    override val pointerIconService: PointerIconService
        get() = TODO("Not yet implemented")
    override val focusManager: FocusManager
        get() = TODO("Not yet implemented")
    override val windowInfo: WindowInfo
        get() = TODO("Not yet implemented")
    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override val fontLoader: Font.ResourceLoader
        get() = TODO("Not yet implemented")
    override val fontFamilyResolver: FontFamily.Resolver
        get() = TODO("Not yet implemented")
    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr
    override var showLayoutBounds: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}

    override fun onRequestMeasure(layoutNode: LayoutNode, forceRequest: Boolean) {
        delegate.requestRemeasure(layoutNode)
    }

    override fun onRequestRelayout(layoutNode: LayoutNode, forceRequest: Boolean) {
        delegate.requestRelayout(layoutNode)
    }

    override fun onAttach(node: LayoutNode) {
    }

    override fun onDetach(node: LayoutNode) {
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset =
        localPosition + position.toOffset()

    override fun calculateLocalPosition(positionInWindow: Offset): Offset =
        positionInWindow - position.toOffset()

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        delegate.measureAndLayout()
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        delegate.measureAndLayout(layoutNode, constraints)
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
        delegate.forceMeasureTheSubtree(layoutNode)
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        TODO("Not yet implemented")
    }

    override fun onSemanticsChange() {
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        TODO("Not yet implemented")
    }

    override val measureIteration: Long
        get() = 0

    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")
    override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        onEndListeners += listener
    }

    override fun onEndApplyChanges() {
        while (onEndListeners.isNotEmpty()) {
            onEndListeners.removeAt(0).invoke()
        }
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override val sharedDrawScope = LayoutNodeDrawScope()
}

private fun List<LogEntry>.verifyOnPointerEventCall(
    index: Int,
    expectedPif: PointerInputFilter? = null,
    expectedEvent: PointerEvent,
    expectedPass: PointerEventPass,
    expectedBounds: IntSize? = null
) {
    val logEntry = this[index]
    assertThat(logEntry).isInstanceOf(OnPointerEventEntry::class.java)
    val entry = logEntry as OnPointerEventEntry
    if (expectedPif != null) {
        assertThat(entry.pointerInputFilter).isSameInstanceAs(expectedPif)
    }
    PointerEventSubject
        .assertThat(entry.pointerEvent)
        .isStructurallyEqualTo(expectedEvent)
    assertThat(entry.pass).isEqualTo(expectedPass)
    if (expectedBounds != null) {
        assertThat(entry.bounds).isEqualTo(expectedBounds)
    }
}

private fun List<LogEntry>.verifyOnCancelCall(
    index: Int
) {
    val logEntry = this[index]
    assertThat(logEntry).isInstanceOf(OnCancelEntry::class.java)
}