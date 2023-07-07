/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)
package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType
import org.junit.Test

class NodeChainTests {

    @Test
    fun testInsertsAndDeletesAtTail() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
            .withModifiers(a, b, c, b)
            .assertStringEquals("[a,b,c,b]")
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
    }

    @Test
    fun testInsertsAndDeletesAtHead() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
            .withModifiers(c, a, b, c)
            .assertStringEquals("[c,a,b,c]")
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
    }

    @Test
    fun testInsertsInMiddle() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
            .withModifiers(a, b, a, c)
            .assertStringEquals("[a,b,a,c]")
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
    }

    @Test
    fun testRemovingEverything() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, c)
            .assertStringEquals("[a,b,c]")
            .withModifiers(Modifier)
            .assertStringEquals("[]")
    }

    @Test
    fun testSwapModifierInMiddle() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, b, b, c)
            .assertStringEquals("[a,b,b,b,c]")
            .withModifiers(a, b, c, b, c)
            .assertStringEquals("[a,b,c,b,c]")
    }

    @Test
    fun testSwapModifierAtHead() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, b, b, c)
            .assertStringEquals("[a,b,b,b,c]")
            .withModifiers(c, b, b, b, c)
            .assertStringEquals("[c,b,b,b,c]")
    }

    @Test
    fun testSwapModifierAtTail() {
        val (a, b, c) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a, b, b, b, c)
            .assertStringEquals("[a,b,b,b,c]")
            .withModifiers(a, b, b, b, a)
            .assertStringEquals("[a,b,b,b,a]")
    }

    @Test
    fun testReused() {
        val (a1, b1, c1) = reusableModifiers("a", "b", "c")
        val (a2, b2, c2) = reusableModifiers("a", "b", "c")
        chainTester()
            .withModifiers(a1, b1, c1)
            .assertStringEquals("[a,b,c]")
            .withModifiers(a2, b2, c2)
            .assertStringEquals("[a,b,c]")
    }

    @Test
    fun testSimple() {
        val a1 = modifierA()
        val a2 = modifierA()
        val b1 = modifierB()
        val c1 = modifierC()
        val c2 = modifierC()
        assert(areObjectsOfSameType(c1, c2))
        assert(!areObjectsOfSameType(a1, b1))
        chainTester()
            .withModifiers(a1, b1, c1)
            .assertStringEquals("[a,b,c]")
            .clearLog()
            .withModifiers(a2, c2)
            .assertStringEquals("[a,c]")
            .assertElementDiff(
                """
                 a
                -b
                 c
                """.trimIndent()
            ).apply {
                val (entA, entC) = nodes
                withModifiers(a1, modifierD(), b1)
                val (entA2, entB2) = nodes
                assert(entA === entA2)
                assert(entC !== entB2)
            }
    }

    // TODO(b/241856927)
    // - aggregate masks are correct
    // - tree traversal
    // - next/prev and entity types
    // - entity modifier lifecycles
    // - reuse vs recreate etc.
    // - number of inserts/deletes/etc for different types of updates
    // - ensure which same-size updates go through diff vs not
    // - ensure that entities in chain are attached, out of chain are detached
}
