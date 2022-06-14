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

import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
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
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HitPathTrackerTest {

    private lateinit var hitPathTracker: HitPathTracker
    private val layoutNode = LayoutNode(0, 0, 100, 100).also {
        it.attach(MockOwner())
    }

    @Before
    fun setup() {
        hitPathTracker = HitPathTracker(layoutNode.outerLayoutNodeWrapper)
    }

    @Test
    fun addHitPath_emptyHitResult_resultIsCorrect() {
        val pif1: PointerInputFilter = PointerInputFilterMock()
        val pif2: PointerInputFilter = PointerInputFilterMock()
        val pif3: PointerInputFilter = PointerInputFilterMock()
        val pointerId = PointerId(1)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_existingNonMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = PointerInputFilterMock()
        val pif2: PointerInputFilter = PointerInputFilterMock()
        val pif3: PointerInputFilter = PointerInputFilterMock()
        val pif4: PointerInputFilter = PointerInputFilterMock()
        val pif5: PointerInputFilter = PointerInputFilterMock()
        val pif6: PointerInputFilter = PointerInputFilterMock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)

        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))
        hitPathTracker.addHitPath(pointerId2, listOf(pif4, pif5, pif6))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(pif4).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(pif5).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(pif6).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_completeMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = PointerInputFilterMock()
        val pif2: PointerInputFilter = PointerInputFilterMock()
        val pif3: PointerInputFilter = PointerInputFilterMock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)
        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))

        hitPathTracker.addHitPath(pointerId2, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId1)
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId1)
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_partiallyMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = PointerInputFilterMock()
        val pif2: PointerInputFilter = PointerInputFilterMock()
        val pif3: PointerInputFilter = PointerInputFilterMock()
        val pif4: PointerInputFilter = PointerInputFilterMock()
        val pif5: PointerInputFilter = PointerInputFilterMock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)
        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))

        hitPathTracker.addHitPath(pointerId2, listOf(pif1, pif4, pif5))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                    children.add(
                        Node(pif4).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(pif5).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun dispatchChanges_noNodes_doesNotCrash() {
        hitPathTracker.dispatchChanges(internalPointerEventOf(down(0)))
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_pointerInputHandlerCalled() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val log = pif.log.getOnPointerEventLog()

        // Verify call count
        assertThat(log).hasSize(3)
        // Verify call values
        PointerEventPass.values().forEachIndexed { index, value ->
            PointerEventSubject
                .assertThat(log[index].pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(down(13)))
            assertThat(log[index].pass).isEqualTo(value)
        }
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatches_pointerInputHandlersCalledInCorrectOrder() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(log)
        val pif2 = PointerInputFilterMock(log)
        val pif3 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val onPointerEventLog =
            log.getOnPointerEventLog().filter { it.pass == PointerEventPass.Initial }
        assertThat(onPointerEventLog).hasSize(3)
        assertThat(onPointerEventLog[0].pointerInputFilter).isEqualTo(pif1)
        assertThat(onPointerEventLog[1].pointerInputFilter).isEqualTo(pif2)
        assertThat(onPointerEventLog[2].pointerInputFilter).isEqualTo(pif3)
        onPointerEventLog.forEach {
            PointerEventSubject
                .assertThat(it.pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(down(13)))
        }
    }

    @Test
    fun dispatchChanges_hasDownAndUpPath_pointerInputHandlersCalledInCorrectOrder() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(log)
        val pif2 = PointerInputFilterMock(log)
        val pif3 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val onPointerEventLog = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(onPointerEventLog).hasSize(6)
        assertThat(onPointerEventLog[0].pointerInputFilter).isEqualTo(pif1)
        assertThat(onPointerEventLog[1].pointerInputFilter).isEqualTo(pif2)
        assertThat(onPointerEventLog[2].pointerInputFilter).isEqualTo(pif3)
        assertThat(onPointerEventLog[3].pointerInputFilter).isEqualTo(pif3)
        assertThat(onPointerEventLog[4].pointerInputFilter).isEqualTo(pif2)
        assertThat(onPointerEventLog[5].pointerInputFilter).isEqualTo(pif1)
        onPointerEventLog.forEach {
            PointerEventSubject
                .assertThat(it.pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(down(13)))
        }
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_eventsSplitCorrectlyAndCallOrderCorrect() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(log)
        val pif2 = PointerInputFilterMock(log)
        val pif3 = PointerInputFilterMock(log)
        val pif4 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(3), listOf(pif1, pif2))
        hitPathTracker.addHitPath(PointerId(5), listOf(pif3, pif4))
        val event1 = down(3)
        val event2 = down(5).moveTo(10, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        val log1 = log
            .getOnPointerEventLog()
            .filter { it.pointerInputFilter == pif1 || it.pointerInputFilter == pif2 }
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }
        val log2 = log
            .getOnPointerEventLog()
            .filter { it.pointerInputFilter == pif3 || it.pointerInputFilter == pif4 }
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(log1).hasSize(4)
        assertThat(log2).hasSize(4)

        log1.forEach {
            PointerEventSubject
                .assertThat(it.pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(event1))
        }

        assertThat(log1[0].pointerInputFilter).isEqualTo(pif1)
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(pif2)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[2].pointerInputFilter).isEqualTo(pif2)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[3].pointerInputFilter).isEqualTo(pif1)
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log2[0].pointerInputFilter).isEqualTo(pif3)
        assertThat(log2[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log2[1].pointerInputFilter).isEqualTo(pif4)
        assertThat(log2[1].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log2[2].pointerInputFilter).isEqualTo(pif4)
        assertThat(log2[2].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log2[3].pointerInputFilter).isEqualTo(pif3)
        assertThat(log2[3].pass).isEqualTo(PointerEventPass.Main)
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_eventsSplitCorrectlyAndCallOrderCorrect() {
        val log = mutableListOf<LogEntry>()
        val parent = PointerInputFilterMock(log)
        val child1 = PointerInputFilterMock(log)
        val child2 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(3), listOf(parent, child1))
        hitPathTracker.addHitPath(PointerId(5), listOf(parent, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        val log1 = log
            .getOnPointerEventLog()
            .filter { it.pointerInputFilter == parent || it.pointerInputFilter == child1 }
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }
        val log2 = log
            .getOnPointerEventLog()
            .filter { it.pointerInputFilter == parent || it.pointerInputFilter == child2 }
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(log1).hasSize(4)
        assertThat(log2).hasSize(4)

        // Verifies that the events traverse between parent and child1 in the correct order.
        assertThat(log1[0].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1, event2))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1))
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1))
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1, event2))
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        // Verifies that the events traverse between parent and child2 in the correct order.
        assertThat(log1[0].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1, event2))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1))
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1))
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(event1, event2))
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_eventsDoNotSplitAndCallOrderCorrect() {
        val log = mutableListOf<LogEntry>()
        val child1 = PointerInputFilterMock(log)
        val child2 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(3), listOf(child1, child2))
        hitPathTracker.addHitPath(PointerId(5), listOf(child1, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        val log1 = log
            .getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        // Verify call count
        assertThat(log1).hasSize(4)

        // Verify PointerEvent
        log1.forEach {
            PointerEventSubject
                .assertThat(it.pointerEvent)
                .isStructurallyEqualTo(pointerEventOf(event1, event2))
        }

        // Verify dispatch order
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[0].pointerInputFilter).isEqualTo(child1)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(child2)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[2].pointerInputFilter).isEqualTo(child2)
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[3].pointerInputFilter).isEqualTo(child1)
    }

    @Test
    fun dispatchChanges_noNodes_nothingChanges() {
        val internalPointerEvent = internalPointerEventOf(down(5))

        hitPathTracker.dispatchChanges(internalPointerEvent)

        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes.values.first())
            .isStructurallyEqualTo(down(5))
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_changesAreUpdatedCorrectly() {
        val pif1 = PointerInputFilterMock(
            pointerEventHandler = { pointerEvent, _, _ ->
                pointerEvent.changes.map {
                    if (it.pressed != it.previousPressed) it.consume()
                    it
                }
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(pif1))

        val internalPointerEvent = internalPointerEventOf(down(13))

        hitPathTracker.dispatchChanges(internalPointerEvent)

        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes.values.first())
            .isStructurallyEqualTo(down(13).apply { if (pressed != previousPressed) consume() })
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatchesAndDownAndUpPaths_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, _, _ ->
                pointerEvent.changes.map {
                    it.consume()
                }
                pointerEvent.changes
            }
        )

        val pif2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, _, _ ->
                pointerEvent.changes.map {
                    it.consume()
                }
                pointerEvent.changes
            }
        )

        val pif3 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, _, _ ->
                pointerEvent.changes.map {
                    it.consume()
                }
                pointerEvent.changes
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))
        val actualChange = down(13).moveTo(10, 0f, 0f)
        val expectedChange = actualChange.deepCopy()
        val consumedExpectedChange = actualChange.deepCopy().apply { consume() }

        val internalPointerEvent = internalPointerEventOf(actualChange)

        hitPathTracker.dispatchChanges(internalPointerEvent)

        val log1 = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(log1[0].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(expectedChange))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedChange
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedChange
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[3].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedChange
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[4].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[4].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedChange
                )
            )
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[5].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[5].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedChange
                )
            )
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Main)

        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes.values.first())
            .isStructurallyEqualTo(
                consumedExpectedChange
            )
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        if (it.positionChange() != Offset.Zero) it.consume()
                    }
                    pointerEvent.changes
                }
        )
        val pif2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        if (it.positionChange() != Offset.Zero) it.consume()
                    }
                    pointerEvent.changes
                }
        )
        val pif3 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        it.consume()
                    }
                    pointerEvent.changes
                }
        )
        val pif4 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        it.consume()
                    }
                    pointerEvent.changes
                }
        )
        hitPathTracker.addHitPath(PointerId(3), listOf(pif1, pif2))
        hitPathTracker.addHitPath(PointerId(5), listOf(pif3, pif4))
        val actualEvent1 = down(3).moveTo(10, 0f, 30f)
        val actualEvent2 = down(5).moveTo(10, 0f, 30f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()
        val consumedExpectedEvent1 = expectedEvent1.deepCopy().apply { consume() }
        val consumedExpectedEvent2 = expectedEvent2.deepCopy().apply { consume() }

        val internalPointerEvent = internalPointerEventOf(actualEvent1, actualEvent2)

        hitPathTracker.dispatchChanges(internalPointerEvent)

        val log1 = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }
            .filter { it.pointerInputFilter == pif1 || it.pointerInputFilter == pif2 }

        val log2 = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }
            .filter { it.pointerInputFilter == pif3 || it.pointerInputFilter == pif4 }

        assertThat(log1[0].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(expectedEvent1))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent1
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent1
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent1
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log2[0].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log2[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(expectedEvent2))
        assertThat(log2[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log2[1].pointerInputFilter).isEqualTo(pif4)
        PointerEventSubject
            .assertThat(log2[1].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent2
                )
            )
        assertThat(log2[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log2[2].pointerInputFilter).isEqualTo(pif4)
        PointerEventSubject
            .assertThat(log2[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent2
                )
            )
        assertThat(log2[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log2[3].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log2[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedExpectedEvent2
                )
            )
        assertThat(log2[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(internalPointerEvent.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent1.id])
            .isStructurallyEqualTo(
                consumedExpectedEvent1
            )
        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent2.id])
            .isStructurallyEqualTo(
                consumedExpectedEvent2
            )
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val parent = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        if (it.positionChange() != Offset.Zero) it.consume()
                    }
                    pointerEvent.changes
                }
        )

        val child1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        if (it.positionChange() != Offset.Zero) it.consume()
                    }
                    pointerEvent.changes
                }
        )

        val child2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        if (it.positionChange() != Offset.Zero) it.consume()
                    }
                    pointerEvent.changes
                }
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(parent, child1))
        hitPathTracker.addHitPath(PointerId(5), listOf(parent, child2))
        val actualEvent1 = down(3).moveTo(10, 0f, 30f)
        val actualEvent2 = down(5).moveTo(10, 0f, 30f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()
        val consumedEvent1 = expectedEvent1.deepCopy().apply { consume() }
        val consumedEvent2 = expectedEvent2.deepCopy().apply { consume() }

        val internalPointerEvent = internalPointerEventOf(actualEvent1, actualEvent2)

        hitPathTracker.dispatchChanges(internalPointerEvent)

        val log1 = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(log1[0].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(expectedEvent1, expectedEvent2))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(consumedEvent1)
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(consumedEvent1)
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(consumedEvent2)
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[4].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[4].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(consumedEvent2)
            )
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[5].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[5].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(consumedEvent1, consumedEvent2)
            )
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Main)

        assertThat(internalPointerEvent.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent1.id])
            .isStructurallyEqualTo(consumedEvent1)

        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent2.id])
            .isStructurallyEqualTo(consumedEvent2)
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val child1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        it.consume()
                    }
                    pointerEvent.changes
                }
        )
        val child2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, _, _ ->
                    pointerEvent.changes.map {
                        it.consume()
                    }
                    pointerEvent.changes
                }
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(child1, child2))
        hitPathTracker.addHitPath(PointerId(5), listOf(child1, child2))
        val actualEvent1 = down(3).moveTo(10, 0f, 0f)
        val actualEvent2 = down(5).moveTo(10, 0f, 0f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()
        val consumedEvent1 = expectedEvent1.deepCopy().apply { consume() }
        val consumedEvent2 = expectedEvent2.deepCopy().apply { consume() }

        val internalPointerEvent = internalPointerEventOf(actualEvent1, actualEvent2)

        hitPathTracker.dispatchChanges(internalPointerEvent)

        val log1 = log.getOnPointerEventLog()
            .filter { it.pass == PointerEventPass.Initial || it.pass == PointerEventPass.Main }

        assertThat(log1[0].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[0].pointerEvent)
            .isStructurallyEqualTo(pointerEventOf(expectedEvent1, expectedEvent2))
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[1].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[1].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedEvent1,
                    consumedEvent2
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedEvent1,
                    consumedEvent2
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    consumedEvent1,
                    consumedEvent2
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(internalPointerEvent.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent1.id])
            .isStructurallyEqualTo(
                consumedEvent1
            )
        PointerInputChangeSubject
            .assertThat(internalPointerEvent.changes[actualEvent2.id])
            .isStructurallyEqualTo(
                consumedEvent2
            )
    }

    @Test
    fun removeDetachedPointerInputFilters_noNodes_hitResultJustHasRootAndDoesNotCrash() {
        val throwable = catchThrowable {
            hitPathTracker.removeDetachedPointerInputFilters()
        }

        assertThat(throwable).isNull()
        assertThat(areEqual(hitPathTracker.root, NodeParent()))
    }

    @Test
    fun removeDetachedPointerInputFilters_complexNothingDetached_nothingRemovedNoCancelsCalled() {

        // Arrange.

        val log = mutableListOf<LogEntry>()

        val pif1 = PointerInputFilterMock(log)
        val pif2 = PointerInputFilterMock(log)
        val pif3 = PointerInputFilterMock(log)
        val pif4 = PointerInputFilterMock(log)
        val pif5 = PointerInputFilterMock(log)
        val pif6 = PointerInputFilterMock(log)
        val pif7 = PointerInputFilterMock(log)
        val pif8 = PointerInputFilterMock(log)
        val pif9 = PointerInputFilterMock(log)

        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)
        val pointerId3 = PointerId(3)
        val pointerId4 = PointerId(4)
        val pointerId5 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(pif1))
        hitPathTracker.addHitPath(pointerId2, listOf(pif3, pif2))
        hitPathTracker.addHitPath(pointerId3, listOf(pif6, pif5, pif4))
        hitPathTracker.addHitPath(pointerId4, listOf(pif9, pif7))
        hitPathTracker.addHitPath(pointerId5, listOf(pif9, pif8))

        // Act.

        hitPathTracker.removeDetachedPointerInputFilters()

        // Assert.

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId1)
                }
            )
            children.add(
                Node(pif3).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
            children.add(
                Node(pif6).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(pif5).apply {
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(pif4).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(pif9).apply {
                    pointerIds.add(pointerId4)
                    pointerIds.add(pointerId5)
                    children.add(
                        Node(pif7).apply {
                            pointerIds.add(pointerId4)
                        }
                    )
                    children.add(
                        Node(pif8).apply {
                            pointerIds.add(pointerId5)
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        // Assert

        assertThat(log.getOnCancelLog()).hasSize(0)
    }

    //  compositionRoot, root -> middle -> leaf
    @Test
    fun removeDetachedPointerInputFilters_1PathRootDetached_allRemovedAndCorrectCancels() {
        val log = mutableListOf<LogEntry>()
        val root = PointerInputFilterMock(
            log = log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle = PointerInputFilterMock(
            log = log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf = PointerInputFilterMock(
            log = log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        hitPathTracker.addHitPath(PointerId(0), listOf(root, middle, leaf))

        hitPathTracker.removeDetachedPointerInputFilters()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()

        val log1 = log.getOnCancelLog()
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root)
    }

    //  compositionRoot -> root, middle -> child
    @Test
    fun removeDetachedPointerInputFilters_1PathMiddleDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()
        val root = PointerInputFilterMock(log)
        val middle = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val child = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId = PointerId(0)
        hitPathTracker.addHitPath(pointerId, listOf(root, middle, child))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId)
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(child)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle)
    }

    //  compositionRoot -> root -> middle, leaf
    @Test
    fun removeDetachedPointerInputFilters_1PathLeafDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()
        val root = PointerInputFilterMock(log)
        val middle = PointerInputFilterMock(log)
        val leaf = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId = PointerId(0)
        hitPathTracker.addHitPath(pointerId, listOf(root, middle, leaf))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(1)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf)
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1Detached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(log)
        val leaf1 = PointerInputFilterMock(log)

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(log)

        val root3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(leaf2).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle3)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root3)
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1MiddleDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock()

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(leaf2).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf3).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1LeafDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(log)
        val leaf1 = PointerInputFilterMock(log)

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock(log)
        val middle3 = PointerInputFilterMock(log)
        val leaf3 = PointerInputFilterMock(log)

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf3).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(1)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf2)
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2Detached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock()

        val root3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(leaf2).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(6)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root1)
        assertThat(log1[3].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[4].pointerInputFilter).isEqualTo(middle3)
        assertThat(log1[5].pointerInputFilter).isEqualTo(root3)
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2MiddlesDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf3).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(4)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[3].pointerInputFilter).isEqualTo(middle2)
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2LeafsDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(log)
        val leaf1 = PointerInputFilterMock(log)

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[1].pointerInputFilter).isEqualTo(leaf3)
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot, root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3Detached_allRemovedAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent()

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(9)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root1)
        assertThat(log1[3].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[4].pointerInputFilter).isEqualTo(middle2)
        assertThat(log1[5].pointerInputFilter).isEqualTo(root2)
        assertThat(log1[6].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[7].pointerInputFilter).isEqualTo(middle3)
        assertThat(log1[8].pointerInputFilter).isEqualTo(root3)
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3, middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3MiddlesDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock(log)
        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(6)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[3].pointerInputFilter).isEqualTo(middle2)
        assertThat(log1[4].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[5].pointerInputFilter).isEqualTo(middle3)
    }

    //  compositionRoot -> root1 -> middle1, leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3LeafsDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(log)
        val middle1 = PointerInputFilterMock(log)
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock(log)
        val middle3 = PointerInputFilterMock(log)
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                        }
                    )
                }
            )
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf3)
    }

    // compositionRoot, root1 -> middle1 -> leaf1
    // compositionRoot -> root2, middle2, leaf2
    // compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3RootsStaggeredDetached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root2 = PointerInputFilterMock(log)
        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val root3 = PointerInputFilterMock(log)
        val middle3 = PointerInputFilterMock(log)
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root2).apply {
                    pointerIds.add(pointerId2)
                }
            )
            children.add(
                Node(root3).apply {
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(6)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root1)
        assertThat(log1[3].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[4].pointerInputFilter).isEqualTo(middle2)
        assertThat(log1[5].pointerInputFilter).isEqualTo(leaf3)
    }

    // compositionRoot, root ->
    //   middle1 -> leaf1
    //   middle2 -> leaf2
    //   middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3MiddlesDetached_allRemovedAndCorrectCancels() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent()

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog().filter {
            it.pointerInputFilter == leaf1 ||
                it.pointerInputFilter == middle1 ||
                it.pointerInputFilter == root
        }

        val log2 = log.getOnCancelLog().filter {
            it.pointerInputFilter == leaf2 ||
                it.pointerInputFilter == middle2 ||
                it.pointerInputFilter == root
        }

        val log3 = log.getOnCancelLog().filter {
            it.pointerInputFilter == leaf3 ||
                it.pointerInputFilter == middle3 ||
                it.pointerInputFilter == root
        }

        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(root)

        assertThat(log2).hasSize(3)
        assertThat(log2[0].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log2[1].pointerInputFilter).isEqualTo(middle2)
        assertThat(log2[2].pointerInputFilter).isEqualTo(root)

        assertThat(log3).hasSize(3)
        assertThat(log3[0].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log3[1].pointerInputFilter).isEqualTo(middle3)
        assertThat(log3[2].pointerInputFilter).isEqualTo(root)
    }

    // compositionRoot -> root
    //   -> middle1 -> leaf1
    //   -> middle2 -> leaf2
    //   , middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3Middles1Detached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle1 = PointerInputFilterMock(log)
        val leaf1 = PointerInputFilterMock(log)

        val middle2 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(log)

        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                    children.add(
                        Node(middle2).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(leaf2).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle3)
    }

    // compositionRoot -> root
    //   , middle1 -> leaf1
    //   , middle2 -> leaf2
    //   -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3Middles2Detached_removesAndCancelsCorrect() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle3 = PointerInputFilterMock(log)
        val leaf3 = PointerInputFilterMock(log)

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle3).apply {
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf3).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(4)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[3].pointerInputFilter).isEqualTo(middle2)
    }

    // compositionRoot -> root
    //   , middle1 -> leaf1
    //   , middle2 -> leaf2
    //   , middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3MiddlesAllDetached_allMiddlesRemoved() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val middle3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(6)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middle1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[3].pointerInputFilter).isEqualTo(middle2)
        assertThat(log1[4].pointerInputFilter).isEqualTo(leaf3)
        assertThat(log1[5].pointerInputFilter).isEqualTo(middle3)
    }

    // compositionRoot -> root -> middle
    //   -> leaf1
    //   , leaf2
    //   -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3Leafs1Detached_correctLeafRemoved() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle = PointerInputFilterMock(log)

        val leaf1 = PointerInputFilterMock(log)
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(log)

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            pointerIds.add(pointerId2)
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                            children.add(
                                Node(leaf3).apply {
                                    pointerIds.add(pointerId3)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(1)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf2)
    }

    // compositionRoot -> root -> middle
    //   , leaf1
    //   -> leaf2
    //   , leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3Leafs2Detached_correctLeafsRemoved() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle = PointerInputFilterMock(log)

        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(log)
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            pointerIds.add(pointerId2)
                            pointerIds.add(pointerId3)
                            children.add(
                                Node(leaf2).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(leaf3)
    }

    // compositionRoot -> root -> middle
    //   , leaf1
    //   , leaf2
    //   , leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3LeafsAllDetached_allLeafsRemoved() {
        val log = mutableListOf<LogEntry>()

        val root = PointerInputFilterMock(log)

        val middle = PointerInputFilterMock(log)

        val leaf1 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf2 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )
        val leaf3 = PointerInputFilterMock(
            log,
            layoutCoordinates = LayoutCoordinatesStub(false)
        )

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            pointerIds.add(pointerId2)
                            pointerIds.add(pointerId3)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(leaf1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(leaf2)
        assertThat(log1[2].pointerInputFilter).isEqualTo(leaf3)
    }

    // arrange: root(3) -> middle(3) -> leaf(3)
    // act: 3 is removed
    // assert: no path
    @Test
    fun removeHitPath_onePathPointerIdRemoved_hitTestResultIsEmpty() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(3).up(1L)))

        val expectedRoot = NodeParent()

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // arrange: root(3) -> middle(3) -> leaf(3)
    // act: 99 is removed
    // assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_onePathOtherPointerIdRemoved_hitTestResultIsNotChanged() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(99).up(1L), down(3))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange:
    // root(3) -> middle(3) -> leaf(3)
    // root(5) -> middle(5) -> leaf(5)
    //
    // Act:
    // 5 is removed
    //
    // Act:
    // root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_2IndependentPaths1PointerIdRemoved_resultContainsRemainingPath() {
        val root1: PointerInputFilter = PointerInputFilterMock()
        val middle1: PointerInputFilter = PointerInputFilterMock()
        val leaf1: PointerInputFilter = PointerInputFilterMock()

        val root2: PointerInputFilter = PointerInputFilterMock()
        val middle2: PointerInputFilter = PointerInputFilterMock()
        val leaf2: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(5).up(1L), down(3))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root1).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle1).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf1).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // root(3,5) -> middle(3,5) -> leaf(3,5)
    // 3 is removed
    // root(5) -> middle(5) -> leaf(5)
    @Test
    fun removeHitPath_2PathsShareNodes1PointerIdRemoved_resultContainsRemainingPath() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle, leaf))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(3).up(1L), down(5))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId2)
                            children.add(
                                Node(leaf).apply {
                                    pointerIds.add(pointerId2)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5) -> middle(5)
    @Test
    fun removeHitPath_2PathsShare2NodesLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(3).up(1L), down(5))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_2PathsShare2NodesShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(5).up(1L), down(3))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5)
    @Test
    fun removeHitPath_2PathsShare1NodeLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(3).up(1L), down(5))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId2)
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_2PathsShare1NodeShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root: PointerInputFilter = PointerInputFilterMock()
        val middle: PointerInputFilter = PointerInputFilterMock()
        val leaf: PointerInputFilter = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root))

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(down(5).up(1L), down(3))
        )

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(root).apply {
                    pointerIds.add(pointerId1)
                    children.add(
                        Node(middle).apply {
                            pointerIds.add(pointerId1)
                            children.add(
                                Node(leaf).apply {
                                    pointerIds.add(pointerId1)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun processCancel_nothingTracked_doesNotCrash() {
        hitPathTracker.processCancel()
    }

    // Pin -> Ln
    @Test
    fun processCancel_singlePin_cancelHandlerIsCalled() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pif))

        hitPathTracker.processCancel()

        assertThat(pif.log.getOnCancelLog()).hasSize(1)
    }

    // Pin -> Pin -> Pin
    @Test
    fun processCancel_3Pins_cancelHandlersCalledOnceInOrder() {
        val log = mutableListOf<LogEntry>()
        val childPif = PointerInputFilterMock(log)
        val middlePif = PointerInputFilterMock(log)
        val parentPif = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(
            PointerId(3),
            listOf(parentPif, middlePif, childPif)
        )

        hitPathTracker.processCancel()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(childPif)
        assertThat(log1[1].pointerInputFilter).isEqualTo(middlePif)
        assertThat(log1[2].pointerInputFilter).isEqualTo(parentPif)
    }

    // PIN -> PIN
    // PIN -> PIN
    @Test
    fun processCancel_2IndependentPathsFromRoot_cancelHandlersCalledOnceInOrder() {
        val log = mutableListOf<LogEntry>()
        val pifParent1 = PointerInputFilterMock(log)
        val pifChild1 = PointerInputFilterMock(log)
        val pifParent2 = PointerInputFilterMock(log)
        val pifChild2 = PointerInputFilterMock(log)

        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent1, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent2, pifChild2))

        hitPathTracker.processCancel()

        val log1 = log.getOnCancelLog()

        assertThat(log1).hasSize(4)
        assertThat(log1[0].pointerInputFilter).isEqualTo(pifChild1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(pifParent1)
        assertThat(log1[2].pointerInputFilter).isEqualTo(pifChild2)
        assertThat(log1[3].pointerInputFilter).isEqualTo(pifParent2)
    }

    // PIN -> PIN
    //     -> PIN
    @Test
    fun processCancel_2BranchingPaths_cancelHandlersCalledOnceInOrder() {
        val log = mutableListOf<LogEntry>()
        val pifParent = PointerInputFilterMock(log)
        val pifChild1 = PointerInputFilterMock(log)
        val pifChild2 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent, pifChild2))

        hitPathTracker.processCancel()

        val log1 = log.getOnCancelLog()
            .filter { it.pointerInputFilter == pifChild1 || it.pointerInputFilter == pifParent }
        val log2 = log.getOnCancelLog()
            .filter { it.pointerInputFilter == pifChild2 || it.pointerInputFilter == pifParent }
        assertThat(log1).hasSize(2)
        assertThat(log1[0].pointerInputFilter).isEqualTo(pifChild1)
        assertThat(log1[1].pointerInputFilter).isEqualTo(pifParent)
        assertThat(log2).hasSize(2)
        assertThat(log2[0].pointerInputFilter).isEqualTo(pifChild2)
        assertThat(log2[1].pointerInputFilter).isEqualTo(pifParent)
    }

    // Pin -> Ln
    @Test
    fun processCancel_singlePin_cleared() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pif))

        hitPathTracker.processCancel()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    // Pin -> Pin -> Pin
    @Test
    fun processCancel_3Pins_cleared() {
        val childPif = PointerInputFilterMock()
        val middlePif = PointerInputFilterMock()
        val parentPif = PointerInputFilterMock()
        hitPathTracker.addHitPath(
            PointerId(3),
            listOf(parentPif, middlePif, childPif)
        )

        hitPathTracker.processCancel()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    // PIN -> PIN
    // PIN -> PIN
    @Test
    fun processCancel_2IndependentPathsFromRoot_cleared() {
        val pifParent1 = PointerInputFilterMock()
        val pifChild1 = PointerInputFilterMock()
        val pifParent2 = PointerInputFilterMock()
        val pifChild2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent1, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent2, pifChild2))

        hitPathTracker.processCancel()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    // PIN -> PIN
    //     -> PIN
    @Test
    fun processCancel_2BranchingPaths_cleared() {
        val pifParent = PointerInputFilterMock()
        val pifChild1 = PointerInputFilterMock()
        val pifChild2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent, pifChild2))

        hitPathTracker.processCancel()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    private enum class DispatchingPif {
        Parent, Middle, Child
    }

    // Tests related to reporting whether or not a pointer input filter was dispatched to.

    @Test
    fun dispatchChanges_noNodes_reportsWasDispatchedToNothing() {
        val hitSomething = hitPathTracker.dispatchChanges(internalPointerEventOf(down(0)))
        assertThat(hitSomething).isFalse()
    }

    @Test
    fun dispatchChanges_1NodeDispatchToNode_reportsWasDispatchedToSomething() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        val hitSomething = hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        assertThat(hitSomething).isTrue()
    }

    @Test
    fun dispatchChanges_1NodeDispatchToDifferentNode_reportsWasDispatchedToNothing() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        val hitSomething = hitPathTracker.dispatchChanges(internalPointerEventOf(down(69)))

        assertThat(hitSomething).isFalse()
    }

    // Tests related to retaining and releasing hit paths.

    @Test
    fun dispatchChanges_pifRemovesSelfDuringInitial_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchChanges_pifRemovesSelfDuringMain_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchChanges_pifRemovesSelfDuringFinal_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchChanges_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {
        val layoutCoordinates = LayoutCoordinatesStub(true)
        lateinit var pifRef: PointerInputFilter
        val pif = PointerInputFilterMock(
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == removalPass) {
                        layoutCoordinates.isAttached = false
                        pifRef.isAttached = false
                    }
                    pointerEvent.changes
                },
            layoutCoordinates = layoutCoordinates
        )
        pifRef = pif
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val log = pif.log.getOnPointerEventLog()

        var passedRemovalPass = false
        PointerEventPass.values().forEachIndexed { index, pass ->
            if (!passedRemovalPass) {
                assertThat(log[index].pass).isEqualTo(pass)
                passedRemovalPass = pass == removalPass
            }
        }
    }

    @Test
    fun dispatchChanges_pifRemovedByParentDuringInitial_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchChanges_pifRemovedByParentDuringMain_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchChanges_pifRemovedByParentDuringFinal_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchChanges_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {
        val log = mutableListOf<LogEntry>()
        val childLayoutCoordinates = LayoutCoordinatesStub(true)
        val childPif = PointerInputFilterMock(
            log,
            layoutCoordinates = childLayoutCoordinates
        )
        val parentPif = PointerInputFilterMock(
            log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == removalPass) {
                        childLayoutCoordinates.isAttached = false
                        childPif.isAttached = false
                    }
                    pointerEvent.changes
                }
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(parentPif, childPif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val log1 = log.getOnPointerEventLog().filter { it.pointerInputFilter == childPif }
        val count =
            when (removalPass) {
                PointerEventPass.Initial -> 0
                PointerEventPass.Main -> 2
                PointerEventPass.Final -> 2
            }
        assertThat(log1).hasSize(count)
        PointerEventPass.values().forEachIndexed { index, pass ->
            if (index < count) {
                assertThat(log1[index].pass).isEqualTo(pass)
            }
        }
    }

    @Test
    fun dispatchChanges_pifRemovedByChildDuringInitial_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchChanges_pifRemovedByChildDuringMain_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchChanges_pifRemovedByChildDuringFinal_noPassesReceivedAfterwards() {
        dispatchChanges_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchChanges_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {
        val log = mutableListOf<LogEntry>()
        val parentLayoutCoordinates = LayoutCoordinatesStub(true)
        val parentPif = PointerInputFilterMock(
            log,
            layoutCoordinates = parentLayoutCoordinates
        )
        val childPif = PointerInputFilterMock(
            log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == removalPass) {
                        parentLayoutCoordinates.isAttached = false
                        parentPif.isAttached = false
                    }
                    pointerEvent.changes
                }
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(parentPif, childPif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val log1 = log.getOnPointerEventLog().filter { it.pointerInputFilter == parentPif }
        val count =
            when (removalPass) {
                PointerEventPass.Initial -> 1
                PointerEventPass.Main -> 1
                PointerEventPass.Final -> 3
            }
        assertThat(log1).hasSize(count)
        PointerEventPass.values().forEachIndexed { index, pass ->
            if (index < count) {
                assertThat(log1[index].pass).isEqualTo(pass)
            }
        }
    }

    @Test
    fun dispatchChanges_pifMovesSelfDuringInitial_pointerCoordsCorrectAfterMove() {
        dispatchChanges_pifMovesSelfDuringDispatch_pointerCoordsCorrectAfterMove(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchChanges_pifMovesSelfDuringMain_pointerCoordsCorrectAfterMove() {
        dispatchChanges_pifMovesSelfDuringDispatch_pointerCoordsCorrectAfterMove(
            PointerEventPass.Main
        )
    }

    // This is a bit of a weird test. For performance reasons, HitPathTracker mutates an
    // InternalPointerEvent with position changes.  Previously there was a bug where during
    // dispatch, the pointer positions would be offset by the location of the PointerInputFilter,
    // then they would be dispatched to the PointerInputFilter, that dispatch would move the
    // PointerInputFilter, and then we'd get the new location of the PointerInputFilter and to
    // undo the position change that occurred before dispatch.  That position change after
    // dispatch is simply meant to reset the position back to being global for the next dispatch
    // to another PointerInputFilter.
    //
    // This test makes sure we don't create that bug again accidentally.
    private fun dispatchChanges_pifMovesSelfDuringDispatch_pointerCoordsCorrectAfterMove(
        movePass: PointerEventPass
    ) {
        val log = mutableListOf<LogEntry>()
        val layoutCoordinates = LayoutCoordinatesStub(true)
        val pif = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == movePass) {
                        layoutCoordinates.additionalOffset = Offset(500f, 500f)
                    }
                    pointerEvent.changes
                },
            layoutCoordinates = layoutCoordinates
        )
        val parent = PointerInputFilterMock(log)
        val child = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(13), listOf(parent, pif, child))

        val actual = internalPointerEventOf(down(13, 120, 1.0f, 1.0f))
        val expected = pointerEventOf(down(13, 120, 1.0f, 1.0f))

        hitPathTracker.dispatchChanges(actual)

        val log1 = log.getOnPointerEventLog()
            .filter { it.pointerInputFilter == parent || it.pointerInputFilter == child }

        assertThat(log1).hasSize(6)
        log1.forEach {
            PointerEventSubject
                .assertThat(it.pointerEvent)
                .isStructurallyEqualTo(expected)
        }
    }

    @Test
    fun dispatchChanges_pifMovesSelfDuringFinal_pointerCoordsCorrectAfterMove() {
        dispatchChanges_pifMovesSelfDuringDispatch_pointerCoordsCorrectAfterMove(
            PointerEventPass.Final
        )
    }

    @Test
    fun addHitPath_hoverMove_noChange() {
        val log = mutableListOf<LogEntry>()
        val parentLayoutCoordinates = LayoutCoordinatesStub(true)
        val pif1: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = parentLayoutCoordinates
        )
        val pif2: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = parentLayoutCoordinates
        )
        val pif3: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = parentLayoutCoordinates
        )
        val pointerId = PointerId(0)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId)
                                }
                            )
                        }
                    )
                }
            )
        }

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent())

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        assertHoverEvent(
            log,
            pif1 to PointerEventType.Enter,
            pif2 to PointerEventType.Enter,
            pif3 to PointerEventType.Enter,
        )

        log.clear()

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent())

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        // When the same position is sent, it should ignore the change.
        assertThat(log).hasSize(0)
    }

    private fun assertHoverEvent(
        log: List<LogEntry>,
        vararg filterAndTypes: Pair<PointerInputFilter, PointerEventType>
    ) {
        assertThat(log).hasSize(filterAndTypes.size * 3)
        log.forEachIndexed { index, logEntry ->
            val pass = when {
                index < filterAndTypes.size -> PointerEventPass.Initial
                index < filterAndTypes.size * 2 -> PointerEventPass.Main
                else -> PointerEventPass.Final
            }
            val filterIndex = when {
                index < filterAndTypes.size -> index
                index < filterAndTypes.size * 2 -> filterAndTypes.size * 2 - index - 1
                else -> index - (filterAndTypes.size * 2)
            }

            val (filter, type) = filterAndTypes[filterIndex]

            assertOnPointerEventEntry(logEntry, "LogEntry[$index]", pass, type, filter)
        }
    }

    private fun assertOnPointerEventEntry(
        logEntry: LogEntry,
        message: String,
        pass: PointerEventPass,
        pointerEventType: PointerEventType,
        pointerInputFilter: PointerInputFilter
    ) {
        assertThat(logEntry).isInstanceOf(OnPointerEventEntry::class.java)
        logEntry as OnPointerEventEntry
        assertWithMessage(message).that(logEntry.pass).isEqualTo(pass)
        assertWithMessage(message).that(logEntry.pointerEvent.type).isEqualTo(pointerEventType)
        assertWithMessage(message).that(logEntry.pointerInputFilter).isEqualTo(pointerInputFilter)
    }

    @Test
    fun addHitPath_hoverMove_enterExit() {
        val log = mutableListOf<LogEntry>()
        val layoutCoordinates = layoutNode.outerLayoutNodeWrapper
        val pif1: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pif2: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pif3: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pointerId = PointerId(0)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2))

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent())

        assertHoverEvent(
            log,
            pif1 to PointerEventType.Enter,
            pif2 to PointerEventType.Enter
        )

        log.clear()

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId)
                        }
                    )
                    children.add(
                        Node(pif3).apply {
                            pointerIds.add(pointerId)
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent())

        // These have some that appear out of order because of the different branches
        assertThat(log).hasSize(9)
        assertOnPointerEventEntry(
            log[0], "LogEntry[0]", PointerEventPass.Initial, PointerEventType.Move, pif1
        )
        assertOnPointerEventEntry(
            log[1], "LogEntry[1]", PointerEventPass.Initial, PointerEventType.Exit, pif2
        )
        assertOnPointerEventEntry(
            log[2], "LogEntry[2]", PointerEventPass.Main, PointerEventType.Exit, pif2
        )
        assertOnPointerEventEntry(
            log[3], "LogEntry[3]", PointerEventPass.Initial, PointerEventType.Enter, pif3
        )
        assertOnPointerEventEntry(
            log[4], "LogEntry[3]", PointerEventPass.Main, PointerEventType.Enter, pif3
        )
        assertOnPointerEventEntry(
            log[5], "LogEntry[5]", PointerEventPass.Main, PointerEventType.Move, pif1
        )
        assertOnPointerEventEntry(
            log[6], "LogEntry[6]", PointerEventPass.Final, PointerEventType.Move, pif1
        )
        assertOnPointerEventEntry(
            log[7], "LogEntry[7]", PointerEventPass.Final, PointerEventType.Exit, pif2
        )
        assertOnPointerEventEntry(
            log[8], "LogEntry[8]", PointerEventPass.Final, PointerEventType.Enter, pif3
        )

        val expectedAfterDispatch = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif3).apply {
                            pointerIds.add(pointerId)
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedAfterDispatch)).isTrue()
    }

    @Test
    fun addHitPath_hoverExit() {
        val log = mutableListOf<LogEntry>()
        val layoutCoordinates = layoutNode.outerLayoutNodeWrapper
        val pif1: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pif2: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pif3: PointerInputFilter = PointerInputFilterMock(
            log = log,
            layoutCoordinates = layoutCoordinates
        )
        val pointerId = PointerId(0)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent())

        log.clear()

        hitPathTracker.addHitPath(pointerId, listOf())

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent(ACTION_HOVER_EXIT))

        assertHoverEvent(
            log,
            pif1 to PointerEventType.Exit,
            pif2 to PointerEventType.Exit,
            pif3 to PointerEventType.Exit,
        )

        val expectedAfterDispatch = NodeParent()
        assertThat(areEqual(hitPathTracker.root, expectedAfterDispatch)).isTrue()
    }

    @Test
    fun dispatchChangesClearsStaleIds() {
        val layoutCoordinates = LayoutCoordinatesStub(isAttached = true)
        val pif1: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = layoutCoordinates
        )
        val pif2: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = layoutCoordinates
        )
        val pif3: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = layoutCoordinates
        )
        val pointerId = PointerId(0)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId)
                            children.add(
                                Node(pif3).apply {
                                    pointerIds.add(pointerId)
                                }
                            )
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        hitPathTracker.dispatchChanges(hoverInternalPointerEvent(ACTION_HOVER_ENTER))

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(5)))

        val expectedAfterDispatch = NodeParent()
        assertThat(areEqual(hitPathTracker.root, expectedAfterDispatch)).isTrue()
    }

    @Test
    fun dispatchChangesClearsStaleIdsPartialHit() {
        val parentLayoutCoordinates = LayoutCoordinatesStub(true)
        val pif1: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = parentLayoutCoordinates
        )
        val pif2: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = parentLayoutCoordinates
        )
        val pif3: PointerInputFilter = PointerInputFilterMock(
            layoutCoordinates = parentLayoutCoordinates
        )
        val pointerId1 = PointerId(0)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))
        hitPathTracker.addHitPath(pointerId2, listOf(pif1, pif2))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(5)))

        val expectedAfterDispatch = NodeParent().apply {
            children.add(
                Node(pif1).apply {
                    pointerIds.add(pointerId2)
                    children.add(
                        Node(pif2).apply {
                            pointerIds.add(pointerId2)
                        }
                    )
                }
            )
        }
        assertThat(areEqual(hitPathTracker.root, expectedAfterDispatch)).isTrue()
    }

    private fun areEqual(actualNode: NodeParent, expectedNode: NodeParent): Boolean {
        var check = true

        if (actualNode.children.size != expectedNode.children.size) {
            return false
        }
        actualNode.children.forEach { child ->
            check = check && expectedNode.children.any {
                areEqual(child, it)
            }
        }

        return check
    }

    private fun areEqual(actualNode: Node, expectedNode: Node): Boolean {
        if (actualNode.pointerInputFilter !== expectedNode.pointerInputFilter) {
            return false
        }

        if (actualNode.pointerIds.size != expectedNode.pointerIds.size) {
            return false
        }
        var check = true
        actualNode.pointerIds.forEach {
            check = check && expectedNode.pointerIds.contains(it)
        }
        if (!check) {
            return false
        }

        if (actualNode.children.size != expectedNode.children.size) {
            return false
        }
        actualNode.children.forEach { child ->
            check = check && expectedNode.children.any {
                areEqual(child, it)
            }
        }

        return check
    }
}

