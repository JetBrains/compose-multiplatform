/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Area
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Col
import org.jetbrains.compose.web.dom.Colgroup
import org.jetbrains.compose.web.dom.Embed
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Link
import org.jetbrains.compose.web.dom.Meta
import org.jetbrains.compose.web.dom.Param
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tfoot
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.compose.web.dom.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class SpecialHtmlTagsTest {
    @Test
    fun storedVoidAndSpecialLambdasReuseTheirEncodedHtml() {
        val pages = listOf<Pair<String, @Composable () -> Unit>>(
            "<meta>" to { Meta() },
            "<link>" to { Link() },
            "<area>" to { Area() },
            "<track>" to { Track() },
            "<embed>" to { Embed() },
            "<param>" to { Param() },
            "<source>" to { Source() },
            "<br>" to { Br() },
            "<hr>" to { Hr() },
            "<col>" to { Col() },
            "<script></script>" to { Script() },
            "<title></title>" to { Title() },
            "<iframe></iframe>" to { Iframe() },
            "<pre></pre>" to { Pre() },
        )
        for ((expected, page) in pages) {
            assertHtml(expected, page = page)
        }
    }

    @Test
    fun literalAttrsWorkForMetadataAndExternalScripts() {
        assertHtml(
            "<div><meta name=\"description\" content=\"Hello &amp; goodbye\">" +
                "<link rel=\"stylesheet\" href=\"/style.css\"></div>",
            precomputed = false
        ) { MetadataPage() }
        assertHtml("<script src=\"/app.js?a=1&amp;b=2\" defer></script>") {
            Script(attrs = {
                attr("src", "/app.js?a=1&b=2")
                attr("defer", "")
            })
        }
        assertHtml("<track default=\"yes\" kind=\"captions\">") {
            Track(attrs = {
                attr("default", "yes")
                attr("kind", "captions")
            })
        }
    }

    @Test
    fun tableContainersStayOnTheOrdinaryPath() {
        val pages = listOf<Pair<String, @Composable () -> Unit>>(
            "<table></table>" to { Table() },
            "<colgroup></colgroup>" to { Colgroup() },
            "<thead></thead>" to { Thead() },
            "<tbody></tbody>" to { Tbody() },
            "<tfoot></tfoot>" to { Tfoot() },
            "<table><colgroup><col></colgroup><tbody> <tr><td>cell</td></tr></tbody></table>" to
                { LiteralTablePage() },
        )
        for ((expected, page) in pages) {
            val html = composeHtmlToString { page() }
            assertEquals(expected, html)
            assertNotSame(html, composeHtmlToString { page() })
        }
    }

    @Test
    fun textModesPreserveEscapingNormalizationAndLeadingNewlines() {
        assertHtml("<pre>\n\n&lt;&amp;</pre>", precomputed = false) { LiteralPrePage() }
        assertHtml("<title>&lt;&amp;title</title>", precomputed = false) { LiteralTitlePage() }
        assertHtml("<iframe><b>&\ncontent\uFFFD</iframe>", precomputed = false) { LiteralIframePage() }
        assertHtml("<pre><br>\ntext</pre>") {
            Pre {
                Br()
                Text("\ntext")
            }
        }
        assertHtml("<iframe></iframe-prefix></iframe>") { Iframe { Text("</iframe-prefix>") } }
    }

    @Test
    fun dynamicTextIsEvaluatedOnEveryRender() {
        for (value in listOf("first", "second")) {
            assertEquals(
                "<div><pre>$value</pre><title>$value</title><iframe>$value</iframe></div>",
                composeHtmlToString { DynamicSpecialTextPage(value) }
            )
        }
    }

    @Test
    fun voidContentStillRunsOnEveryRender() {
        var calls = 0
        repeat(2) {
            assertEquals("<area>", composeHtmlToString {
                Area {
                    calls++
                    Text("discarded")
                }
            })
            assertEquals(it + 1, calls)
        }
    }

    @Test
    fun invalidTablesAndRawTextFailDuringRendering() {
        // Loading this source file must not execute its invalid examples.
        composeHtmlToString { MetadataPage() }
        val invalidPages = listOf<@Composable () -> Unit>(
            { InvalidTableRowPage() },
            { InvalidTableTextPage() },
            { InvalidColgroupPage() },
            { InvalidIframeTextPage() },
            { InvalidIframeChildPage() },
        )
        for (page in invalidPages) {
            assertFailsWith<IllegalArgumentException> { composeHtmlToString { page() } }
        }
    }

    @Test
    fun independentlyLoweredRootsStillValidateTheirTableParent() {
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Table { LiteralPrePage() } } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Colgroup { LiteralTitlePage() } } }
    }

    private fun assertHtml(expected: String, precomputed: Boolean = true, page: @Composable () -> Unit) {
        for (hydratable in listOf(false, true)) {
            val html = composeHtmlToString(hydratable = hydratable) { page() }
            assertEquals(expected, html)
            val again = composeHtmlToString(hydratable = hydratable) { page() }
            if (precomputed) assertSame(html, again) else assertNotSame(html, again)
        }
    }
}
