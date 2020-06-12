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

@file:OptIn(InternalComposeApi::class)
package androidx.compose

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SlotTableTests {

    @Test
    fun testCanCreate() {
        SlotTable()
    }

    // Raw slot tests (testing the buffer gap)

    @Test
    fun testCanInsert() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(37)
            writer.skip()
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testValidateSlots() {
        val slots = testSlotsNumbered()
        slots.read { reader ->
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
            }
        }
    }

    @Test
    fun testInsertAtTheStart() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.beginInsert()
            writer.skip()
            writer.set(-1)
            writer.endInsert()
        }
        slots.read { reader ->
            assertEquals(-1, reader.next())
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
            }
        }
    }

    @Test
    fun testInsertAtTheEnd() {
        val slots = testSlotsNumbered()

        val current = slots.read { reader ->
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
            }
            reader.current
        }

        slots.write { writer ->
            writer.current = current
            writer.beginInsert()
            writer.skip()
            writer.set(-1)
            writer.endInsert()
        }

        slots.read { reader ->
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
            }
            assertEquals(-1, reader.next())
        }
    }

    @Test
    fun testInsertInTheMiddle() {
        val slots = testSlotsNumbered()
        val current = slots.read { reader ->
            for (i in 0 until 50) {
                assertEquals(i, reader.next())
            }
            reader.current
        }
        slots.write { writer ->
            writer.current = current
            writer.beginInsert()
            writer.skip()
            writer.set(-1)
            writer.endInsert()
        }
        slots.read { reader ->
            for (i in 0 until 100) {
                if (i == 50) assertEquals(-1, reader.next())
                assertEquals(i, reader.next())
            }
        }
    }

    @Test
    fun testRemoveAtTheStart() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.remove(0, 50)
        }
        slots.read { reader ->
            for (i in 50 until 100)
                assertEquals(i, reader.next())
        }
    }

    @Test
    fun testRemoveAtTheEnd() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.remove(50, 50)
        }
        slots.read { reader ->
            for (i in 0 until 50)
                assertEquals(i, reader.next())
        }
    }

    @Test
    fun testRemoveInTheMiddle() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.remove(25, 50)
        }
        slots.read { reader ->
            for (i in 0 until 25)
                assertEquals(i, reader.next())
            for (i in 75 until 100)
                assertEquals(i, reader.next())
        }
    }

    @Test
    fun testRemoveTwoSlicesBackToFront() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.remove(70, 10)
            writer.remove(40, 10)
        }
        slots.read { reader ->
            for (i in 0 until 40)
                assertEquals(i, reader.next())
            for (i in 50 until 70)
                assertEquals(i, reader.next())
            for (i in 80 until 100)
                assertEquals(i, reader.next())
        }
    }

    @Test
    fun testRemoveTwoSlicesFrontToBack() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.remove(40, 10)
            writer.remove(60, 10) // Actually deletes the 70s as they have slid down 10
        }
        slots.read { reader ->
            for (i in 0 until 40)
                assertEquals(i, reader.next())
            for (i in 50 until 70)
                assertEquals(i, reader.next())
            for (i in 80 until 100)
                assertEquals(i, reader.next())
        }
    }

    // Anchor tests

    @Test
    fun testAllocateAnchors() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (1..7).map { writer.anchor(it * 10) } }
        slots.read { reader ->
            for (index in 1..7) {
                val anchor = anchors[index - 1]
                assertEquals(index * 10, reader.get(anchor))
            }
        }
    }

    @Test
    fun testAnchorsTrackInserts() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (1..7).map { writer.anchor(it * 10) } }
        slots.write { writer ->
            writer.current = 40
            writer.beginInsert()
            repeat(50) { writer.skip() }
            writer.endInsert()
        }
        slots.read { reader ->
            for (index in 1..7) {
                val anchor = anchors[index - 1]
                assertEquals(index * 10, reader.get(anchor))
            }
        }
    }

    @Test
    fun testEmptySlotTableAnchorAtNegativeOneStaysNegativeOne() {
        val slots = SlotTable()
        val anchor = slots.anchor(-1)
        assertEquals(-1, slots.anchorLocation(anchor))
        slots.write { writer ->
            writer.beginInsert()
            writer.skip()
            writer.endInsert()
        }
        assertEquals(-1, slots.anchorLocation(anchor))
    }

    @Test
    fun testAnchorTracksExactRemovesUpwards() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (1..7).map { writer.anchor(it * 10) } }
        slots.write { writer ->
            for (index in 1..7) {
                writer.remove(index * 10 - (index - 1), 1)
                assertEquals(-1, anchors[index - 1].location(slots))
            }
        }
    }

    @Test
    fun testAnchorTracksExactRemovesDownwards() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (1..7).map { writer.anchor(it * 10) } }
        slots.write { writer ->
            for (index in 7 downTo 1) {
                writer.remove(index * 10, 1)
                assertEquals(-1, anchors[index - 1].location(slots))
            }
        }
    }

    @Test
    fun testAnchorTracksExtactRemovesInnerOuter() {
        val slots = testSlotsNumbered()
        val expectedLocations = (1..7).map { it * 10 }.toMutableList()
        val anchors = slots.write { writer -> expectedLocations.map { writer.anchor(it) } }
        slots.write { writer ->
            for (index in listOf(4, 5, 3, 6, 2, 7, 1)) {
                val location = expectedLocations[index - 1]
                writer.remove(location, 1)
                assertEquals(-1, anchors[index - 1].location(slots))
                for (i in expectedLocations.indices) {
                    if (expectedLocations[i] > location) expectedLocations[i]--
                }
            }
        }
    }

    @Test
    fun testAnchorTracksExactRemovesOuterInner() {
        val slots = testSlotsNumbered()
        val expectedLocations = (1..7).map { it * 10 }.toMutableList()
        val anchors = slots.write { writer -> expectedLocations.map { writer.anchor(it) } }
        slots.write { writer ->
            for (index in listOf(1, 7, 2, 6, 3, 5, 4)) {
                val location = expectedLocations[index - 1]
                writer.remove(location, 1)
                assertEquals(-1, anchors[index - 1].location(slots))
                for (i in expectedLocations.indices) {
                    if (expectedLocations[i] > location) expectedLocations[i]--
                }
            }
        }
    }

    @Test
    fun testAnchorTrackRemoves() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (1..7).map { writer.anchor(it * 10) } }
        slots.write { writer ->
            writer.remove(40, 20)
        }
        slots.read { reader ->
            for (index in 1..7) {
                val anchor = anchors[index - 1]
                val expected = (index * 10).let { if (it in 40 until 60) SlotTable.EMPTY else it }
                assertEquals(expected, reader.get(anchor))
            }
        }
    }

    @Test
    fun testAnchorMoves() {
        val slots = SlotTable()

        fun buildSlots(range: List<Int>): Map<Anchor, Any?> {
            val anchors = mutableListOf<Pair<Anchor, Any?>>()
            slots.write { writer ->
                fun item(value: Int, block: () -> Unit) {
                    writer.startGroup(value)
                    block()
                    writer.endGroup()
                }

                fun value(value: Any?) {
                    writer.update(value)
                }

                writer.beginInsert()
                for (i in range) {
                    item(i) {
                        value(i * 100)
                        anchors.add(slots.anchor(writer.current - 1) to i * 100)
                    }
                }
                writer.endInsert()
            }
            slots.verifyWellFormed()
            return anchors.toMap()
        }

        fun validate(anchors: Map<Anchor, Any?>) {
            slots.verifyWellFormed()
            slots.read { reader ->
                for (anchor in anchors) {
                    assertEquals(anchor.value, reader.get(slots.anchorLocation(anchor.key)))
                }
            }
        }

        fun moveItems() {
            slots.write { writer ->
                writer.skipGroup()
                writer.moveGroup(4)
                writer.skipGroup()
                writer.skipGroup()
                writer.moveGroup(1)
                writer.skipGroup()
                writer.skipGroup()
                writer.moveGroup(1)
            }
        }

        val expected = listOf(1, 2, 3, 4, 5, 6, 7)
        val anchors = buildSlots(expected)
        validate(anchors)
        moveItems()
        validate(anchors)
    }

    @Test
    fun testRemovingDuplicateAnchorsMidRange() {
        val slots = testSlotsNumbered()

        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(20, 20) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    @Test
    fun testRemovingDuplicateAnchorsStartRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(30, 20) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    @Test
    fun testRemovingDuplicateAnchorsEndRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(20, 11) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    @Test
    fun testDuplicateAnchorIdentity() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(it * 5) } }
        slots.write { writer ->
            anchors.forEachIndexed { index, anchor ->
                assertSame(anchor, writer.anchor(index * 5))
            }
        }
    }

    // Semantic tests (testing groups and nodes)

    @Test
    fun testEmptySlotTable() {
        val slotTable = SlotTable()
        slotTable.verifyWellFormed()

        slotTable.read { reader ->
            assertEquals(0, reader.groupKey)
        }
    }

    @Test
    fun testExtractKeys() {
        val slots = testItems()
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            val keys = reader.extractKeys()
            assertEquals(10, keys.size)
            keys.forEachIndexed { i, keyAndLocation ->
                assertEquals(i, keyAndLocation.key)
                assertEquals(i, keyAndLocation.dataKey)
            }
        }
    }

    @Test
    fun testMoveAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup(treeRoot)
            writer.moveGroup(5)
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            reader.expectGroup(5)
            for (i in 0 until 5) {
                reader.expectGroup(i)
            }
            for (i in 6 until 10) {
                reader.expectGroup(i)
            }
            reader.endGroup()
        }
    }

    @Test
    fun testRemoveAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup(treeRoot)
            for (i in 0 until 5) {
                writer.skipGroup()
            }
            writer.removeGroup()
            for (i in 6 until 10) {
                writer.skipGroup()
            }
            writer.endGroup()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 5) {
                reader.expectGroup(i)
            }
            for (i in 6 until 10) {
                reader.expectGroup(i)
            }
            reader.endGroup()
        }
    }

    @Test
    fun testCountNodes() {
        val slots = testItems()
        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 10) {
                val count = reader.expectGroup(i)
                assertEquals(i + 1, count)
            }
            reader.endGroup()
        }
    }

    @Test
    fun testCountNestedNodes() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            repeat(10) {
                writer.startGroup(0)
                repeat(3) {
                    writer.startNode(1, 1)
                    writer.endNode()
                }
                assertEquals(3, writer.endGroup())
            }
            assertEquals(30, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            assertEquals(30, reader.expectGroup(0))
            reader.endGroup()
        }
    }

    @Test
    fun testUpdateNestedNodeCountOnInsert() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            repeat(10) {
                writer.startGroup(0)
                repeat(3) {
                    writer.startGroup(0)
                    writer.startNode(1, 1)
                    writer.endNode()
                    assertEquals(1, writer.endGroup())
                }
                assertEquals(3, writer.endGroup())
            }
            assertEquals(30, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.write { writer ->
            writer.startGroup(treeRoot)
            writer.startGroup(0)

            repeat(3) {
                assertEquals(3, writer.skipGroup())
            }

            writer.startGroup(0)
            writer.beginInsert()
            repeat(2) {
                writer.startGroup(0)
                writer.startNode(1, 1)
                writer.endNode()
                assertEquals(1, writer.endGroup())
            }
            writer.endInsert()
            repeat(3) { writer.skipGroup() }
            assertEquals(5, writer.endGroup())

            repeat(6) {
                assertEquals(3, writer.skipGroup())
            }

            assertEquals(32, writer.endGroup())
            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testUpdateNestedNodeCountOnRemove() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            repeat(10) {
                writer.startGroup(0)
                repeat(3) {
                    writer.startGroup(0)
                    writer.startNode(1, 1)
                    writer.endNode()
                    assertEquals(1, writer.endGroup())
                }
                assertEquals(3, writer.endGroup())
            }
            assertEquals(30, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.write { writer ->
            writer.startGroup(treeRoot)
            writer.startGroup(0)

            repeat(3) {
                assertEquals(3, writer.skipGroup())
            }

            writer.startGroup(0)

            repeat(2) { writer.removeGroup() }
            repeat(1) { writer.skipGroup() }
            assertEquals(1, writer.endGroup())

            repeat(6) {
                assertEquals(3, writer.skipGroup())
            }

            assertEquals(28, writer.endGroup())

            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testNodesResetNodeCount() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            writer.startNode(1, 1)
            repeat(10) {
                writer.startNode(1, 1)
                writer.startGroup(0)
                repeat(3) {
                    writer.startNode(1, 1)
                    writer.endNode()
                }
                assertEquals(3, writer.endGroup())
                writer.endNode()
            }
            writer.endNode()
            assertEquals(1, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testSkipANode() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            writer.startNode(1, 1)
            repeat(10) {
                writer.startNode(1, 1)
                writer.startGroup(0)
                repeat(3) {
                    writer.startNode(1, 1)
                    writer.endNode()
                }
                assertEquals(3, writer.endGroup())
                writer.endNode()
            }
            writer.endNode()
            assertEquals(1, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            reader.startGroup()
            assertEquals(1, reader.skipNode())
            reader.endGroup()
            reader.endGroup()
        }
    }

    @Test
    fun testStartEmpty() {
        val slots = SlotTable()
        slots.read { reader ->
            reader.beginEmpty()
            reader.startGroup()
            assertEquals(true, reader.inEmpty)
            assertEquals(SlotTable.EMPTY, reader.next())
            reader.endGroup()
            reader.endEmpty()
        }
    }

    @Test
    fun testReportGroupSize() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(0)
            repeat(10) {
                writer.startNode(1, 1)
                writer.endNode()
            }
            writer.endGroup()
            writer.startGroup(705)
            writer.update(42)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            assertEquals(true, reader.isGroup)
            val size = reader.groupSize
            val savedCurrent = reader.current
            reader.skipGroup()
            assertEquals(size, reader.current - savedCurrent - 1)
            assertTrue(reader.isGroup)
            reader.startGroup()
            assertEquals(42, reader.next())
            assertTrue(reader.isGroupEnd)
            reader.endGroup()
            assertTrue(reader.isGroupEnd)
            reader.endGroup()
        }
    }

    @Test
    fun testIsGroups() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(0)
            writer.startGroup(1)
            writer.endGroup()
            writer.startNode(2, 2)
            writer.endNode()
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            assertEquals(true, reader.isGroup)
            reader.startGroup()
            assertEquals(false, reader.isGroup)
            assertEquals(true, reader.isGroupEnd)
            reader.endGroup()
            assertEquals(true, reader.isNode)
            assertEquals(false, reader.isGroupEnd)
            reader.startNode()
            assertEquals(false, reader.isNode)
            assertEquals(true, reader.isGroupEnd)
            reader.endNode()
            reader.endGroup()
        }

        slots.write { writer ->
            writer.startGroup(0)
            assertEquals(true, writer.isGroup)
            writer.startGroup(1)
            assertEquals(false, writer.isGroup)
            writer.endGroup()
            assertEquals(true, writer.isNode)
            writer.startNode(2, 2)
            assertEquals(false, writer.isNode)
            writer.endNode()
            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testMoveGroup() {
        val slots = SlotTable()

        val anchors = mutableListOf<Anchor>()

        fun buildSlots() {
            slots.write { writer ->
                fun item(key: Int, block: () -> Unit) {
                    writer.startGroup(key, key)
                    block()
                    writer.endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    writer.startNode(key, key)
                    block()
                    writer.endNode()
                }

                fun value(value: Any) {
                    writer.update(value)
                }

                fun innerItem(i: Int) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        item(35) {
                                            value(36)
                                            item(37) {
                                                value(39)
                                                element(40) {
                                                    value(42)
                                                    value(43)
                                                    element(44) {
                                                        value(46)
                                                        value(47)
                                                    }
                                                    element(48) {
                                                        value(50)
                                                        value(51)
                                                        value(52)
                                                        value(53)
                                                    }
                                                    element(54) {
                                                        value(56)
                                                        value(57)
                                                        value(58)
                                                        value(59)
                                                    }
                                                    element(60) {
                                                        value(62)
                                                        anchors.add(
                                                            slots.anchor(writer.current - 1)
                                                        )
                                                        value(63)
                                                        value(64)
                                                        value(65)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Build a slot table to that duplicates the structure of the slot table produced
                // in the code generation test testMovement()
                writer.beginInsert()
                item(0) {
                    item(2) {
                        item(4) {
                            item(6) {
                                value(8)
                                item(9) {
                                    item(11) {
                                        item(12) {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)
                                                    for (i in 1..5) {
                                                        innerItem(i)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                writer.endInsert()
            }
        }

        fun validateSlots(range: List<Int>) {
            slots.verifyWellFormed()
            slots.read { reader ->
                fun value(value: Any?) {
                    assertEquals(value, reader.next())
                }

                fun item(key: Int, block: () -> Unit) {
                    assertEquals(reader.groupKey, key)
                    assertEquals(reader.groupDataKey, key)
                    reader.startGroup()
                    block()
                    reader.endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    assertEquals(reader.groupDataKey, key)
                    reader.startNode()
                    block()
                    reader.endNode()
                }

                fun innerBlock(i: Int) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        item(35) {
                                            value(36)
                                            item(37) {
                                                value(39)
                                                element(40) {
                                                    value(42)
                                                    value(43)
                                                    element(44) {
                                                        value(46)
                                                        value(47)
                                                    }
                                                    element(48) {
                                                        value(50)
                                                        value(51)
                                                        value(52)
                                                        value(53)
                                                    }
                                                    element(54) {
                                                        value(56)
                                                        value(57)
                                                        value(58)
                                                        value(59)
                                                    }
                                                    element(60) {
                                                        value(62)
                                                        value(63)
                                                        value(64)
                                                        value(65)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(0) {
                    item(2) {
                        item(4) {
                            item(6) {
                                value(8)
                                item(9) {
                                    item(11) {
                                        item(12) {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)
                                                    for (i in range) {
                                                        innerBlock(i)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun moveItem5Up() {
            slots.write { writer ->
                fun item(key: Int, block: () -> Unit) {
                    writer.startGroup(key, key)
                    block()
                    writer.endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    writer.startNode(key, key)
                    block()
                    writer.endNode()
                }

                fun value(value: Any) {
                    writer.update(value)
                }
                item(0) {
                    item(2) {
                        item(4) {
                            item(6) {
                                value(8)
                                item(9) {
                                    item(11) {
                                        item(12) {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)

                                                    // Skip three items
                                                    writer.skipGroup()
                                                    writer.skipGroup()
                                                    writer.skipGroup()

                                                    // Move one item up
                                                    writer.moveGroup(1)

                                                    // Skip them
                                                    writer.skipGroup()
                                                    writer.skipGroup()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        buildSlots()
        validateSlots((1..5).toList())
        moveItem5Up()
        validateSlots(listOf(1, 2, 3, 5, 4))

        // Validate that all the anchors still refer to a slot with value 62
        slots.read { reader ->
            for (anchor in anchors) {
                assertEquals(62, reader.get(slots.anchorLocation(anchor)))
            }
        }
    }

    @Test
    fun testGroupPath() {
        val table = testItems()
        repeat(table.size) { location ->
            val path = table.groupPathTo(location)
            var previous = Int.MAX_VALUE
            table.read { reader ->
                for (group in path) {
                    assertTrue(group <= location)

                    val size = reader.groupSize(group)
                    assertTrue(size < previous)
                    previous = size
                }
                if (reader.isGroup(location)) {
                    assertTrue(path.last() == location)
                }
            }
        }
    }

    @Test
    fun testValidateSlotTableIndexes() {
        val (slots, _) = narrowTrees()
        slots.verifyWellFormed()
    }

    @Test
    fun testRemoveRandomGroup() {
        val (slots, anchors) = narrowTrees()
        val random = Random(1000)
        val slotsToRemove = anchors.shuffled(random)
        slotsToRemove.forEachIndexed { index, anchor ->
            if (anchor.valid) {
                try {
                    slots.write { writer ->
                        writer.startGroup(treeRoot)

                        // Seek to the group's parent
                        writer.skip(writer.parentIndex(anchor) - writer.current)
                        if (writer.isNode) writer.startNode() else writer.startGroup()
                        (EMPTY)

                        // Skip to the group location
                        writer.skip(slots.anchorLocation(anchor) - writer.current)
                        writer.removeGroup()

                        // Close the parent
                        writer.skipToGroupEnd()
                        writer.endGroup()

                        // Close the root
                        writer.skipToGroupEnd()
                        writer.endGroup()
                    }
                    slots.verifyWellFormed()
                } catch (e: Throwable) {
                    throw Exception(
                        "Error removing group at ${slots.anchorLocation(anchor)} (index=$index)",
                        e
                    )
                }
            }
        }
    }

    @Test
    fun testMovingGroups() {
        val random = Random(1116)
        val (sourceTable, sourceAnchors) = narrowTrees()
        val destTable = SlotTable()

        destTable.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)

            writer.startGroup(1122)
            writer.endGroup()

            writer.endGroup()
            writer.endInsert()
        }

        val slotsToMove = sourceAnchors.shuffled(random)
        val slotKeys = sourceTable.read { reader ->
            slotsToMove.map { anchor ->
                val location = sourceTable.anchorLocation(anchor)
                reader.groupKey(location)
            }
        }

        val movedAnchors = mutableSetOf<Anchor>()
        slotsToMove.forEachIndexed { index, anchor ->
            try {
                if (anchor !in movedAnchors) {
                    destTable.write { writer ->
                        writer.startGroup(treeRoot)
                        writer.startGroup(1122)

                        writer.skipToGroupEnd()
                        writer.beginInsert()
                        movedAnchors += writer.moveFrom(sourceTable, anchor.location(sourceTable))
                        writer.endInsert()

                        writer.endGroup()
                        writer.endGroup()
                    }

                    // Both the source and destinations should be well-formed.
                    destTable.verifyWellFormed()
                    sourceTable.verifyWellFormed()
                }
            } catch (e: Throwable) {
                throw Exception(
                    "Error removing group at ${anchor.loc} (index=$index)",
                    e
                )
            }
        }

        // Verify the anchors still point to the correct groups
        val movedKeys = destTable.read { reader ->
            slotsToMove.map { anchor ->
                val location = destTable.anchorLocation(anchor)
                reader.groupKey(location)
            }
        }
        assertEquals(slotKeys.size, movedKeys.size, "slot keys changed")
        for (index in slotKeys.indices) {
            val sourceKey = slotKeys[index]
            val movedKey = movedKeys[index]
            assertEquals(sourceKey, movedKey, "Key at $index changed")
        }
    }
}

fun testSlotsNumbered(): SlotTable {
    val slotTable = SlotTable()
    slotTable.write { writer ->
        writer.beginInsert()
        repeat(100) {
            writer.update(it)
        }
        writer.endInsert()
    }
    return slotTable
}

private const val treeRoot = -1
private const val elementKey = 100

// Creates 0 until 10 items each with 10 elements numbered 0...n with 0..n slots
fun testItems(): SlotTable {
    val slots = SlotTable()
    slots.write { writer ->
        writer.beginInsert()
        writer.startGroup(treeRoot)

        fun item(key: Int, block: () -> Unit) {
            writer.startGroup(key, key)
            block()
            writer.endGroup()
        }

        fun element(key: Int, block: () -> Unit) {
            writer.startNode(key, key)
            block()
            writer.endNode()
        }

        for (key in 0 until 10) {
            item(key) {
                for (item in 0..key) {
                    element(elementKey) {
                        for (element in 0..key)
                            writer.update(element)
                    }
                }
            }
        }

        writer.endGroup()
        writer.endInsert()
    }

    return slots
}

fun narrowTrees(): Pair<SlotTable, List<Anchor>> {
    val slots = SlotTable()
    val anchors = mutableListOf<Anchor>()
    slots.write { writer ->
        writer.beginInsert()
        writer.startGroup(treeRoot)

        fun item(key: Int, block: () -> Unit) {
            writer.startGroup(key)
            block()
            writer.endGroup()
        }

        fun element(key: Int, block: () -> Unit) {
            writer.startNode(key, key)
            block()
            writer.endNode()
        }

        fun tree(key: Int, width: Int, depth: Int) {
            item(key) {
                anchors.add(writer.anchor(writer.current - 1))
                if (width > 0)
                    for (childKey in 1..width) {
                        tree(childKey, width - 1, depth + 1)
                    }
                else if (depth > 0) {
                    tree(1001, width, depth - 1)
                } else {
                    repeat(depth + 2) {
                        element(-1) { }
                    }
                }
            }
        }

        element(1000) {
            tree(0, 5, 5)
        }
        writer.endGroup()
        writer.endInsert()
    }

    return slots to anchors
}

fun SlotReader.expectGroup(key: Any): Int {
    assertEquals(key, groupKey)
    return skipGroup()
}
