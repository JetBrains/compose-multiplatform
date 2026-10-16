/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.composeHtmlToString as renderHtml
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Meta
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.compose.web.dom.Tr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class StringRenderingScopeTest {
    @Test
    fun browserOnlyFileHasNoPregeneratedHtmlFields() {
        assertFalse(Class.forName("BrowserOnlySamplesKt").declaredFields.any {
            it.name.startsWith("__htmlFragment")
        })
        assertEquals("<span>browser only</span>", composeHtmlToString { BrowserOnlyPage() })
        assertEquals("<span>stored</span>", composeHtmlToString(content = storedBrowserContent))
        assertEquals("<span>lookalike</span>", composeHtmlToString(content = lookalikeContent))
    }

    @Test
    fun inlineContentReusesItsEncodedString() {
        fun render(hydratable: Boolean) = composeHtmlToString(hydratable = hydratable) {
            Div { Span { Text("Hello <world> & \"friends\"\r\né😀") } }
        }
        for (hydratable in listOf(false, true)) {
            val html = render(hydratable)
            assertEquals(composeHtmlToString(hydratable) { StaticPage() }, html)
            assertSame(html, render(hydratable))
        }
    }

    @Test
    fun resolvedAliasesAndNamedContentEnableLowering() {
        fun render() = renderHtml(content = { Span { Text("aliased") } }, hydratable = false)
        assertEquals("<span>aliased</span>", render())
        assertSame(render(), render())
    }

    @Test
    fun storedLambdasAndFunctionReferencesReuseTheirEncodedString() {
        val content: @Composable () -> Unit = { Span { Text("local") } }
        fun renderStored() = composeHtmlToString(content = content)
        fun renderReference() = composeHtmlToString(content = ::referencedPage)
        assertEquals("<span>local</span>", renderStored())
        assertEquals("<span>referenced</span>", renderReference())
        assertSame(renderStored(), renderStored())
        assertSame(renderReference(), renderReference())
    }

    @Test
    fun sharedContentCanBeDeclaredOutsideTheStringCall() {
        fun render() = composeHtmlToString { Span { Text("scoped") } }
        val outside: @Composable () -> Unit = { Span { Text("outside") } }
        assertSame(render(), render())
        assertSame(composeHtmlToString(content = outside), composeHtmlToString(content = outside))
    }

    @Test
    fun sharedDeclarationsCanBeReachedAcrossSourceFiles() {
        assertEquals("<span>shared lambda</span>", renderSharedLambda())
        assertSame(renderSharedLambda(), renderSharedLambda())
        assertEquals("<span>shared function</span>", renderSharedFunction())
        assertSame(renderSharedFunction(), renderSharedFunction())
        assertSame(renderSharedCall(), renderSharedCall())
        assertSame(renderForwardedShared(), renderForwardedShared())
        // A neighboring declaration with no SSR caller is still left alone.
        assertNotSame(composeHtmlToString { UnusedSharedPage() }, composeHtmlToString { UnusedSharedPage() })
    }

    @Test
    fun forwardedParametersAndDefaultsCanSupplyContent() {
        fun render(content: @Composable () -> Unit = { Span { Text("default") } }) =
            composeHtmlToString(content = content)
        val content: @Composable () -> Unit = { Span { Text("forwarded") } }
        assertEquals("<span>default</span>", render())
        assertSame(render(), render())
        assertEquals("<span>forwarded</span>", render(content))
        assertSame(render(content), render(content))
    }

    @Test
    fun recursiveCallsDoNotPreventPregeneration() {
        fun render() = composeHtmlToString { recursivePage(2) }
        assertEquals("<span>recursive</span>", render())
        assertSame(render(), render())
    }

    @Test
    fun dynamicParentsStillAllowStaticChildrenInsideStringContent() {
        fun render(value: String) = composeHtmlToString {
            Div(attrs = { id(value) }) {
                Span { Text("constant") }
                Text(value)
            }
        }
        assertEquals("<div id=\"first\"><span>constant</span>first</div>", render("first"))
        assertEquals("<div id=\"second\"><span>constant</span>second</div>", render("second"))
    }

    @Test
    fun inlineLiteralAttributesKeepTheirSerializationAndStringReuse() {
        fun render() = composeHtmlToString {
            Span(attrs = {
                classes("label", "accent")
                id("greeting")
                attr("title", "\"<&>\r\n\t😀")
                attr("aria-label", "Hello")
            }) { Text("text") }
        }
        assertEquals(
            "<span id=\"greeting\" title=\"&quot;&lt;&amp;&gt;&#13;&#10;&#9;😀\"" +
                " aria-label=\"Hello\" class=\"label accent\">text</span>",
            render()
        )
        assertSame(render(), render())
    }

    @Test
    fun inlineSpecialTagsKeepTheirSerializationAndStringReuse() {
        val renders = listOf<Pair<String, () -> String>>(
            "<meta name=\"description\" content=\"&lt;&amp;\">" to {
                composeHtmlToString { Meta(attrs = { attr("name", "description"); attr("content", "<&") }) }
            },
            "<pre>\n\n&lt;&amp;</pre>" to { composeHtmlToString { Pre { Text("\n<&") } } },
            "<title>&lt;&amp;title</title>" to { composeHtmlToString { Title { Text("<&"); Text("title") } } },
            "<iframe><b>&\ncontent\uFFFD</iframe>" to {
                composeHtmlToString { Iframe { Text("<b>&\r"); Text("\ncontent\u0000") } }
            },
        )
        for ((expected, render) in renders) {
            assertEquals(expected, render())
            assertSame(render(), render())
        }
    }

    @Test
    fun inlineTableRootsReuseTheirHtmlAndStillValidateParents() {
        fun renderRow() = composeHtmlToString {
            Tr { Th { Text("heading") }; Td { Text("value") } }
        }
        assertEquals("<tr><th>heading</th><td>value</td></tr>", renderRow())
        assertSame(renderRow(), renderRow())
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { Span { Text("invalid parent") } } }
        }
    }

    @Test
    fun nestedStringCallsRestoreTheOuterScope() {
        fun render() = composeHtmlToString {
            val inner = composeHtmlToString { Span { Text("inner") } }
            assertEquals("<span>inner</span>", inner)
            Span { Text("outer") }
        }
        assertEquals("<span>outer</span>", render())
        assertSame(render(), render())
    }

    @Composable
    private fun referencedPage() {
        Span { Text("referenced") }
    }

    @Composable
    private fun recursivePage(depth: Int) {
        if (depth > 0) recursivePage(depth - 1) else Span { Text("recursive") }
    }
}
