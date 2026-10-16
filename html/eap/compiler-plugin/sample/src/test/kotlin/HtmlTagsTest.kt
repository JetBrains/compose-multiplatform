/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Address
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Aside
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Blockquote
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Caption
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.DDescription
import org.jetbrains.compose.web.dom.DList
import org.jetbrains.compose.web.dom.DTerm
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.Em
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.HTMLMap
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Legend
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Meter
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Object
import org.jetbrains.compose.web.dom.Ol
import org.jetbrains.compose.web.dom.Output
import org.jetbrains.compose.web.dom.Picture
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Progress
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Sub
import org.jetbrains.compose.web.dom.Sup
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.dom.Ul
import org.jetbrains.compose.web.dom.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertNotSame

class HtmlTagsTest {
    @Test
    fun storedWrapperLambdasReachedFromStringRenderingAreLowered() {
        val pages = listOf<Pair<String, @Composable () -> Unit>>(
            "head" to { Head() },
            "body" to { Body() },
            "address" to { Address() },
            "article" to { Article() },
            "aside" to { Aside() },
            "header" to { Header() },
            "section" to { Section() },
            "nav" to { Nav() },
            "main" to { Main() },
            "footer" to { Footer() },
            "em" to { Em() },
            "i" to { I() },
            "b" to { B() },
            "small" to { Small() },
            "sup" to { Sup() },
            "sub" to { Sub() },
            "blockquote" to { Blockquote() },
            "code" to { Code() },
            "ul" to { Ul() },
            "ol" to { Ol() },
            "li" to { Li() },
            "dl" to { DList() },
            "dt" to { DTerm() },
            "dd" to { DDescription() },
            "audio" to { Audio() },
            "video" to { Video() },
            "picture" to { Picture() },
            "canvas" to { Canvas() },
            "map" to { HTMLMap() },
            "datalist" to { Datalist() },
            "fieldset" to { Fieldset() },
            "legend" to { Legend() },
            "meter" to { Meter() },
            "output" to { Output() },
            "progress" to { Progress() },
            "object" to { Object() },
            "caption" to { Caption() },
            "button" to { Button() },
        )
        for ((tag, page) in pages) {
            val html = composeHtmlToString { page() }
            assertEquals("<$tag></$tag>", html)
            assertSame(html, composeHtmlToString { page() }, tag)
        }
    }

    @Test
    fun explicitTagNamesWorkInNestedTrees() {
        val page: @Composable () -> Unit = {
            Article(attrs = { id("article") }) {
                DList {
                    DTerm { Text("term") }
                    DDescription { Text("<&") }
                }
                HTMLMap { Text("map") }
            }
        }
        for (hydratable in listOf(false, true)) {
            val html = composeHtmlToString(hydratable = hydratable) { page() }
            assertEquals(
                "<article id=\"article\"><dl><dt>term</dt><dd>&lt;&amp;</dd></dl>" +
                    "<map>map</map></article>",
                html
            )
            assertSame(html, composeHtmlToString(hydratable = hydratable) { page() })
        }
    }

    @Test
    fun specialSerializationPreservesOutputAndInlineScriptFallback() {
        assertEquals("<pre>\n\nhello</pre>", composeHtmlToString { Pre { Text("\nhello") } })
        assertEquals("<iframe><b>&</iframe>", composeHtmlToString { Iframe { Text("<b>&") } })
        assertEquals("<script>a < b && c > d</script>", composeHtmlToString {
            Script(InlineScript("a < b && c > d"))
        })
        assertEquals("<br>", composeHtmlToString { Br() })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Script(InlineScript("</script>")) }
        }
    }

    @Test
    fun standaloneHeadingsAndParagraphsKeepTheirOutput() {
        val lowered = composeHtmlToString { HeadingsPage() }
        assertEquals(
            "<div><h1>one</h1><h2>two</h2><h3>three</h3><h4>four</h4>" +
                "<h5>five</h5><h6>six</h6><p id=\"paragraph\">paragraph</p></div>",
            lowered
        )
        assertNotSame(lowered, composeHtmlToString { HeadingsPage() })
    }

    @Test
    fun standaloneRowsAndCellsKeepTheirOutput() {
        for (hydratable in listOf(false, true)) {
            val row = composeHtmlToString(hydratable = hydratable) { RowPage() }
            val heading = composeHtmlToString(hydratable = hydratable) { HeaderCellPage() }
            val cell = composeHtmlToString(hydratable = hydratable) { CellPage() }
            assertEquals("<th>heading</th>", heading)
            assertEquals("<td title=\"cell\">value</td>", cell)
            assertEquals("<tr class=\"row\">$heading$cell</tr>", row)
            assertNotSame(row, composeHtmlToString(hydratable = hydratable) { RowPage() })
            assertNotSame(heading, composeHtmlToString(hydratable = hydratable) { HeaderCellPage() })
            assertNotSame(cell, composeHtmlToString(hydratable = hydratable) { CellPage() })
            assertEquals(
                "<table><tbody>$row</tbody></table>",
                composeHtmlToString(hydratable = hydratable) { Table { Tbody { RowPage() } } }
            )
            assertEquals(
                "<table><tbody><tr>$heading$cell</tr></tbody></table>",
                composeHtmlToString(hydratable = hydratable) {
                    Table {
                        Tbody {
                            Tr {
                                HeaderCellPage()
                                CellPage()
                            }
                        }
                    }
                }
            )
        }
    }

    @Test
    fun fragmentRootsStillRespectTableParents() {
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Table { RowPage() } } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Tbody { CellPage() } } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Tr { HeadingsPage() } } }
    }

    @Test
    fun invalidRowContentFailsDuringRendering() {
        composeHtmlToString { RowPage() }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { InvalidRowTextPage() } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { InvalidRowChildPage() } }
    }
}
