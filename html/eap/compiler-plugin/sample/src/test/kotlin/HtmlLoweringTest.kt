/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Text
import sample.user.UserDefinedPage
import sample.user.customTagCalls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class HtmlLoweringTest {
    @Test
    fun userDefinedTagWrappersKeepTheirRuntimeEffects() {
        repeat(3) {
            val callsBefore = customTagCalls
            assertEquals("<div id=\"custom\">text</div>", composeHtmlToString { UserDefinedPage() })
            assertEquals(callsBefore + 1, customTagCalls)
        }
    }

    @Test
    fun fragmentsAreAvailableDuringFileInitialization() {
        assertEquals("<span>initialized</span>", htmlAtInitialization)
    }

    @Test
    fun standaloneComposableMatchesOrdinaryRenderingWithoutPregeneration() {
        val value = "Hello <world> & \"friends\"\r\né😀"
        for (hydratable in listOf(false, true)) {
            val ordinary = composeHtmlToString(hydratable = hydratable) { DynamicPage(value) }
            val lowered = composeHtmlToString(hydratable = hydratable) { StaticPage() }
            assertEquals(
                "<div><span>Hello &lt;world&gt; &amp; \"friends\"&#13;\né😀</span></div>",
                lowered
            )
            assertEquals(ordinary, lowered)
            // A reusable composable remains ordinary even when called from the SSR lambda.
            assertNotSame(lowered, composeHtmlToString(hydratable = hydratable) { StaticPage() })
        }
    }

    @Test
    fun dynamicTextIsEvaluatedOnEachRender() {
        for (value in listOf("first", "<&", "", "next")) {
            val ordinary = composeHtmlToString { Span { Text(value) } }
            assertEquals("<div>$ordinary</div>", composeHtmlToString { DynamicPage(value) })
        }
    }

    @Test
    fun adjacentTextKeepsHydrationBoundaries() {
        val first = "first"
        val second = "second"
        for (hydratable in listOf(false, true)) {
            val ordinary = composeHtmlToString(hydratable = hydratable) {
                Span {
                    Text(first)
                    Text(second)
                }
            }
            assertEquals(ordinary, composeHtmlToString(hydratable = hydratable) { AdjacentTextPage() })
        }
    }

    @Test
    fun attributesAndUnrelatedFunctionsKeepTheirBehavior() {
        assertEquals("<span class=\"label\">text</span>", composeHtmlToString { AttributedPage() })
        assertEquals("local:<!--c-->text", composeHtmlToString { ShadowedPage() })
    }

    @Test
    fun invalidTextFailsWhenRendered() {
        // InvalidTextPage must not fail during initialization of the shared file facade.
        composeHtmlToString { StaticPage() }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { InvalidTextPage() }
        }
    }

    @Test
    fun loweredSubtreeStillChecksItsParent() {
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { StaticPage() } }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { DynamicPage("text") } }
        }
    }
}
