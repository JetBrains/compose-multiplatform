package com.google.r4a

import junit.framework.TestCase

class SlotTableTests : TestCase() {
    fun testCanCreate() {
        SlotTable()
    }

    // Raw slot tests (testing the buffer gap)

    fun testCanInsert() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.next()
        slots.endGroup()
        slots.endInsert()
    }

    fun testValidateSlots() {
        val slots = testSlotsNumbered()
        for (i in 0 until 100) {
            assertEquals(i, slots.next())
        }
    }

    fun testPrevious() {
        val slots = testSlotsNumbered()
        for (i in 0 until 100) {
            assertEquals(i, slots.next())
            slots.previous()
            assertEquals(i, slots.next())
        }
        for (i in 99 downTo 0) {
            slots.previous()
            assertEquals(i, slots.next())
            slots.previous()
        }
    }

    fun testInsertAtTheStart() {
        val slots = testSlotsNumbered()
        slots.beginInsert()
        slots.next()
        slots.set(-1)
        slots.endInsert()
        slots.reset()
        assertEquals(-1, slots.next())
        for (i in 0 until 100) {
            assertEquals(i, slots.next())
        }
    }

    fun testInsertAtTheEnd() {
        val slots = testSlotsNumbered()
        for (i in 0 until 100) {
            assertEquals(i, slots.next())
        }
        slots.beginInsert()
        slots.next()
        slots.set(-1)
        slots.endInsert()
        slots.reset()
        for (i in 0 until 100) {
            assertEquals(i, slots.next())
        }
        assertEquals(-1, slots.next())
    }

    fun testInsertInTheMiddle() {
        val slots = testSlotsNumbered()
        for (i in 0 until 50) {
            assertEquals(i, slots.next())
        }
        slots.beginInsert()
        slots.next()
        slots.set(-1)
        slots.endInsert()
        slots.reset()
        for (i in 0 until 100) {
            if (i == 50) assertEquals(-1, slots.next())
            assertEquals(i, slots.next())
        }
    }

    fun testRemoveAtTheStart() {
        val slots = testSlotsNumbered()
        slots.remove(0, 50)
        for (i in 50 until 100)
            assertEquals(i, slots.next())
    }

    fun testRemoveAtTheEnd() {
        val slots = testSlotsNumbered()
        slots.remove(50, 50)
        for (i in 0 until 50)
            assertEquals(i, slots.next())
    }

    fun testRemoveInTheMiddle() {
        val slots = testSlotsNumbered()
        slots.remove(25, 50)
        for (i in 0 until 25)
            assertEquals(i, slots.next())
        for (i in 75 until 100)
            assertEquals(i, slots.next())
    }

    fun testRemoveTwoSlicesBackToFront() {
        val slots = testSlotsNumbered()
        slots.remove(70, 10)
        slots.remove(40, 10)
        for (i in 0 until 40)
            assertEquals(i, slots.next())
        for (i in 50 until 70)
            assertEquals(i, slots.next())
        for (i in 80 until 100)
            assertEquals(i, slots.next())
    }

    fun testRemoveTwoSlicesFrontToBack() {
        val slots = testSlotsNumbered()
        slots.remove(40, 10)
        slots.remove(60, 10) // Actually deletes the 70s as they have slid down 10
        for (i in 0 until 40)
            assertEquals(i, slots.next())
        for (i in 50 until 70)
            assertEquals(i, slots.next())
        for (i in 80 until 100)
            assertEquals(i, slots.next())
    }

    // Anchor tests

    fun testAllocateAnchors() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        for (index in 1..7) {
            val anchor = anchors[index - 1]
            assertEquals(index * 10 , slots.get(anchor))
        }
    }

    fun testAnchorsTrackInserts() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        slots.current = 40
        slots.beginInsert()
        repeat(50) { slots.next() }
        slots.endInsert()
        for (index in 1..7) {
            val anchor = anchors[index - 1]
            assertEquals(index * 10 , slots.get(anchor))
        }
    }

    fun testAnchorTracksExactRemovesUpwards() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        for (index in 1..7) {
            slots.remove(index * 10, 1)
            assertEquals(-1, anchors[index - 1].location(slots))
        }
    }

    fun testAnchorTracksExactRemovesDownwards() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        for (index in 7 downTo 1 ) {
            slots.remove(index * 10, 1)
            assertEquals(-1, anchors[index - 1].location(slots))
        }
    }

    fun testAnchorTracksExtactRemovesInnerOuter() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        for (index in listOf(4, 5, 3, 6, 2, 7, 1) ) {
            slots.remove(index * 10, 1)
            assertEquals(-1, anchors[index - 1].location(slots))
        }
    }

    fun testAnchorTracksExactRemovesOuterInner() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        for (index in listOf(1, 7, 2, 6, 3, 5, 4) ) {
            slots.remove(index * 10, 1)
            assertEquals(-1, anchors[index - 1].location(slots))
        }
    }

    fun testAnchorTrackRemoves() {
        val slots = testSlotsNumbered()
        val anchors = (1..7).map { slots.anchor(it * 10) }
        slots.remove(40, 20)
        for (index in 1..7) {
            val anchor = anchors[index - 1]
            val expected = (index * 10).let { if (it in 40..60) SlotTable.EMPTY else it }
            assertEquals(expected , slots.get(anchor))
        }
    }

    fun testRemovingDuplicateAnchorsMidRange() {
        val slots = testSlotsNumbered()
        val anchors = (0 until 10).map { slots.anchor(30) }
        slots.remove(20, 20)
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testRemovingDuplicateAnchorsStartRange() {
        val slots = testSlotsNumbered()
        val anchors = (0 until 10).map { slots.anchor(30) }
        slots.remove(30, 20)
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testRemovingDuplicateAnchorsEndRange() {
        val slots = testSlotsNumbered()
        val anchors = (0 until 10).map { slots.anchor(30) }
        slots.remove(20, 11)
        for (anchor in anchors) {
            assertEquals(-1, anchor.location(slots))
        }
    }

    fun testDuplicateAnchorIdentity() {
        val slots = testSlotsNumbered()
        val anchors = (0 until 10).map { slots.anchor( it * 5) }
        anchors.forEachIndexed { index, anchor ->
            assertTrue(anchor ===  slots.anchor(index * 5))
        }
    }

    // Semantic tests (testing groups and nodes)

    fun testExtractKeys() {
        val slots = testItems()
        slots.startGroup()
        val keys = slots.extractItemKeys()
        assertEquals(10, keys.size)
        keys.forEachIndexed { i, keyAndLocation ->
            assertEquals(i, keyAndLocation.key)
        }
    }

    fun testMoveAnItem() {
        val slots = testItems()
        slots.startGroup()
        slots.moveItem(5)
        slots.expectItem(5)
        for (i in 0 until 5) {
            slots.expectItem(i)
        }
        for (i in 6 until 10) {
            slots.expectItem(i)
        }
        slots.endGroup()
    }

    fun testRemoveAnItem() {
        val slots = testItems()
        slots.startGroup()
        for (i in 0 until 5) {
            slots.expectItem(i)
        }
        slots.removeItem()
        for (i in 6 until 10) {
            slots.skipItem()
        }
        slots.endGroup()

        slots.reset()

        slots.startGroup()
        for (i in 0 until 5) {
            slots.expectItem(i)
        }
        for (i in 6 until 10) {
            slots.expectItem(i)
        }
        slots.endGroup()
    }

    fun testCountNodes() {
        val slots = testItems()
        slots.startGroup()
        for (i in 0 until 10) {
            val count = slots.expectItem(i)
            assertEquals(i + 1, count)
        }
        slots.endGroup()
    }

    fun testCountNestedNodes() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startNode()
                slots.endNode()
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.startGroup()
        assertEquals(30, slots.expectItem(null))
        slots.endGroup()
    }

    fun testUpdateNestedNodeCountOnInsert() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startItem(null)
                slots.startNode()
                slots.endNode()
                assertEquals(1, slots.endItem())
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.startGroup()
        slots.startItem(null)

        repeat(3) {
            assertEquals(3, slots.skipItem())
        }

        slots.startItem(null)
        slots.beginInsert()
        repeat(2) {
            slots.startItem(null)
            slots.startNode()
            slots.endNode()
            assertEquals(1, slots.endItem())
        }
        slots.endInsert()
        repeat(3) { slots.skipItem() }
        assertEquals(5, slots.endItem())

        repeat(6) {
            assertEquals(3, slots.skipItem())
        }

        assertEquals(32, slots.endItem())
        slots.endGroup()
    }

    fun testUpdateNestedNodeCountOnRemove() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startItem(null)
                slots.startNode()
                slots.endNode()
                assertEquals(1, slots.endItem())
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.startGroup()
        slots.startItem(null)

        repeat(3) {
            assertEquals(3, slots.skipItem())
        }

        slots.startItem(null)

        repeat(2) { slots.removeItem() }
        repeat(1) { slots.skipItem() }
        assertEquals(1, slots.endItem())

        repeat(6) {
            assertEquals(3, slots.skipItem())
        }

        assertEquals(28, slots.endItem())

        slots.endGroup()
    }

    fun testNodesResetNodeCount() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        slots.startNode()
        repeat(10) {
            slots.startNode()
            slots.startItem(null)
            repeat(3) {
                slots.startNode()
                slots.endNode()
            }
            assertEquals(3, slots.endItem())
            slots.endNode()
        }
        slots.endNode()
        assertEquals(1, slots.endItem())
        slots.endGroup()
        slots.endInsert()
    }

    fun testSkipANode() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        slots.startNode()
        repeat(10) {
            slots.startNode()
            slots.startItem(null)
            repeat(3) {
                slots.startNode()
                slots.endNode()
            }
            assertEquals(3, slots.endItem())
            slots.endNode()
        }
        slots.endNode()
        assertEquals(1, slots.endItem())
        slots.endGroup()
        slots.endInsert()

        slots.reset()
        slots.startGroup()
        slots.startItem(null)
        assertEquals(1, slots.skipNode())
        slots.endGroup()
        slots.endGroup()
    }

    fun testMemo() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        slots.startMemo()
        repeat(10) { slots.update(it) }
        slots.endMemo()
        slots.endItem()
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.beginReading()
        slots.startGroup()
        slots.startItem(null)
        assertEquals(true, slots.isMemoGroup)
        slots.startMemo()
        repeat(10) {
            assertEquals(it, slots.next())
        }
        slots.endMemo()
        slots.endItem()
        slots.endGroup()
        slots.endReading()
        slots.reset()
    }

    fun testSkipMemos() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.update(null)
        repeat(10) {
            slots.startMemo()
            slots.update(it)
            slots.endMemo()
        }
        slots.startNode()
        slots.update(42)
        slots.endNode()
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.beginReading()
        slots.startGroup()
        assertEquals(null, slots.next())
        slots.skipMemos()
        slots.startNode()
        assertEquals(42, slots.next())
        slots.endNode()
        slots.endGroup()
        slots.endReading()
        slots.reset()
    }

    fun testStartEmpty() {
        val slots = SlotTable()
        slots.beginEmpty()
        slots.startGroup()
        assertEquals(true, slots.inEmpty)
        assertEquals(SlotTable.EMPTY, slots.next())
        slots.endGroup()
        slots.endEmpty()
        slots.reset()
    }

    fun testReportGroupSize() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startItem(null)
        repeat(10) {
            slots.startNode()
            slots.endNode()
        }
        slots.endItem()
        slots.update(42)
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.startGroup()
        slots.next()
        assertEquals(true, slots.isGroup)
        val size = slots.groupSize
        val savedCurrent = slots.current
        slots.skipGroup()
        assertEquals(size, slots.current - savedCurrent - 1)
        assertEquals(42, slots.next())
        assertEquals(true, slots.isGroupEnd)
        slots.endGroup()
    }

    fun testIsGroups() {
        val slots = SlotTable()
        slots.beginInsert()
        slots.startGroup()
        slots.startGroup()
        slots.endGroup()
        slots.startNode()
        slots.endNode()
        slots.endGroup()
        slots.endInsert()
        slots.reset()

        slots.startGroup()
        assertEquals(true, slots.isGroup)
        slots.startGroup()
        assertEquals(false, slots.isGroup)
        assertEquals(true, slots.isGroupEnd)
        slots.endGroup()
        assertEquals(true, slots.isNode)
        assertEquals(false, slots.isGroupEnd)
        slots.startNode()
        assertEquals(false, slots.isNode)
        assertEquals(true, slots.isGroupEnd)
        slots.endNode()
        slots.endGroup()
    }

    fun testReportUncertainNodeCount() {
        val slots = SlotTable()
        slots.beginReading()
        slots.reportUncertainNodeCount()
        slots.endReading()
        slots.reset()
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
    slots.beginInsert()
    slots.startGroup()

    fun item(key: Any?, memoGroup: Boolean = false, block: () -> Unit) {
        slots.startItem(key)
        if (memoGroup) {
            slots.startMemo()
            slots.update(key)
            slots.endMemo()
        }
        block()
        slots.endItem()
    }

    fun element(key: Any?, block: () -> Unit) {
        item(key) {
            slots.startNode()
            block()
            slots.endNode()
        }
    }

    for (key in 0 until 10) {
        item(key, memoGroup = key % 4 == 0) {
            for (item in 0..key) {
                element(elementKey) {
                    for (element in 0..key)
                        slots.update(element)
                }
            }

        }
    }

    slots.endGroup()
    slots.endInsert()
    slots.reset()

    return slots
}

fun SlotTable.startItem(key: Any?) {
    if (isReading) {
        org.junit.Assert.assertEquals(key, next())
    } else {
        update(key)
    }

    startGroup()
}

fun SlotTable.endItem(): Int = endGroup()

fun SlotTable.expectItem(key: Any?): Int {
    org.junit.Assert.assertEquals(key, next())
    skipMemo()
    return skipGroup()
}
