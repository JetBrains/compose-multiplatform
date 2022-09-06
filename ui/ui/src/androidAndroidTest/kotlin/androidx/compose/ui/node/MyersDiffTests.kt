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

package androidx.compose.ui.node

import org.junit.Assert.assertEquals
import org.junit.Test

class MyersDiffTests {

    @Test
    fun testDiff() {
        val a = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val b = listOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10)
        val (c, log) = executeListDiff(a, b)
        assertEquals(b, c)
        assertEquals(
            """
            Equals(x = 10, y = 9)
            Equals(x = 9, y = 8)
            Equals(x = 8, y = 7)
            Equals(x = 7, y = 6)
            Equals(x = 6, y = 5)
            Remove(5)
            Equals(x = 4, y = 4)
            Equals(x = 3, y = 3)
            Equals(x = 2, y = 2)
            Equals(x = 1, y = 1)
            Equals(x = 0, y = 0)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun stringDiff() {
        stringDiff(
            "ihfiwjfowijefoiwjfe",
            "ihfawwjwfowwijefwicwfe"
        )

        stringDiff("", "abcde")

        stringDiff("abcde", "")

        stringDiff(
            "aaaa",
            "bbbb",
            """
            Remove(3)
            Remove(2)
            Remove(1)
            Remove(0)
            Insert(b at 0)
            Insert(b at 0)
            Insert(b at 0)
            Insert(b at 0)
            """.trimIndent()
        )

        stringDiff("abcd", "bcda")

        stringDiff(
            "abc",
            "abccbacbac"
        )
    }
}

fun stringDiff(before: String, after: String, expectedLog: String? = null) {
    val (result, log) = executeListDiff(before.toCharArray().asList(), after.toCharArray().asList())
    if (expectedLog != null) {
        assertEquals(expectedLog, log.joinToString("\n"))
    }
    assertEquals(result.joinToString(separator = ""), after)
}

data class DiffResult<T>(val result: List<T>, val log: List<String>)

fun <T> executeListDiff(x: List<T>, y: List<T>): DiffResult<T> {
    val log = mutableListOf<String>()
    val result = x.toMutableList()
    executeDiff(x.size, y.size, object : DiffCallback {
        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            return x[oldIndex] == y[newIndex]
        }

        override fun insert(atIndex: Int, newIndex: Int) {
            log.add("Insert(${y[newIndex]} at $atIndex)")
            result.add(atIndex, y[newIndex])
        }

        override fun remove(oldIndex: Int) {
            log.add("Remove($oldIndex)")
            result.removeAt(oldIndex)
        }

        override fun same(oldIndex: Int, newIndex: Int) {
            log.add("Equals(x = $oldIndex, y = $newIndex)")
        }
    })
    return DiffResult(result, log)
}