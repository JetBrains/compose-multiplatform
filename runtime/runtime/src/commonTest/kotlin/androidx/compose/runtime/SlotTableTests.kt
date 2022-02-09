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
package androidx.compose.runtime

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(InternalComposeApi::class)
class SlotTableTests {
    @Test
    fun testCanCreate() {
        SlotTable()
    }

    @Test
    fun testIsEmpty() {
        val slots = SlotTable()
        assertTrue(slots.isEmpty)
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.endGroup()
            writer.endInsert()
        }
        assertFalse(slots.isEmpty)
    }

    @Test
    fun testCanInsert() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(37)
            writer.update(1)
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testValidateSlots() {
        val slots = testSlotsNumbered()
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            repeat(100) {
                assertEquals(it, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    @Test
    fun testInsertAtTheStart() {
        val slots = testSlotsNumbered()
        slots.verifyWellFormed()
        slots.write { writer ->
            writer.startGroup()
            writer.beginInsert()
            writer.startGroup(-100)
            writer.endGroup()
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            assertEquals(-100, reader.groupKey)
            reader.skipGroup()
            repeat(100) {
                assertEquals(it, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    @Test
    fun testInsertAtTheEnd() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.startGroup()
            writer.skipToGroupEnd()
            writer.beginInsert()
            writer.startGroup(-100)
            writer.endGroup()
            writer.endInsert()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            repeat(100) {
                assertEquals(it, reader.groupKey)
                reader.skipGroup()
            }
            assertEquals(-100, reader.groupKey)
            reader.skipGroup()
            reader.endGroup()
        }
    }

    @Test
    fun testInsertInTheMiddle() {
        val slots = testSlotsNumbered()
        val seekAmount = slots.read { reader ->
            reader.startGroup()
            val start = reader.currentGroup
            repeat(50) {
                reader.skipGroup()
            }
            reader.currentGroup - start
        }
        slots.write { writer ->
            writer.startGroup()
            writer.advanceBy(seekAmount)
            writer.beginInsert()
            writer.startGroup(-100)
            writer.endGroup()
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            repeat(50) { reader.skipGroup() }
            assertEquals(-100, reader.groupKey)
            reader.skipToGroupEnd()
            reader.endGroup()
        }
    }

    @Test
    fun testRemoveAtTheStart() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.startGroup()
            repeat(50) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            for (i in 50 until 100) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    @Test
    fun testRemoveAtTheEnd() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.startGroup()
            repeat(50) { writer.skipGroup() }
            repeat(50) { writer.removeGroup() }
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            repeat(50) {
                assertEquals(it, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    @Test
    fun testRemoveInTheMiddle() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.startGroup()
            repeat(25) { writer.skipGroup() }
            repeat(50) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 25) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            for (i in 75 until 100) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    @Test
    fun testRemoveTwoSlices() {
        val slots = testSlotsNumbered()
        slots.write { writer ->
            writer.startGroup()
            repeat(40) { writer.skipGroup() }
            repeat(10) { writer.removeGroup() }
            repeat(20) { writer.skipGroup() }
            repeat(10) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 40) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            for (i in 50 until 70) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            for (i in 80 until 100) {
                assertEquals(i, reader.groupKey)
                reader.skipGroup()
            }
            reader.endGroup()
        }
    }

    // Anchor tests

    @Test
    fun testAllocateAnchors() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader ->
            val anchors = mutableListOf<Anchor>()
            reader.startGroup()
            repeat(7) {
                repeat(10) { reader.skipGroup() }
                anchors.add(reader.anchor())
            }
            reader.skipToGroupEnd()
            reader.endGroup()
            anchors
        }
        slots.read { reader ->
            anchors.forEachIndexed { index, anchor ->
                val key = reader.groupKey(anchor.toIndexFor(slots))
                assertEquals((index + 1) * 10, key)
            }
        }
    }

    @Test
    fun testAnchorsTrackInserts() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader ->
            val anchors = mutableListOf<Anchor>()
            reader.startGroup()
            repeat(7) {
                repeat(10) { reader.skipGroup() }
                anchors.add(reader.anchor())
            }
            reader.skipToGroupEnd()
            reader.endGroup()
            anchors
        }
        slots.write { writer ->
            writer.startGroup()
            repeat(41) { writer.skipGroup() }
            writer.beginInsert()
            repeat(30) {
                writer.startGroup(-100 - it)
                writer.endGroup()
            }
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.read { reader ->
            for (index in 1..7) {
                val anchor = anchors[index - 1]
                assertEquals(index * 10, reader.groupKey(anchor))
            }
        }
    }

    @Test
    fun testEmptySlotTableAnchorAtNegativeOneStaysNegativeOne() {
        val slots = SlotTable()
        val anchor = slots.read { reader -> reader.anchor(-1) }
        assertEquals(-1, anchor.toIndexFor(slots))
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(-100)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        assertEquals(-1, anchor.toIndexFor(slots))
    }

    @Test
    fun testAnchorTracksExactRemovesUpwards() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader -> (1..7).map { reader.anchor(it * 10 + 1) } }
        slots.write { writer ->
            writer.startGroup()
            writer.skipGroup()
            for (index in 1..7) {
                writer.advanceBy(9)
                val removedAnchors = writer.removeGroup()
                assertTrue(removedAnchors)
                assertFalse(anchors[index - 1].valid)
            }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testAnchorTrackRemoves() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader -> (1..7).map { reader.anchor(it * 10 + 1) } }
        slots.write { writer ->
            writer.startGroup()
            writer.advanceBy(40)
            repeat(20) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            for (index in 1..7) {
                val anchor = anchors[index - 1]
                val expected = (index * 10).let {
                    if (it in 40 until 60) 0 else it
                }
                assertEquals(expected, reader.groupKey(anchor))
            }
        }
    }

    @Test
    fun testAnchorMoves() {
        val slots = SlotTable()

        fun buildSlots(range: List<Int>): Map<Anchor, Any?> {
            val anchors = mutableMapOf<Anchor, Any?>()
            slots.write { writer ->
                fun item(value: Int, block: () -> Unit) {
                    writer.startGroup(value)
                    block()
                    writer.endGroup()
                }

                fun element(key: Int) {
                    writer.startGroup(key)
                    writer.endGroup()
                }

                writer.beginInsert()
                writer.startGroup(treeRoot)
                for (i in range) {
                    item(i) {
                        val key = i * 100
                        anchors[writer.anchor()] = key
                        element(key)
                    }
                }
                writer.endGroup()
                writer.endInsert()
            }
            slots.verifyWellFormed()
            return anchors
        }

        fun validate(anchors: Map<Anchor, Any?>) {
            slots.verifyWellFormed()
            slots.read { reader ->
                for (anchor in anchors) {
                    assertEquals(anchor.value, reader.groupKey(anchor.key))
                }
            }
        }

        fun moveItems() {
            slots.write { writer ->
                writer.startGroup()
                writer.skipGroup()
                writer.moveGroup(4)
                writer.skipGroup()
                writer.skipGroup()
                writer.moveGroup(1)
                writer.skipGroup()
                writer.moveGroup(1)
                writer.skipToGroupEnd()
                writer.endGroup()
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

        val anchors = slots.read { reader -> (0 until 10).map { reader.anchor(30) } }
        slots.write { writer ->
            writer.startGroup()
            repeat(20) { writer.skipGroup() }
            repeat(20) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        for (anchor in anchors) {
            assertFalse(anchor.valid)
        }
    }

    @Test
    fun testRemovingDuplicateAnchorsStartRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader -> (0 until 10).map { reader.anchor(30) } }
        slots.write { writer ->
            writer.startGroup()
            repeat(29) { writer.skipGroup() }
            repeat(30) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        for (anchor in anchors) {
            assertFalse(anchor.valid)
        }
    }

    @Test
    fun testRemovingDuplicateAnchorsEndRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader -> (0 until 10).map { reader.anchor(30) } }
        slots.write { writer ->
            writer.startGroup()
            repeat(19) { writer.skipGroup() }
            repeat(11) { writer.removeGroup() }
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        for (anchor in anchors) {
            assertFalse(anchor.valid)
        }
    }

    @Test
    fun testDuplicateAnchorIdentity() {
        val slots = testSlotsNumbered()
        val anchors = slots.read { reader -> (0 until 10).map { reader.anchor(it * 5) } }
        slots.read { reader ->
            anchors.forEachIndexed { index, anchor ->
                assertSame(anchor, reader.anchor(index * 5))
            }
        }
    }

    // Group with slots test

    @Test
    fun testEmptySlotTable() {
        val slotTable = SlotTable()
        slotTable.verifyWellFormed()

        slotTable.read { reader ->
            assertEquals(0, reader.groupKey)
        }
    }

    @Test
    fun testTestItems() {
        val slots = testItems()
        slots.verifyWellFormed()
        validateItems(slots)
    }

    @Test
    fun testExtractKeys() {
        val slots = testItems()
        slots.verifyWellFormed()
        val expectedLocations = mutableListOf<Int>()
        val expectedNodes = mutableListOf<Int>()
        slots.read { reader ->
            reader.startGroup()
            while (!reader.isGroupEnd) {
                expectedLocations.add(reader.currentGroup)
                expectedNodes.add(if (reader.isNode) 1 else reader.nodeCount)
                reader.skipGroup()
            }
            reader.endGroup()
        }
        slots.read { reader ->
            reader.startGroup()
            val keys = reader.extractKeys()
            assertEquals(10, keys.size)
            keys.forEachIndexed { i, keyAndLocation ->
                assertEquals(i + 1, keyAndLocation.key)
                assertEquals(i + 1, keyAndLocation.objectKey)
                assertEquals(expectedLocations[i], keyAndLocation.location)
                assertEquals(expectedNodes[i], keyAndLocation.nodes)
                assertEquals(i, keyAndLocation.index)
            }
        }
    }

    @Test
    fun testInsertAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup(treeRoot)
            writer.skipGroup()
            writer.beginInsert()
            writer.startGroup(1000)
            writer.update(10)
            writer.update(20)
            writer.endGroup()
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            reader.skipGroup()
            assertEquals(1000, reader.groupKey)
            reader.startGroup()
            assertEquals(10, reader.next())
            assertEquals(20, reader.next())
            reader.endGroup()
            reader.skipToGroupEnd()
            reader.endGroup()
        }
    }

    @Test
    fun removeAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup()
            writer.skipGroup()
            writer.removeGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testMoveAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup(treeRoot)
            writer.skipGroup()
            writer.moveGroup(4)
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            reader.expectGroup(1)
            reader.expectGroup(6)
            reader.expectGroup(2)
            reader.expectGroup(3)
            reader.expectGroup(4)
            reader.expectGroup(5)
            reader.expectGroup(7)
            reader.expectGroup(8)
            reader.expectGroup(9)
            reader.expectGroup(10)
            reader.endGroup()
        }
    }

    @Test
    fun testCountNodes() {
        val slots = testItems()
        slots.read { reader ->
            reader.startGroup()
            for (i in 1..10) {
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
                    writer.endGroup()
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
                    writer.endGroup()
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
            writer.startGroup()
            writer.startGroup()

            repeat(3) {
                assertEquals(3, writer.skipGroup())
            }

            writer.startGroup()
            writer.beginInsert()
            repeat(2) {
                writer.startGroup(-100)
                writer.startNode(1, 1)
                writer.endGroup()
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
                    writer.endGroup()
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
                    writer.endGroup()
                }
                assertEquals(3, writer.endGroup())
                writer.endGroup()
            }
            writer.endGroup()
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
                    writer.endGroup()
                }
                assertEquals(3, writer.endGroup())
                writer.endGroup()
            }
            writer.endGroup()
            assertEquals(1, writer.endGroup())
            writer.endGroup()
            writer.endInsert()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.startGroup()
            reader.startGroup()
            assertEquals(1, reader.skipGroup())
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
            assertEquals(Composer.Empty, reader.next())
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
                writer.endGroup()
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
            val size = reader.groupSize
            val savedCurrent = reader.currentGroup
            reader.skipGroup()
            assertEquals(size, reader.currentGroup - savedCurrent)
            reader.startGroup()
            assertEquals(42, reader.next())
            assertTrue(reader.isGroupEnd)
            reader.endGroup()
            assertTrue(reader.isGroupEnd)
            reader.endGroup()
        }
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
                    writer.endGroup()
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
                                                    anchors.add(writer.anchor())
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
                    assertEquals(reader.groupObjectKey, key)
                    reader.startGroup()
                    block()
                    reader.endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    assertEquals(reader.groupObjectKey, key)
                    reader.startNode()
                    block()
                    reader.endGroup()
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
                    writer.endGroup()
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

        // Validate tha the anchors still refer to a slot with value 62
        slots.read { reader ->
            for (anchor in anchors) {
                assertEquals(60, reader.groupObjectKey(anchor.toIndexFor(slots)))
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
        slots.verifyWellFormed()
        val random = Random(1000)
        val slotsToRemove = anchors.shuffled(random)
        slotsToRemove.forEach { anchor ->
            if (anchor.valid) {
                slots.write { writer ->
                    writer.startGroup(treeRoot)

                    // Skip to the group location
                    writer.seek(anchor)

                    // Ensure parent is started.
                    writer.ensureStarted(writer.parent(anchor))

                    // Remove the group.
                    writer.removeGroup()

                    // Close the parent
                    writer.skipToGroupEnd()
                    writer.endGroup()

                    // Close the root
                    writer.skipToGroupEnd()
                    writer.endGroup()
                }
                slots.verifyWellFormed()
            }
        }
    }

    @Test
    fun testMovingEntireTable() {
        val (sourceTable, _) = narrowTrees()
        val destinationTable = SlotTable()
        val groupsSize = sourceTable.groupsSize
        val slotsSize = sourceTable.slotsSize
        destinationTable.write { writer ->
            writer.beginInsert()
            writer.moveFrom(sourceTable, 0)
            writer.endInsert()
        }
        sourceTable.verifyWellFormed()
        destinationTable.verifyWellFormed()
        assertEquals(0, sourceTable.groupsSize)
        assertEquals(0, sourceTable.slotsSize)
        assertEquals(groupsSize, destinationTable.groupsSize)
        assertEquals(slotsSize, destinationTable.slotsSize)
    }

    @Test
    fun testMovingOneGroup() {
        val sourceTable = SlotTable()
        val anchors = mutableListOf<Anchor>()
        sourceTable.write { writer ->
            writer.beginInsert()
            anchors.add(writer.anchor())
            writer.startGroup(10)
            writer.update(100)
            writer.update(200)
            writer.endGroup()
            writer.endInsert()
        }
        sourceTable.verifyWellFormed()

        val destinationTable = SlotTable()
        destinationTable.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(1000)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        destinationTable.verifyWellFormed()

        destinationTable.write { writer ->
            writer.startGroup()
            writer.startGroup()
            writer.beginInsert()
            writer.moveFrom(
                sourceTable,
                anchors.first().toIndexFor(sourceTable)
            )
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        destinationTable.verifyWellFormed()
        destinationTable.read { reader ->
            assertEquals(10, reader.groupKey(anchors.first()))
        }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun testMovingANodeGroup() {
        val sourceTable = SlotTable()
        val anchors = mutableListOf<Anchor>()
        sourceTable.write { writer ->
            writer.beginInsert()
            anchors.add(writer.anchor())
            writer.startNode(10, 10)
            writer.update(100)
            writer.update(200)
            writer.endGroup()
            writer.endInsert()
        }
        sourceTable.verifyWellFormed()

        val destinationTable = SlotTable()
        destinationTable.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(1000)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        destinationTable.verifyWellFormed()

        destinationTable.write { writer ->
            writer.startGroup()
            writer.startGroup()
            writer.beginInsert()
            writer.moveFrom(
                sourceTable,
                anchors.first().toIndexFor(sourceTable)
            )
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        destinationTable.verifyWellFormed()
        destinationTable.read { reader ->
            val anchor = anchors.first()
            assertEquals(125, reader.groupKey(anchor))
            assertEquals(10, reader.groupObjectKey(anchor.toIndexFor(destinationTable)))
        }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun testMovingMultipleRootGroups() {
        val sourceTable = SlotTable()
        val anchors = mutableListOf<Anchor>()
        val moveCount = 5
        sourceTable.write { writer ->
            writer.beginInsert()
            repeat(moveCount) {
                anchors.add(writer.anchor())
                writer.startGroup(10)
                writer.update(100)
                writer.update(200)
                writer.endGroup()
            }
            writer.endInsert()
        }
        sourceTable.verifyWellFormed()

        val destinationTable = SlotTable()
        destinationTable.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startGroup(1000)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        destinationTable.verifyWellFormed()

        destinationTable.write { writer ->
            writer.startGroup()
            writer.startGroup()
            writer.beginInsert()
            writer.moveFrom(
                sourceTable,
                anchors.first().toIndexFor(sourceTable)
            )
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        destinationTable.verifyWellFormed()
        destinationTable.read { reader ->
            assertEquals(10, reader.groupKey(anchors.first()))
        }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun testMovingGroups() {
        val random = Random(1116)
        val (sourceTable, sourceAnchors) = narrowTrees()
        val destinationTable = SlotTable()

        destinationTable.write { writer ->
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
                val location = anchor.toIndexFor(sourceTable)
                reader.groupKey(location)
            }
        }

        val movedAnchors = mutableSetOf<Anchor>()
        slotsToMove.forEach { anchor ->
            if (anchor !in movedAnchors) {
                destinationTable.write { writer ->
                    writer.startGroup(treeRoot)
                    writer.startGroup(1122)

                    writer.skipToGroupEnd()
                    writer.beginInsert()
                    movedAnchors += writer.moveFrom(
                        sourceTable,
                        anchor.toIndexFor(sourceTable)
                    )
                    sourceTable.verifyWellFormed()
                    writer.verifyDataAnchors()
                    writer.endInsert()

                    writer.endGroup()
                    writer.endGroup()
                }

                // Both the source and destinations should be well-formed.
                destinationTable.verifyWellFormed()
                sourceTable.verifyWellFormed()
            }
        }

        // Verify the anchors still point to the correct groups
        val movedKeys = destinationTable.read { reader ->
            slotsToMove.map { anchor ->
                val location = anchor.toIndexFor(destinationTable)
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

    @Test
    fun testToIndexFor() {
        val (slots, anchors) = narrowTrees()
        val indexes = anchors.map { it.toIndexFor(slots) }
        slots.write { writer ->
            indexes.forEachIndexed { i, index ->
                assertEquals(index, anchors[i].toIndexFor(writer))
            }
        }
    }

    @Test
    fun testReaderGroupSize() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(index: Int, expectedParent: Int): Int {
                assertEquals(expectedParent, reader.parentOf(index))
                val size = reader.groupSize(index)
                var child = index + 1
                val end = index + size
                while (child < end) {
                    child += testGroup(child, index)
                }
                return size
            }
            testGroup(0, -1)
        }
    }

    @Test
    fun testWriterGroupSize() {
        val slots = testItems()
        slots.write { writer ->
            fun testGroup(index: Int): Int {
                val size = writer.groupSize(index)
                var child = index + 1
                val end = index + size
                while (child < end) {
                    child += testGroup(child)
                }
                return size
            }
            testGroup(0)
        }
    }

    @Test
    fun testReaderParentNodes() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(): Pair<Int, Int> {
                val isNode = reader.isNode
                reader.startGroup()
                var childNodes = 0
                var expectedNodes = 0
                while (!reader.isGroupEnd) {
                    val (groupNodes, parentNodes) = testGroup()
                    childNodes += groupNodes
                    if (expectedNodes > 0) {
                        assertEquals(expectedNodes, parentNodes)
                    } else {
                        expectedNodes = parentNodes
                    }
                }
                assertEquals(expectedNodes, childNodes)
                reader.endGroup()
                return (if (isNode) 1 else childNodes) to reader.parentNodes
            }

            testGroup()
        }
    }

    @Test
    fun testReaderParent() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(expectedParent: Int) {
                assertEquals(expectedParent, reader.parent)
                val current = reader.currentGroup
                reader.group {
                    val end = current + reader.groupSize(current)
                    while (reader.currentGroup < end) {
                        testGroup(current)
                    }
                }
            }
            testGroup(-1)
        }
    }

    @Test
    fun testWriterParent() {
        val slots = testItems()
        slots.write { writer ->
            fun testGroup(expectedParent: Int) {
                assertEquals(expectedParent, writer.parent)
                val current = writer.currentGroup
                writer.group {
                    val end = current + writer.groupSize(current)
                    while (writer.currentGroup < end) {
                        testGroup(current)
                    }
                }
            }
            testGroup(-1)
        }
    }

    @Test
    fun testReaderParentIndex() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(index: Int, expectedParent: Int): Int {
                assertEquals(expectedParent, reader.parent(index))
                val size = reader.groupSize(index)
                val end = index + size
                var child = index + 1
                while (child < end) {
                    child += testGroup(child, index)
                }
                return size
            }
            testGroup(0, -1)
        }
    }

    @Test
    fun testWriterParentIndex() {
        val slots = testItems()
        slots.write { writer ->
            fun testGroup(index: Int, expectedParent: Int): Int {
                assertEquals(expectedParent, writer.parent(index))
                val size = writer.groupSize(index)
                val end = index + size
                var child = index + 1
                while (child < end) {
                    child += testGroup(child, index)
                }
                return size
            }
            testGroup(0, -1)
        }
    }

    @Test
    fun testReaderIsNode() {
        val slots = testItems()
        slots.read { reader ->
            var count = 0
            fun countNodes() {
                if (reader.isNode) {
                    count++
                    reader.skipGroup()
                } else {
                    reader.startGroup()
                    while (!reader.isGroupEnd)
                        countNodes()
                    reader.endGroup()
                }
            }
            countNodes()

            assertEquals(reader.nodeCount(0), count)
        }
    }

    @Test
    fun testWriterIsNode() {
        val slots = testItems()
        val expectedCount = slots.read { it.nodeCount(0) }
        slots.write { writer ->
            var count = 0
            fun countNodes() {
                if (writer.isNode) {
                    count++
                    writer.skipGroup()
                } else {
                    writer.startGroup()
                    while (!writer.isGroupEnd)
                        countNodes()
                    writer.endGroup()
                }
            }
            countNodes()

            assertEquals(expectedCount, count)
        }
    }

    @Test
    fun testReaderIsNodeIndex() {
        val slots = testItems()
        slots.read { reader ->
            var count = 0
            fun countNodes(index: Int): Int {
                val size = reader.groupSize(index)
                if (reader.isNode(index)) count++
                else {
                    val end = index + size
                    var child = index + 1
                    while (child < end) {
                        child += countNodes(child)
                    }
                }
                return size
            }
            countNodes(0)

            assertEquals(reader.nodeCount(0), count)
        }
    }

    @Test
    fun testReaderNodeIndex() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(index: Int): Int {
                if (reader.isNode(index)) {
                    assertEquals(
                        expected = "node for key ${reader.groupObjectKey(index)}",
                        actual = reader.node(index)
                    )
                }
                val size = reader.groupSize(index)
                val end = index + size
                var child = index + 1
                while (child < end) {
                    child += testGroup(child)
                }
                return size
            }
            testGroup(0)
        }
    }

    @Test
    fun testWriterNodeIndex() {
        val slots = testItems()
        slots.write { writer ->
            fun testGroup(index: Int): Int {
                val node = writer.node(index)
                if (node != null) {
                    assertEquals(
                        expected = "node for key ${writer.groupObjectKey(index)}",
                        actual = node
                    )
                }
                val size = writer.groupSize(index)
                val end = index + size
                var child = index + 1
                while (child < end) {
                    child += testGroup(child)
                }
                return size
            }
            testGroup(0)
        }
    }

    @Test
    fun testReaderHasObjectKeyIndex() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(index: Int): Int {
                if (!reader.isNode(index) && reader.hasObjectKey(index)) {
                    assertEquals(reader.groupKey(index), reader.groupObjectKey(index))
                }
                val size = reader.groupSize(index)
                val end = index + size
                var child = index + 1
                while (child < end) {
                    child += testGroup(child)
                }
                return size
            }
            testGroup(0)
        }
    }

    @Test
    fun testGroupEnd() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup() {
                reader.startGroup()
                val expectedEnd = reader.groupEnd
                while (!reader.isGroupEnd) {
                    testGroup()
                }
                assertEquals(expectedEnd, reader.currentGroup)
                reader.endGroup()
            }
            testGroup()
        }
    }

    @Test
    fun testGroupEndByIndex() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup(index: Int): Int {
                val expectedGroupEnd = reader.groupEnd(index)
                val size = reader.groupSize(index)
                var child = index + 1
                val end = index + size
                while (child < end) {
                    child += testGroup(child)
                }
                assertEquals(child, expectedGroupEnd)
                return size
            }

            testGroup(0)
        }
    }

    @Test
    fun testCurrentEnd() {
        val slots = testItems()
        slots.read { reader ->
            fun testGroup() {
                reader.startGroup()
                val expectedEnd = reader.groupEnd
                while (!reader.isGroupEnd) {
                    assertEquals(expectedEnd, reader.currentEnd)
                    testGroup()
                }
                reader.endGroup()
            }
            testGroup()
        }
    }

    @Test
    fun testReaderGroupAux() {
        val slots = SlotTable()
        val object1 = object {}
        val object2 = object {}
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            writer.startData(1, object1)
            writer.endGroup()
            writer.startData(2, 2, object2)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        slots.read { reader ->
            reader.startGroup()
            assertEquals(object1, reader.groupAux)
            reader.skipGroup()
            assertEquals(object2, reader.groupAux)
            reader.skipGroup()
            reader.endGroup()
        }
    }

    @Test
    fun testReaderGroupAuxByIndex() {
        val slots = SlotTable()
        val object1 = object {}
        val object2 = object {}
        var object1Index = 0
        var object2Index = 0
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            object1Index = writer.currentGroup
            writer.startData(1, object1)
            writer.endGroup()
            object2Index = writer.currentGroup
            writer.startData(2, 2, object2)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        slots.read { reader ->
            assertEquals(object1, reader.groupAux(object1Index))
            assertEquals(object2, reader.groupAux(object2Index))
        }
    }

    @Test
    fun testWriterGroupAuxByIndex() {
        val slots = SlotTable()
        val object1 = object {}
        val object2 = object {}
        var object1Index = 0
        var object2Index = 0
        var emptyIndex = 0
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            object1Index = writer.currentGroup
            writer.startData(1, object1)
            writer.endGroup()
            object2Index = writer.currentGroup
            writer.startData(2, 2, object2)
            writer.endGroup()
            emptyIndex = writer.currentGroup
            writer.startGroup(3)
            writer.endGroup()
            writer.endGroup()
            writer.endInsert()
        }
        slots.write { writer ->
            assertEquals(object1, writer.groupAux(object1Index))
            assertEquals(object2, writer.groupAux(object2Index))
            assertEquals(Composer.Empty, writer.groupAux(emptyIndex))
        }
    }

    @Test
    fun testWriterGroupKeyByIndex() {
        val slots = testItems()
        val keyIndexMap = mutableMapOf<Int, Int>()
        slots.read { reader ->
            fun collectGroup() {
                keyIndexMap[reader.currentGroup] = reader.groupKey
                reader.startGroup()
                while (!reader.isGroupEnd) {
                    collectGroup()
                }
                reader.endGroup()
            }

            collectGroup()
        }
        slots.write { writer ->
            for ((index, expectedKey) in keyIndexMap) {
                assertEquals(expectedKey, writer.groupKey(index))
            }
        }
    }

    @Test
    fun testWriterGroupObjectKeyByIndex() {
        val slots = testItems()
        val keyIndexMap = mutableMapOf<Int, Any?>()
        slots.read { reader ->
            fun collectGroup() {
                keyIndexMap[reader.currentGroup] = reader.groupObjectKey
                reader.startGroup()
                while (!reader.isGroupEnd) {
                    collectGroup()
                }
                reader.endGroup()
            }

            collectGroup()
        }
        slots.write { writer ->
            for ((index, expectedKey) in keyIndexMap) {
                assertEquals(expectedKey, writer.groupObjectKey(index))
            }
        }
    }

    @Test
    fun testReposition() {
        val slots = testItems()
        val parentsOf = mutableMapOf<Int, Int>()
        slots.read { reader ->
            fun collectGroup() {
                parentsOf[reader.currentGroup] = reader.parent
                reader.startGroup()
                while (!reader.isGroupEnd) {
                    collectGroup()
                }
                reader.endGroup()
            }
            collectGroup()
        }

        slots.read { reader ->
            for ((index, parent) in parentsOf) {
                reader.reposition(index)
                assertEquals(parent, reader.parent)
            }
        }
    }

    @Test
    fun testUpdatingNodeWithStartNode() {
        val slots = SlotTable()
        val anchors = mutableListOf<Anchor>()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(10) {
                        // start a node group with its node.
                        anchors.add(writer.anchor())
                        writer.startNode(30)
                        writer.endGroup()
                        // start another node the same way
                        anchors.add(writer.anchor())
                        writer.startNode(40)
                        writer.endGroup()
                    }
                }
            }
        }

        slots.write { writer ->
            val index = anchors[0].toIndexFor(writer)
            writer.advanceBy(index - writer.currentGroup)
            writer.ensureStarted(writer.parent(index))
            writer.startNode(30, node = 300)
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.advanceBy(anchors[1].toIndexFor(writer) - writer.currentGroup)
            writer.startNode(40, node = 400)
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }

        slots.read { reader ->
            reader.group {
                reader.group(10) {
                    assertEquals(30, reader.groupObjectKey)
                    assertEquals(300, reader.groupNode)
                    reader.skipGroup()
                    assertEquals(40, reader.groupObjectKey)
                    assertEquals(400, reader.groupNode)
                    reader.skipGroup()
                }
            }
        }
    }

    @Test
    fun testSeekToInsertingAtTheEndOfTheTable() {
        val slots = SlotTable()
        var parentAnchor = slots.read { it.anchor() }
        var insertAnchor = slots.read { it.anchor() }
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(100) {
                        parentAnchor = writer.anchor()
                        writer.group(10) {
                            writer.nodeGroup(5, 500)
                            writer.nodeGroup(6, 600)
                            insertAnchor = writer.anchor()
                        }
                    }
                }
            }
        }

        slots.verifyWellFormed()

        slots.write { writer ->
            writer.startGroup()
            writer.seek(parentAnchor)
            writer.startGroup()
            writer.seek(insertAnchor)
            writer.beginInsert()
            writer.nodeGroup(7, 700)
            writer.endInsert()
            writer.endGroup()
            writer.endGroup()
        }

        slots.verifyWellFormed()

        slots.read { reader ->
            reader.group(treeRoot) {
                reader.group(100) {
                    reader.group(10) {
                        reader.expectNode(5, 500) { }
                        reader.expectNode(6, 600) { }
                        reader.expectNode(7, 700) { }
                    }
                }
            }
        }
    }

    @Test
    fun testSeekToInsertingAtTheStartOfAGroup() {
        val slots = SlotTable()
        var parentAnchor = slots.read { it.anchor() }
        var insertAnchor = slots.read { it.anchor() }
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(100) {
                        parentAnchor = writer.anchor()
                        writer.group(10) {
                            insertAnchor = writer.anchor()
                            writer.nodeGroup(5, 500)
                            writer.nodeGroup(6, 600)
                        }
                    }
                }
            }
        }

        slots.verifyWellFormed()

        slots.write { writer ->
            writer.startGroup()
            writer.seek(insertAnchor)
            writer.ensureStarted(parentAnchor)
            writer.beginInsert()
            writer.nodeGroup(7, 700)
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.endGroup()
        }

        slots.verifyWellFormed()

        slots.read { reader ->
            reader.group(treeRoot) {
                reader.group(100) {
                    reader.group(10) {
                        reader.expectNode(7, 700) { }
                        reader.expectNode(5, 500) { }
                        reader.expectNode(6, 600) { }
                    }
                }
            }
        }
    }

    @Test
    fun testSeekToInsertingAtInTheMiddleOfAGroup() {
        val slots = SlotTable()
        var parentAnchor = slots.read { it.anchor() }
        var insertAnchor = slots.read { it.anchor() }
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(100) {
                        parentAnchor = writer.anchor()
                        writer.group(10) {
                            writer.nodeGroup(5, 500)
                            insertAnchor = writer.anchor()
                            writer.nodeGroup(6, 600)
                        }
                    }
                }
            }
        }

        slots.verifyWellFormed()

        slots.write { writer ->
            writer.startGroup()
            writer.seek(insertAnchor)
            writer.ensureStarted(parentAnchor)
            writer.beginInsert()
            writer.nodeGroup(7, 700)
            writer.endInsert()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.endGroup()
        }

        slots.verifyWellFormed()

        slots.read { reader ->
            reader.group(treeRoot) {
                reader.group(100) {
                    reader.group(10) {
                        reader.expectNode(5, 500) { }
                        reader.expectNode(7, 700) { }
                        reader.expectNode(6, 600) { }
                    }
                }
            }
        }
    }

    @Test
    fun testUpdatingNodeWithUpdateParentNode() {
        val slots = SlotTable()
        val anchors = mutableListOf<Anchor>()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(10) {
                        // start a node group with its node.
                        anchors.add(writer.anchor())
                        writer.startNode(30)
                        writer.endGroup()
                        // start another node the same way
                        anchors.add(writer.anchor())
                        writer.startNode(40)
                        writer.endGroup()
                    }
                }
            }
        }

        slots.write { writer ->
            writer.group {
                writer.advanceBy(anchors[0].toIndexFor(writer) - writer.currentGroup)
                writer.startGroup()
                writer.updateParentNode(300)
                writer.endGroup()
                writer.advanceBy(anchors[1].toIndexFor(writer) - writer.currentGroup)
                writer.startGroup()
                writer.updateParentNode(400)
                writer.endGroup()
                writer.skipToGroupEnd()
            }
        }

        slots.read { reader ->
            reader.group {
                reader.group(10) {
                    assertEquals(30, reader.groupObjectKey)
                    assertEquals(300, reader.groupNode)
                    reader.skipGroup()
                    assertEquals(40, reader.groupObjectKey)
                    assertEquals(400, reader.groupNode)
                    reader.skipGroup()
                }
            }
        }
    }

    @Test
    fun testUpdatingNodeWithUpdateNode() {
        val slots = SlotTable()
        val anchors = mutableListOf<Anchor>()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(10) {
                        // start a node group with its node.
                        anchors.add(writer.anchor())
                        writer.startNode(30)
                        writer.endGroup()
                        // start another node the same way
                        anchors.add(writer.anchor())
                        writer.startNode(40)
                        writer.endGroup()
                    }
                }
            }
        }

        slots.write { writer ->
            writer.group {
                writer.advanceBy(anchors[0].toIndexFor(writer) - writer.currentGroup)
                writer.updateNode(300)
                writer.advanceBy(anchors[1].toIndexFor(writer) - writer.currentGroup)
                writer.updateNode(400)
                writer.skipToGroupEnd()
            }
        }

        slots.read { reader ->
            reader.group {
                reader.group(10) {
                    assertEquals(30, reader.groupObjectKey)
                    assertEquals(300, reader.groupNode)
                    reader.skipGroup()
                    assertEquals(40, reader.groupObjectKey)
                    assertEquals(400, reader.groupNode)
                    reader.skipGroup()
                }
            }
        }
    }

    @Test
    fun testUpdatingAuxWithUpdateAux() {
        val slots = SlotTable()
        val anchors = mutableListOf<Anchor>()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    writer.group(10) {
                        anchors.add(writer.anchor())
                        writer.startData(30, null)
                        writer.endGroup()
                        anchors.add(writer.anchor())
                        writer.startData(40, null)
                        writer.endGroup()
                    }
                }
            }
        }

        slots.write { writer ->
            writer.group {
                writer.advanceBy(anchors[0].toIndexFor(writer) - writer.currentGroup)
                writer.updateAux(300)
                writer.advanceBy(anchors[1].toIndexFor(writer) - writer.currentGroup)
                writer.updateAux(400)
                writer.skipToGroupEnd()
            }
        }

        slots.read { reader ->
            reader.group {
                reader.group(10) {
                    assertEquals(30, reader.groupKey)
                    assertEquals(300, reader.groupAux)
                    reader.skipGroup()
                    assertEquals(40, reader.groupKey)
                    assertEquals(400, reader.groupAux)
                    reader.skipGroup()
                }
            }
        }
    }

    @Test
    fun testWriterSetByIndex() {
        val slots = SlotTable()
        val outerGroups = 10
        val outerGroupKeyBase = 100
        val innerGroups = 10
        val innerGroupKeyBase = 1000
        val dataCount = 5

        data class SlotInfo(
            val anchor: Anchor,
            val index: Int,
            val value: Int
        )

        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    repeat(outerGroups) { outerKey ->
                        writer.group(outerKey + outerGroupKeyBase) {
                            repeat(innerGroups) { innerKey ->
                                writer.group(innerKey + innerGroupKeyBase) {
                                    repeat(dataCount) {
                                        writer.update(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun validate(dataOffset: Int = 0): List<SlotInfo> {
            val slotInfo = mutableListOf<SlotInfo>()
            slots.read { reader ->
                reader.group(treeRoot) {
                    repeat(outerGroups) { outerKey ->
                        reader.group(outerKey + outerGroupKeyBase) {
                            repeat(innerGroups) { innerKey ->
                                val anchor = reader.anchor()
                                reader.group(innerKey + innerGroupKeyBase) {
                                    repeat(dataCount) {
                                        val index = reader.groupSlotIndex
                                        val value = reader.next() as Int
                                        slotInfo.add(SlotInfo(anchor, index, value))
                                        assertEquals(it + dataOffset, value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return slotInfo
        }

        val slotInfo = validate()

        val dataOffset = 10
        slots.write { writer ->
            for ((anchor, index, value) in slotInfo) {
                writer.seek(anchor)
                val previous = writer.set(index, value + dataOffset)
                assertEquals(value, previous)
            }
        }

        validate(dataOffset)
    }

    @Test
    fun testReaderSlot() {
        val groups = 10
        val items = 10
        val slots = SlotTable()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    repeat(groups) { key ->
                        writer.group(key) {
                            repeat(items) { item ->
                                writer.update(item)
                            }
                        }
                    }
                }
            }
        }
        slots.read { reader ->
            reader.group {
                repeat(groups) {
                    reader.group {
                        repeat(items) { item ->
                            assertEquals(reader.slot, item)
                            assertEquals(item, reader.next())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testWriterSkip() {
        val groups = 10
        val items = 10
        val slots = SlotTable()
        slots.write { writer ->
            writer.insert {
                writer.group(treeRoot) {
                    repeat(groups) { key ->
                        writer.group(key) {
                            repeat(items) { item ->
                                writer.update(item)
                            }
                        }
                    }
                }
            }
        }
        slots.write { writer ->
            writer.group {
                repeat(groups) {
                    writer.group {
                        repeat(items) { item ->
                            if (item % 2 == 0) {
                                writer.update(item * 100)
                            } else {
                                writer.skip()
                            }
                        }
                    }
                }
            }
        }
        slots.read { reader ->
            reader.group {
                repeat(groups) {
                    reader.group {
                        repeat(items) { item ->
                            assertEquals(reader.next(), if (item % 2 == 0) item * 100 else item)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testWriterGroupSlots() {
        val slots = testItems()
        val allSlots = slots.write { writer -> writer.groupSlots() }.toList()
        val sumSlots = slots.write { writer ->
            val list = mutableListOf<Any?>()
            writer.startGroup()
            while (!writer.isGroupEnd) {
                list.addAll(writer.groupSlots().toList())
                writer.skipGroup()
            }
            writer.endGroup()
            list
        }
        assertEquals(allSlots.size, sumSlots.size)
        allSlots.forEachIndexed { index, item ->
            assertEquals(item, sumSlots[index])
        }
    }

    @Test
    fun testWriterClosed() {
        val slots = testItems()
        val writer = slots.openWriter()
        assertFalse(writer.closed)
        writer.close()
        assertTrue(writer.closed)
    }

    @Test
    fun testOwnsAnchor() {
        val (slots1, anchors1) = narrowTrees()
        val (slots2, anchors2) = narrowTrees()
        for (anchor in anchors1) {
            assertTrue(slots1.ownsAnchor(anchor))
            assertFalse(slots2.ownsAnchor(anchor))
        }
        for (anchor in anchors2) {
            assertFalse(slots1.ownsAnchor(anchor))
            assertTrue(slots2.ownsAnchor(anchor))
        }
    }

    @Test
    fun testMultipleRoots() {
        val slots = SlotTable()
        val anchors = mutableListOf<Anchor>()
        repeat(10) {
            slots.write { writer ->
                anchors.add(writer.anchor())
                writer.beginInsert()
                writer.startGroup(it + 100)
                repeat(it) { writer.update(it) }
                repeat(it) {
                    writer.startGroup(it + 1000)
                    writer.endGroup()
                }
                writer.endGroup()
                writer.endInsert()
            }
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testCanRestoreParent() {
        val anchors = mutableMapOf<Int, List<Anchor>>()
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(treeRoot)
            repeat(10) { outerKey ->
                val nestedAnchors = mutableListOf<Anchor>()
                anchors[outerKey] = nestedAnchors
                writer.startGroup(outerKey)
                repeat(10) { innerKey ->
                    writer.startGroup(innerKey + 1000)
                    repeat(10) { anchoredKey ->
                        if (anchoredKey % 3 == 0)
                            nestedAnchors.add(writer.anchor())
                        writer.startGroup(anchoredKey + 2000)
                        writer.update("anchored value")
                        writer.endGroup()
                    }
                    writer.endGroup()
                }
                writer.endGroup()
            }
            writer.endGroup()
            writer.endInsert()
        }
        slots.read { reader ->
            reader.startGroup() // root
            repeat(10) { outerKey ->
                assertEquals(outerKey, reader.groupKey)
                reader.startGroup()
                val nestedAnchors = anchors[outerKey] ?: error("Missing anchor list for $outerKey")
                val parent = reader.parent
                for (anchor in nestedAnchors) {
                    reader.reposition(anchor.toIndexFor(slots))
                    assertTrue(reader.groupKey >= 2000)
                    reader.startGroup()
                    assertEquals("anchored value", reader.next())
                    reader.endGroup()
                }
                reader.restoreParent(parent)
                reader.skipToGroupEnd()
                reader.endGroup()
            }
        }
    }

    @Test
    fun testCanRemoveRootGroup() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup(100, 100)
            writer.endGroup()
            writer.startGroup(200, 200)
            writer.update(300)
            writer.update(400)
            writer.endGroup()
            writer.endInsert()
        }
        slots.read { reader ->
            reader.expectGroup(100, 100)
            reader.expectGroup(200, 200) {
                assertEquals(300, reader.next())
                assertEquals(400, reader.next())
            }
        }
        slots.write { writer ->
            writer.removeGroup()
        }
        slots.read { reader ->
            reader.expectGroup(200, 200) {
                assertEquals(300, reader.next())
                assertEquals(400, reader.next())
            }
        }
        slots.write { writer ->
            writer.removeGroup()
        }
        assertTrue(slots.isEmpty)
    }

    @Test
    fun testReplacesWithZeroSizeGroup() {
        val outerGroupCount = 10
        val outerKeyBase = 0
        val innerGroupCount = 10
        val innerKeyBase = 100
        val bottomGroupCount = 5
        val bottomKeyBase = 1000
        val replaceMod = 2
        val slots = SlotTable().also {
            it.write { writer ->
                writer.beginInsert()
                writer.startGroup(treeRoot)
                repeat(outerGroupCount) { outerKey ->
                    writer.startGroup(outerKeyBase + outerKey)
                    repeat(innerGroupCount) { innerKey ->
                        writer.startGroup(innerKeyBase + innerKey)
                        repeat(bottomGroupCount) { bottomKey ->
                            writer.startGroup(bottomKeyBase + bottomKey, bottomKey)
                            writer.update("Some data")
                            writer.endGroup()
                        }
                        writer.endGroup()
                    }
                    writer.endGroup()
                }
                writer.endGroup()
                writer.endInsert()
            }
        }
        slots.verifyWellFormed()
        val sourceTable = SlotTable().also {
            it.write { writer ->
                writer.beginInsert()
                repeat(outerGroupCount * innerGroupCount) {
                    writer.startGroup(0)
                    writer.endGroup()
                }
                writer.endInsert()
            }
        }
        sourceTable.verifyWellFormed()
        slots.write { writer ->
            writer.startGroup()
            repeat(outerGroupCount) {
                writer.startGroup()
                repeat(innerGroupCount) { innerGroupKey ->
                    if (innerGroupKey % replaceMod == 0) {
                        writer.beginInsert()
                        writer.moveFrom(sourceTable, 0)
                        writer.endInsert()
                        writer.removeGroup()
                    } else {
                        writer.skipGroup()
                    }
                    writer.verifyDataAnchors()
                }
                writer.endGroup()
            }
            writer.endGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testInsertOfZeroGroups() {
        val sourceAnchors = mutableListOf<Anchor>()
        val sourceTable = SlotTable().also {
            it.write { writer ->
                writer.beginInsert()
                sourceAnchors.add(writer.anchor())
                writer.startGroup(0)
                writer.update("0: Some value")
                writer.endGroup()
                sourceAnchors.add(writer.anchor())
                writer.startGroup(0)
                repeat(5) {
                    writer.startGroup(1)
                    writer.endGroup()
                }
                writer.endGroup()
                sourceAnchors.add(writer.anchor())
                writer.startGroup(0)
                writer.endGroup()
                writer.endInsert()
            }
        }

        var container = Anchor(0)
        val destinationAnchors = mutableListOf<Anchor>()
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.update("10: Some data")
                        }
                        container = writer.anchor()
                        writer.group(100) {
                            destinationAnchors.add(writer.anchor())
                            writer.group(500) { }
                            destinationAnchors.add(writer.anchor())
                            writer.group(1000) {
                                writer.update("1000: Some data")
                            }
                            destinationAnchors.add(writer.anchor())
                            writer.group(2000) {
                                writer.update("2000: Some data")
                            }
                        }
                    }
                }
            }
        }

        slots.write { writer ->
            var started = false
            repeat(sourceAnchors.size) {
                val sourceAnchor = sourceAnchors[it]
                val destinationAnchor = destinationAnchors[it]
                writer.advanceBy(destinationAnchor.toIndexFor(writer) - writer.currentGroup)
                if (!started) {
                    writer.ensureStarted(0)
                    writer.ensureStarted(container)
                    started = true
                }
                writer.beginInsert()
                writer.moveFrom(
                    sourceTable,
                    sourceAnchor.toIndexFor(sourceTable)
                )
                writer.endInsert()
            }
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }

        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                reader.expectGroup(10) {
                    reader.expectData("10: Some data")
                }
                reader.expectGroup(100) {
                    reader.expectGroup(0)
                    reader.expectGroup(500)
                    reader.expectGroup(0)
                    reader.expectGroup(1000) {
                        reader.expectData("1000: Some data")
                    }
                    reader.expectGroup(0)
                    reader.expectGroup(2000) {
                        reader.expectData("2000: Some data")
                    }
                }
            }
        }
    }

    @Test
    fun testMoveOfZeroGroup() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.group(100) {
                                writer.update("100: 1")
                                writer.update("100: 2")
                            }
                            writer.group(200) {
                                writer.update("200: 1")
                                writer.update("200: 2")
                            }
                            writer.group(300) {
                                writer.update("300: 1")
                                writer.update("300: 2")
                            }
                            // Empty group
                            writer.group(0) { }
                            writer.group(400) {
                                writer.update("400: 1")
                                writer.update("400: 2")
                            }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                reader.expectGroup(10) {
                    reader.expectGroup(100) {
                        reader.expectData("100: 1")
                        reader.expectData("100: 2")
                    }
                    reader.expectGroup(200) {
                        reader.expectData("200: 1")
                        reader.expectData("200: 2")
                    }
                    reader.expectGroup(300) {
                        reader.expectData("300: 1")
                        reader.expectData("300: 2")
                    }
                    reader.expectGroup(0)
                    reader.expectGroup(400) {
                        reader.expectData("400: 1")
                        reader.expectData("400: 2")
                    }
                }
            }
        }
        slots.write { writer ->
            writer.startGroup()
            writer.startGroup()
            writer.skipGroup()
            writer.insert {
                writer.group(150) {
                    writer.update("150: 1")
                    writer.update("150: 2")
                }
            }
            writer.skipGroup()
            writer.moveGroup(1)
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.endGroup()
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                reader.expectGroup(10) {
                    reader.expectGroup(100) {
                        reader.expectData("100: 1")
                        reader.expectData("100: 2")
                    }
                    reader.expectGroup(150) {
                        reader.expectData("150: 1")
                        reader.expectData("150: 2")
                    }
                    reader.expectGroup(200) {
                        reader.expectData("200: 1")
                        reader.expectData("200: 2")
                    }
                    reader.expectGroup(0)
                    reader.expectGroup(300) {
                        reader.expectData("300: 1")
                        reader.expectData("300: 2")
                    }
                    reader.expectGroup(400) {
                        reader.expectData("400: 1")
                        reader.expectData("400: 2")
                    }
                }
            }
        }
    }

    @Test
    fun testReaderGet() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.update("10: 0")
                            writer.update("10: 1")
                            writer.update("10: 2")
                        }
                        writer.group(20) {
                            writer.update("20: 0")
                            writer.update("20: 1")
                            writer.update("20: 2")
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            reader.startGroup()
            assertEquals("10: 0", reader.get(0))
            assertEquals("10: 1", reader.get(1))
            assertEquals("10: 2", reader.get(2))
            reader.endGroup()
            reader.startGroup()
            assertEquals("20: 0", reader.get(0))
            assertEquals("20: 1", reader.get(1))
            assertEquals("20: 2", reader.get(2))
            reader.endGroup()
            reader.endGroup()
        }
    }

    @Test
    fun testReaderGroupGet() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.update("10: 0")
                            writer.update("10: 1")
                            writer.update("10: 2")
                        }
                        writer.group(20) {
                            writer.update("20: 0")
                            writer.update("20: 1")
                            writer.update("20: 2")
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            reader.startGroup()
            assertEquals("10: 0", reader.groupGet(0))
            assertEquals("10: 1", reader.groupGet(1))
            assertEquals("10: 2", reader.groupGet(2))
            reader.skipGroup()
            assertEquals("20: 0", reader.groupGet(0))
            assertEquals("20: 1", reader.groupGet(1))
            assertEquals("20: 2", reader.groupGet(2))
            reader.skipGroup()
            reader.endGroup()
        }
    }

    @Test
    fun testReaderSize() {
        val slots = testItems()
        slots.read { reader ->
            assertEquals(slots.groupsSize, reader.size)
        }
    }

    @Test
    fun testWriterSize() {
        val slots = testItems()
        slots.write { writer ->
            assertEquals(slots.groupsSize, writer.size)
            writer.startGroup()
            writer.insert {
                writer.group(1000) {
                    writer.update("1000: 1")
                }
            }
            assertEquals(slots.groupsSize + 1, writer.size)
            writer.skipToGroupEnd()
            writer.endGroup()
        }
    }

    @Test
    fun testGroupSlotCount() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            repeat(5) { item ->
                                writer.update("Data $item")
                            }
                        }
                    }
                }
            }
        }
        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                assertEquals(reader.groupSlotCount, 5)
                reader.skipGroup()
            }
        }
    }

    @Test
    fun testRemoveDataBoundaryCondition() {
        // Remove when the slot table contains amount that would make the slotGapSize 0
        // Test insert exactly 64 data slots.
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        repeat(4) {
                            writer.group(it * 10 + 100) {
                                repeat(8) { value ->
                                    writer.update(value)
                                }
                            }
                        }
                        writer.group(1000) {
                            repeat(16) { value ->
                                writer.update(value)
                            }
                        }
                        repeat(2) {
                            writer.group(it * 10 + 200) {
                                repeat(8) { value ->
                                    writer.update(value)
                                }
                            }
                        }
                        repeat(10) {
                            writer.group(300 + it) { }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        slots.write { writer ->
            writer.group(treeRoot) {
                repeat(4) { writer.skipGroup() }
                writer.removeGroup()
                writer.skipGroup()
                writer.set(4, 100)
                writer.skipToGroupEnd()
            }
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testInsertDataBoundaryCondition() {
        // Test insert exactly 64 data slots.
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.group(100) {
                                repeat(10) { item -> writer.update(item) }
                            }
                            writer.group(200) {
                                repeat(10) { item -> writer.update(item) }
                            }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        val sourceTable = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(150) {
                        repeat(64) { item -> writer.update("Inserted item $item") }
                    }
                }
            }
        }
        sourceTable.verifyWellFormed()

        slots.write { writer ->
            writer.group {
                writer.group {
                    writer.skipGroup()
                    writer.insert {
                        writer.moveFrom(sourceTable, 0)
                    }
                    writer.skipToGroupEnd()
                }
            }
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                reader.expectGroup(10) {
                    reader.expectGroup(100) {
                        repeat(10) { item -> reader.expectData(item) }
                    }
                    reader.expectGroup(150) {
                        repeat(64) { item -> reader.expectData("Inserted item $item") }
                    }
                    reader.expectGroup(200) {
                        repeat(10) { item -> reader.expectData(item) }
                    }
                }
            }
        }
    }

    @Test
    fun testGroupsBoundaryCondition() {
        // Test inserting exactly 32 groups with 2 data items each
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        writer.group(10) {
                            writer.group(100) {
                                repeat(10) { item -> writer.update(item) }
                            }
                            writer.group(200) {
                                repeat(10) { item -> writer.update(item) }
                            }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        val sourceTable = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(150) {
                        repeat(2) { item -> writer.update("Inserted item $item") }
                        repeat(31) { key ->
                            writer.group(150 + key) {
                                repeat(2) { item -> writer.update("Inserted item $item") }
                            }
                        }
                    }
                }
            }
        }
        sourceTable.verifyWellFormed()

        slots.write { writer ->
            writer.group {
                writer.group {
                    writer.skipGroup()
                    writer.insert {
                        writer.moveFrom(sourceTable, 0)
                    }
                    writer.skipToGroupEnd()
                }
            }
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.expectGroup(treeRoot) {
                reader.expectGroup(10) {
                    reader.expectGroup(100) {
                        repeat(10) { item -> reader.expectData(item) }
                    }
                    reader.expectGroup(150) {
                        repeat(2) { item -> reader.expectData("Inserted item $item") }
                        repeat(31) { key ->
                            reader.expectGroup(150 + key) {
                                repeat(2) { item -> reader.expectData("Inserted item $item") }
                            }
                        }
                    }
                    reader.expectGroup(200) {
                        repeat(10) { item -> reader.expectData(item) }
                    }
                }
            }
        }
    }

    @Test // regression b/173822943
    fun testGroupInsertBoundaryCondition() {
        // Test inserting when there is an empty gap.
        SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(treeRoot) {
                        repeat(7) { outer ->
                            writer.group(100 + outer) {
                                repeat(8) { inner ->
                                    writer.group(200 + inner) { }
                                }
                            }
                        }
                    }
                }
            }

            it.verifyWellFormed()

            it.write { writer ->
                writer.group {
                    repeat(3) { writer.skipGroup() }
                    writer.insert {
                        writer.group(300) { }
                    }
                    writer.verifyParentAnchors()
                    writer.skipToGroupEnd()
                }
            }
        }
    }

    @Test
    fun canRepositionReaderPastEndOfTable() {
        val slots = SlotTable().also {
            it.write { writer ->
                // Create exactly 256 groups
                repeat(256) {
                    writer.insert {
                        writer.startGroup(0)
                        writer.endGroup()
                    }
                }
            }
        }

        slots.read { reader ->
            reader.reposition(reader.size)
            // Expect the above not to crash.
        }
    }

    @Test
    fun canRemoveFromFullTable() {
        // Create a table that is exactly 64 entries
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    repeat(7) { outer ->
                        writer.group(10 + outer) {
                            repeat(8) { inner ->
                                writer.group(inner) { }
                            }
                        }
                    }
                    writer.group(30) { }
                }
            }
        }
        slots.verifyWellFormed()

        // Remove the first group
        slots.write { writer ->
            writer.removeGroup()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun canInsertAuxData() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    // Insert a normal aux data.
                    writer.startData(10, 10, "10")
                    writer.endGroup()

                    // Insert using insertAux
                    writer.startGroup(20)
                    writer.insertAux("20")
                    writer.endGroup()

                    // Insert using insertAux after a slot value was added.
                    writer.startGroup(30)
                    writer.update(300)
                    writer.insertAux("30")
                    writer.endGroup()

                    // Insert using insertAux after a group with an object key
                    writer.startGroup(40, 40)
                    writer.insertAux("40")
                    writer.endGroup()

                    // Insert aux into an object key with a value slot and then add another value.
                    writer.startGroup(50, 50)
                    writer.update(500)
                    writer.insertAux("50")
                    writer.update(501)
                    writer.endGroup()

                    // Insert aux after two slot values and then add another value.
                    writer.startGroup(60)
                    writer.update(600)
                    writer.update(601)
                    writer.insertAux("60")
                    writer.update(602)
                    writer.endGroup()

                    // Write a trail group to ensure that the slot table is valid after the
                    // insertAux
                    writer.startGroup(1000)
                    writer.update(10000)
                    writer.update(10001)
                    writer.endGroup()
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            assertEquals(10, reader.groupKey)
            assertEquals(10, reader.groupObjectKey)
            assertEquals("10", reader.groupAux)
            reader.skipGroup()
            assertEquals(20, reader.groupKey)
            assertEquals("20", reader.groupAux)
            reader.skipGroup()
            assertEquals(30, reader.groupKey)
            assertEquals("30", reader.groupAux)
            reader.startGroup()
            assertEquals(300, reader.next())
            reader.endGroup()
            assertEquals(40, reader.groupKey)
            assertEquals(40, reader.groupObjectKey)
            assertEquals("40", reader.groupAux)
            reader.skipGroup()
            assertEquals(50, reader.groupKey)
            assertEquals(50, reader.groupObjectKey)
            assertEquals("50", reader.groupAux)
            reader.startGroup()
            assertEquals(500, reader.next())
            assertEquals(501, reader.next())
            reader.endGroup()
            assertEquals(60, reader.groupKey)
            assertEquals("60", reader.groupAux)
            reader.startGroup()
            assertEquals(600, reader.next())
            assertEquals(601, reader.next())
            assertEquals(602, reader.next())
            reader.endGroup()
            assertEquals(1000, reader.groupKey)
            reader.startGroup()
            assertEquals(10000, reader.next())
            assertEquals(10001, reader.next())
            reader.endGroup()
        }
    }

    @Test
    fun incorrectUsageReportsInternalException() = expectError("internal") {
        val table = SlotTable()
        table.write {
            table.write { }
        }
    }

    @Test
    fun prioritySet_Ordering() {
        val set = PrioritySet()

        repeat(100) {
            Random.nextInt().let {
                if (it < Int.MAX_VALUE)
                    set.add(it)
                set.validateHeap()
            }
        }
        var lastValue = Int.MAX_VALUE
        while (set.isNotEmpty()) {
            val m = set.takeMax()
            assertTrue(lastValue > m)
            lastValue = m
        }
    }

    @Test
    fun prioritySet_Completeness() {
        val set = PrioritySet()
        val values = Array(100) { it }.also { it.shuffle() }
        values.forEach {
            set.add(it)
            set.validateHeap()
        }

        repeat(100) {
            val expected = 99 - it
            assertFalse(set.isEmpty())
            assertEquals(expected, set.takeMax())
            set.validateHeap()
        }
        assertTrue(set.isEmpty())
    }

    @Test
    fun prioritySet_Deduplicate() {
        val set = PrioritySet()
        val values = Array(100) { it / 4 }.also { it.shuffle() }
        values.forEach {
            set.add(it)
            set.validateHeap()
        }

        repeat(25) {
            val expected = 24 - it
            assertFalse(set.isEmpty())
            assertEquals(expected, set.takeMax())
            set.validateHeap()
        }

        assertTrue(set.isEmpty())
    }

    @Test
    fun canMarkAGroup() {
        val table = SlotTable()
        table.write { writer ->
            writer.insert {
                writer.group(0) {
                    writer.group(1) {
                        writer.group(2) {
                            writer.markGroup()
                        }
                        writer.group(3) {
                            writer.group(4) { }
                        }
                    }
                    writer.group(5) {
                        writer.markGroup()
                        writer.group(6) {
                            writer.markGroup()
                        }
                    }
                }
            }
        }
        table.verifyWellFormed()
        table.read { reader ->
            fun assertMark() = assertTrue(reader.hasMark(reader.parent))
            fun assertNoMark() = assertFalse(reader.hasMark(reader.parent))
            fun assertContainsMark() = assertTrue(reader.containsMark(reader.parent))
            fun assertDoesNotContainMarks() = assertFalse(reader.containsMark(reader.parent))

            reader.group(0) {
                assertNoMark()
                assertContainsMark()
                reader.group(1) {
                    assertNoMark()
                    assertContainsMark()
                    reader.group(2) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                    reader.group(3) {
                        assertNoMark()
                        assertDoesNotContainMarks()
                        reader.group(4) {
                            assertNoMark()
                            assertDoesNotContainMarks()
                        }
                    }
                }
                reader.group(5) {
                    assertMark()
                    assertContainsMark()
                    reader.group(6) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                }
            }
        }
    }

    @Test
    fun canRemoveAMarkedGroups() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.insert {
                writer.group(0) {
                    repeat(10) { key ->
                        writer.group(key) {
                            if (key % 2 == 0) writer.markGroup()
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            assertTrue(reader.containsMark(0))
        }

        slots.write { writer ->
            writer.group(0) {
                repeat(10) { key ->
                    if (key % 2 == 0)
                        writer.removeGroup()
                    else
                        writer.skipGroup()
                }
            }
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            assertFalse(reader.containsMark(0))
        }
    }

    @Test
    fun canInsertAMarkedGroup() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.insert {
                writer.group(0) {
                    writer.group(1) { }
                }
            }
        }
        slots.verifyWellFormed()

        slots.write { writer ->
            writer.group(0) {
                writer.group(1) {
                    writer.insert {
                        writer.group(2) {
                            writer.markGroup()
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            fun assertMark() = assertTrue(reader.hasMark(reader.parent))
            fun assertNoMark() = assertFalse(reader.hasMark(reader.parent))
            fun assertContainsMark() = assertTrue(reader.containsMark(reader.parent))
            fun assertDoesNotContainMarks() = assertFalse(reader.containsMark(reader.parent))

            reader.group(0) {
                assertNoMark()
                assertContainsMark()
                reader.group(1) {
                    assertNoMark()
                    assertContainsMark()
                    reader.group(2) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                }
            }
        }
    }

    @Test
    fun canInsertAMarkedTableGroup() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.insert {
                writer.group(0) { }
            }
        }
        slots.verifyWellFormed()

        val insertTable = SlotTable()
        insertTable.write { writer ->
            writer.insert {
                writer.group(1) {
                    writer.group(2) {
                        writer.markGroup()
                    }
                }
            }
        }
        insertTable.verifyWellFormed()

        slots.write { writer ->
            writer.group(0) {
                writer.insert {
                    writer.moveFrom(insertTable, 0)
                }
            }
        }
        slots.verifyWellFormed()
        slots.read { reader ->
            assertTrue(reader.containsMark(0))
        }
    }

    @Test
    fun canMoveTo() {
        val slots = SlotTable()
        var anchor = Anchor(-1)

        // Create a slot table
        slots.write { writer ->
            writer.insert {
                writer.group(100) {
                    writer.group(200) {
                        repeat(5) {
                            writer.group(1000 + it) {
                                writer.group(2000 + it) {
                                    if (it == 3) {
                                        anchor = writer.anchor(writer.parent)
                                        writer.markGroup(writer.parent)
                                    }
                                    repeat(it) { node ->
                                        writer.nodeGroup(2000 + node, node)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(slots.ownsAnchor(anchor))

        // Move the anchored group into another table
        val movedNodes = SlotTable()
        movedNodes.write { movedNodesWriter ->
            movedNodesWriter.insert {
                slots.write { writer ->
                    writer.group {
                        writer.group {
                            repeat(5) {
                                if (it == 3) {
                                    writer.moveTo(anchor, 0, movedNodesWriter)
                                }
                                writer.skipGroup()
                            }
                        }
                    }
                }
            }
        }

        // Validate the slot table
        assertFalse(slots.ownsAnchor(anchor))
        assertTrue(movedNodes.ownsAnchor(anchor))
        slots.verifyWellFormed()
        movedNodes.verifyWellFormed()

        slots.read { reader ->
            reader.expectGroup(100) {
                reader.expectGroup(200) {
                    repeat(5) {
                        reader.expectGroup(1000 + it) {
                            if (it != 3) {
                                reader.expectGroup(2000 + it) {
                                    repeat(it) { node ->
                                        reader.expectNode(2000 + node, node)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        movedNodes.read { reader ->
            reader.expectGroup(2003) {
                repeat(3) { node ->
                    reader.expectNode(2000 + node, node)
                }
            }
        }

        // Insert the nodes back
        slots.write { writer ->
            writer.group {
                writer.group {
                    repeat(5) {
                        if (it == 3) {
                            writer.group {
                                writer.insert {
                                    writer.moveFrom(movedNodes, 0)
                                }
                            }
                        } else writer.skipGroup()
                    }
                }
            }
        }

        // Validate the move back
        assertTrue(slots.ownsAnchor(anchor))
        assertFalse(movedNodes.ownsAnchor(anchor))
        slots.verifyWellFormed()
        movedNodes.verifyWellFormed()

        assertEquals(0, movedNodes.groupsSize)

        slots.read { reader ->
            reader.expectGroup(100) {
                reader.expectGroup(200) {
                    repeat(5) {
                        reader.expectGroup(1000 + it) {
                            reader.expectGroup(2000 + it) {
                                repeat(it) { node ->
                                    reader.expectNode(2000 + node, node)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canDeleteAGroupAfterMovingPartOfItsContent() {
        val slots = SlotTable()
        var deleteAnchor = Anchor(-1)
        var moveAnchor = Anchor(-1)

        // Create a slot table
        slots.write { writer ->
            writer.insert {
                writer.group(100) {
                    writer.group(200) {
                        writer.group(300) {
                            writer.group(400) {
                                writer.group(500) {
                                    deleteAnchor = writer.anchor(writer.parent)
                                    writer.nodeGroup(501, 501) {
                                        writer.group(600) {
                                            writer.group(700) {
                                                moveAnchor = writer.anchor(writer.parent)
                                                writer.markGroup(writer.parent)
                                                writer.group(800) {
                                                    writer.nodeGroup(801, 801)
                                                }
                                                writer.group(900) {
                                                    writer.nodeGroup(901, 901)
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

        val movedNodes = SlotTable()
        movedNodes.write { movedNodesWriter ->
            movedNodesWriter.insert {
                slots.write { writer ->
                    val deleteLocation = writer.anchorIndex(deleteAnchor)

                    writer.advanceBy(deleteLocation)
                    writer.ensureStarted(0)
                    writer.ensureStarted(writer.parent(deleteLocation))
                    writer.moveTo(moveAnchor, 0, movedNodesWriter)
                    assertEquals(deleteLocation, writer.currentGroup)
                    writer.removeGroup()
                    writer.skipToGroupEnd()
                    writer.endGroup()
                    writer.skipToGroupEnd()
                    writer.endGroup()
                }
            }
        }

        movedNodes.verifyWellFormed()
        slots.verifyWellFormed()

        // Validate slots
        slots.read { reader ->
            reader.expectGroup(100) {
                reader.expectGroup(200) {
                    reader.expectGroup(300) {
                        reader.expectGroup(400)
                    }
                }
            }
        }

        // Validate moved nodes
        movedNodes.read { reader ->
            reader.expectGroup(700) {
                reader.expectGroup(800) {
                    reader.expectNode(801, 801)
                }
                reader.expectGroup(900) {
                    reader.expectNode(901, 901)
                }
            }
        }
    }

    @Test
    fun canMoveAndDeleteAfterAnInsert() {
        val slots = SlotTable()
        var insertAnchor = Anchor(-1)
        var deleteAnchor = Anchor(-1)
        var moveAnchor = Anchor(-1)

        // Create a slot table
        slots.write { writer ->
            writer.insert {
                writer.group(100) {
                    writer.group(200) {
                        writer.group(300) {
                            writer.group(400) {
                                writer.group(450) {
                                    insertAnchor = writer.anchor(writer.parent)
                                }
                                writer.group(500) {
                                    deleteAnchor = writer.anchor(writer.parent)
                                    writer.nodeGroup(501, 501) {
                                        writer.group(600) {
                                            writer.group(700) {
                                                moveAnchor = writer.anchor(writer.parent)
                                                writer.markGroup(writer.parent)
                                                writer.group(800) {
                                                    writer.nodeGroup(801, 801)
                                                }
                                                writer.group(900) {
                                                    writer.nodeGroup(901, 901)
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

        val movedNodes = SlotTable()
        movedNodes.write { movedNodesWriter ->
            movedNodesWriter.insert {
                slots.write { writer ->
                    writer.seek(insertAnchor)
                    writer.ensureStarted(0)
                    writer.group() {
                        writer.insert {
                            writer.group(455) {
                                writer.nodeGroup(456, 456)
                            }
                        }
                    }

                    // Move and delete
                    val deleteLocation = writer.anchorIndex(deleteAnchor)
                    writer.seek(deleteAnchor)
                    assertEquals(deleteLocation, writer.currentGroup)
                    writer.ensureStarted(0)
                    writer.ensureStarted(writer.parent(deleteLocation))
                    writer.moveTo(moveAnchor, 0, movedNodesWriter)
                    assertEquals(deleteLocation, writer.currentGroup)
                    writer.removeGroup()
                    writer.skipToGroupEnd()
                    writer.endGroup()
                    writer.skipToGroupEnd()
                    writer.endGroup()
                }
            }
        }

        movedNodes.verifyWellFormed()
        slots.verifyWellFormed()
    }

    @Test
    fun canMoveAGroupFromATableIntoAnotherGroup() {
        val slots = SlotTable()
        var insertAnchor = Anchor(-1)

        // Create a slot table
        slots.write { writer ->
            writer.insert {
                writer.group(100) {
                    writer.group(200) {
                        writer.group(300) {
                            writer.group(400) {
                                writer.group(410) {
                                    writer.update(1)
                                    writer.update(2)
                                }
                                writer.group(450) {
                                    insertAnchor = writer.anchor(writer.parent)
                                }
                                writer.group(460) {
                                    writer.update(3)
                                    writer.update(4)
                                }
                            }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        val insertTable = SlotTable()
        insertTable.write { writer ->
            writer.insert {
                writer.group(1000) {
                    writer.update(100)
                    writer.update(200)
                    writer.nodeGroup(125, 1000)
                    writer.nodeGroup(125, 2000)
                }
            }
        }
        insertTable.verifyWellFormed()

        slots.write { writer ->
            writer.seek(insertAnchor)
            writer.ensureStarted(0)
            writer.ensureStarted(writer.parent(writer.currentGroup))
            writer.moveIntoGroupFrom(0, insertTable, 0)
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
        }
        slots.verifyWellFormed()

        slots.read { reader ->
            reader.expectGroup(100) {
                reader.expectGroup(200) {
                    reader.expectGroup(300) {
                        reader.expectGroup(400) {
                            reader.expectGroup(410) {
                                reader.expectData(1)
                                reader.expectData(2)
                            }
                            reader.expectGroup(450) {
                                reader.expectGroup(1000) {
                                    reader.expectData(100)
                                    reader.expectData(200)
                                    reader.expectNode(125, 1000)
                                    reader.expectNode(125, 2000)
                                }
                            }
                            reader.expectGroup(460) {
                                reader.expectData(3)
                                reader.expectData(4)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canMoveAGroupFromATableIntoAnotherGroupAndModifyThatGroup() {
        val slots = SlotTable()
        var insertAnchor = Anchor(-1)

        // Create a slot table
        slots.write { writer ->
            writer.insert {
                writer.group(100) {
                    writer.group(200) {
                        writer.group(300) {
                            writer.group(400) {
                                writer.group(410) {
                                    writer.update(1)
                                    writer.update(2)
                                }
                                writer.group(450) {
                                    insertAnchor = writer.anchor(writer.parent)
                                }
                                writer.group(460) {
                                    writer.update(3)
                                    writer.update(4)
                                }
                            }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        val insertTable = SlotTable()
        insertTable.write { writer ->
            writer.insert {
                writer.group(1000) {
                    writer.update(100)
                    writer.update(200)
                    writer.nodeGroup(125, 1000)
                    writer.nodeGroup(125, 2000)
                }
            }
        }
        insertTable.verifyWellFormed()

        val (previous1, previous2) = slots.write { writer ->
            writer.seek(insertAnchor)
            writer.ensureStarted(0)
            writer.ensureStarted(writer.parent(writer.currentGroup))
            writer.moveIntoGroupFrom(0, insertTable, 0)
            writer.startGroup()
            writer.startGroup()
            val previous1 = writer.update(300)
            val previous2 = writer.update(400)
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
            writer.skipToGroupEnd()
            writer.endGroup()
            previous1 to previous2
        }
        slots.verifyWellFormed()

        assertEquals(100, previous1)
        assertEquals(200, previous2)

        slots.read { reader ->
            reader.expectGroup(100) {
                reader.expectGroup(200) {
                    reader.expectGroup(300) {
                        reader.expectGroup(400) {
                            reader.expectGroup(410) {
                                reader.expectData(1)
                                reader.expectData(2)
                            }
                            reader.expectGroup(450) {
                                reader.expectGroup(1000) {
                                    reader.expectData(300)
                                    reader.expectData(400)
                                    reader.expectNode(125, 1000)
                                    reader.expectNode(125, 2000)
                                }
                            }
                            reader.expectGroup(460) {
                                reader.expectData(3)
                                reader.expectData(4)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(InternalComposeApi::class)
internal inline fun SlotWriter.group(block: () -> Unit) {
    startGroup()
    block()
    endGroup()
}

@OptIn(InternalComposeApi::class)
internal inline fun SlotWriter.group(key: Int, block: () -> Unit) {
    startGroup(key)
    block()
    endGroup()
}

@OptIn(InternalComposeApi::class)
internal inline fun SlotWriter.nodeGroup(key: Int, node: Any, block: () -> Unit = { }) {
    startNode(key, node)
    block()
    endGroup()
}
@OptIn(InternalComposeApi::class)
internal inline fun SlotWriter.insert(block: () -> Unit) {
    beginInsert()
    block()
    endInsert()
}

@OptIn(InternalComposeApi::class)
internal inline fun SlotReader.group(key: Int, block: () -> Unit) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    endGroup()
}

@OptIn(InternalComposeApi::class)
internal inline fun SlotReader.group(block: () -> Unit) {
    startGroup()
    block()
    endGroup()
}

@OptIn(InternalComposeApi::class)
private inline fun SlotReader.expectNode(key: Int, node: Any, block: () -> Unit = { }) {
    assertEquals(key, groupObjectKey)
    assertEquals(node, groupNode)
    startNode()
    block()
    endGroup()
}

private const val treeRoot = -1
private const val elementKey = 100

@OptIn(InternalComposeApi::class)
private fun testSlotsNumbered(): SlotTable {
    val slotTable = SlotTable()
    slotTable.write { writer ->
        writer.beginInsert()
        writer.startGroup(treeRoot)
        repeat(100) {
            writer.startGroup(it)
            writer.endGroup()
        }
        writer.endGroup()
        writer.endInsert()
    }
    return slotTable
}

// Creates 0 until 10 items each with 10 elements numbered 0...n with 0..n slots
@OptIn(InternalComposeApi::class)
private fun testItems(): SlotTable {
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
            writer.startNode(key, "node for key $key")
            block()
            writer.endGroup()
        }

        for (key in 1..10) {
            item(key) {
                for (item in 0..key) {
                    element(key * elementKey + item) {
                        for (element in 0..key)
                            writer.update(-element)
                    }
                }
            }
        }

        writer.endGroup()
        writer.endInsert()
    }

    return slots
}

@OptIn(InternalComposeApi::class)
private fun validateItems(slots: SlotTable) {
    slots.read { reader ->
        check(reader.groupKey == treeRoot) { "Invalid root key" }
        reader.startGroup()

        fun item(key: Int, block: () -> Unit) {
            check(reader.groupKey == key) {
                "Unexpected key at ${reader.currentGroup}, expected $key, " +
                    "received ${reader.groupKey}"
            }
            check(reader.groupObjectKey == key) {
                "Unexpected data key at ${reader.currentGroup}, expected $key, received ${
                reader.groupObjectKey
                }"
            }
            reader.startGroup()
            block()
            reader.endGroup()
        }

        fun element(key: Int, block: () -> Unit) {
            check(reader.isNode) { "Expected a node group" }
            check(reader.groupObjectKey == key) { "Invalid node key at ${reader.currentGroup}" }
            check(reader.groupNode == "node for key $key") {
                "Unexpected node value at ${reader.currentGroup}"
            }
            reader.startNode()
            block()
            reader.endGroup()
        }

        for (key in 1..10) {
            item(key) {
                for (item in 0..key) {
                    element(key * elementKey + item) {
                        for (element in 0..key) {
                            val received = reader.next()
                            check(-element == received) {
                                "Unexpected slot value $element received $received"
                            }
                        }
                    }
                }
            }
        }

        reader.endGroup()
    }
}

@OptIn(InternalComposeApi::class)
private fun narrowTrees(): Pair<SlotTable, List<Anchor>> {
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
            writer.endGroup()
        }

        fun tree(key: Int, width: Int, depth: Int) {
            anchors.add(writer.anchor())
            item(key) {
                when {
                    width > 0 -> for (childKey in 1..width) {
                        tree(childKey, width - 1, depth + 1)
                    }
                    depth > 0 -> {
                        tree(1001, width, depth - 1)
                    }
                    else -> {
                        repeat(depth + 2) {
                            element(-1) { }
                        }
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

@OptIn(InternalComposeApi::class)
private fun SlotReader.expectGroup(key: Int): Int {
    assertEquals(key, groupKey)
    return skipGroup()
}

@OptIn(InternalComposeApi::class)
private fun SlotReader.expectGroup(
    key: Int,
    block: () -> Unit
) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    endGroup()
}

@OptIn(InternalComposeApi::class)
private fun SlotReader.expectData(value: Any) {
    assertEquals(value, next())
}

@OptIn(InternalComposeApi::class)
private fun SlotReader.expectGroup(
    key: Int,
    objectKey: Any?,
    block: () -> Unit = { skipToGroupEnd() }
) {
    assertEquals(key, groupKey)
    assertEquals(objectKey, groupObjectKey)
    startGroup()
    block()
    endGroup()
}

private fun <T> Iterator<T>.toList(): List<T> {
    val list = mutableListOf<T>()
    while (hasNext()) {
        list.add(next())
    }
    return list
}

internal fun expectError(message: String, block: () -> Unit) {
    var exceptionThrown = false
    try {
        block()
    } catch (e: Throwable) {
        exceptionThrown = true
        assertTrue(
            e.message?.contains(message) == true,
            "Expected \"${e.message}\" to contain \"$message\""
        )
    }
    assertTrue(
        exceptionThrown,
        "Expected test to throw an exception containing \"$message\""
    )
}