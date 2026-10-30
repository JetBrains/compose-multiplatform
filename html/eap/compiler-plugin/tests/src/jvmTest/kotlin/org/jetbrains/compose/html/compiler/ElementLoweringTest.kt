/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.html.compiler.samples.ComposableText
import org.jetbrains.compose.html.compiler.samples.ContainerReturn
import org.jetbrains.compose.html.compiler.samples.ContentReturn
import org.jetbrains.compose.html.compiler.samples.DirectContainer
import org.jetbrains.compose.html.compiler.samples.DirectGreeting
import org.jetbrains.compose.html.compiler.samples.OversizedLiteral
import org.jetbrains.compose.html.compiler.samples.RememberedText
import org.jetbrains.compose.html.compiler.samples.ScopedContainer
import org.jetbrains.compose.html.compiler.samples.ScopedDynamicText
import org.jetbrains.compose.html.compiler.samples.ScopedGetter
import org.jetbrains.compose.html.compiler.samples.ScopedLiteralText
import org.jetbrains.compose.html.compiler.samples.ScopedUser
import org.jetbrains.compose.html.compiler.samples.StoredContentAttributes
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.org.objectweb.asm.ClassReader

class ElementLoweringTest {
    @Test
    fun directElementsCallTheBridgeWithoutContentLambdas() {
        for (method in listOf("DirectGreeting", "DirectContainer")) {
            val calls = calls(method)
            assertTrue(calls.containsAll(setOf("openLoweredRegion", "writeText", "closeElement")), "$method: $calls")
            assertFalse("appendText" in calls, "$method: $calls")
            assertFalse(calls.any {
                it in setOf(
                    "Div",
                    "Span",
                    "Text",
                    "TagElement",
                )
            })
            assertFalse(calls.any { it.startsWith("composableLambda") })
        }
    }

    @Test
    fun composableTextUsesRegionsWithoutContentLambdas() {
        for (method in listOf("ComposableText", "RememberedText")) {
            val calls = calls(method)
            assertTrue(calls.containsAll(setOf("openLoweredRegion", "Text", "closeElement")), "$method: $calls")
            assertFalse(
                "Span" in calls,
                "$method: $calls"
            )
            assertFalse(calls.any { it.startsWith("composableLambda") }, "$method: $calls")
            assertTrue(fieldReads("DirectElementsKt", method).isEmpty(), method)
        }
        assertTrue("<span class=\"user\">" in methodStrings("DirectElementsKt", "ComposableText"))
    }

