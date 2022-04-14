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

package androidx.compose.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

class AbstractApplierTest {
    private val root = Node("Root")
    private val applier = NodeApplier(root)

    @Test fun upFromRootThrows() {
        try {
            applier.up()
            fail()
        } catch (_: IllegalStateException) {
        }
    }

    @Test fun downGoesDown() {
        val one = Node("one")
        applier.insertTopDown(0, one)
        applier.down(one)
        assertSame(one, applier.current)

        val two = Node("two")
        applier.insertTopDown(0, two)
        applier.down(two)
        assertSame(two, applier.current)
    }

    @Test fun upGoesUp() {
        val one = Node("one")
        applier.insertTopDown(0, one)
        applier.down(one)
        val two = Node("two")
        applier.insertTopDown(0, two)
        applier.down(two)

        applier.up()
        assertSame(one, applier.current)
        applier.up()
        assertSame(root, applier.current)
    }

    @Test fun clearClearsAndPointsToRoot() {
        val child = Node("child")
        applier.insertTopDown(0, child)
        applier.down(child)

        applier.clear()
        assertSame(root, applier.current)
        assertEquals(emptyList<Node>(), root.children)
    }

    @Test fun removeSingle() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.remove
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        val four = Node("four")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)
        applier.insertTopDown(3, four)

        applier.remove(1, 1) // Middle
        assertEquals(listOf(one, three, four), root.children)
        applier.remove(2, 1) // End
        assertEquals(listOf(one, three), root.children)
        applier.remove(0, 1) // Start
        assertEquals(listOf(three), root.children)
    }

    @Test fun removeMultiple() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.remove
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        val four = Node("four")
        val five = Node("five")
        val six = Node("six")
        val seven = Node("seven")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)
        applier.insertTopDown(3, four)
        applier.insertTopDown(4, five)
        applier.insertTopDown(5, six)
        applier.insertTopDown(6, seven)

        applier.remove(2, 2) // Middle
        assertEquals(listOf(one, two, five, six, seven), root.children)
        applier.remove(3, 2) // End
        assertEquals(listOf(one, two, five), root.children)
        applier.remove(0, 2) // Start
        assertEquals(listOf(five), root.children)
    }

    @Test fun moveSingleHigher() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.move
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)

        applier.move(0, 3, 1)
        assertEquals(listOf(two, three, one), root.children)

        // Do adjacent moves as this is currently specialized to do a swap.
        applier.move(0, 1, 1)
        assertEquals(listOf(three, two, one), root.children)
        applier.move(1, 2, 1)
        assertEquals(listOf(three, one, two), root.children)
    }

    @Test fun moveSingleLower() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.move
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)

        applier.move(2, 0, 1)
        assertEquals(listOf(three, one, two), root.children)

        // Do adjacent moves as this is currently specialized to do a swap.
        applier.move(1, 2, 1)
        assertEquals(listOf(three, two, one), root.children)
        applier.move(0, 1, 1)
        assertEquals(listOf(two, three, one), root.children)
    }

    @Test fun moveMultipleHigher() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.move
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        val four = Node("four")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)
        applier.insertTopDown(3, four)

        applier.move(0, 4, 2)
        assertEquals(listOf(three, four, one, two), root.children)
    }

    @Test fun moveMultipleLower() {
        // Note: NodeApplier delegates to AbstractApplier's MutableList.move
        // helper which is what is being tested here.
        val one = Node("one")
        val two = Node("two")
        val three = Node("three")
        val four = Node("four")
        applier.insertTopDown(0, one)
        applier.insertTopDown(1, two)
        applier.insertTopDown(2, three)
        applier.insertTopDown(3, four)

        applier.move(2, 0, 2)
        assertEquals(listOf(three, four, one, two), root.children)
    }
}

private class Node(val name: String) {
    val children = mutableListOf<Node>()
    override fun toString() = name + children.joinToString(",", "(", ")")
}

private class NodeApplier(root: Node) : AbstractApplier<Node>(root) {
    override fun insertTopDown(index: Int, instance: Node) {
        current.children.add(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: Node) { }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        current.children.clear()
    }
}