class LayoutCoordinatesStub(
    override var isAttached: Boolean = true
) : LayoutCoordinates {

    var additionalOffset = Offset.Zero

    override val size: IntSize
        get() = IntSize(Constraints.Infinity, Constraints.Infinity)

    override val providedAlignmentLines: Set<AlignmentLine>
        get() = TODO("not implemented")

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() = null
    override val parentCoordinates: LayoutCoordinates?
        get() = null

    override fun windowToLocal(relativeToWindow: Offset): Offset = relativeToWindow

    override fun localToWindow(relativeToLocal: Offset): Offset = relativeToLocal

    override fun localToRoot(relativeToLocal: Offset): Offset = relativeToLocal

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset = relativeToSource

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect {
        TODO("Not yet implemented")
    }

    override fun get(alignmentLine: AlignmentLine): Int {
        TODO("not implemented")
    }
}

@OptIn(InternalCoreApi::class)
private class MockOwner(
    val position: IntOffset = IntOffset.Zero,
    override val root: LayoutNode = LayoutNode()
) : Owner {
    val onRequestMeasureParams = mutableListOf<LayoutNode>()
    val onAttachParams = mutableListOf<LayoutNode>()
    val onDetachParams = mutableListOf<LayoutNode>()
    var layoutChangeCount = 0

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
    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")
    @OptIn(ExperimentalComposeUiApi::class)
    override val autofill: Autofill?
        get() = TODO("Not yet implemented")
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
    override var showLayoutBounds: Boolean = false
    override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun onEndApplyChanges() {
        TODO("Not yet implemented")
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        onRequestMeasureParams += layoutNode
        if (affectsLookahead) {
            layoutNode.markLookaheadMeasurePending()
        }
        layoutNode.markMeasurePending()
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            layoutNode.markLookaheadLayoutPending()
        }
        layoutNode.markLayoutPending()
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        TODO("Not yet implemented")
    }

    override fun onAttach(node: LayoutNode) {
        onAttachParams += node
    }

    override fun onDetach(node: LayoutNode) {
        onDetachParams += node
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset =
        localPosition + position.toOffset()

    override fun calculateLocalPosition(positionInWindow: Offset): Offset =
        positionInWindow - position.toOffset()

    override fun requestFocus(): Boolean = false

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        return object : OwnedLayer {
            override fun updateLayerProperties(
                scaleX: Float,
                scaleY: Float,
                alpha: Float,
                translationX: Float,
                translationY: Float,
                shadowElevation: Float,
                rotationX: Float,
                rotationY: Float,
                rotationZ: Float,
                cameraDistance: Float,
                transformOrigin: TransformOrigin,
                shape: Shape,
                clip: Boolean,
                renderEffect: RenderEffect?,
                ambientShadowColor: Color,
                spotShadowColor: Color,
                layoutDirection: LayoutDirection,
                density: Density
            ) {
            }

            override fun isInLayer(position: Offset) = true

            override fun move(position: IntOffset) {
            }

            override fun resize(size: IntSize) {
            }

            override fun drawLayer(canvas: Canvas) {
                drawBlock(canvas)
            }

            override fun updateDisplayList() {
            }

            override fun invalidate() {
            }

            override fun destroy() {
            }

            override fun mapBounds(rect: MutableRect, inverse: Boolean) {
            }

            override fun reuseLayer(
                drawBlock: (Canvas) -> Unit,
                invalidateParentLayer: () -> Unit
            ) {
            }

            override fun mapOffset(point: Offset, inverse: Boolean) = point
        }
    }

    override fun onSemanticsChange() {
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        layoutChangeCount++
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        TODO("Not yet implemented")
    }

    override var measureIteration: Long = 0
    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")

    override val sharedDrawScope = LayoutNodeDrawScope()
}
