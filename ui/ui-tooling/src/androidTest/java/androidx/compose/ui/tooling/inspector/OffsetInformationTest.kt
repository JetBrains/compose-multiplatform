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

package androidx.compose.ui.tooling.inspector

import androidx.compose.ui.tooling.Group
import androidx.compose.ui.tooling.Inspectable
import androidx.compose.ui.tooling.SlotTableRecord
import androidx.compose.ui.tooling.ToolingTest
import androidx.compose.ui.tooling.asTree
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OffsetInformationTest : ToolingTest() {
    @Test
    fun testOffset() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                OffsetData()
            }
        }

        val table = slotTableRecord.store.first()
        val tree = table.asTree()
        val offsets = tree.all().filter {
            it.location?.sourceFile == "OffsetData.kt" &&
                it.name != null && it.name != "remember"
        }.map {
            it.name!! to it.location!!.offset
        }

        assertArrayEquals(
            arrayListOf(
                "MyComposeTheme" to 1665,
                "Column" to 1690,
                "Text" to 1747,
                "Greeting" to 2000,
                "Text" to 2835,
                "Surface" to 2115,
                "Button" to 2160,
                "Text" to 2183,
                "Surface" to 2032,
                "TextButton" to 2349,
                "Row" to 2490
            ),
            offsets
        )
    }
}

fun Group.all(): Iterable<Group> {
    val result = mutableListOf<Group>()
    fun appendAll(group: Group) {
        result.add(group)
        group.children.forEach { appendAll(it) }
    }
    appendAll(this)
    return result
}

fun <T> assertArrayEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    transform: (T) -> String = { "$it" }
) {
    TestCase.assertEquals(
        expected.joinToString("\n", transform = transform),
        actual.joinToString("\n", transform = transform)
    )
}
