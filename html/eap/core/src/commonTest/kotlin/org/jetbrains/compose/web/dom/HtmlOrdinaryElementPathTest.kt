/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HtmlOrdinaryElementPathTest {
    @Test
    fun repeatedStringsUseTheCorrectEscapingContextAndChangingValuesStayFresh() {
        val key = "ordinary-changing-escape-context"
        val initial = "abcdefgh&\"\n\t"
        repeat(3) {
            assertEquals("<div title=\"abcdefgh&amp;&quot;&#10;&#9;\">abcdefgh&amp;\"\n\t</div>",
                composeHtmlToString(key = key) {
                    Div(attrs = { title(initial) }) { Text(initial) }
                })
        }
        assertEquals("<div title=\"changed&amp;&lt;\">changed&amp;&lt;</div>",
            composeHtmlToString(key = key) {
                Div(attrs = { title("changed&<") }) { Text("changed&<") }
            })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = key) { Text("abcdefgh\u0000") }
        }
    }

    @Test
    fun changingAttributePositionsDoesNotLeakValuesAcrossPooledRenders() {
        val key = "ordinary-changing-attribute-positions"
        assertEquals("<div id=\"outer\"><span></span><span title=\"last\"></span></div>",
            composeHtmlToString(key = key) {
                Div(attrs = { id("outer") }) {
                    Span()
                    Span(attrs = { title("last") })
                }
            })
        assertEquals("<div><span class=\"next\"><span></span></span></div>",
            composeHtmlToString(key = key) {
                Div {
                    Span(attrs = { classes("next") }) { Span() }
                }
            })
        assertEquals("<div title=\"fresh\"></div>", composeHtmlToString(key = key) {
            Div(attrs = { title("fresh") })
        })
    }

    @Test
    fun failureBetweenAttributeFreeElementsDoesNotContaminateTheNextRequest() {
        val key = "ordinary-recovery-after-invalid-text"
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = key) {
                Div { Span { Text("bad\u0000text") } }
            }
        }
        assertEquals("<div><span title=\"é😀&amp;\">é😀&lt;</span></div>",
            composeHtmlToString(key = key) {
                Div { Span(attrs = { title("é😀&") }) { Text("é😀<") } }
            })
    }
}
