/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class HydrationStateUnescapeTest {
    private fun original(value: String): String = value
        .replace("&lt;", "<")
        .replace("&#13;", "\r")
        .replace("&#0;", "\u0000")
        .replace("&amp;", "&")

    @Test
    fun nativeDecoderMatchesSequentialProtocolReplacements() {
        val fragments = listOf(
            "&lt;", "&#13;", "&#0;", "&amp;", "&amp;lt;", "&amp;#13;",
            "&amp;#0;", "&amp;amp;", "&", "lt;", "&#", "13;", "0;", "&LT;",
            "&unknown;", "<", "\r", "\u0000", "Å中😀", "\uD800", "\uDC00", " plain ",
        )
        for (a in fragments) {
            for (b in fragments) {
                assertEquals(original(a + b), (a + b).unescapeFromHydrationStateElement())
            }
        }

        val random = Random(9126)
        repeat(1_000) {
            val value = List(30) { fragments[random.nextInt(fragments.size)] }.joinToString("")
            assertEquals(original(value), value.unescapeFromHydrationStateElement())
            assertEquals(value, value.escapeForHydrationStateElement().unescapeFromHydrationStateElement())
        }
        assertEquals("unchanged", "unchanged".unescapeFromHydrationStateElement())
    }
}
