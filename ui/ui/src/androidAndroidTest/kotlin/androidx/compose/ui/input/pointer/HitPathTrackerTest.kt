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

import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.milliseconds
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HitPathTrackerTest {

    private lateinit var hitPathTracker: HitPathTracker

    @Before
    fun setup() {
        hitPathTracker = HitPathTracker()
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
    fun addHitPath_1NodeAdded_initHandlerCalledWithValidCustomMessageDispatcher() {
        val pif = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(3), listOf(pif))

        assertThat(pif.log.getOnInitLog()).hasSize(1)
    }

    @Test
    fun addHitPath_3NodesAdded_allInitHandlersCalledWithValidCustomMessageDispatcher() {
        val pifParent = PointerInputFilterMock()
        val pifMiddle = PointerInputFilterMock()
        val pifChild = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent, pifMiddle, pifChild))

        assertThat(pifParent.log.getOnInitLog()).hasSize(1)
        assertThat(pifMiddle.log.getOnInitLog()).hasSize(1)
        assertThat(pifChild.log.getOnInitLog()).hasSize(1)
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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

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
        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(5)))

        PointerInputChangeSubject
            .assertThat(result.changes.values.first())
            .isStructurallyEqualTo(down(5))
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_changesAreUpdatedCorrectly() {
        val pif1 = PointerInputFilterMock(
            pointerEventHandler = { pointerEvent, _, _ ->
                pointerEvent.changes.map {
                    it.consumeDownChange()
                    it
                }
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(pif1))

        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        PointerInputChangeSubject
            .assertThat(result.changes.values.first())
            .isStructurallyEqualTo(down(13).apply { consumeDownChange() })
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatchesAndDownAndUpPaths_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, pass, _ ->
                pointerEvent.changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 1f
                            PointerEventPass.Main -> 6f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
                pointerEvent.changes
            }
        )

        val pif2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, pass, _ ->
                pointerEvent.changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 2f
                            PointerEventPass.Main -> 5f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
                pointerEvent.changes
            }
        )

        val pif3 = PointerInputFilterMock(
            log = log,
            pointerEventHandler = { pointerEvent, pass, _ ->
                pointerEvent.changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 3f
                            PointerEventPass.Main -> 4f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
                pointerEvent.changes
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))
        val actualChange = down(13).moveTo(10.milliseconds, 0f, 0f)
        val expectedChange = actualChange.deepCopy()

        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(actualChange))

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
                    expectedChange.apply { consumePositionChange(0f, 1f) }
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedChange.apply { consumePositionChange(0f, 2f) }
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[3].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedChange.apply { consumePositionChange(0f, 3f) }
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[4].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[4].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedChange.apply { consumePositionChange(0f, 4f) }
                )
            )
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[5].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[5].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedChange.apply { consumePositionChange(0f, 5f) }
                )
            )
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Main)

        PointerInputChangeSubject
            .assertThat(result.changes.values.first())
            .isStructurallyEqualTo(
                expectedChange.apply { consumePositionChange(0f, 6f) }
            )
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val pif1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 1f
                                PointerEventPass.Main -> 4f
                                else -> 0f
                            }
                        it.consumePositionChange(0f, yConsume)
                    }
                    pointerEvent.changes
                }
        )
        val pif2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 2f
                                PointerEventPass.Main -> 3f
                                else -> 0f
                            }
                        it.consumePositionChange(0f, yConsume)
                    }
                    pointerEvent.changes
                }
        )
        val pif3 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> -1f
                                PointerEventPass.Main -> -4f
                                else -> 0f
                            }
                        it.consumePositionChange(0f, yConsume)
                    }
                    pointerEvent.changes
                }
        )
        val pif4 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> -2f
                                PointerEventPass.Main -> -3f
                                else -> 0f
                            }
                        it.consumePositionChange(0f, yConsume)
                    }
                    pointerEvent.changes
                }
        )
        hitPathTracker.addHitPath(PointerId(3), listOf(pif1, pif2))
        hitPathTracker.addHitPath(PointerId(5), listOf(pif3, pif4))
        val actualEvent1 = down(3).moveTo(10.milliseconds, 0f, 0f)
        val actualEvent2 = down(5).moveTo(10.milliseconds, 0f, 0f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(actualEvent1, actualEvent2)
        )

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
                    expectedEvent1.apply { consumePositionChange(0f, 1f) }
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(pif2)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 2f) }
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(pif1)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 3f) }
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
                    expectedEvent2.apply { consumePositionChange(0f, -1f) }
                )
            )
        assertThat(log2[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log2[2].pointerInputFilter).isEqualTo(pif4)
        PointerEventSubject
            .assertThat(log2[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent2.apply { consumePositionChange(0f, -2f) }
                )
            )
        assertThat(log2[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log2[3].pointerInputFilter).isEqualTo(pif3)
        PointerEventSubject
            .assertThat(log2[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent2.apply { consumePositionChange(0f, -3f) }
                )
            )
        assertThat(log2[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(result.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent1.id])
            .isStructurallyEqualTo(
                expectedEvent1.apply { consumePositionChange(0f, 4f) }
            )
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent2.id])
            .isStructurallyEqualTo(
                expectedEvent2.apply { consumePositionChange(0f, -4f) }
            )
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val parent = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 1f
                                PointerEventPass.Main -> 10f
                                else -> 0f
                            }
                        it.consumePositionChange(
                            0f,
                            yConsume
                        )
                    }
                    pointerEvent.changes
                }
        )

        val child1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 2f
                                PointerEventPass.Main -> 20f
                                else -> 0f
                            }
                        it.consumePositionChange(
                            0f,
                            yConsume
                        )
                    }
                    pointerEvent.changes
                }
        )

        val child2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 4f
                                PointerEventPass.Main -> 40f
                                else -> 0f
                            }
                        it.consumePositionChange(
                            0f,
                            yConsume
                        )
                    }
                    pointerEvent.changes
                }
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(parent, child1))
        hitPathTracker.addHitPath(PointerId(5), listOf(parent, child2))
        val actualEvent1 = down(3).moveTo(10.milliseconds, 0f, 0f)
        val actualEvent2 = down(5).moveTo(10.milliseconds, 0f, 0f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(actualEvent1, actualEvent2)
        )

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
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 1f) }
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 2f) }
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent2.apply { consumePositionChange(0f, 1f) }
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[4].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[4].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent2.apply { consumePositionChange(0f, 4f) }
                )
            )
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[5].pointerInputFilter).isEqualTo(parent)
        PointerEventSubject
            .assertThat(log1[5].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 20f) },
                    expectedEvent2.apply { consumePositionChange(0f, 40f) }
                )
            )
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Main)

        assertThat(result.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent1.id])
            .isStructurallyEqualTo(
                expectedEvent1.apply { consumePositionChange(0f, 10f) }
            )
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent2.id])
            .isStructurallyEqualTo(
                expectedEvent2.apply { consumePositionChange(0f, 10f) }
            )
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_changesAreUpdatedCorrectly() {
        val log = mutableListOf<LogEntry>()
        val child1 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 1f
                                PointerEventPass.Main -> 4f
                                else -> 0f
                            }
                        it.consumePositionChange(
                            0f,
                            yConsume
                        )
                    }
                    pointerEvent.changes
                }
        )
        val child2 = PointerInputFilterMock(
            log = log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    pointerEvent.changes.map {
                        val yConsume =
                            when (pass) {
                                PointerEventPass.Initial -> 2f
                                PointerEventPass.Main -> 3f
                                else -> 0f
                            }
                        it.consumePositionChange(
                            0f,
                            yConsume
                        )
                    }
                    pointerEvent.changes
                }
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(child1, child2))
        hitPathTracker.addHitPath(PointerId(5), listOf(child1, child2))
        val actualEvent1 = down(3).moveTo(10.milliseconds, 0f, 0f)
        val actualEvent2 = down(5).moveTo(10.milliseconds, 0f, 0f)
        val expectedEvent1 = actualEvent1.deepCopy()
        val expectedEvent2 = actualEvent2.deepCopy()

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(actualEvent1, actualEvent2)
        )

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
                    expectedEvent1.apply { consumePositionChange(0f, 1f) },
                    expectedEvent2.apply { consumePositionChange(0f, 1f) }
                )
            )
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)

        assertThat(log1[2].pointerInputFilter).isEqualTo(child2)
        PointerEventSubject
            .assertThat(log1[2].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 2f) },
                    expectedEvent2.apply { consumePositionChange(0f, 2f) }
                )
            )
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[3].pointerInputFilter).isEqualTo(child1)
        PointerEventSubject
            .assertThat(log1[3].pointerEvent)
            .isStructurallyEqualTo(
                pointerEventOf(
                    expectedEvent1.apply { consumePositionChange(0f, 3f) },
                    expectedEvent2.apply { consumePositionChange(0f, 3f) }
                )
            )
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(result.changes).hasSize(2)
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent1.id])
            .isStructurallyEqualTo(
                expectedEvent1.apply { consumePositionChange(0f, 4f) }
            )
        PointerInputChangeSubject
            .assertThat(result.changes[actualEvent2.id])
            .isStructurallyEqualTo(
                expectedEvent2.apply { consumePositionChange(0f, 4f) }
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

        val pointerId = PointerId(3)

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf))

        hitPathTracker.removeHitPath(pointerId)

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
        val pointerId2 = PointerId(99)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))

        hitPathTracker.removeHitPath(pointerId2)

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

        hitPathTracker.removeHitPath(pointerId2)

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

        hitPathTracker.removeHitPath(pointerId1)

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

        hitPathTracker.removeHitPath(pointerId1)

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

        hitPathTracker.removeHitPath(pointerId2)

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

        hitPathTracker.removeHitPath(pointerId1)

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

        hitPathTracker.removeHitPath(pointerId2)

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

    @Test
    fun dispatchCustomEvent_1NodeItDispatches_nothingReceivesDispatch() {

        // Arrange

        lateinit var dispatcher: CustomEventDispatcher

        val pif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )

        hitPathTracker.addHitPath(PointerId(3), listOf(pif))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert

        assertThat(pif.log.getOnCustomEventLog()).hasSize(0)
    }

    @Test
    fun dispatchCustomEvent_1Path3NodesParentDispatches_dispatchCorrect() {
        dispatchCustomEvent_1Path3Nodes_dispatchCorrect(DispatchingPif.Parent)
    }

    @Test
    fun dispatchCustomEvent_1Path3NodesMiddleDispatches_dispatchCorrect() {
        dispatchCustomEvent_1Path3Nodes_dispatchCorrect(DispatchingPif.Middle)
    }

    @Test
    fun dispatchCustomEvent_1Path3NodesChildDispatches_dispatchCorrect() {
        dispatchCustomEvent_1Path3Nodes_dispatchCorrect(DispatchingPif.Child)
    }

    private enum class DispatchingPif {
        Parent, Middle, Child
    }

    private fun dispatchCustomEvent_1Path3Nodes_dispatchCorrect(
        dispatchingPif: DispatchingPif
    ) {
        // Arrange

        lateinit var dispatcher: CustomEventDispatcher
        lateinit var parentPif: PointerInputFilter
        lateinit var middlePif: PointerInputFilter
        lateinit var childPif: PointerInputFilter

        lateinit var seniorPif: PointerInputFilter
        lateinit var juniorPif: PointerInputFilter
        val dispatcherInitHandler: (CustomEventDispatcher) -> Unit = { dispatcher = it }

        val log = mutableListOf<LogEntry>()

        when (dispatchingPif) {
            DispatchingPif.Parent -> {
                parentPif = PointerInputFilterMock(
                    log,
                    initHandler = dispatcherInitHandler
                )
                middlePif = PointerInputFilterMock(log)
                seniorPif = middlePif
                childPif = PointerInputFilterMock(log)
                juniorPif = childPif
            }
            DispatchingPif.Middle -> {
                parentPif = PointerInputFilterMock(log)
                seniorPif = parentPif
                middlePif = PointerInputFilterMock(
                    log,
                    initHandler = dispatcherInitHandler
                )
                childPif = PointerInputFilterMock(log)
                juniorPif = childPif
            }
            DispatchingPif.Child -> {
                parentPif = PointerInputFilterMock(log)
                seniorPif = parentPif
                middlePif = PointerInputFilterMock(log)
                juniorPif = middlePif
                childPif = PointerInputFilterMock(
                    log,
                    initHandler = dispatcherInitHandler
                )
            }
        }

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPif, middlePif, childPif))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert

        val log1 = log.getOnCustomEventLog()

        assertThat(log1).hasSize(6)

        assertThat(log1[0].pointerInputFilter).isEqualTo(seniorPif)
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(juniorPif)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[2].pointerInputFilter).isEqualTo(juniorPif)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[3].pointerInputFilter).isEqualTo(seniorPif)
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[4].pointerInputFilter).isEqualTo(seniorPif)
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Final)
        assertThat(log1[5].pointerInputFilter).isEqualTo(juniorPif)
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Final)
    }

    @Test
    fun dispatchCustomEvent_1Parent2ChildrenParentDispatches_dispatchCorrect() {

        lateinit var dispatcher: CustomEventDispatcher

        val log = mutableListOf<LogEntry>()

        val parentPin = PointerInputFilterMock(
            log,
            initHandler = { dispatcher = it }
        )
        val childPin1 = PointerInputFilterMock(log)
        val childPin2 = PointerInputFilterMock(log)

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPin, childPin1))
        hitPathTracker.addHitPath(PointerId(4), listOf(parentPin, childPin2))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert

        val log1 = log.getOnCustomEventLog()

        assertThat(log1).hasSize(6)

        assertThat(log1[0].pointerInputFilter).isEqualTo(childPin1)
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(childPin1)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[2].pointerInputFilter).isEqualTo(childPin2)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[3].pointerInputFilter).isEqualTo(childPin2)
        assertThat(log1[3].pass).isEqualTo(PointerEventPass.Main)

        assertThat(log1[4].pointerInputFilter).isEqualTo(childPin1)
        assertThat(log1[4].pass).isEqualTo(PointerEventPass.Final)
        assertThat(log1[5].pointerInputFilter).isEqualTo(childPin2)
        assertThat(log1[5].pass).isEqualTo(PointerEventPass.Final)
    }

    @Test
    fun dispatchCustomEvent_1Parent2ChildrenChild1Dispatches_dispatchCorrect() {
        dispatchCustomEvent_1Parent2ChildrenChildDispatches_dispatchCorrect(
            true
        )
    }

    @Test
    fun dispatchCustomEvent_1Parent2ChildrenChild2Dispatches_dispatchCorrect() {
        dispatchCustomEvent_1Parent2ChildrenChildDispatches_dispatchCorrect(
            false
        )
    }

    private fun dispatchCustomEvent_1Parent2ChildrenChildDispatches_dispatchCorrect(
        firstChildDispatches: Boolean
    ) {
        // Arrange

        val log = mutableListOf<LogEntry>()

        val parentPif = PointerInputFilterMock(log)
        lateinit var childPif1: PointerInputFilter
        lateinit var childPif2: PointerInputFilter

        lateinit var dispatcher: CustomEventDispatcher
        val initHandler: (CustomEventDispatcher) -> Unit = { dispatcher = it }

        if (firstChildDispatches) {
            childPif1 = PointerInputFilterMock(
                log,
                initHandler = initHandler
            )
            childPif2 = PointerInputFilterMock(log)
        } else {
            childPif1 = PointerInputFilterMock(log)
            childPif2 = PointerInputFilterMock(
                log,
                initHandler = initHandler
            )
        }

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(4), listOf(parentPif, childPif2))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        val log1 = log.getOnCustomEventLog()

        assertThat(log1).hasSize(3)

        assertThat(log1[0].pointerInputFilter).isEqualTo(parentPif)
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(parentPif)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[2].pointerInputFilter).isEqualTo(parentPif)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Final)
    }

    // Tests related to reporting whether or not a pointer input filter was dispatched to.

    @Test
    fun dispatchChanges_noNodes_reportsWasDispatchedToNothing() {
        val (_, hitSomething) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(0)))
        assertThat(hitSomething).isFalse()
    }

    @Test
    fun dispatchChanges_1NodeDispatchToNode_reportsWasDispatchedToSomething() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        val (_, hitSomething) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        assertThat(hitSomething).isTrue()
    }

    @Test
    fun dispatchChanges_1NodeDispatchToDifferentNode_reportsWasDispatchedToNothing() {
        val pif = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        val (_, hitSomething) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(69)))

        assertThat(hitSomething).isFalse()
    }

    // Tests related to retaining and releasing hit paths.

    /**
     * Verifies that if a hit path is added and retained, when it is removed, it is not actually
     * removed.
     */
    @Test
    fun removeHitPath_idRetained_nodeIsRetained() {
        lateinit var dispatcher: CustomEventDispatcher
        val pif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))
        dispatcher.retainHitPaths(setOf(PointerId(13)))

        hitPathTracker.removeHitPath(PointerId(13))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif).apply {
                    pointerIds.add(PointerId(13))
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if there are 2 branching hit paths, 1 is retained, and both are removed, the
     * 1 that was retained remains.
     */
    @Test
    fun removeHitPath_2Branches1RetainedBothRemoved_retainedBranchRemains() {
        lateinit var dispatcher: CustomEventDispatcher
        val parentPif = PointerInputFilterMock()
        val childPif1 = PointerInputFilterMock()
        val childPif2 = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )

        hitPathTracker.addHitPath(PointerId(1), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(2), listOf(parentPif, childPif2))
        dispatcher.retainHitPaths(setOf(PointerId(1)))

        hitPathTracker.removeHitPath(PointerId(1))
        hitPathTracker.removeHitPath(PointerId(2))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(parentPif).apply {
                    pointerIds.add(PointerId(1))
                    children.add(
                        Node(childPif1).apply {
                            pointerIds.add(PointerId(1))
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if there are 2 branching hit paths, both are retained, and both are removed,
     * that both remain.
     */
    @Test
    fun removeHitPath_2Branches2RetainedBothRemoved_bothBranchesRemain() {
        lateinit var dispatcher: CustomEventDispatcher
        val parentPif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        val childPif1 = PointerInputFilterMock()
        val childPif2 = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(1), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(2), listOf(parentPif, childPif2))
        dispatcher.retainHitPaths(setOf(PointerId(1), PointerId(2)))

        hitPathTracker.removeHitPath(PointerId(1))
        hitPathTracker.removeHitPath(PointerId(2))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(parentPif).apply {
                    pointerIds.addAll(listOf(PointerId(1), PointerId(2)))
                    children.add(
                        Node(childPif1).apply {
                            pointerIds.add(PointerId(1))
                        }
                    )
                    children.add(
                        Node(childPif2).apply {
                            pointerIds.add(PointerId(2))
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if a hit path is retained, then removed, then released, it is removed.
     */
    @Test
    fun releasePointerId_idRetainedAndPathRemoved_nodeIsRemoved() {
        lateinit var dispatcher: CustomEventDispatcher
        val pif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))
        dispatcher.retainHitPaths(setOf(PointerId(13)))
        hitPathTracker.removeHitPath(PointerId(13))

        dispatcher.releaseHitPaths(setOf(PointerId(13)))

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    /**
     * Verifies that if there are 2 hit paths, both are retained, both are removed, and
     * then 1 is released, the other retained branch remains.
     */
    @Test
    fun releasePointerId_2Branches2RetainedBothRemoved1Released_correctBranchRemains() {
        lateinit var dispatcher: CustomEventDispatcher
        val parentPif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        val childPif1 = PointerInputFilterMock()
        val childPif2 = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(1), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(2), listOf(parentPif, childPif2))
        dispatcher.retainHitPaths(setOf(PointerId(1), PointerId(2)))

        hitPathTracker.removeHitPath(PointerId(1))
        hitPathTracker.removeHitPath(PointerId(2))

        dispatcher.releaseHitPaths(setOf(PointerId(1)))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(parentPif).apply {
                    pointerIds.add(PointerId(2))
                    children.add(
                        Node(childPif2).apply {
                            pointerIds.add(PointerId(2))
                        }
                    )
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if there are 2 hit paths, 1 is retained, they are both removed, and
     * then the one that was retained is released, that no branches remain.
     */
    @Test
    fun releasePointerId_2Branches1RetainedBothRemoved1Released_noBranchesRemain() {
        lateinit var dispatcher: CustomEventDispatcher
        val parentPif = PointerInputFilterMock()
        val childPif1 = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        val childPif2 = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(1), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(2), listOf(parentPif, childPif2))
        dispatcher.retainHitPaths(setOf(PointerId(1)))

        hitPathTracker.removeHitPath(PointerId(1))
        hitPathTracker.removeHitPath(PointerId(2))

        dispatcher.releaseHitPaths(setOf(PointerId(1)))

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
    }

    /**
     * Verifies that if a hit path is retained and then released (without it ever actually being
     * removed) then the hit path remains.
     */
    @Test
    fun releasePointerId_idRetainedButPathNotRemoved_pathNotRemoved() {
        lateinit var dispatcher: CustomEventDispatcher
        val pif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))
        dispatcher.retainHitPaths(setOf(PointerId(13)))

        dispatcher.releaseHitPaths(setOf(PointerId(13)))

        val expectedRoot = NodeParent().apply {
            children.add(
                Node(pif).apply {
                    pointerIds.add(PointerId(13))
                }
            )
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if a hit path is retained, and then removed, and a dispatch of a
     * custom event occurs, it will be dispatched to the retained path.
     */
    @Test
    fun dispatchCustomEvent_idRetainedAndPathRemoved_customEventReachesNode() {
        val log = mutableListOf<LogEntry>()
        lateinit var dispatcher: CustomEventDispatcher
        val pif = PointerInputFilterMock(
            log,
            initHandler = { dispatcher = it }
        )
        val pif2 = PointerInputFilterMock(log)
        hitPathTracker.addHitPath(PointerId(13), listOf(pif, pif2))
        dispatcher.retainHitPaths(setOf(PointerId(13)))
        hitPathTracker.removeHitPath(PointerId(13))
        val event = TestCustomEvent("87483")

        dispatcher.dispatchCustomEvent(event)

        val log1 = log.getOnCustomEventLog()
        assertThat(log1).hasSize(3)
        assertThat(log1[0].pointerInputFilter).isEqualTo(pif2)
        assertThat(log1[0].pass).isEqualTo(PointerEventPass.Initial)
        assertThat(log1[1].pointerInputFilter).isEqualTo(pif2)
        assertThat(log1[1].pass).isEqualTo(PointerEventPass.Main)
        assertThat(log1[2].pointerInputFilter).isEqualTo(pif2)
        assertThat(log1[2].pass).isEqualTo(PointerEventPass.Final)
    }

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
        val pif = PointerInputFilterMock(
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == removalPass) {
                        layoutCoordinates.isAttached = false
                    }
                    pointerEvent.changes
                },
            layoutCoordinates = layoutCoordinates
        )
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
        val parentPif = PointerInputFilterMock(
            log,
            pointerEventHandler =
                { pointerEvent, pass, _ ->
                    if (pass == removalPass) {
                        childLayoutCoordinates.isAttached = false
                    }
                    pointerEvent.changes
                }
        )
        val childPif = PointerInputFilterMock(
            log,
            layoutCoordinates = childLayoutCoordinates
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

        val actual = internalPointerEventOf(down(13, 120.milliseconds, 1.0f, 1.0f))
        val expected = pointerEventOf(down(13, 120.milliseconds, 1.0f, 1.0f))

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
    fun dispatchCustomMessage_pifRemovesSelfDuringInitial_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovesSelfDuringMain_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovesSelfDuringFinal_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchCustomMessage_pifRemovesSelfDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {

        val log = mutableListOf<LogEntry>()

        lateinit var dispatcher: CustomEventDispatcher

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(
            log,
            initHandler = { dispatcher = it }
        )
        val receivingPif = PointerInputFilterMock(
            log,
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            },
            layoutCoordinates = layoutCoordinates
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, receivingPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        val log1 = log.getOnCustomEventLog().filter { it.pointerInputFilter == receivingPif }
        val count = removalPass.ordinal + 1
        assertThat(log1).hasSize(count)
        PointerEventPass.values().forEachIndexed { index, pass ->
            if (index < count) {
                assertThat(log1[index].pass).isEqualTo(pass)
            }
        }
    }

    @Test
    fun dispatchCustomMessage_pifRemovedByParentDuringInitial_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovedByParentDuringMain_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovedByParentDuringFinal_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchCustomMessage_pifRemovedByParentDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {
        lateinit var dispatcher: CustomEventDispatcher

        val log = mutableListOf<LogEntry>()

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(
            log,
            initHandler = { dispatcher = it }
        )
        val parentPif = PointerInputFilterMock(
            log,
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            }
        )
        val childPif = PointerInputFilterMock(
            log,
            layoutCoordinates = layoutCoordinates
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, parentPif, childPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        val log1 = log.getOnCustomEventLog().filter { it.pointerInputFilter == childPif }
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
    fun dispatchCustomMessage_pifRemovedByChildDuringInitial_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Initial
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovedByChildDuringMain_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Main
        )
    }

    @Test
    fun dispatchCustomMessage_pifRemovedByChildDuringFinal_noPassesReceivedAfterwards() {
        dispatchCustomMessage_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
            PointerEventPass.Final
        )
    }

    private fun dispatchCustomMessage_pifRemovedByChildDuringDispatch_noPassesReceivedAfterwards(
        removalPass: PointerEventPass
    ) {
        lateinit var dispatcher: CustomEventDispatcher

        val log = mutableListOf<LogEntry>()

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(
            log,
            initHandler = { dispatcher = it }
        )
        val parentPif = PointerInputFilterMock(
            log,
            layoutCoordinates = layoutCoordinates
        )
        val childPif = PointerInputFilterMock(
            log,
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, parentPif, childPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        val log1 = log.getOnCustomEventLog().filter { it.pointerInputFilter == parentPif }
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

    private fun areEqual(actualNode: NodeParent, expectedNode: NodeParent): Boolean {
        var check = true

        if (actualNode.children.size != expectedNode.children.size) {
            return false
        }
        for (child in actualNode.children) {
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
        for (child in actualNode.children) {
            check = check && expectedNode.children.any {
                areEqual(child, it)
            }
        }

        return check
    }
}

internal data class TestCustomEvent(val value: String) : CustomEvent

class LayoutCoordinatesStub(
    override var isAttached: Boolean = true
) : LayoutCoordinates {

    var additionalOffset = Offset.Zero

    override val size: IntSize
        get() = IntSize(Constraints.Infinity, Constraints.Infinity)

    override val providedAlignmentLines: Set<AlignmentLine>
        get() = TODO("not implemented")

    override val parentCoordinates: LayoutCoordinates?
        get() = TODO("not implemented")

    override fun globalToLocal(global: Offset): Offset {
        TODO("not implemented")
    }

    override fun localToGlobal(local: Offset): Offset {
        assertThat(isAttached).isTrue()
        return local + additionalOffset
    }

    override fun localToRoot(local: Offset): Offset {
        TODO("not implemented")
    }

    override fun childToLocal(child: LayoutCoordinates, childLocal: Offset): Offset {
        TODO("not implemented")
    }

    override fun childBoundingBox(child: LayoutCoordinates): Rect {
        TODO("not implemented")
    }

    override fun get(line: AlignmentLine): Int {
        TODO("not implemented")
    }
}