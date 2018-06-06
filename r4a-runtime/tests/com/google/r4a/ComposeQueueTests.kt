@file:Suppress("unused")

package com.google.r4a

import junit.framework.TestCase
import org.junit.Test

class ComposeQueueTests : TestCase() {

    private val ascendingIntComparator = { a: Int, b: Int -> a - b }

    @Test
    fun testSortOrder() {
        val queue = ComposeQueue(ascendingIntComparator)

        // add in random order
        queue.add(5)
        queue.add(3)
        queue.add(2)
        queue.add(4)
        queue.add(1)

        val result = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val el = queue.pop()!!
            result.add(el)
        }

        // list pops in sorted ascending order
        result.assertEquals(1, 2, 3, 4, 5)
    }


    @Test
    fun testMidAdd() {
        val queue = ComposeQueue(ascendingIntComparator)

        queue.add(5)
        queue.add(4)
        queue.add(3)
        queue.add(2)
        queue.add(1)

        val result = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val el = queue.pop()!!
            if (el == 2) {
                // in the middle of our iteration, we add elements
                // that wouldn't have been iterated over yet
                queue.add(10)
                queue.add(11)
            }
            result.add(el)
        }

        // iteration order is correct
        result.assertEquals(1, 2, 3, 4, 5, 10, 11)
    }

    @Test
    fun testMidAddLessThan() {
        val queue = ComposeQueue(ascendingIntComparator)

        queue.add(5)
        queue.add(4)
        queue.add(3)
        queue.add(2)
        queue.add(1)

        val result = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val el = queue.pop()!!
            if (el == 3) {
                // in the middle of our iteration we add elements that would have already been iterated over
                queue.add(1)
                queue.add(2)
            }
            result.add(el)
        }

        // the added elements end up getting iterated immediately after we added them
        result.assertEquals(1, 2, 3, 1, 2, 4, 5)
    }

    private fun Collection<Int>.assertEquals(vararg ints: Int) {
        assertEquals(ints.joinToString { "$it" }, this.joinToString { "$it" })
    }
}