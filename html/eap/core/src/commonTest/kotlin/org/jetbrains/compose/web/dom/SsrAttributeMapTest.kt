/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SsrAttributeMapTest {
    @Test
    fun insertionOverwriteRemoveAndClearMatchLinkedMap() {
        val expected = linkedMapOf<String, String>()
        val actual = SsrAttributeMap()
        for (count in listOf(0, 1, 16, 17, 50, 2)) {
            expected.clear()
            actual.clear()
            repeat(count) { index ->
                assertEquals(expected.put("a$index", "$index"), actual.put("a$index", "$index"))
            }
            repeat(count) { index ->
                assertEquals(expected.put("a$index", "new$index"), actual.put("a$index", "new$index"))
            }
            assertEquals<Map<String, String>>(expected, actual)
            assertEquals(expected.keys.toList(), actual.keys.toList())
            repeat(count / 2) { index -> assertEquals(expected.remove("a$index"), actual.remove("a$index")) }
            val pairs = mutableListOf<Pair<String, String>>()
            actual.forEachHtmlAttribute { name, value -> pairs.add(name to value) }
            assertEquals(expected.toList(), pairs)
            assertEquals(expected.hashCode(), actual.hashCode())
        }
    }

    @Test
    fun entryMutationAndIteratorRemovalUpdateBackingStorage() {
        val map = SsrAttributeMap()
        map["a"] = "first"
        map["b"] = "second"
        val iterator = map.entries.iterator()
        assertEquals("first", iterator.next().setValue("changed"))
        assertEquals("changed", map["a"])
        iterator.remove()
        assertEquals("b", iterator.next().key)
        assertFalse(iterator.hasNext())
        assertEquals(mapOf("b" to "second"), map.toMap())
    }

    @Test
    fun retainedEntryViewRemainsBackedAfterFallbackAndClear() {
        val map = SsrAttributeMap()
        map["first"] = "original"
        val entries = map.entries
        val first = entries.first()
        repeat(17) { map["data-$it"] = "$it" }
        assertEquals(map.size, entries.size)
        assertEquals(map.keys.toList(), entries.map { it.key })
        assertEquals("original", first.setValue("updated"))
        assertEquals("updated", map["first"])
        val iterator = entries.iterator()
        assertEquals("first", iterator.next().key)
        iterator.remove()
        assertFalse(map.containsKey("first"))
        map.clear()
        assertEquals(0, entries.size)
        map["last"] = "value"
        assertEquals(listOf("last"), entries.map { it.key })
    }

    @Test
    fun renderPreservesAttributeOrderAcrossThresholdAndReuse() {
        for (count in listOf(1, 16, 17, 50, 0, 2)) {
            val html = composeHtmlToString(key = "attributes-threshold") {
                TagElement<Element>("div", {
                    repeat(count) { attr("data-$it", "$it") }
                    if (count > 0) attr("data-0", "replacement")
                }, null)
            }
            val attrs = (0 until count).joinToString("") { " data-$it=\"${if (it == 0) "replacement" else "$it"}\"" }
            assertEquals("<div$attrs></div>", html)
        }
    }
}
