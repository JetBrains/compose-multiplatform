/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongHtmlEscapingJvmTest {
    @Test
    fun specializedSerializerMatchesEveryUtf16CodeUnit() {
        val padding = "a".repeat(128)
        for (code in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
            for (attribute in listOf(false, true)) {
                for (offset in 0..3) {
                    val value = padding + "a".repeat(offset) + code.toChar() + padding
                    val output = StringBuilder("prefix:")
                    if (code == 0) {
                        assertFailsWith<IllegalArgumentException> { output.appendEscapedHtmlString(value, attribute) }
                    } else {
                        assertTrue(output.appendEscapedHtmlString(value, attribute))
                        assertEquals("prefix:" + reference(value, attribute), output.toString(), "code=$code offset=$offset")
                    }
                }
            }
        }
    }

    @Test
    fun dispatchKeepsShortPathsAndHandlesThresholdsAndUnicode() {
        val random = Random(342625)
        val alphabet = "abc f|~@MF éα日本語😀&<>\r\"\n\t"
        for (length in listOf(0, 31, 32, 64, 127, 128, 129, 255, 256, 257, 1024, 8192)) {
            repeat(16) {
                val value = buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
                assertEquals(reference(value, false), buildString { appendEscapedText(value) })
                assertEquals("<div title=\"${reference(value, true)}\"></div>",
                    composeHtmlToString { Div(attrs = { title(value) }) })
                val output = StringBuilder("prefix:")
                val handled = output.appendEscapedHtmlString(value, true)
                if (length < 256) {
                    assertFalse(handled)
                    assertEquals("prefix:", output.toString())
                } else {
                    assertTrue(handled)
                    assertEquals("prefix:" + reference(value, true), output.toString())
                }
            }
        }
    }

    @Test
    fun nativeSuffixGuardCannotSkipAnEscapeOrNul() {
        for (character in "&<>\r\"\n\t\u0000") {
            for (position in listOf(0, 31, 32, 127, 128, 255, 256, 1024)) {
                val value = "&" + "a".repeat(position) + character + "a".repeat(1024)
                for (attribute in listOf(false, true)) {
                    if (character == '\u0000') {
                        assertFailsWith<IllegalArgumentException> {
                            StringBuilder().appendEscapedHtmlString(value, attribute)
                        }
                    } else {
                        val output = StringBuilder()
                        assertTrue(output.appendEscapedHtmlString(value, attribute))
                        assertEquals(reference(value, attribute), output.toString())
                    }
                }
            }
        }
    }

    private fun reference(value: String, attribute: Boolean): String {
        var result = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;")
        if (attribute) result = result.replace("\"", "&quot;").replace("\n", "&#10;").replace("\t", "&#9;")
        return result
    }
}
