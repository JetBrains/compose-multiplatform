/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ComposeWebInternalApi::class)
class HtmlNamespaceEscapingTest {
    @Test
    fun htmlAndForeignNamespacesKeepTheSameEntityRules() {
        val prefix = "é😀".repeat(40)
        val cases = listOf(
            Triple("0123456789", "0123456789", "0123456789"),
            Triple("<&>\"\n\t\r", "&lt;&amp;&gt;&quot;&#10;&#9;&#13;", "&lt;&amp;&gt;\"\n\t&#13;"),
            Triple(prefix + "&\"", prefix + "&amp;&quot;", prefix + "&amp;\""),
        )
        for ((value, attribute, text) in cases) {
            for ((namespace, tag) in listOf(HtmlNamespace to "div", "http://www.w3.org/2000/svg" to "svg")) {
                assertEquals("<$tag title=\"$attribute\">$text</$tag>", composeHtmlToString(validateStrictly = true) {
                    TagElementNS<Element>(tag, namespace, { attr("title", value) }) { Text(value) }
                })
            }
        }
    }

    @Test
    fun pooledRendersCanAlternateNamespacesAndValues() {
        repeat(8) { index ->
            val namespace = if (index % 2 == 0) HtmlNamespace else "http://www.w3.org/2000/svg"
            val tag = if (index % 2 == 0) "div" else "svg"
            assertEquals(
                "<$tag title=\"$index&amp;\">$index&lt;</$tag>",
                composeHtmlToString(key = "mixed-namespace-escaping", validateStrictly = true) {
                    TagElementNS<Element>(tag, namespace, { attr("title", "$index&") }) { Text("$index<") }
                })
        }
    }

    @Test
    fun foreignNulIsRejectedAfterUnescapedPooledValues() {
        val namespace = "http://www.w3.org/2000/svg"
        assertEquals("<svg title=\"safe\">safe</svg>", composeHtmlToString(key = "foreign-escaping-nul") {
            TagElementNS<Element>("svg", namespace, { attr("title", "safe") }) { Text("safe") }
        })
        for (value in listOf("\u0000", "é😀".repeat(40) + '\u0000')) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(key = "foreign-escaping-nul") {
                    TagElementNS<Element>("svg", namespace, { attr("title", value) }, null)
                }
            }
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(key = "foreign-escaping-nul") {
                    TagElementNS<Element>("svg", namespace, null) { Text(value) }
                }
            }
        }
    }
}