    @Test
    fun composableTextPreservesEffectsNestedTextAndDiscardedContent() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            val boundary = if (hydratable) "<!--c-->" else ""
            val expected = "before<span class=\"user\">prefix${boundary}&lt;&amp;</span>" +
                    "<span>remembered</span><input>after${boundary}adjacent"
            val actual = render(strict, hydratable, stream) {
                Text("before")
                ComposableText {
                    effects.add("value")
                    Text("prefix")
                    "<&"
                }
                RememberedText("remembered")
                TagElement<HTMLElement>("input", null) {
                    ComposableText {
                        effects.add("discarded")
                        Text("discarded")
                        "discarded"
                    }
                }
                Text("after")
                Text("adjacent")
            }
            assertEquals(expected, actual)
            assertEquals(listOf("value", "discarded"), effects)
        }
    }

    @Test
    fun specialTagsUseRegionsAndLabeledReturnsKeepOriginalCalls() {
        assertTrue("openLoweredRegion" in calls("SpecialText"))
        assertTrue(fieldReads("DirectElementsKt", "SpecialText").isEmpty())
        assertTrue("Div" in calls("ContainerReturn"))
        assertEquals("<div></div><div>shown</div>", composeHtmlToString {
            ContainerReturn(true) {}
            ContainerReturn(false) {}
        })
    }

    @Test
    fun containersPreserveEffectsLoopsConditionsAndTextBoundaries() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            val content: @Composable () -> Unit = {
                Text("before")
                DirectContainer(listOf("<&", "", "é😀"), effects::add)
                DirectGreeting {
                    effects.add("greeting")
                    "dynamic<&"
                }
                Text("after")
                Text("adjacent")
            }
            val boundary = if (hydratable) "<!--c-->" else ""
            val expected = "before<div class=\"container\"><span>head</span><span>value: &lt;&amp;</span>" +
                    "empty<span>value: é😀</span>footer<span>middle</span><span>sibling</span><span>tail</span></div>" +
                    "<div id=\"greeting\" title=\"Hello &amp; goodbye\" class=\"card\">dynamic&lt;&amp;</div>" +
                    "after${boundary}adjacent"
            val actual = render(strict, hydratable, stream, content)
            assertEquals(expected, actual)
            assertEquals(listOf("open", "footer", "close", "greeting"), effects)
        }
    }

    @Test
    fun staticChildrenMergeIntoOpeningAndClosingTags() {
        val constants = methodStrings("DirectElementsKt", "DirectContainer")
        assertTrue("<div class=\"container\"><span>head</span>" in constants)
        assertTrue("<span>tail</span></div>" in constants)
        assertTrue("<span>middle</span><span>sibling</span>" in constants)
    }

    private fun calls(method: String): Set<String> = methodCalls("DirectElementsKt", method).toSet()

    @Test
    fun selectsExpectedLoweringPathsAndRetainsUnsupportedAttributes() {
        val expected = mapOf(
            "StoredContentAttributes" to "Div",
            "ScopedContainer" to "Div",
            "ScopedDynamicText" to "writeText",
            "ScopedGetter" to "writeText",
            "OversizedLiteral" to "beginStaticHtmlFragment",
            "ContentReturn" to "Span",
        )
        for ((method, helper) in expected) {
            val calls = runtimeCalls(method)
            assertTrue(helper in calls, "$method: $calls")
            if (method !in setOf("StoredContentAttributes", "ScopedContainer", "ContentReturn")) {
                assertFalse("Span" in calls || "Div" in calls, "$method: $calls")
            }
        }
        assertTrue("finishMixedElement" in runtimeCalls("AttributeEffects"))
        assertEquals(
            setOf("<span class=\"label\">&lt;&amp;&#13;😀</span>"),
            staticHtmlConstants("ScopedElementsKt", "ScopedLiteralText"),
        )
    }

    @Test
    fun oversizedLiteralsStreamInChunks() {
        val expected = "<span>" + "&lt;".repeat(2_000) + "</span>"
        forEachRenderMode { strict, hydratable, stream ->
            assertEquals(expected, render(strict, hydratable, stream) { OversizedLiteral() })
        }
    }

    @Test
    fun gettersRunOncePerRenderAndNeverDuringInitialization() {
        var calls = 0
        val user = ScopedUser { "user-${++calls}" }
        assertEquals(0, calls)
        repeat(2) { render ->
            assertEquals("<span class=\"user\">user-${render + 1}</span>", composeHtmlToString {
                ScopedGetter(user)
            })
            assertEquals(render + 1, calls)
        }
    }

    @Test
    fun originalAttributesSupportStoredNullableAndEffectfulContent() {
        val effects = mutableListOf<String>()
        fun content(): ContentBuilder<HTMLDivElement> {
            effects.add("argument")
            return {
                effects.add("content")
                Text("stored")
            }
        }
        repeat(2) {
            assertEquals("<div title=\"A &amp; B\" class=\"card\">stored</div>", composeHtmlToString {
                StoredContentAttributes(content())
            })
        }
        assertEquals(listOf("argument", "content", "argument", "content"), effects)
        assertEquals("<div title=\"A &amp; B\" class=\"card\"></div>", composeHtmlToString {
            StoredContentAttributes(null)
        })
    }

    @Test
    fun containersAndTextRetainOrderAcrossRendersAndStreams() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            repeat(2) { render ->
                val content: @Composable () -> Unit = {
                    Text("before")
                    ScopedContainer("user-$render", effects::add)
                    ScopedDynamicText {
                        effects.add("price")
                        "price-$render"
                    }
                    ScopedLiteralText()
                    Text("after")
                }
                val expected = "before<div class=\"card\">user-$render<span class=\"details\">details</span>end</div>" +
                        "<span class=\"price\">price-$render</span><span class=\"label\">&lt;&amp;&#13;😀</span>after"
                assertEquals(expected, render(strict, hydratable, stream, content))
                assertEquals(List(render + 1) { listOf("container", "text", "price") }.flatten(), effects)
            }
        }
    }

    @Test
    fun earlyReturnsAndInvalidParentsKeepEvaluationOrder() {
        assertEquals("<span class=\"return\"></span>", composeHtmlToString { ContentReturn(true) })
        assertEquals("<span class=\"return\">shown</span>", composeHtmlToString { ContentReturn(false) })
        for (stream in listOf(false, true)) {
            val effects = mutableListOf<String>()
            assertFailsWith<IllegalArgumentException> {
                render(strict = true, hydratable = true, stream = stream) {
                    Table {
                        Span {
                            Text(run {
                                effects.add("text")
                                "invalid child"
                            })
                        }
                    }
                }
            }
            assertTrue(effects.isEmpty())
            assertFailsWith<IllegalStateException> {
                render(strict = true, hydratable = true, stream = stream) {
                    Div {
                        effects.add("before")
                        ScopedDynamicText {
                            effects.add("text")
                            error("text failure")
                        }
                        effects.add("after")
                    }
                }
            }
            assertEquals(listOf("before", "text"), effects)
        }
    }

    @Test
    fun textModesAndRuntimeValidationArePreserved() {
        for (stream in listOf(false, true)) {
            assertEquals(
                "<pre>\n\n&lt;&amp;</pre><title>&lt;&amp;</title><iframe><&</iframe><span></span>",
                render(strict = true, hydratable = true, stream = stream) {
                    Pre { Text("\n<&") }
                    Title { Text("<&") }
                    Iframe { Text("<&") }
                    Span { Text("") }
                })
            for (value in listOf("\n<&", "")) {
                assertEquals(
                    "<pre>${if (value.isEmpty()) "" else "\n\n&lt;&amp;"}</pre>",
                    render(strict = true, hydratable = true, stream = stream) { Pre { Text(value) } })
            }
            assertFailsWith<IllegalArgumentException> {
                render(strict = true, hydratable = true, stream = stream) { ScopedDynamicText { "bad\u0000" } }
            }
        }
    }

    private fun runtimeCalls(method: String): Set<String> =
        methodCalls("ScopedElementsKt", method, "org/jetbrains/compose/web/dom/").toSet()

    @Test
    fun specialElementsMatchOrdinaryRenderingAndValidation() {
        comparison.assertEquivalent()
    }

    @Test
    fun specialElementsUseWriterRegions() {
        for (tag in tags) {
            val method = methodName(tag)
            val calls = classMethodCalls(comparison.bytecode).getValue(method)
            assertTrue("openLoweredRegion" in calls, "$method: $calls")
            assertTrue("closeElement" in calls, "$method: $calls")
            assertFalse("TagElement" in calls, method)
        }
    }

    @Test
    fun dynamicTablesMergeSectionsAndCells() {
        val calls = classMethodCalls(comparison.bytecode).getValue("DynamicTable")
        assertFalse(calls.any {
            it in setOf("Table", "Thead", "Tbody", "Tr", "Th", "Td", "PreparedContainer", "PreparedTextElement")
        }, calls.toString())
        val html = methodStrings(comparison.bytecode, "DynamicTable")
        assertTrue("<table><thead><tr><th>symbol</th><th>price</th></tr></thead><tbody>" in html)
        assertTrue("</td><td><strong>" in html)
    }

    @Test
    fun writerRegionsUsePrecomputedNewlineFlags() {
        val path = "/org/jetbrains/compose/web/dom/HtmlStringWriterContext.class"
        val bytecode = requireNotNull(javaClass.getResourceAsStream(path)).use { ClassReader(it.readBytes()) }
        val calls = classMethodCalls(bytecode)
        for (method in listOf("openLoweredRegion", "advanceLoweredRegion", "finishMixedElement")) {
            assertFalse("isHtmlLeadingNewlineElement" in calls.getValue(method), method)
        }
    }

    companion object {
        private val tags = listOf(
            "pre", "textarea", "listing", "title",
            "table", "caption", "colgroup", "thead", "tbody", "tfoot", "tr", "td", "th",
        )

        private fun methodName(tag: String): String = "Text" + tag.replaceFirstChar { it.uppercase() }

        private val comparison by lazy {
            val cases = tags.associate { tag ->
                methodName(tag) to """
                    TagElement<Element>("$tag", { classes("constant") }) {
                        Text("")
                        Text("\n")
                        Text(Mark(name))
                        Text("tail")
                    }
                    Text("after")
                    Text("adjacent")
                """.trimIndent()
            } + mapOf(
                "MixedLeadingNewline" to """
                    Pre(attrs = { attr("title", Mark(name)) }) {
                        Text("")
                        Text(Mark("\n" + name))
                    }
                """.trimIndent(),
                "ForeignLeadingNewlineNames" to """
                    TagElementNS<Element>("pre", "urn:test", null) { Text(Mark("\n" + name)) }
                    TagElementNS<Element>("textarea", "urn:test", { attr("title", name) }) {
                        Text(Mark("\n" + name))
                    }
                """.trimIndent(),
                "ChunkedSpecialOpening" to """
                    Pre(attrs = { attr("title", "${"x".repeat(9_000)}") }) {
                        Text(Mark("\n" + name))
                    }
                """.trimIndent(),
                "LeadingNewlineAfterEmptyText" to """
                    Pre {
                        Text("")
                        Text(Mark("\n" + name))
                        Text("tail")
                    }
                """.trimIndent(),
                "NestedSpecialElements" to """
                    Div {
                        Pre {
                            Text(Mark("\n" + name))
                            Span { Text(name) }
                            Text("tail")
                        }
                        Title {
                            Text("first")
                            Text(name)
                        }
                        TagElement<Element>("textarea", { attr("title", name) }) {
                            Text("")
                            Text(Mark("\n" + name))
                            Text("tail")
                        }
                    }
                """.trimIndent(),
                "TableValidationOrder" to """
                    Table {
                        Text(Mark(" "))
                        Span { Text(Mark(name)) }
                    }
                """.trimIndent(),
                "TableTextAfterStaticChild" to """
                    Table {
                        Tbody {
                            Tr {
                                Td { Text("constant") }
                                Text(Mark(name))
                            }
                        }
                    }
                """.trimIndent(),
                "TableCells" to """
                    Table {
                        Tbody {
                            Tr {
                                Td { Text(Mark(name)) }
                                Th { Text(name) }
                            }
                        }
                    }
                """.trimIndent(),
                "DynamicTable" to """
                    Table {
                        Thead { Tr { Th { Text("symbol") }; Th { Text("price") } } }
                        Tbody {
                            listOf(name, "", "last").forEachIndexed { index, value ->
                                Tr(attrs = { classes(if (index % 2 == 0) "even" else "odd") }) {
                                    Td { Text(index.toString()) }
                                    Td {
                                        A(attrs = { attr("href", Mark("/stocks/" + value)) }) {
                                            Text(Mark(value))
                                        }
                                    }
                                    Td { TagElement<Element>("strong", null) { Text(value) } }
                                    if (index > 0) {
                                        Td { Text(value) }
                                    } else {
                                        Td(attrs = { classes("minus") }) { Text(value) }
                                    }
                                }
                            }
                        }
                        Tfoot { Tr { Td { Text(Mark("total")) } } }
                    }
                """.trimIndent(),
                "EmptyDynamicTable" to "Table { Tbody { for (value in emptyList<String>()) Tr { Td { Text(value) } } } }",
                "TableWhitespace" to """
                    Table {
                        Text("\n")
                        Tbody { Text(" "); Tr { Td { Text(Mark(name)) } }; Text(" ") }
                        Text(" ")
                    }
                """.trimIndent(),
                "InvalidStaticRow" to "Table { Tr { Td { Text(\"constant\") } }; Text(Mark(name)) }",
                "InvalidDynamicCell" to "Tbody { Td(attrs = { attr(\"title\", Mark(name)) }) { Text(Mark(name)) } }",
                "InvalidStaticTableText" to "Table { Text(\"invalid\"); Tbody { Tr { Td { Text(Mark(name)) } } } }",
                "DiscardedSpecialElements" to """
                    TagElement<Element>("input", null) {
                        Pre { Text(Mark("\n" + name)) }
                        Table { Tr { Text(Mark(name)) } }
                    }
                """.trimIndent(),
                "ComposableSpecialText" to """
                    Pre { Text(Value(name)) }
                    Title { Text(Value(name)) }
                """.trimIndent(),
            )
            LoweringComparison(
                cases, declarations = """
                @Composable private fun Value(name: String): String {
                    Text("prefix")
                    return Mark("\n" + name)
                }
            """.trimIndent()
            )
        }
    }
}
