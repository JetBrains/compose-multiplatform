/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.*

private val htmlAtInitialization = composeHtmlToString { Span { Text("initialized") } }

class HtmlLoweringTest {
    @Test
    fun standaloneFunctionsAndStoredContentFollowTheCompilerOption() {
        val fields = Class.forName("${javaClass.packageName}.SupportSamplesKt").declaredFields
        assertFalse(fields.any { it.name.startsWith("__html") })
        assertEquals(
            setOf("<span>shared lambda</span>", "<span>shared function</span>"),
            staticHtmlConstants("SupportSamplesKt"),
        )
        assertEquals("<span>initialized</span>", htmlAtInitialization)
        assertEquals("<span>shared lambda</span>", composeHtmlToString(content = sharedContent))
        assertEquals("<span>shared function</span>", composeHtmlToString(content = ::SharedPage))

        val page: @Composable () -> Unit = { Div { Span { Text("<&é😀") } } }
        val html = composeHtmlToString(content = page)
        assertEquals("<div><span>&lt;&amp;é😀</span></div>", html)
        assertSame(html, composeHtmlToString(content = page))
    }

    @Test
    fun dynamicParentsRetainEffectsAndStaticChildren() {
        var calls = 0
        repeat(2) {
            val html = composeHtmlToString {
                Div(attrs = { id("value-${++calls}") }) {
                    Span { Text("constant") }
                    Text("value-$calls")
                }
            }
            assertEquals(it + 1, calls)
            assertEquals("<div id=\"value-$calls\"><span>constant</span>value-$calls</div>", html)
        }
    }

    @Test
    fun nestedAdjacentTextKeepsHydrationBoundariesInLoweredFragments() {
        for (hydratable in listOf(false, true)) {
            val boundary = if (hydratable) "<!--c-->" else ""
            val nestedBoundary = "<!--c-->"
            val expected = "<div>before<span>first${nestedBoundary}second</span>after${nestedBoundary}tail</div>"
            assertEquals(expected, composeHtmlToString(hydratable = hydratable) { AdjacentTextPage() })
            val chunks = StringBuilder()
            composeHtmlToStream(hydratable = hydratable, chunkSize = 1, sink = { chunks.append(it) }) {
                Text("head")
                AdjacentTextPage()
                Text("end")
                Text("next")
            }
            assertEquals("head${expected}end${boundary}next", chunks.toString())
        }
    }

    @Test
    fun similarlyNamedWrappersKeepTheirRuntimeEffects() {
        repeat(2) {
            val before = customTagCalls
            assertEquals("<div id=\"custom\">text</div>", composeHtmlToString { UserDefinedPage() })
            assertEquals(before + 1, customTagCalls)
        }
        @Composable
        fun Span(content: @Composable () -> Unit) {
            Text("local:")
            content()
        }
        assertEquals("local:<!--c-->text", composeHtmlToString { Span { Text("text") } })
    }

