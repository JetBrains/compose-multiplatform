/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.group
import androidx.compose.runtime.insert
import androidx.compose.runtime.nodeGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(InternalComposeApi::class)
class CompositionDataTests {

    @Test
    fun canGetCompositionDataFromSlotTable() {
        val slots = SlotTable()
        val compositionData = slots as CompositionData
        assertTrue(compositionData.compositionGroups.toList().isEmpty())
    }

    @Test
    fun canIterateASlotTable() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(1) {
                        for (i in 1..5) {
                            writer.group(i * 10) {
                                for (j in 1..i) {
                                    writer.update(i * 100 + j)
                                }
                            }
                        }
                    }
                }
            }
        }

        slots.verifyWellFormed()

        val list = mutableListOf<Int>()
        fun iterate(compositionData: CompositionData) {
            for (group in compositionData.compositionGroups) {
                list.add(group.key as Int)
                for (data in group.data) {
                    list.add(data as Int)
                }
                iterate(group)
            }
        }

        iterate(slots)

        assertEquals(
            listOf(
                1, 10, 101,
                20, 201, 202,
                30, 301, 302, 303,
                40, 401, 402, 403, 404,
                50, 501, 502, 503, 504, 505
            ),
            list
        )
    }

    @Test
    fun canFindNodes() {
        val data = List(26) { 'A' + it }
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(0) {
                        fun emit(a: List<Char>) {
                            if (a.isNotEmpty()) {
                                writer.group(1) {
                                    val mid = (a.size - 1) / 2 + 1
                                    writer.nodeGroup(10, a[0])
                                    if (mid > 1)
                                        emit(a.subList(1, mid))
                                    if (mid < a.size)
                                        emit(a.subList(mid, a.size))
                                }
                            }
                        }

                        emit(data)
                    }
                }
            }
        }

        val collected = mutableListOf<Char>()

        fun collect(data: CompositionData) {
            for (group in data.compositionGroups) {
                if (group.node != null) {
                    collected.add(group.node as Char)
                }
                collect(group)
            }
        }

        collect(slots)

        assertEquals(data, collected)
    }

    @Test
    fun canFindSourceInfo() {
        val slots = SlotTable().also {
            var data = 0
            it.write { writer ->
                writer.insert {
                    writer.group(0) {
                        fun emit(depth: Int) {
                            if (depth == 0) {
                                writer.startData(100, aux = "$data")
                                data++
                                writer.endGroup()
                            } else {
                                if (depth == 2) {
                                    writer.startData(depth * 1000, aux = "$data")
                                    data++
                                } else writer.startGroup(depth)
                                emit(depth - 1)
                                emit(depth - 1)
                                writer.endGroup()
                            }
                        }
                        emit(5)
                    }
                }
            }
        }

        val collected = mutableListOf<String>()

        fun collect(data: CompositionData) {
            for (group in data.compositionGroups) {
                val sourceInfo = group.sourceInfo
                if (sourceInfo != null) {
                    collected.add(sourceInfo)
                }
                collect(group)
            }
        }

        collect(slots)

        assertEquals(List(40) { "$it" }, collected)
    }

    @Test
    fun writeDuringIterationCausesException() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(0) {
                        repeat(10) { index ->
                            writer.group(100 + index) { }
                        }
                    }
                }
            }
        }

        fun insertAGroup() {
            slots.write { writer ->
                writer.group {
                    repeat(3) { writer.group { } }
                    writer.insert {
                        writer.group(200) { }
                    }
                    writer.skipToGroupEnd()
                }
            }
        }

        val groups = slots.compositionGroups.iterator()
        insertAGroup()

        assertFailsWith(ConcurrentModificationException::class) {
            // Expect this to cause an exception
            groups.next()
        }
    }

    @Test
    fun iterationDuringWriteCausesException() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(0) {
                        repeat(10) { index ->
                            writer.group(100 + index) { }
                        }
                    }
                }
            }
        }

        slots.write { writer ->
            writer.group {
                repeat(3) { writer.group { } }
                writer.insert {
                    writer.group(200) { }
                }
                writer.skipToGroupEnd()

                assertFailsWith(ConcurrentModificationException::class) {
                    // Expect this to throw an exception
                    slots.compositionGroups.first()
                }
            }
        }
    }

    @Test
    fun canFindAGroupInCompositionData() {
        val slots = SlotTable().also {
            it.write { writer ->
                writer.insert {
                    writer.group(0) {
                        repeat(10) { index ->
                            writer.group(100 + index) { }
                        }
                    }
                }
            }
        }

        val identity = slots.compositionGroups.first().compositionGroups.drop(5).first().identity
        assertEquals(identity, slots.find(identity!!)?.identity)
    }
}