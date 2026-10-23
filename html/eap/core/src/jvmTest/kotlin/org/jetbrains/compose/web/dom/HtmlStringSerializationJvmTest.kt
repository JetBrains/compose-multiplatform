/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.random.Random

class HtmlStringSerializationJvmTest {
    @Test
    fun specializedSerializerMatchesEscapingForEveryCodeUnit() {
        val padding = "a".repeat(128)
        for (code in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
            for (offset in 0..3) {
                val value = padding + "a".repeat(offset) + code.toChar() + padding
                for (attribute in listOf(false, true)) {
                    val output = StringBuilder("prefix:")
                    if (code == 0) {
                        assertFailsWith<IllegalArgumentException> { output.appendEscapedHtmlString(value, attribute) }
                    } else {
                        assertTrue(output.appendEscapedHtmlString(value, attribute))
                        assertEquals("prefix:" + referenceEscape(value, attribute), output.toString(), "Code unit $code")
                    }
                }
            }
        }
    }

    @Test
    fun serializersMatchForVariedEscapePositionsAndDensities() {
        val random = Random(342625)
        val alphabet = "abc f|~@MF éα日本語😀&<>\r\"\n\t"
        for (length in listOf(0, 31, 32, 64, 127, 128, 129, 255, 256, 257, 1024, 8192)) {
            repeat(16) {
                val value = buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
                assertEquals(referenceEscape(value, false), buildString { appendEscapedText(value) })
                assertEquals(referenceEscape(value, true), buildString { appendEscapedAttribute(value) })
            }
        }
    }

    private fun referenceEscape(value: String, attribute: Boolean): String {
        var result = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;")
        if (attribute) result = result.replace("\"", "&quot;").replace("\n", "&#10;").replace("\t", "&#9;")
        return result
    }

    @Test
    fun fastPathChecksEveryUtf16CodeUnitInTextAndAttributes() {
        for (paddingLength in listOf(32, 128)) {
            val padding = "a".repeat(paddingLength)
            for (code in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
                val character = code.toChar()
                val textSafe = character !in "\u0000&<>\r"
                val attributeSafe = textSafe && character !in "\"\n\t"
                for (offset in 0..3) {
                    val value = padding + "a".repeat(offset) + character + padding
                    assertEquals(
                        textSafe, canAppendUnescapedHtmlString(value, attribute = false), "Text code unit $code at $offset",
                    )
                    assertEquals(
                        attributeSafe, canAppendUnescapedHtmlString(value, attribute = true), "Attribute code unit $code at $offset",
                    )
                }
            }
        }
    }

    @Test
    fun fastPathChecksOnlyTheRequestedSuffixAndKeepsTheShortRunCutoff() {
        for (attribute in listOf(false, true)) {
            for (length in listOf(0, 1, 30, 31, 32, 33, 64, 127, 128, 129, 1024)) {
                val value = "\u0000&<>\r\"\n\t" + "a".repeat(length)
                assertEquals(length >= 32, canAppendUnescapedHtmlString(value, attribute, startIndex = 8))
            }
            for (character in "\u0000&<>\r" + if (attribute) "\"\n\t" else "") {
                for (length in (32..35) + (128..131)) {
                    assertFalse(canAppendUnescapedHtmlString("a".repeat(length - 1) + character, attribute))
                }
                for (position in listOf(0, 31, 32, 64, 127, 128, 129)) {
                    val value = "&" + "a".repeat(position) + character + "a".repeat(64)
                    assertFalse(canAppendUnescapedHtmlString(value, attribute, startIndex = 1))
                    assertTrue(canAppendUnescapedHtmlString(value, attribute, startIndex = position + 2))
                }
            }
        }
    }
}