    @Test
    fun invalidTextFailsDuringRendering() {
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Span { Text("bad\u0000") } } }
    }

    @Test
    fun completeTablesAndTheirParentsUseOneStaticFragment() {
        val samples = mapOf(
            "ConstantTable" to tableHtml("<!--c-->"),
            "NestedConstantTable" to "<div>before<table><tbody><tr><td>cell</td></tr></tbody></table>after</div>",
            "EmptyConstantTable" to "<table><caption></caption><colgroup></colgroup><thead></thead>" +
                    "<tbody></tbody><tfoot></tfoot></table>",
        )
        for ((method, html) in samples) {
            assertEquals(setOf(html), staticHtmlConstants("CompilerHydrationKt", method), method)
            val calls = methodCalls("CompilerHydrationKt", method)
            assertEquals(1, calls.count { it == "beginStaticHtml" }, method)
            assertFalse(calls.any {
                it in setOf(
                    "Table",
                    "Colgroup",
                    "Thead",
                    "Tbody",
                    "Tfoot",
                    "Tr",
                    "Td",
                    "Th",
                    "Text"
                )
            }, method)
        }
    }

    @Test
    fun tablesPreserveRenderingAndValidateTheirRuntimeParent() {
        forEachRenderMode { strict, hydratable, stream ->
            val expected = tableHtml("<!--c-->")
            assertEquals(expected, render(strict, hydratable, stream) { ConstantTable() })
            val invalid: @Composable () -> Unit = { Table { ConstantTable() } }
            if (strict) {
                assertFailsWith<IllegalArgumentException> { render(strict, hydratable, stream, invalid) }
            } else {
                assertEquals("<table>$expected</table>", render(strict, hydratable, stream, invalid))
            }
        }
    }

    @Test
    fun aliasesAndVoidAttributesSerializeCorrectly() {
        assertHtml("<article><dl><dt>term</dt><dd>&lt;&amp;</dd></dl><map>map</map></article>") {
            Article {
                DList { DTerm { Text("term") }; DDescription { Text("<&") } }
                HTMLMap { Text("map") }
            }
        }
        assertHtml(
            "<div><meta name=\"description\" content=\"Hello &amp; goodbye\">" +
                    "<link rel=\"stylesheet\" href=\"/style.css\"></div>"
        ) {
            Div {
                Meta(attrs = { attr("name", "description"); attr("content", "Hello & goodbye") })
                Link(attrs = { attr("rel", "stylesheet"); attr("href", "/style.css") })
            }
        }
        assertHtml("<script src=\"/app.js?a=1&amp;b=2\" defer></script>") {
            Script(attrs = { attr("src", "/app.js?a=1&b=2"); attr("defer", "") })
        }
        assertHtml("<track default=\"yes\" kind=\"captions\">") {
            Track(attrs = { attr("default", "yes"); attr("kind", "captions") })
        }
    }

    @Test
    fun tableContainersAndStandaloneRowsKeepTheirContent() {
        assertHtml("<th>heading</th>") { Th { Text("heading") } }
        assertHtml("<td title=\"cell\">value</td>") { Td(attrs = { attr("title", "cell") }) { Text("value") } }
        assertHtml("<tr><td>cell</td></tr>") { Row() }
        assertHtml("<table><colgroup><col></colgroup><tbody> <tr><td>cell</td></tr></tbody></table>") {
            Table { Colgroup { Col() }; Tbody { Text(" "); Row() } }
        }
    }

    @Test
    fun textModesPreserveEscapingBoundariesAndLeadingNewlines() {
        assertHtml("<pre>\n\n&lt;&amp;</pre>") { Pre { Text(""); Text("\n<&") } }
        assertHtml("<pre><br>\ntext</pre>") { Pre { Br(); Text("\ntext") } }
        assertHtml("<title>&lt;&amp;title</title>") { Title { Text("<&"); Text("title") } }
        assertHtml("<iframe><b>&\ncontent\uFFFD</iframe>") { Iframe { Text("<b>&\r"); Text("\ncontent\u0000") } }
        assertHtml("<iframe><!--c--></iframe>") { Iframe { Text("<!--c-->") } }
        assertHtml("<iframe></iframe-prefix></iframe>") { Iframe { Text("</iframe-prefix>") } }
        assertHtml("<script>a < b && c > d</script>") { Script(InlineScript("a < b && c > d")) }
        assertHtml("<noscript>fallback</noscript>") { Noscript { Text("fallback") } }
    }

    @Test
    fun specialTextAndDiscardedVoidContentKeepRuntimeEffects() {
        for (value in listOf("first", "second")) {
            assertHtml("<div><pre>$value</pre><title>$value</title><iframe>$value</iframe></div>") {
                Div { Pre { Text(value) }; Title { Text(value) }; Iframe { Text(value) } }
            }
        }
        var calls = 0
        repeat(2) {
            assertHtml("<area>") { Area { calls++; Text("discarded") } }
            assertEquals(it + 1, calls)
        }
    }

    @Test
    fun invalidStructureFailsDuringRendering() {
        val invalid = listOf<@Composable () -> Unit>(
            { Table { Row() } },
            { Table { Text("invalid") } },
            { Colgroup { Div() } },
            { Tbody { Td { Text("invalid") } } },
            { Tr { Text("invalid") } },
            { Tr { Div() } },
            { Iframe { Text("</IFR"); Text("AME >") } },
            { Iframe { Div() } },
            { Script(InlineScript("</script>")) },
            { InvalidRoot { Text("text") } },
            { Noscript { Noscript { Text("nested") } } },
        )
        for (page in invalid) {
            assertFailsWith<IllegalArgumentException> { composeHtmlToString(validateStrictly = true, content = page) }
        }
    }

    @Test
    fun tableParentChecksOnlyRunInStrictModeForStaticAndDynamicChildren() {
        for (stream in listOf(false, true)) {
            for (dynamic in listOf(false, true)) {
                val page: @Composable () -> Unit = {
                    Table {
                        if (dynamic) DynamicRow("cell") else Row()
                    }
                }

                fun render(strict: Boolean): String {
                    if (!stream) return composeHtmlToString(validateStrictly = strict, content = page)
                    val output = StringBuilder()
                    org.jetbrains.compose.web.composeHtmlToStream(
                        sink = { output.append(it) }, chunkSize = 1,
                        validateStrictly = strict, content = page
                    )
                    return output.toString()
                }
                assertEquals("<table><tr><td>cell</td></tr></table>", render(false))
                assertFailsWith<IllegalArgumentException> { render(true) }
            }
        }
    }

    private fun assertHtml(expected: String, page: @Composable () -> Unit) {
        assertEquals(expected, composeHtmlToString(content = page))
    }

    private fun tableHtml(boundary: String): String {
        val row = "<tr><td title=\"&lt;&amp;&quot;\">a&lt;&amp;${boundary}b</td>" +
                "<td><table><tbody><tr><td>nested</td></tr></tbody></table></td></tr>"
        return "<table id=\"constant-table\"><caption>Results</caption><colgroup><col span=\"2\"></colgroup>" +
                "<thead><tr><th>Name</th></tr></thead><tbody>\n" + row.repeat(2) +
                "</tbody><tfoot><tr><td>Total</td></tr></tfoot></table>"
    }
}

@Composable
private fun AdjacentTextPage() {
    Div {
        Text("before")
        Span { Text(""); Text("first"); Text(""); Text("second") }
        Text("after")
        Text("tail")
    }
}

@Composable
private fun Row() {
    Tr { Td { Text("cell") } }
}

@Composable
private fun DynamicRow(value: String) {
    Tr { Td { Text(value) } }
}

@Composable
@LowerableHtmlTag("bad tag")
private fun InvalidRoot(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) {
    TagElement("bad tag", attrs, content)
}

@Composable
@LowerableHtmlTag("noscript")
private fun Noscript(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) {
    TagElement("noscript", attrs, content)
}
