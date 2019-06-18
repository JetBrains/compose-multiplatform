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

package androidx.compose

import junit.framework.TestCase
import org.junit.Assert

class SlotTableTests : TestCase() {
    fun testCanCreate() {
        SlotTable()
    }

    // Raw slot tests (testing the buffer gap)

    fun testCanInsert() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.skip()
            writer.endGroup()
            writer.endInsert()
        }
    }

    fun testValidateSlots() {
        val slots = testSlotsNumbered()
        slots.read { reader ->
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
            }
        }
    }

    fun testPrevious() {
        val slots = testSlotsNumbered()
        slots.read { reader ->
            for (i in 0 until 100) {
                assertEquals(i, reader.next())
                reader.previous()
                assertEquals(i, reader.next())
            }
            for (i in 99 downTo 0) {
                reader.previous()
                assertEquals(i, reader.next())
                reader.previous()
            }
        }
    }

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

    fun testAnchorTracksExtactRemovesInnerOuter() {
        val slots = testSlotsNumbered()
        val expectedLocations = (1..7).map { it * 10 }.toMutableList()
        val anchors = slots.write { writer -> expectedLocations.map { writer.anchor(it) } }
        slots.write { writer ->
            for (index in listOf(4, 5, 3, 6, 2, 7, 1)) {
                val location = expectedLocations[index-1]
                writer.remove(location, 1)
                assertEquals(-1, anchors[index - 1].location(slots))
                for (i in expectedLocations.indices) {
                    if (expectedLocations[i] > location) expectedLocations[i]--
                }
            }
        }
    }

    fun testAnchorTracksExactRemovesOuterInner() {
        val slots = testSlotsNumbered()
        val expectedLocations = (1..7).map { it * 10 }.toMutableList()
        val anchors = slots.write { writer -> expectedLocations.map { writer.anchor(it) } }
        slots.write { writer ->
            for (index in listOf(1, 7, 2, 6, 3, 5, 4)) {
                val location = expectedLocations[index-1]
                writer.remove(location, 1)
                assertEquals(-1, anchors[index - 1].location(slots))
                for (i in expectedLocations.indices) {
                    if (expectedLocations[i] > location) expectedLocations[i]--
                }
            }
        }
    }

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

    fun testAnchorMoves() {
        val slots = SlotTable()

        fun buildSlots(range: List<Int>): Map<Anchor, Any?> {
            val anchors = mutableListOf<Pair<Anchor, Any?>>()
            slots.write { writer ->
                fun item(value: Any?, block: () -> Unit) {
                    writer.startItem(value)
                    block()
                    writer.endItem()
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
            return anchors.toMap()
        }

        fun validate(anchors: Map<Anchor, Any?>) {
            slots.read { reader ->
                for (anchor in anchors) {
                    assertEquals(anchor.value, reader.get(slots.anchorLocation(anchor.key)))
                }
            }
        }

        fun moveItems() {
            slots.write { writer ->
                writer.skipItem()
                writer.moveItem(4)
                writer.skipItem()
                writer.skipItem()
                writer.moveItem(1)
                writer.skipItem()
                writer.skipItem()
                writer.moveItem(1)
            }
        }

        val expected = listOf(1, 2, 3, 4, 5, 6, 7)
        val anchors = buildSlots(expected)
        validate(anchors)
        moveItems()
        validate(anchors)
    }

    fun testRemovingDuplicateAnchorsMidRange() {
        val slots = testSlotsNumbered()

        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(20, 20) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testRemovingDuplicateAnchorsStartRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(30, 20) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testRemovingDuplicateAnchorsEndRange() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(30) } }
        slots.write { it.remove(20, 11) }
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testDuplicateAnchorIdentity() {
        val slots = testSlotsNumbered()
        val anchors = slots.write { writer -> (0 until 10).map { writer.anchor(it * 5) } }
        slots.write { writer ->
            anchors.forEachIndexed { index, anchor ->
                assertTrue(anchor === writer.anchor(index * 5))
            }
        }
    }

    // Semantic tests (testing groups and nodes)

    fun testExtractKeys() {
        val slots = testItems()
        slots.read { reader ->
            reader.startGroup()
            val keys = reader.extractItemKeys()
            assertEquals(10, keys.size)
            keys.forEachIndexed { i, keyAndLocation ->
                assertEquals(i, keyAndLocation.key)
            }
        }
    }

    fun testMoveAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup()
            writer.moveItem(5)
        }
        slots.read { reader ->
            reader.startGroup()
            reader.expectItem(5)
            for (i in 0 until 5) {
                reader.expectItem(i)
            }
            for (i in 6 until 10) {
                reader.expectItem(i)
            }
            reader.endGroup()
        }
    }

    fun testRemoveAnItem() {
        val slots = testItems()
        slots.write { writer ->
            writer.startGroup()
            for (i in 0 until 5) {
                writer.skipItem()
            }
            writer.removeItem()
            for (i in 6 until 10) {
                writer.skipItem()
            }
            writer.endGroup()
        }

        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 5) {
                reader.expectItem(i)
            }
            for (i in 6 until 10) {
                reader.expectItem(i)
            }
            reader.endGroup()
        }
    }

    fun testCountNodes() {
        val slots = testItems()
        slots.read { reader ->
            reader.startGroup()
            for (i in 0 until 10) {
                val count = reader.expectItem(i)
                assertEquals(i + 1, count)
            }
            reader.endGroup()
        }
    }

    fun testCountNestedNodes() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            repeat(10) {
                writer.startItem(null)
                repeat(3) {
                    writer.startNode()
                    writer.endNode()
                }
                assertEquals(3, writer.endItem())
            }
            assertEquals(30, writer.endItem())
            writer.endGroup()
            writer.endInsert()
        }

        slots.read { reader ->
            reader.startGroup()
            assertEquals(30, reader.expectItem(null))
            reader.endGroup()
        }
    }

    fun testUpdateNestedNodeCountOnInsert() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            repeat(10) {
                writer.startItem(null)
                repeat(3) {
                    writer.startItem(null)
                    writer.startNode()
                    writer.endNode()
                    assertEquals(1, writer.endItem())
                }
                assertEquals(3, writer.endItem())
            }
            assertEquals(30, writer.endItem())
            writer.endGroup()
            writer.endInsert()
        }

        slots.write { writer ->
            writer.startGroup()
            writer.startItem(null)

            repeat(3) {
                assertEquals(3, writer.skipItem())
            }

            writer.startItem(null)
            writer.beginInsert()
            repeat(2) {
                writer.startItem(null)
                writer.startNode()
                writer.endNode()
                assertEquals(1, writer.endItem())
            }
            writer.endInsert()
            repeat(3) { writer.skipItem() }
            assertEquals(5, writer.endItem())

            repeat(6) {
                assertEquals(3, writer.skipItem())
            }

            assertEquals(32, writer.endItem())
            writer.endGroup()
        }
    }

    fun testUpdateNestedNodeCountOnRemove() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            repeat(10) {
                writer.startItem(null)
                repeat(3) {
                    writer.startItem(null)
                    writer.startNode()
                    writer.endNode()
                    assertEquals(1, writer.endItem())
                }
                assertEquals(3, writer.endItem())
            }
            assertEquals(30, writer.endItem())
            writer.endGroup()
            writer.endInsert()
        }

        slots.write { writer ->
            writer.startGroup()
            writer.startItem(null)

            repeat(3) {
                assertEquals(3, writer.skipItem())
            }

            writer.startItem(null)

            repeat(2) { writer.removeItem() }
            repeat(1) { writer.skipItem() }
            assertEquals(1, writer.endItem())

            repeat(6) {
                assertEquals(3, writer.skipItem())
            }

            assertEquals(28, writer.endItem())

            writer.endGroup()
        }
    }

    fun testNodesResetNodeCount() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            writer.startNode()
            repeat(10) {
                writer.startNode()
                writer.startItem(null)
                repeat(3) {
                    writer.startNode()
                    writer.endNode()
                }
                assertEquals(3, writer.endItem())
                writer.endNode()
            }
            writer.endNode()
            assertEquals(1, writer.endItem())
            writer.endGroup()
            writer.endInsert()
        }
    }

    fun testSkipANode() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            writer.startNode()
            repeat(10) {
                writer.startNode()
                writer.startItem(null)
                repeat(3) {
                    writer.startNode()
                    writer.endNode()
                }
                assertEquals(3, writer.endItem())
                writer.endNode()
            }
            writer.endNode()
            assertEquals(1, writer.endItem())
            writer.endGroup()
            writer.endInsert()
        }

        slots.read { reader ->
            reader.startGroup()
            reader.startItem(null)
            assertEquals(1, reader.skipNode())
            reader.endItem()
            reader.endGroup()
        }
    }

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

    fun testReportGroupSize() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startItem(null)
            repeat(10) {
                writer.startNode()
                writer.endNode()
            }
            writer.endItem()
            writer.update(42)
            writer.endGroup()
            writer.endInsert()
        }

        slots.read { reader ->
            reader.startGroup()
            reader.next()
            assertEquals(true, reader.isGroup)
            val size = reader.groupSize
            val savedCurrent = reader.current
            reader.skipGroup()
            assertEquals(size, reader.current - savedCurrent - 1)
            assertEquals(42, reader.next())
            assertEquals(true, reader.isGroupEnd)
            reader.endGroup()
        }
    }

    fun testIsGroups() {
        val slots = SlotTable()
        slots.write { writer ->
            writer.beginInsert()
            writer.startGroup()
            writer.startGroup()
            writer.endGroup()
            writer.startNode()
            writer.endNode()
            writer.endGroup()
            writer.endInsert()
        }

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
            writer.startGroup()
            assertEquals(true, writer.isGroup)
            writer.startGroup()
            assertEquals(false, writer.isGroup)
            writer.endGroup()
            assertEquals(true, writer.isNode)
            writer.startNode()
            assertEquals(false, writer.isNode)
            writer.endNode()
            writer.endGroup()
        }
    }

    fun testReportUncertainNodeCount() {
        val slots = SlotTable()
        slots.read { reader -> reader.reportUncertainNodeCount() }
    }

    fun testMoveGroup() {
        val slots = SlotTable()

        val anchors = mutableListOf<Anchor>()

        fun buildSlots() {
            slots.write { writer ->
                fun group(block: () -> Unit) {
                    writer.startGroup()
                    block()
                    writer.endGroup()
                }

                fun item(key: Any?, block: () -> Unit) {
                    writer.startItem(key)
                    block()
                    writer.endItem()
                }

                fun element(key: Any?, block: () -> Unit) {
                    writer.update(key)
                    writer.startNode()
                    block()
                    writer.endNode()
                }

                fun value(value: Any) {
                    writer.update(value)
                }

                fun innerItem(i: Any) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        group {
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
                                        group {
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
            slots.read { reader ->
                fun group(block: () -> Unit) {
                    reader.startGroup()
                    block()
                    reader.endGroup()
                }

                fun value(value: Any?) {
                    Assert.assertEquals(value, reader.next())
                }

                fun item(key: Any?, block: () -> Unit) {
                    reader.startItem(key)
                    block()
                    reader.endItem()
                }

                fun element(key: Any?, block: () -> Unit) {
                    value(key)
                    reader.startNode()
                    block()
                    reader.endNode()
                }

                fun innerBlock(i: Any) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        group {
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
                                        group {
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
                fun group(block: () -> Unit) {
                    writer.startGroup()
                    block()
                    writer.endGroup()
                }

                fun item(key: Any?, block: () -> Unit) {
                    writer.startItem(key)
                    block()
                    writer.endItem()
                }

                fun element(key: Any?, block: () -> Unit) {
                    writer.update(key)
                    writer.startNode()
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
                                        group {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)

                                                    // Skip three items
                                                    writer.skipItem()
                                                    writer.skipItem()
                                                    writer.skipItem()

                                                    // Move one item up
                                                    writer.moveItem(1)

                                                    // Skip them
                                                    writer.skipItem()
                                                    writer.skipItem()
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
                Assert.assertEquals(62, reader.get(slots.anchorLocation(anchor)))
            }
        }
    }
}

fun testSlotsNumbered(): SlotTable {
    val items = arrayOfNulls<Any?>(100)
    repeat(100) {
        items[it] = it
    }
    return SlotTable(items)
}

private val elementKey = object{}
// Creates 0 until 10 items each with 10 elements numbered 0...n with 0..n slots
fun testItems(): SlotTable {
    val slots = SlotTable()
    slots.write { writer ->
        writer.beginInsert()
        writer.startGroup()

        fun item(key: Any?, block: () -> Unit) {
            writer.startItem(key)
            block()
            writer.endItem()
        }

        fun element(key: Any?, block: () -> Unit) {
            item(key) {
                writer.startNode()
                block()
                writer.endNode()
            }
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

fun SlotReader.startItem(key: Any?) {
    org.junit.Assert.assertEquals(key, next())
    startGroup()
}
fun SlotReader.endItem(): Int = endGroup()

fun SlotReader.expectItem(key: Any?): Int {
    org.junit.Assert.assertEquals(key, next())
    return skipGroup()
}

fun SlotWriter.startItem(key: Any?) {
    update(key)
    startGroup()
}
fun SlotWriter.endItem(): Int = endGroup()
