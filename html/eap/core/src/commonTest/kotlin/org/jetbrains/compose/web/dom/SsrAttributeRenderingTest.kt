/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SsrAttributeRenderingTest {
    @Test
    fun classTokensEscapeLikeAJoinedAttributeAcrossRenderers() {
        val tokens = listOf("", "a&<\"", "\n\t\r", "é😀", "a&<\"")
        val expected = composeHtmlToString(validateStrictly = false) {
            Div({ attr("class", tokens.joinToString(" ")) })
        }
        val content: @Composable () -> Unit = { Div({ classes(tokens) }) }
        for (key in listOf(null, "class-token-escaping")) {
            assertEquals(expected, composeHtmlToString(key = key, validateStrictly = false, content = content))
            val chunks = mutableListOf<String>()
            composeHtmlToStream(
                sink = chunks::add, chunkSize = 3, key = key,
                validateStrictly = false, content = content,
            )
            assertEquals(expected, chunks.joinToString(""))
        }
    }

    @Test
    fun validatesClassesEvenWhenOverriddenAndRecoversAfterFailure() {
        for (key in listOf(null, "class-token-validation")) {
            for (invalid in listOf(listOf(""), listOf("two words"), listOf("a", "a"))) {
                val content: @Composable () -> Unit = {
                    Div({ classes(invalid); attr("class", "override") })
                }
                assertFailsWith<IllegalArgumentException> {
                    composeHtmlToString(key = key, validateStrictly = true, content = content)
                }
                assertFailsWith<IllegalArgumentException> {
                    composeHtmlToStream(sink = {}, key = key, validateStrictly = true, content = content)
                }
                assertEquals("<div class=\"valid\"></div>", composeHtmlToString(key = key) {
                    Div({ classes("valid") })
                })
            }
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(key = key, validateStrictly = false) { Div({ classes("nul\u0000") }) }
            }
        }
    }

    @Test
    fun compactAttributesKeepOrderAndClearAcrossFallbackAndRenderers() {
        for (key in listOf(null, "compact-attribute-rendering")) {
            for (count in listOf(1, 16, 17, 50, 0, 2)) {
                val content: @Composable () -> Unit = {
                    Div({
                        repeat(count) { attr("data-$it", "$it") }
                        if (count > 0) attr("data-0", "replacement")
                        classes("one", "two")
                    })
                    Span()
                    Div({ classes("three"); attr("class", "override") })
                }
                val attrs = (0 until count).joinToString("") {
                    " data-$it=\"${if (it == 0) "replacement" else "$it"}\""
                }
                val expected = "<div$attrs class=\"one two\"></div><span></span><div class=\"override\"></div>"
                assertEquals(expected, composeHtmlToString(key = key, content = content))
                val chunks = mutableListOf<String>()
                composeHtmlToStream(sink = chunks::add, chunkSize = 3, key = key, content = content)
                assertEquals(expected, chunks.joinToString(""))
            }
        }
    }
}
