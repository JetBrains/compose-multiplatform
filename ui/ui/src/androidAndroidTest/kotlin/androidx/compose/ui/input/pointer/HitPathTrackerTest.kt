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

import androidx.test.filters.SmallTest
import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class HitPathTrackerTest {

    private lateinit var hitPathTracker: HitPathTracker

    @Before
    fun setup() {
        hitPathTracker = HitPathTracker()
    }

    @Test
    fun addHitPath_emptyHitResult_resultIsCorrect() {
        val pif1: PointerInputFilter = mock()
        val pif2: PointerInputFilter = mock()
        val pif3: PointerInputFilter = mock()
        val pointerId = PointerId(1)

        hitPathTracker.addHitPath(pointerId, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(Node(pif1).apply {
                pointerIds.add(pointerId)
                children.add(Node(pif2).apply {
                    pointerIds.add(pointerId)
                    children.add(Node(pif3).apply {
                        pointerIds.add(pointerId)
                    })
                })
            })
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_existingNonMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = mock()
        val pif2: PointerInputFilter = mock()
        val pif3: PointerInputFilter = mock()
        val pif4: PointerInputFilter = mock()
        val pif5: PointerInputFilter = mock()
        val pif6: PointerInputFilter = mock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)

        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))
        hitPathTracker.addHitPath(pointerId2, listOf(pif4, pif5, pif6))

        val expectedRoot = NodeParent().apply {
            children.add(Node(pif1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(pif2).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(pif3).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
            children.add(Node(pif4).apply {
                pointerIds.add(pointerId2)
                children.add(Node(pif5).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(pif6).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_completeMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = mock()
        val pif2: PointerInputFilter = mock()
        val pif3: PointerInputFilter = mock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)
        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))

        hitPathTracker.addHitPath(pointerId2, listOf(pif1, pif2, pif3))

        val expectedRoot = NodeParent().apply {
            children.add(Node(pif1).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                children.add(Node(pif2).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    children.add(Node(pif3).apply {
                        pointerIds.add(pointerId1)
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_partiallyMatchingTree_resultIsCorrect() {
        val pif1: PointerInputFilter = mock()
        val pif2: PointerInputFilter = mock()
        val pif3: PointerInputFilter = mock()
        val pif4: PointerInputFilter = mock()
        val pif5: PointerInputFilter = mock()
        val pointerId1 = PointerId(1)
        val pointerId2 = PointerId(2)
        hitPathTracker.addHitPath(pointerId1, listOf(pif1, pif2, pif3))

        hitPathTracker.addHitPath(pointerId2, listOf(pif1, pif4, pif5))

        val expectedRoot = NodeParent().apply {
            children.add(Node(pif1).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                children.add(Node(pif2).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(pif3).apply {
                        pointerIds.add(pointerId1)
                    })
                })
                children.add(Node(pif4).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(pif5).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_1NodeAdded_initHandlerCalledWithValidCustomMessageDispatcher() {
        val pif: PointerInputFilter = mock()

        hitPathTracker.addHitPath(PointerId(3), listOf(pif))

        verify(pif).onInit(any())
    }

    @Test
    fun addHitPath_3NodesAdded_allIitHandlersCalledWithValidCustomMessageDispatcher() {
        val pifParent: PointerInputFilter = mock()
        val pifMiddle: PointerInputFilter = mock()
        val pifChild: PointerInputFilter = mock()

        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent, pifMiddle, pifChild))

        verify(pifParent).onInit(any())
        verify(pifMiddle).onInit(any())
        verify(pifChild).onInit(any())
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

        // Verify call count
        verify(pif, times(3)).onPointerEventMock(any(), any(), any())
        // Verify call values
        PointerEventPass.values().forEach {
            verify(pif).onPointerInputMock(
                eq(listOf(down(13))),
                eq(it),
                any() as Any
            )
        }
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatches_pointerInputHandlersCalledInCorrectOrder() {
        val pif1 = PointerInputFilterMock()
        val pif2 = PointerInputFilterMock()
        val pif3 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        // Verify call count
        verify(pif1, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif2, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif3, times(3)).onPointerEventMock(any(), any(), any())
        // Verify call order and values
        inOrder(pif1, pif2, pif3) {
            verify(pif1).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif2).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif3).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
        }
    }

    @Test
    fun dispatchChanges_hasDownAndUpPath_pointerInputHandlersCalledInCorrectOrder() {
        val pif1 = PointerInputFilterMock()
        val pif2 = PointerInputFilterMock()
        val pif3 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        // Verify call count
        verify(pif1, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif2, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif3, times(3)).onPointerEventMock(any(), any(), any())
        // Verify call order and values
        inOrder(pif1, pif2, pif3) {
            verify(pif1).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif2).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif3).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif3).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Main),
                any()
            )
            verify(pif2).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Main),
                any()
            )
            verify(pif1).onPointerInputMock(
                eq(listOf(down(13))),
                eq(PointerEventPass.Main),
                any()
            )
        }
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_eventsSplitCorrectlyAndCallOrderCorrect() {
        val pif1 = PointerInputFilterMock()
        val pif2 = PointerInputFilterMock()
        val pif3 = PointerInputFilterMock()
        val pif4 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pif1, pif2))
        hitPathTracker.addHitPath(PointerId(5), listOf(pif3, pif4))
        val event1 = down(3)
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        // Verify call count
        verify(pif1, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif2, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif3, times(3)).onPointerEventMock(any(), any(), any())
        verify(pif4, times(3)).onPointerEventMock(any(), any(), any())
        // Verify call order and values
        inOrder(pif1, pif2) {
            verify(pif1).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif2).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif2).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Main),
                any()
            )
            verify(pif1).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Main),
                any()
            )
        }
        inOrder(pif3, pif4) {
            verify(pif3).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif4).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(pif4).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Main),
                any()
            )
            verify(pif3).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_eventsSplitCorrectlyAndCallOrderCorrect() {
        val parent = PointerInputFilterMock()
        val child1 = PointerInputFilterMock()
        val child2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(parent, child1))
        hitPathTracker.addHitPath(PointerId(5), listOf(parent, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        // Verify call count
        verify(parent, times(3)).onPointerEventMock(any(), any(), any())
        verify(child1, times(3)).onPointerEventMock(any(), any(), any())
        verify(child2, times(3)).onPointerEventMock(any(), any(), any())

        // Verifies that the events traverse between parent and child1 in the correct order.
        inOrder(
            parent,
            child1
        ) {
            verify(parent).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child1).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child1).onPointerInputMock(
                eq(listOf(event1)),
                eq(PointerEventPass.Main),
                any()
            )
            verify(parent).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }

        // Verifies that the events traverse between parent and child2 in the correct order.
        inOrder(
            parent,
            child2
        ) {
            verify(parent).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child2).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child2).onPointerInputMock(
                eq(listOf(event2)),
                eq(PointerEventPass.Main),
                any()
            )
            verify(parent).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_eventsDoNotSplitAndCallOrderCorrect() {
        val child1 = PointerInputFilterMock()
        val child2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(child1, child2))
        hitPathTracker.addHitPath(PointerId(5), listOf(child1, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

        hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        // Verify call count
        verify(child1, times(3)).onPointerEventMock(any(), any(), any())
        verify(child2, times(3)).onPointerEventMock(any(), any(), any())

        // Verify that order is correct for child1.
        inOrder(
            child1
        ) {
            verify(child1).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child1).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }

        // Verify that order is correct for child2.
        inOrder(
            child2
        ) {
            verify(child2).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child2).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }

        // Verify that first pass hits child1 before second pass hits child2
        inOrder(
            child1,
            child2
        ) {
            verify(child1).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child2).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }

        // Verify that first pass hits child2 before second pass hits child1
        inOrder(
            child1,
            child2
        ) {
            verify(child2).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Initial),
                any()
            )
            verify(child1).onPointerInputMock(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.Main),
                any()
            )
        }
    }

    @Test
    fun dispatchChanges_noNodes_nothingChanges() {
        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(5)))

        assertThat(result.changes.values.first()).isEqualTo(down(5))
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_changesAreUpdatedCorrectly() {
        val pif1 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, _, _ ->
                changes.map { it.consumeDownChange() }
            })
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1))

        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        assertThat(result.changes.values.first()).isEqualTo(down(13).consumeDownChange())
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatchesAndDownAndUpPaths_changesAreUpdatedCorrectly() {
        val pif1 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 2f
                            PointerEventPass.Main -> 64f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        val pif2 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 4f
                            PointerEventPass.Main -> 32f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        val pif3 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 8f
                            PointerEventPass.Main -> 16f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif1, pif2, pif3))
        val change = down(13).moveTo(10.milliseconds, 0f, 130f)

        val (result, _) = hitPathTracker.dispatchChanges(internalPointerEventOf(change))

        verify(pif1).onPointerInputMock(
            eq(listOf(change)), eq(PointerEventPass.Initial), any()
        )
        verify(pif2).onPointerInputMock(
            eq(listOf(change.consumePositionChange(0f, 2f))),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif3).onPointerInputMock(
            eq(listOf(change.consumePositionChange(0f, 6f))), // 2 + 4
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif3).onPointerInputMock(
            eq(listOf(change.consumePositionChange(0f, 14f))), // 2 + 4 + 8
            eq(PointerEventPass.Main),
            any()
        )
        verify(pif2).onPointerInputMock(
            eq(listOf(change.consumePositionChange(0f, 30f))), // 2 + 4 + 8 + 16
            eq(PointerEventPass.Main),
            any()
        )
        verify(pif1).onPointerInputMock(
            eq(listOf(change.consumePositionChange(0f, 62f))), // 2 + 4 + 8 + 16 + 32
            eq(PointerEventPass.Main),
            any()
        )
        assertThat(result.changes.values.first())
            .isEqualTo(change.consumePositionChange(0f, 126f)) // 2 + 4 + 8 + 16 + 32 + 64
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_changesAreUpdatedCorrectly() {
        val pif1 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 2f
                            PointerEventPass.Main -> 12f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        val pif2 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 3f
                            PointerEventPass.Main -> 6f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        val pif3 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> -2f
                            PointerEventPass.Main -> -12f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        val pif4 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> -3f
                            PointerEventPass.Main -> -6f
                            else -> 0f
                        }
                    it.consumePositionChange(0f, yConsume)
                }
            })
        )
        hitPathTracker.addHitPath(PointerId(3), listOf(pif1, pif2))
        hitPathTracker.addHitPath(PointerId(5), listOf(pif3, pif4))
        val event1 = down(3).moveTo(10.milliseconds, 0f, 24f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -24f)

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        verify(pif1).onPointerInputMock(
            eq(listOf(event1)),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif2).onPointerInputMock(
            eq(listOf(event1.consumePositionChange(0f, 2f))),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif2).onPointerInputMock(
            eq(listOf(event1.consumePositionChange(0f, 5f))),
            eq(PointerEventPass.Main),
            any()
        )
        verify(pif1).onPointerInputMock(
            eq(listOf(event1.consumePositionChange(0f, 11f))),
            eq(PointerEventPass.Main),
            any()
        )

        verify(pif3).onPointerInputMock(
            eq(listOf(event2)),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif4).onPointerInputMock(
            eq(listOf(event2.consumePositionChange(0f, -2f))),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(pif4).onPointerInputMock(
            eq(listOf(event2.consumePositionChange(0f, -5f))),
            eq(PointerEventPass.Main),
            any()
        )
        verify(pif3).onPointerInputMock(
            eq(listOf(event2.consumePositionChange(0f, -11f))),
            eq(PointerEventPass.Main),
            any()
        )

        assertThat(result.changes).hasSize(2)
        assertThat(result.changes[event1.id]).isEqualTo(event1.consumePositionChange(0f, 23f))
        assertThat(result.changes[event2.id]).isEqualTo(event2.consumePositionChange(0f, -23f))
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_changesAreUpdatedCorrectly() {
        val parent = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 2
                            PointerEventPass.Main -> 3
                            else -> Int.MAX_VALUE
                        }
                    it.consumePositionChange(
                        0f,
                        (it.positionChange().y.roundToInt() / yConsume).toFloat()
                    )
                }
            })
        )
        val child1 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 5
                            PointerEventPass.Main -> 7
                            else -> Int.MAX_VALUE
                        }
                    it.consumePositionChange(
                        0f,
                        (it.positionChange().y.roundToInt() / yConsume).toFloat()
                    )
                }
            })
        )
        val child2 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 11
                            PointerEventPass.Main -> 13
                            else -> Int.MAX_VALUE
                        }
                    it.consumePositionChange(
                        0f,
                        (it.positionChange().y.roundToInt() / yConsume).toFloat()
                    )
                }
            })
        )
        hitPathTracker.addHitPath(PointerId(3), listOf(parent, child1))
        hitPathTracker.addHitPath(PointerId(5), listOf(parent, child2))
        val event1 = down(3).moveTo(10.milliseconds, 0f, 1000f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -1000f)

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        verify(parent).onPointerInputMock(
            eq(listOf(event1, event2)),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(child1).onPointerInputMock(
            eq(listOf(event1.consumePositionChange(0f, 500f))),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(child2).onPointerInputMock(
            eq(listOf(event2.consumePositionChange(0f, -500f))),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(child1).onPointerInputMock(
            eq(listOf(event1.consumePositionChange(0f, 600f))),
            eq(PointerEventPass.Main),
            any()
        )
        verify(child2).onPointerInputMock(
            eq(listOf(event2.consumePositionChange(0f, -545f))),
            eq(PointerEventPass.Main),
            any()
        )
        verify(parent).onPointerInputMock(
            eq(
                listOf(
                    event1.consumePositionChange(0f, 657f),
                    event2.consumePositionChange(0f, -580f)
                )
            ),
            eq(PointerEventPass.Main),
            any()
        )

        assertThat(result.changes).hasSize(2)
        assertThat(result.changes[event1.id]).isEqualTo(event1.consumePositionChange(0f, 771f))
        assertThat(result.changes[event2.id]).isEqualTo(event2.consumePositionChange(0f, -720f))
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_changesAreUpdatedCorrectly() {
        val child1 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 2
                            PointerEventPass.Main -> 3
                            else -> Int.MAX_VALUE
                        }
                    it.consumePositionChange(
                        0f,
                        (it.positionChange().y.roundToInt() / yConsume).toFloat()
                    )
                }
            })
        )
        val child2 = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                changes.map {
                    val yConsume =
                        when (pass) {
                            PointerEventPass.Initial -> 5
                            PointerEventPass.Main -> 7
                            else -> Int.MAX_VALUE
                        }
                    it.consumePositionChange(
                        0f,
                        (it.positionChange().y.roundToInt() / yConsume).toFloat()
                    )
                }
            })
        )
        hitPathTracker.addHitPath(PointerId(3), listOf(child1, child2))
        hitPathTracker.addHitPath(PointerId(5), listOf(child1, child2))
        val event1 = down(3).moveTo(10.milliseconds, 0f, 1000f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -1000f)

        val (result, _) = hitPathTracker.dispatchChanges(
            internalPointerEventOf(event1, event2)
        )

        verify(child1).onPointerInputMock(
            eq(listOf(event1, event2)),
            eq(PointerEventPass.Initial),
            any()
        )
        verify(child2).onPointerInputMock(
            eq(
                listOf(
                    event1.consumePositionChange(0f, 500f),
                    event2.consumePositionChange(0f, -500f)
                )
            ),
            eq(PointerEventPass.Initial),
            any()
        )

        verify(child2).onPointerInputMock(
            eq(
                listOf(
                    event1.consumePositionChange(0f, 600f),
                    event2.consumePositionChange(0f, -600f)
                )
            ),
            eq(PointerEventPass.Main),
            any()
        )
        verify(child1).onPointerInputMock(
            eq(
                listOf(
                    event1.consumePositionChange(0f, 657f),
                    event2.consumePositionChange(0f, -657f)
                )
            ),
            eq(PointerEventPass.Main),
            any()
        )

        assertThat(result.changes).hasSize(2)
        assertThat(result.changes[event1.id]).isEqualTo(event1.consumePositionChange(0f, 771f))
        assertThat(result.changes[event2.id]).isEqualTo(event2.consumePositionChange(0f, -771f))
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

        val pif1 = PointerInputFilterMock()
        val pif2 = PointerInputFilterMock()
        val pif3 = PointerInputFilterMock()
        val pif4 = PointerInputFilterMock()
        val pif5 = PointerInputFilterMock()
        val pif6 = PointerInputFilterMock()
        val pif7 = PointerInputFilterMock()
        val pif8 = PointerInputFilterMock()
        val pif9 = PointerInputFilterMock()

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
            children.add(Node(pif1).apply {
                pointerIds.add(pointerId1)
            })
            children.add(Node(pif3).apply {
                pointerIds.add(pointerId2)
                children.add(Node(pif2).apply {
                    pointerIds.add(pointerId2)
                })
            })
            children.add(Node(pif6).apply {
                pointerIds.add(pointerId3)
                children.add(Node(pif5).apply {
                    pointerIds.add(pointerId3)
                    children.add(Node(pif4).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
            children.add(Node(pif9).apply {
                pointerIds.add(pointerId4)
                pointerIds.add(pointerId5)
                children.add(Node(pif7).apply {
                    pointerIds.add(pointerId4)
                })
                children.add(Node(pif8).apply {
                    pointerIds.add(pointerId5)
                })
            })
        }
        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        verify(pif1, never()).onCancel()
        verify(pif2, never()).onCancel()
        verify(pif3, never()).onCancel()
        verify(pif4, never()).onCancel()
        verify(pif5, never()).onCancel()
        verify(pif7, never()).onCancel()
        verify(pif8, never()).onCancel()
        verify(pif9, never()).onCancel()
    }

    //  compositionRoot, root -> middle -> leaf
    @Test
    fun removeDetachedPointerInputFilters_1PathRootDetached_allRemovedAndCorrectCancels() {
        val root = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        hitPathTracker.addHitPath(PointerId(0), listOf(root, middle, leaf))

        hitPathTracker.removeDetachedPointerInputFilters()

        assertThat(areEqual(hitPathTracker.root, NodeParent())).isTrue()
        inOrder(leaf, middle, root) {
            verify(leaf).onCancel()
            verify(middle).onCancel()
            verify(root).onCancel()
        }
    }

    //  compositionRoot -> root, middle -> child
    @Test
    fun removeDetachedPointerInputFilters_1PathMiddleDetached_removesAndCancelsCorrect() {
        val root = PointerInputFilterMock()
        val middle = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val child = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId = PointerId(0)
        hitPathTracker.addHitPath(pointerId, listOf(root, middle, child))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId)
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(child, middle) {
            verify(child).onCancel()
            verify(middle).onCancel()
        }
        verify(root, never()).onCancel()
    }

    //  compositionRoot -> root -> middle, leaf
    @Test
    fun removeDetachedPointerInputFilters_1PathLeafDetached_removesAndCancelsCorrect() {
        val root = PointerInputFilterMock()
        val middle = PointerInputFilterMock()
        val leaf = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId = PointerId(0)
        hitPathTracker.addHitPath(pointerId, listOf(root, middle, leaf))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf).onCancel()
        verify(middle, never()).onCancel()
        verify(root, never()).onCancel()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1Detached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock()
        val leaf1 = PointerInputFilterMock()

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock()

        val root3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        verify(leaf1, never()).onCancel()
        verify(middle1, never()).onCancel()
        verify(root1, never()).onCancel()
        verify(leaf2, never()).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        inOrder(leaf3, middle3, root3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
            verify(root3).onCancel()
        }
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1MiddleDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

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
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
        }
        verify(root1, never()).onCancel()
        verify(leaf2, never()).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        verify(leaf3, never()).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots1LeafDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock()
        val leaf1 = PointerInputFilterMock()

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

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
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1, never()).onCancel()
        verify(middle1, never()).onCancel()
        verify(root1, never()).onCancel()
        verify(leaf2).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        verify(leaf3, never()).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2Detached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock()

        val root3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        inOrder(leaf1, middle1, root1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
            verify(root1).onCancel()
        }
        verify(leaf2, never()).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        inOrder(leaf3, middle3, root3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
            verify(root3).onCancel()
        }
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2MiddlesDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

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
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()

        inOrder(leaf1, middle1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
        }
        verify(root1, never()).onCancel()
        inOrder(leaf2, middle2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
        }
        verify(root2, never()).onCancel()
        verify(leaf3, never()).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots2LeafsDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock()
        val leaf1 = PointerInputFilterMock()

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1, never()).onCancel()
        verify(middle1, never()).onCancel()
        verify(root1, never()).onCancel()
        verify(leaf2).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        verify(leaf3).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot, root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3Detached_allRemovedAndCancelsCorrect() {
        val root1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        hitPathTracker.addHitPath(PointerId(3), listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent()

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1, root1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
            verify(root1).onCancel()
        }
        inOrder(leaf2, middle2, root2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
            verify(root2).onCancel()
        }
        inOrder(leaf3, middle3, root3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
            verify(root3).onCancel()
        }
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3, middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3MiddlesDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
        }
        verify(root1, never()).onCancel()
        inOrder(leaf2, middle2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
        }
        verify(root2, never()).onCancel()
        inOrder(leaf3, middle3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
        }
        verify(root3, never()).onCancel()
    }

    //  compositionRoot -> root1 -> middle1, leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3Roots3LeafsDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock()
        val middle1 = PointerInputFilterMock()
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1).onCancel()
        verify(middle1, never()).onCancel()
        verify(root1, never()).onCancel()
        verify(leaf2).onCancel()
        verify(middle2, never()).onCancel()
        verify(root2, never()).onCancel()
        verify(leaf3).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    // compositionRoot, root1 -> middle1 -> leaf1
    // compositionRoot -> root2, middle2, leaf2
    // compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputFilters_3RootsStaggeredDetached_removesAndCancelsCorrect() {

        val root1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root2 = PointerInputFilterMock()
        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val root3 = PointerInputFilterMock()
        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root3, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root2).apply {
                pointerIds.add(pointerId2)
            })
            children.add(Node(root3).apply {
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1, root1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
            verify(root1).onCancel()
        }
        inOrder(leaf2, middle2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
        }
        verify(root2, never()).onCancel()
        verify(leaf3).onCancel()
        verify(middle3, never()).onCancel()
        verify(root3, never()).onCancel()
    }

    // compositionRoot, root ->
    //   middle1 -> leaf1
    //   middle2 -> leaf2
    //   middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3MiddlesDetached_allRemovedAndCorrectCancels() {
        val root = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent()

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1, root) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
            verify(root).onCancel()
        }
        inOrder(leaf2, middle2, root) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
            verify(root).onCancel()
        }
        inOrder(leaf3, middle3, root) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
            verify(root).onCancel()
        }
    }

    // compositionRoot -> root
    //   -> middle1 -> leaf1
    //   -> middle2 -> leaf2
    //   , middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3Middles1Detached_removesAndCancelsCorrect() {

        val root = PointerInputFilterMock()

        val middle1 = PointerInputFilterMock()
        val leaf1 = PointerInputFilterMock()

        val middle2 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock()

        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                })
                children.add(Node(middle2).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf3, middle3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
        }
        verify(leaf2, never()).onCancel()
        verify(middle2, never()).onCancel()
        verify(leaf1, never()).onCancel()
        verify(middle1, never()).onCancel()
        verify(root, never()).onCancel()
    }

    // compositionRoot -> root
    //   , middle1 -> leaf1
    //   , middle2 -> leaf2
    //   -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3Middles2Detached_removesAndCancelsCorrect() {

        val root = PointerInputFilterMock()

        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle3 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
                children.add(Node(middle3).apply {
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
        }
        inOrder(leaf2, middle2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
        }
        verify(leaf3, never()).onCancel()
        verify(middle3, never()).onCancel()
        verify(root, never()).onCancel()
    }

    // compositionRoot -> root
    //   , middle1 -> leaf1
    //   , middle2 -> leaf2
    //   , middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_rootWith3MiddlesAllDetached_allMiddlesRemoved() {

        val root = PointerInputFilterMock()

        val middle1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val middle3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle2, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle3, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        inOrder(leaf1, middle1) {
            verify(leaf1).onCancel()
            verify(middle1).onCancel()
        }
        inOrder(leaf2, middle2) {
            verify(leaf2).onCancel()
            verify(middle2).onCancel()
        }
        inOrder(leaf3, middle3) {
            verify(leaf3).onCancel()
            verify(middle3).onCancel()
        }
        verify(root, never()).onCancel()
    }

    // compositionRoot -> root -> middle
    //   -> leaf1
    //   , leaf2
    //   -> leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3Leafs1Detached_correctLeafRemoved() {

        val root = PointerInputFilterMock()

        val middle = PointerInputFilterMock()

        val leaf1 = PointerInputFilterMock()
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(pointerId3, listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                    children.add(Node(leaf3).apply {
                        pointerIds.add(pointerId3)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1, never()).onCancel()
        verify(leaf2).onCancel()
        verify(leaf3, never()).onCancel()
        verify(middle, never()).onCancel()
        verify(root, never()).onCancel()
    }

    // compositionRoot -> root -> middle
    //   , leaf1
    //   -> leaf2
    //   , leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3Leafs2Detached_correctLeafsRemoved() {

        val root = PointerInputFilterMock()

        val middle = PointerInputFilterMock()

        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock()
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1).onCancel()
        verify(leaf2, never()).onCancel()
        verify(leaf3).onCancel()
        verify(middle, never()).onCancel()
        verify(root, never()).onCancel()
    }

    // compositionRoot -> root -> middle
    //   , leaf1
    //   , leaf2
    //   , leaf3
    @Test
    fun removeDetachedPointerInputFilters_middleWith3LeafsAllDetached_allLeafsRemoved() {

        val root = PointerInputFilterMock()

        val middle = PointerInputFilterMock()

        val leaf1 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf2 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))
        val leaf3 = PointerInputFilterMock(layoutCoordinates = LayoutCoordinatesStub(false))

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)
        val pointerId3 = PointerId(7)

        hitPathTracker.addHitPath(PointerId(3), listOf(root, middle, leaf1))
        hitPathTracker.addHitPath(PointerId(5), listOf(root, middle, leaf2))
        hitPathTracker.addHitPath(PointerId(7), listOf(root, middle, leaf3))

        hitPathTracker.removeDetachedPointerInputFilters()

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                pointerIds.add(pointerId2)
                pointerIds.add(pointerId3)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    pointerIds.add(pointerId2)
                    pointerIds.add(pointerId3)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
        verify(leaf1).onCancel()
        verify(leaf2).onCancel()
        verify(leaf3).onCancel()
        verify(middle, never()).onCancel()
        verify(root, never()).onCancel()
    }

    // arrange: root(3) -> middle(3) -> leaf(3)
    // act: 3 is removed
    // assert: no path
    @Test
    fun removeHitPath_onePathPointerIdRemoved_hitTestResultIsEmpty() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

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
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(99)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))

        hitPathTracker.removeHitPath(pointerId2)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
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
        val root1: PointerInputFilter = mock()
        val middle1: PointerInputFilter = mock()
        val leaf1: PointerInputFilter = mock()

        val root2: PointerInputFilter = mock()
        val middle2: PointerInputFilter = mock()
        val leaf2: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root1, middle1, leaf1))
        hitPathTracker.addHitPath(pointerId2, listOf(root2, middle2, leaf2))

        hitPathTracker.removeHitPath(pointerId2)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root1).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle1).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // root(3,5) -> middle(3,5) -> leaf(3,5)
    // 3 is removed
    // root(5) -> middle(5) -> leaf(5)
    @Test
    fun removeHitPath_2PathsShareNodes1PointerIdRemoved_resultContainsRemainingPath() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle, leaf))

        hitPathTracker.removeHitPath(pointerId1)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId2)
                    children.add(Node(leaf).apply {
                        pointerIds.add(pointerId2)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5) -> middle(5)
    @Test
    fun removeHitPath_2PathsShare2NodesLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle))

        hitPathTracker.removeHitPath(pointerId1)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId2)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId2)
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_2PathsShare2NodesShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root, middle))

        hitPathTracker.removeHitPath(pointerId2)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5)
    @Test
    fun removeHitPath_2PathsShare1NodeLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root))

        hitPathTracker.removeHitPath(pointerId1)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId2)
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removeHitPath_2PathsShare1NodeShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root: PointerInputFilter = mock()
        val middle: PointerInputFilter = mock()
        val leaf: PointerInputFilter = mock()

        val pointerId1 = PointerId(3)
        val pointerId2 = PointerId(5)

        hitPathTracker.addHitPath(pointerId1, listOf(root, middle, leaf))
        hitPathTracker.addHitPath(pointerId2, listOf(root))

        hitPathTracker.removeHitPath(pointerId2)

        val expectedRoot = NodeParent().apply {
            children.add(Node(root).apply {
                pointerIds.add(pointerId1)
                children.add(Node(middle).apply {
                    pointerIds.add(pointerId1)
                    children.add(Node(leaf).apply {
                        pointerIds.add(pointerId1)
                    })
                })
            })
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

        verify(pif).onCancel()
    }

    // Pin -> Pin -> Pin
    @Test
    fun processCancel_3Pins_cancelHandlersCalledOnceInOrder() {
        val childPif = PointerInputFilterMock()
        val middlePif = PointerInputFilterMock()
        val parentPif = PointerInputFilterMock()
        hitPathTracker.addHitPath(
            PointerId(3),
            listOf(parentPif, middlePif, childPif)
        )

        hitPathTracker.processCancel()

        inOrder(
            parentPif,
            middlePif,
            childPif
        ) {
            verify(childPif).onCancel()
            verify(middlePif).onCancel()
            verify(parentPif).onCancel()
        }
    }

    // PIN -> PIN
    // PIN -> PIN
    @Test
    fun processCancel_2IndependentPathsFromRoot_cancelHandlersCalledOnceInOrder() {
        val pifParent1 = PointerInputFilterMock()
        val pifChild1 = PointerInputFilterMock()
        val pifParent2 = PointerInputFilterMock()
        val pifChild2 = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent1, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent2, pifChild2))

        hitPathTracker.processCancel()

        inOrder(pifParent1, pifChild1) {
            verify(pifChild1).onCancel()
            verify(pifParent1).onCancel()
        }
        inOrder(pifParent2, pifChild2) {
            verify(pifChild2).onCancel()
            verify(pifParent2).onCancel()
        }
    }

    // PIN -> PIN
    //     -> PIN
    @Test
    fun processCancel_2BranchingPaths_cancelHandlersCalledOnceInOrder() {
        val pifParent = PointerInputFilterMock()
        val pifChild1 = PointerInputFilterMock()
        val pifChild2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(3), listOf(pifParent, pifChild1))
        hitPathTracker.addHitPath(PointerId(5), listOf(pifParent, pifChild2))

        hitPathTracker.processCancel()

        inOrder(pifParent, pifChild1) {
            verify(pifChild1).onCancel()
            verify(pifParent).onCancel()
        }
        inOrder(pifParent, pifChild2) {
            verify(pifChild2).onCancel()
            verify(pifParent).onCancel()
        }
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

        verify(pif, never()).onCustomEvent(any(), any())
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

        lateinit var pifThatDispatches: PointerInputFilter
        lateinit var seniorPif: PointerInputFilter
        lateinit var juniorPif: PointerInputFilter
        val dispatcherInitHandler: (CustomEventDispatcher) -> Unit = { dispatcher = it }

        when (dispatchingPif) {
            DispatchingPif.Parent -> {
                parentPif = PointerInputFilterMock(
                    initHandler = dispatcherInitHandler
                )
                pifThatDispatches = parentPif
                middlePif = PointerInputFilterMock()
                seniorPif = middlePif
                childPif = PointerInputFilterMock()
                juniorPif = childPif
            }
            DispatchingPif.Middle -> {
                parentPif = PointerInputFilterMock()
                seniorPif = parentPif
                middlePif = PointerInputFilterMock(
                    initHandler = dispatcherInitHandler
                )
                pifThatDispatches = middlePif
                childPif = PointerInputFilterMock()
                juniorPif = childPif
            }
            DispatchingPif.Child -> {
                parentPif = PointerInputFilterMock()
                seniorPif = parentPif
                middlePif = PointerInputFilterMock()
                juniorPif = middlePif
                childPif = PointerInputFilterMock(
                    initHandler = dispatcherInitHandler
                )
                pifThatDispatches = childPif
            }
        }

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPif, middlePif, childPif))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert

        verify(seniorPif, times(3)).onCustomEvent(any(), any())
        verify(juniorPif, times(3)).onCustomEvent(any(), any())
        inOrder(seniorPif, juniorPif) {
            verify(seniorPif).onCustomEvent(event, PointerEventPass.Initial)
            verify(juniorPif).onCustomEvent(event, PointerEventPass.Initial)
            verify(juniorPif).onCustomEvent(event, PointerEventPass.Main)
            verify(seniorPif).onCustomEvent(event, PointerEventPass.Main)
            verify(seniorPif).onCustomEvent(event, PointerEventPass.Final)
            verify(juniorPif).onCustomEvent(event, PointerEventPass.Final)
        }
        verify(pifThatDispatches, never()).onCustomEvent(any(), any())
    }

    @Test
    fun dispatchCustomEvent_1Parent2ChildrenParentDispatches_dispatchCorrect() {

        lateinit var dispatcher: CustomEventDispatcher

        val parentPin = PointerInputFilterMock(initHandler = { dispatcher = it })
        val childPin1 = PointerInputFilterMock()
        val childPin2 = PointerInputFilterMock()

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPin, childPin1))
        hitPathTracker.addHitPath(PointerId(4), listOf(parentPin, childPin2))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert
        inOrder(childPin1) {
            verify(childPin1).onCustomEvent(event, PointerEventPass.Initial)
            verify(childPin1).onCustomEvent(event, PointerEventPass.Main)
            verify(childPin1).onCustomEvent(event, PointerEventPass.Final)
        }
        inOrder(childPin2) {
            verify(childPin2).onCustomEvent(event, PointerEventPass.Initial)
            verify(childPin2).onCustomEvent(event, PointerEventPass.Main)
            verify(childPin2).onCustomEvent(event, PointerEventPass.Final)
        }
        verify(parentPin, never()).onCustomEvent(any(), any())
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

        val parentPif = PointerInputFilterMock()
        lateinit var childPif1: PointerInputFilter
        lateinit var childPif2: PointerInputFilter

        lateinit var dispatcher: CustomEventDispatcher
        val initHandler: (CustomEventDispatcher) -> Unit = { dispatcher = it }

        if (firstChildDispatches) {
            childPif1 = PointerInputFilterMock(
                initHandler = initHandler
            )
            childPif2 = PointerInputFilterMock()
        } else {
            childPif1 = PointerInputFilterMock()
            childPif2 = PointerInputFilterMock(
                initHandler = initHandler
            )
        }

        hitPathTracker.addHitPath(PointerId(3), listOf(parentPif, childPif1))
        hitPathTracker.addHitPath(PointerId(4), listOf(parentPif, childPif2))

        val event = TestCustomEvent("test")

        // Act

        dispatcher.dispatchCustomEvent(event)

        // Assert
        inOrder(parentPif) {
            verify(parentPif).onCustomEvent(event, PointerEventPass.Initial)
            verify(parentPif).onCustomEvent(event, PointerEventPass.Main)
            verify(parentPif).onCustomEvent(event, PointerEventPass.Final)
        }
        verify(childPif1, never()).onCustomEvent(any(), any())
        verify(childPif1, never()).onCustomEvent(any(), any())
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
            children.add(Node(pif).apply {
                pointerIds.add(PointerId(13))
            })
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
            children.add(Node(parentPif).apply {
                pointerIds.add(PointerId(1))
                children.add(Node(childPif1).apply {
                    pointerIds.add(PointerId(1))
                })
            })
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
            children.add(Node(parentPif).apply {
                pointerIds.addAll(listOf(PointerId(1), PointerId(2)))
                children.add(Node(childPif1).apply {
                    pointerIds.add(PointerId(1))
                })
                children.add(Node(childPif2).apply {
                    pointerIds.add(PointerId(2))
                })
            })
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
            children.add(Node(parentPif).apply {
                pointerIds.add(PointerId(2))
                children.add(Node(childPif2).apply {
                    pointerIds.add(PointerId(2))
                })
            })
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
            children.add(Node(pif).apply {
                pointerIds.add(PointerId(13))
            })
        }

        assertThat(areEqual(hitPathTracker.root, expectedRoot)).isTrue()
    }

    /**
     * Verifies that if a hit path is retained, and then removed, and a dispatch of a
     * custom event occurs, it will be dispatched to the retained path.
     */
    @Test
    fun dispatchCustomEvent_idRetainedAndPathRemoved_customEventReachesNode() {
        lateinit var dispatcher: CustomEventDispatcher
        val pif = PointerInputFilterMock(
            initHandler = { dispatcher = it }
        )
        val pif2 = PointerInputFilterMock()
        hitPathTracker.addHitPath(PointerId(13), listOf(pif, pif2))
        dispatcher.retainHitPaths(setOf(PointerId(13)))
        hitPathTracker.removeHitPath(PointerId(13))
        val event = TestCustomEvent("87483")

        dispatcher.dispatchCustomEvent(event)

        inOrder(pif2) {
            verify(pif2).onCustomEvent(event, PointerEventPass.Initial)
            verify(pif2).onCustomEvent(event, PointerEventPass.Main)
            verify(pif2).onCustomEvent(event, PointerEventPass.Final)
        }
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
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                if (pass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
                changes
            }),
            layoutCoordinates = layoutCoordinates
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(pif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            if (!passedRemovalPass) {
                verify(pif).onPointerEventMock(any(), eq(it), any())
                passedRemovalPass = it == removalPass
            } else {
                verify(pif, never()).onPointerEventMock(any(), eq(it), any())
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
        val childLayoutCoordinates = LayoutCoordinatesStub(true)
        val parentPif = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                if (pass == removalPass) {
                    childLayoutCoordinates.isAttached = false
                }
                changes
            })
        )
        val childPif = PointerInputFilterMock(
            layoutCoordinates = childLayoutCoordinates
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(parentPif, childPif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val removalPassIsDown =
            when (removalPass) {
                PointerEventPass.Initial -> true
                PointerEventPass.Final -> true
                else -> false
            }
        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            passedRemovalPass = passedRemovalPass || removalPassIsDown && it == removalPass
            if (!passedRemovalPass) {
                verify(childPif).onPointerEventMock(any(), eq(it), any())
            } else {
                verify(childPif, never()).onPointerEventMock(any(), eq(it), any())
            }
            passedRemovalPass = passedRemovalPass || !removalPassIsDown && it == removalPass
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
        val parentLayoutCoordinates = LayoutCoordinatesStub(true)
        val parentPif = PointerInputFilterMock(
            layoutCoordinates = parentLayoutCoordinates
        )
        val childPif = PointerInputFilterMock(
            pointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                if (pass == removalPass) {
                    parentLayoutCoordinates.isAttached = false
                }
                changes
            })
        )
        hitPathTracker.addHitPath(PointerId(13), listOf(parentPif, childPif))

        hitPathTracker.dispatchChanges(internalPointerEventOf(down(13)))

        val removalPassIsDown =
            when (removalPass) {
                PointerEventPass.Initial -> true
                PointerEventPass.Final -> true
                else -> false
            }
        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            passedRemovalPass = passedRemovalPass || !removalPassIsDown && it == removalPass
            if (!passedRemovalPass) {
                verify(parentPif).onPointerEventMock(any(), eq(it), any())
            } else {
                verify(parentPif, never()).onPointerEventMock(any(), eq(it), any())
            }
            passedRemovalPass = passedRemovalPass || removalPassIsDown && it == removalPass
        }
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

        lateinit var dispatcher: CustomEventDispatcher

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(initHandler = { dispatcher = it })
        val receivingPif = PointerInputFilterMock(
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            },
            layoutCoordinates = layoutCoordinates
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, receivingPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            if (!passedRemovalPass) {
                verify(receivingPif).onCustomEvent(any(), eq(it))
                passedRemovalPass = it == removalPass
            } else {
                verify(receivingPif, never()).onCustomEvent(any(), eq(it))
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

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(initHandler = { dispatcher = it })
        val parentPif = PointerInputFilterMock(
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            }
        )
        val childPif = PointerInputFilterMock(
            layoutCoordinates = layoutCoordinates
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, parentPif, childPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        val removalPassIsDown =
            when (removalPass) {
                PointerEventPass.Initial -> true
                PointerEventPass.Final -> true
                else -> false
            }
        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            passedRemovalPass = passedRemovalPass || removalPassIsDown && it == removalPass
            if (!passedRemovalPass) {
                verify(childPif).onCustomEvent(any(), eq(it))
            } else {
                verify(childPif, never()).onCustomEvent(any(), eq(it))
            }
            passedRemovalPass = passedRemovalPass || !removalPassIsDown && it == removalPass
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

        val layoutCoordinates = LayoutCoordinatesStub(true)

        val dispatchingPif = PointerInputFilterMock(initHandler = { dispatcher = it })
        val parentPif = PointerInputFilterMock(
            layoutCoordinates = layoutCoordinates
        )
        val childPif = PointerInputFilterMock(
            onCustomEvent = { _, pointerEventPass ->
                if (pointerEventPass == removalPass) {
                    layoutCoordinates.isAttached = false
                }
            }
        )

        hitPathTracker.addHitPath(PointerId(13), listOf(dispatchingPif, parentPif, childPif))

        dispatcher.dispatchCustomEvent(object : CustomEvent {})

        val removalPassIsDown =
            when (removalPass) {
                PointerEventPass.Initial -> true
                PointerEventPass.Final -> true
                else -> false
            }
        var passedRemovalPass = false
        PointerEventPass.values().forEach {
            passedRemovalPass = passedRemovalPass || !removalPassIsDown && it == removalPass
            if (!passedRemovalPass) {
                verify(parentPif).onCustomEvent(any(), eq(it))
            } else {
                verify(parentPif, never()).onCustomEvent(any(), eq(it))
            }
            passedRemovalPass = passedRemovalPass || removalPassIsDown && it == removalPass
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

fun PointerInputFilterMock(
    initHandler: (CustomEventDispatcher) -> Unit = mock(),
    pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler()),
    layoutCoordinates: LayoutCoordinates = LayoutCoordinatesStub(true),
    onCustomEvent: (CustomEvent, PointerEventPass) -> Unit = mock()
): PointerInputFilterStub =
    spy(
        PointerInputFilterStub(
            pointerInputHandler,
            initHandler,
            onCustomEvent
        ).apply {
            this.layoutCoordinates = layoutCoordinates
        }
    )

open class PointerInputFilterStub(
    val pointerInputHandler: PointerInputHandler,
    val initHandler: (CustomEventDispatcher) -> Unit,
    val customEventHandler: (CustomEvent, PointerEventPass) -> Unit
) : PointerInputFilter() {

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntSize
    ): List<PointerInputChange> {
        return onPointerInputMock(changes, pass, bounds as Any)
    }

    open fun onPointerInputMock(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: Any
    ): List<PointerInputChange> {
        return pointerInputHandler(changes, pass, bounds as IntSize)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ): List<PointerInputChange> {
        return onPointerEventMock(pointerEvent, pass, bounds as Any)
    }

    open fun onPointerEventMock(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: Any
    ): List<PointerInputChange> {
        return onPointerInput(pointerEvent.changes, pass, bounds as IntSize)
    }

    override fun onCancel() {}

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        initHandler(customEventDispatcher)
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        customEventHandler(customEvent, pass)
    }
}

internal data class TestCustomEvent(val value: String) : CustomEvent

class LayoutCoordinatesStub(
    override var isAttached: Boolean = true
) : LayoutCoordinates {

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
        return local
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