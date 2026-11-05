/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.dom

import kotlin.test.*

class HtmlAttributeNameCacheTest {
    @Test
    fun namesAreBoundedAndInvalidNamesNeverEnterTheCache() {
        val cache = HtmlAttributeNameCache()
        repeat(1000) { cache.requireValid("data-$it") }
        assertEquals(128, cache.size)
        cache.requireValid("data-" + "a".repeat(1000))
        assertEquals(128, cache.size)
        for (name in listOf("", "a b", "a\u0000", "a\u009F", "a'", "a=", "a/", "a>")) {
            repeat(2) { assertFailsWith<IllegalArgumentException> { cache.requireValid(name) } }
        }
        assertEquals(128, cache.size)
    }
}
