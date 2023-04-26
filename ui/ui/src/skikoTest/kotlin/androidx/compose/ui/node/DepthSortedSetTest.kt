/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Adopted copy of compose/ui/ui/src/androidAndroidTest/kotlin/androidx/compose/ui/platform/DepthSortedSetTest.kt
@OptIn(ExperimentalTestApi::class)
class DepthSortedSetTest {

    @Test
    fun sortedByDepth() = runSkikoComposeUiTest {
        setContent {  }

        val root = LayoutNode()
        root.attach(scene.mainOwner!!)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet()
        set.add(child2)
        set.add(child3)
        set.add(root)
        set.add(child1)

        assertEquals(root, set.pop())
        assertEquals(child1, set.pop())
        assertEquals(child2, set.pop())
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun sortedByDepthWithItemsOfTheSameDepth() = runSkikoComposeUiTest {
        setContent {  }

        val root = LayoutNode()
        root.attach(scene.mainOwner!!)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        root.add(child2)
        val child3 = LayoutNode()
        child1.add(child3)

        val set = DepthSortedSet()
        set.add(child1)
        set.add(child3)
        set.add(child2)
        set.add(root)

        assertEquals(root, set.pop())
        val result = set.pop()
        if (result === child1) {
            assertEquals(child2, set.pop())
        } else {
            assertEquals(child2, result)
            assertEquals(child1, set.pop())
        }
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun modifyingSetWhileWeIterate() = runSkikoComposeUiTest {
        setContent {  }

        val root = LayoutNode()
        root.attach(scene.mainOwner!!)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet()
        set.add(child1)
        set.add(child3)
        var expected: LayoutNode? = child1

        set.popEach {
            assertEquals(expected, it)
            if (expected === child1) {
                set.add(child2)
                set.add(root)
                // now we expect root
                expected = root
            } else if (expected === root) {
                // remove child3 so we will never reach it
                set.remove(child3)
                // now we expect the last item
                expected = child2
            } else {
                assertEquals(child2, it)
                // no other items expected, child3 was removed already
                expected = null
            }
        }
        // assert we iterated the whole set
        assertEquals(null, expected)
    }

    @Test
    fun addingNotAttachedNodeThrows() {
        val set = DepthSortedSet()
        assertFailsWith<IllegalStateException> {
            set.add(LayoutNode())
        }
    }

    @Test
    fun modifyingDepthAfterAddingThrows() = runSkikoComposeUiTest {
        setContent {  }

        val root = LayoutNode()
        root.attach(scene.mainOwner!!)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)

        val set = DepthSortedSet()
        set.add(child2)

        // change depth of child2
        child1.removeAt(0, 1)
        root.add(child2)
        // now it is on the same level as child1

        assertTrue(set.isNotEmpty())
        // throws because we changed the depth
        assertFailsWith<IllegalStateException> {
            set.pop()
        }
    }
}
