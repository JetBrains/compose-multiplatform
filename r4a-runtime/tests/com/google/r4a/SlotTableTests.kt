/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import junit.framework.TestCase
import org.junit.Assert

class SlotTableTests : TestCase() {
    fun testCanCreate() {
        SlotTable()
    }

    // Raw slot tests (testing the buffer gap)

    fun testCanInsert() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.next()
        slots.endInsert()
        slots.endGroup()
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

    // Semantic tests (testing groups and containers)

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

    fun testCountContainers() {
        val slots = testItems()
        slots.startGroup()
        for (i in 0 until 10) {
            val count = slots.expectItem(i)
            assertEquals(i + 1, count)
        }
        slots.endGroup()
    }

    fun testCountNestedContainers() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startContainer()
                slots.endContainer()
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endInsert()
        slots.endGroup()
        slots.reset()

        slots.startGroup()
        assertEquals(30, slots.expectItem(null))
        slots.endGroup()
    }

    fun testUpdateNestedContainerCountOnInsert() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startItem(null)
                slots.startContainer()
                slots.endContainer()
                assertEquals(1, slots.endItem())
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endInsert()
        slots.endGroup()
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
            slots.startContainer()
            slots.endContainer()
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

    fun testUpdateNestedContainerCountOnRemove() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        repeat(10) {
            slots.startItem(null)
            repeat(3) {
                slots.startItem(null)
                slots.startContainer()
                slots.endContainer()
                assertEquals(1, slots.endItem())
            }
            assertEquals(3, slots.endItem())
        }
        assertEquals(30, slots.endItem())
        slots.endInsert()
        slots.endGroup()
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

    fun testContainersResetContainerCount() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        slots.startContainer()
        repeat(10) {
            slots.startContainer()
            slots.startItem(null)
            repeat(3) {
                slots.startContainer()
                slots.endContainer()
            }
            assertEquals(3, slots.endItem())
            slots.endContainer()
        }
        slots.endContainer()
        assertEquals(1, slots.endItem())
        slots.endInsert()
        slots.endGroup()
    }

    fun testSkipAContainer() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        slots.startContainer()
        repeat(10) {
            slots.startContainer()
            slots.startItem(null)
            repeat(3) {
                slots.startContainer()
                slots.endContainer()
            }
            assertEquals(3, slots.endItem())
            slots.endContainer()
        }
        slots.endContainer()
        assertEquals(1, slots.endItem())
        slots.endInsert()
        slots.endGroup()

        slots.reset()
        slots.startGroup()
        slots.startItem(null)
        assertEquals(1, slots.skipContainer())
        slots.endGroup()
        slots.endGroup()
    }

    fun testMemo() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        slots.startMemo()
        repeat(10) { slots.update(it) }
        slots.endMemo()
        slots.endItem()
        slots.endInsert()
        slots.endGroup()
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
        slots.startGroup()
        slots.beginInsert()
        slots.update(null)
        repeat(10) {
            slots.startMemo()
            slots.update(it)
            slots.endMemo()
        }
        slots.startContainer()
        slots.update(42)
        slots.endContainer()
        slots.endInsert()
        slots.endGroup()
        slots.reset()

        slots.beginReading()
        slots.startGroup()
        assertEquals(null, slots.next())
        slots.skipMemos()
        slots.startContainer()
        assertEquals(42, slots.next())
        slots.endContainer()
        slots.endGroup()
        slots.endReading()
        slots.reset()
    }

    fun testStartEmpty() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginEmpty()
        assertEquals(true, slots.inEmpty)
        assertEquals(SlotTable.EMPTY, slots.next())
        slots.endEmpty()
        slots.endGroup()
        slots.reset()
    }

    fun testReportGroupSize() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startItem(null)
        repeat(10) {
            slots.startContainer()
            slots.endContainer()
        }
        slots.endItem()
        slots.update(42)
        slots.endInsert()
        slots.endGroup()
        slots.reset()

        slots.startGroup()
        slots.next()
        assertEquals(true, slots.isGroup)
        val size = slots.groupSize
        val savedCurrent = slots.current
        slots.skipGroup()
        assertEquals(size, slots.current - savedCurrent - 2)
        assertEquals(42, slots.next())
        assertEquals(true, slots.isGroupEnd)
        slots.endGroup()
    }

    fun testIsGroups() {
        val slots = SlotTable()
        slots.startGroup()
        slots.beginInsert()
        slots.startGroup()
        slots.endGroup()
        slots.startContainer()
        slots.endContainer()
        slots.endInsert()
        slots.endGroup()
        slots.reset()

        slots.startGroup()
        assertEquals(true, slots.isGroup)
        slots.startGroup()
        assertEquals(false, slots.isGroup)
        assertEquals(true, slots.isGroupEnd)
        slots.endGroup()
        assertEquals(true, slots.isContainer)
        assertEquals(false, slots.isGroupEnd)
        slots.startContainer()
        assertEquals(false, slots.isContainer)
        assertEquals(true, slots.isGroupEnd)
        slots.endContainer()
        slots.endGroup()
    }

    fun testReportUncertainContainerCount() {
        val slots = SlotTable()
        slots.beginReading()
        slots.startGroup()
        slots.reportUncertainContainerCount()
        slots.endGroup()
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

// Creates 0 until 10 items each with 10 elements numbered 0...n with 0..n slots
fun testItems(): SlotTable {
    val slots = SlotTable()
    slots.startGroup()
    slots.beginInsert()

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
            slots.startContainer()
            block()
            slots.endContainer()
        }
    }

    for (key in 0 until 10) {
        item(key, memoGroup = key % 4 == 0) {
            for (item in 0..key) {
                element(null) {
                    for (element in 0..key)
                        slots.update(element)
                }
            }

        }
    }

    slots.endInsert()
    slots.endGroup()
    slots.reset()

    return slots
}

fun SlotTable.startItem(key: Any?) {
    if (isReading) {
        Assert.assertEquals(key, next())
    } else {
        update(key)
    }

    startGroup()
}

fun SlotTable.endItem(): Int = endGroup()

fun SlotTable.expectItem(key: Any?): Int {
    Assert.assertEquals(key, next())
    skipMemo()
    return skipGroup()
}