/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLInputElement
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.attributes.required
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HtmlSerializationTest {
    @Test
    fun rejectsNulInTextAndAttributes() {
        for (hydratable in listOf(false, true)) {
            for (tag in listOf("div", "title", "textarea")) {
                val failure = assertFailsWith<IllegalArgumentException> {
                    composeHtmlToString(hydratable) {
                        TagElement<Element>(tag, null) { Text("a\u0000b") }
                    }
                }
                assertContains(failure.message.orEmpty(), "NUL")
            }
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(hydratable) { Div({ attr("title", "a\u0000b") }) }
            }
            assertContains(failure.message.orEmpty(), "NUL")
        }
    }

    @Test
    fun rejectsTableChildrenThatRequireParserRepair() {
        val invalidChildren = mapOf(
            "table" to listOf("tr", "td", "th", "col", "div"),
            "tbody" to listOf("td", "th", "div"),
            "thead" to listOf("td", "th", "div"),
            "tfoot" to listOf("td", "th", "div"),
            "tr" to listOf("div"),
            "colgroup" to listOf("div", "script", "style"),
        )
        listOf(false, true).forEach { hydratable ->
            invalidChildren.forEach { (parent, children) ->
                children.forEach { child ->
                    val failure = assertFailsWith<IllegalArgumentException>("$parent > $child") {
                        composeHtmlToString(hydratable) {
                            TagElement<Element>(parent.uppercase(), null) {
                                TagElement<Element>(child.uppercase(), null, null)
                            }
                        }
                    }
                    assertContains(failure.message.orEmpty(), "inside <$parent>")
                    val suggestion = when (child) {
                        "col" -> "Colgroup { }"
                        "tr", "td", "th" -> if (parent == "table") "Tbody { }" else "Tr { }"
                        else -> "outside the table"
                    }
                    assertContains(failure.message.orEmpty(), suggestion)
                }
            }
        }
    }

    @Test
    fun tableContainersAllowOnlyAsciiWhitespaceText() {
        listOf(false, true).forEach { hydratable ->
            listOf("table", "tbody", "thead", "tfoot", "tr", "colgroup").forEach { parent ->
                listOf("x", " x ", "\u00A0", "\u000B").forEach { text ->
                    val failure = assertFailsWith<IllegalArgumentException> {
                        composeHtmlToString(hydratable) {
                            TagElement<Element>(parent, null) { Text(text) }
                        }
                    }
                    assertContains(failure.message.orEmpty(), "Non-whitespace text")
                    assertContains(failure.message.orEmpty(), "inside <$parent>")
                }
                assertEquals("<$parent>\t\n\u000C </$parent>", composeHtmlToString(hydratable) {
                    TagElement<Element>(parent, null) { Text("\t\n\u000C ") }
                })
            }
        }
    }

    @Test
    fun rendersNamedCustomAndPlatformBuildersWithoutCreatingDomElements() {
        val customBuilder = object : ElementBuilder<Element> {
            override val tagName = "MY-WIDGET"
            override fun create(): Element = error("Must not create a DOM element")
        }
        assertEquals("<my-widget>custom</my-widget><div>built-in</div>", composeHtmlToString {
            TagElement(customBuilder, null) { Text("custom") }
            TagElement(ElementBuilder.createBuilder<Element>("DIV"), null) { Text("built-in") }
        })
    }

    @Test
    fun normalizesOnlyAsciiLettersInTagNames() {
        assertEquals("<my-Él></my-Él>", composeHtmlToString {
            TagElement<Element>("MY-ÉL", null, null)
        })
    }

    @Test
    fun rejectsInvalidClassTokensEvenWhenAnExplicitClassOverridesThem() {
        listOf("", "a b", "a\tb", "a\nb", "a\rb", "a\u000Cb").forEach { token ->
            listOf(false, true).forEach { overrideClass ->
                assertFailsWith<IllegalArgumentException>(token) {
                    composeHtmlToString {
                        Div({
                            classes(token)
                            if (overrideClass) attr("class", "valid")
                        })
                    }
                }
            }
        }
        assertEquals("<div class=\"a\u00A0b\"></div>", composeHtmlToString {
            Div({ classes("a\u00A0b") })
        })
    }

    @Test
    fun noscriptEscapesTextAndAllowsFallbackElements() {
        assertEquals(
            "<noscript>&lt;/noscript&gt; &amp; &lt;b&gt;<span>fallback</span></noscript>",
            composeHtmlToString {
                TagElement<Element>("noscript", null) {
                    Text("</noscript> & <b>")
                    Span { Text("fallback") }
                }
            },
        )
    }

    @Test
    fun noscriptRejectsEndTagsInSerializedDescendants() {
        listOf(">", " ", "/", "\t", "\n", "\r", "\u000C").forEach { delimiter ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElement<Element>("noscript", null) {
                        TagElement<Element>("script", null) {
                            Text("</no")
                            Text("SCRIPT$delimiter")
                        }
                    }
                }
            }
            assertContains(failure.message.orEmpty(), "<noscript> content")
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElement<Element>("noscript", null) {
                    TagElement<Element>("noscript", null) { Text("nested") }
                }
            }
        }
        assertEquals("<noscript><script></noscripture></script></noscript>", composeHtmlToString {
            TagElement<Element>("noscript", null) {
                Script(InlineScript("</noscripture>"))
            }
        })
    }

    @Test
    fun rendersTextChildrenInRawTextElements() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes").forEach { tag ->
            val html = composeHtmlToString {
                TagElement<Element>(tag.uppercase(), null) {
                    Text("A & B")
                    Text(" < C")
                }
            }

            assertEquals("<$tag>A & B < C</$tag>", html)
        }
    }

    @Test
    fun rejectsRawTextEndTagsSplitAcrossChildren() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes").forEach { tag ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElement<Element>(tag, null) {
                        Text("</${tag.take(2)}")
                        Text("${tag.drop(2).uppercase()}>")
                    }
                }
            }

            assertContains(failure.message.orEmpty(), "Raw text for <$tag>")
        }
    }

    @Test
    fun genericScriptsUseInlineScriptValidation() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElement<Element>("script", null) {
                    Text("<!-- <scr")
                    Text("ipt>")
                }
            }
        }
        assertContains(failure.message.orEmpty(), "inside <!-- escaped text")

        val srcFailure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElement<Element>("script", { attr("src", "/app.js") }) {
                    Text("console.log('inline')")
                }
            }
        }
        assertContains(srcFailure.message.orEmpty(), "cannot be combined with a src attribute")
    }

    @Test
    fun rejectsElementChildrenInRawTextElements() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes").forEach { tag ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElement<Element>(tag, null) {
                        Div { Text("nested content") }
                    }
                }
            }

            assertContains(failure.message.orEmpty(), "element children inside <$tag>")
        }
    }

    @Test
    fun rendersRawTextElementsWithoutChildren() {
        val tags = listOf("script", "style", "iframe", "xmp", "noembed", "noframes")
        val html = composeHtmlToString {
            tags.forEach { tag ->
                TagElement<Element>(tag, { id(tag) }, null)
            }
        }

        assertEquals(tags.joinToString("") { "<$it id=\"$it\"></$it>" }, html)
    }

    @Test
    fun stillEscapesTextOutsideRawTextElements() {
        val html = composeHtmlToString {
            Title { Text("A & B < C") }
            TagElement<Element>("textarea", null) { Text("A & B < C") }
            Div { Text("<script>alert(1)</script>") }
        }

        assertEquals(
            "<title>A &amp; B &lt; C</title><textarea>A &amp; B &lt; C</textarea>" +
                "<div>&lt;script&gt;alert(1)&lt;/script&gt;</div>",
            html,
        )
    }

    @Test
    fun rendersEveryHtmlVoidElementWithoutAnEndTag() {
        val voidElementNames = listOf(
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr",
        )

        val html = composeHtmlToString {
            voidElementNames.forEach { tagName ->
                TagElement<Element>(
                    tagName = tagName,
                    applyAttrs = null,
                    content = null,
                )
            }
        }

        assertEquals(
            voidElementNames.joinToString(separator = "") { "<$it>" },
            html,
        )
    }

    @Test
    fun alwaysClosesNonVoidElements() {
        val html = composeHtmlToString {
            Div()
            TagElement<Element>(
                tagName = "custom-element",
                applyAttrs = null,
                content = null,
            )
        }

        assertEquals("<div></div><custom-element></custom-element>", html)
    }

    @Test
    fun minimizesBooleanAttributesButQuotesOrdinaryAttributes() {
        val html = composeHtmlToString {
            TagElement<HTMLInputElement>(
                tagName = "input",
                applyAttrs = {
                    disabled()
                    required()
                    readOnly()
                    attr("value", "")
                    attr("contenteditable", "false")
                },
                content = null,
            )
        }

        assertEquals(
            "<input disabled required readonly value=\"\" contenteditable=\"false\">",
            html,
        )
    }

    @Test
    fun preservesExplicitBooleanAttributeValues() {
        val html = composeHtmlToString {
            TagElement<Element>(
                tagName = "button",
                applyAttrs = {
                    attr("disabled", "false")
                    attr("aria-disabled", "false")
                },
                content = null,
            )
        }

        assertEquals(
            "<button disabled=\"false\" aria-disabled=\"false\"></button>",
            html,
        )
    }

    @Test
    fun formatsClassesAsOrderedUniqueTokens() {
        val html = composeHtmlToString {
            Div({
                classes("first", "second", "first")
                classes("third")
            })
            Div({
                classes("ignored")
                attr("class", "manual  value")
            })
            Div({
                classes(emptyList())
            })
            Div({
                classes("ignored")
                attr("CLASS", "manual upper")
            })
        }

        assertEquals(
            "<div class=\"first second third\"></div>" +
                "<div class=\"manual  value\"></div>" +
                "<div></div>" +
                "<div class=\"manual upper\"></div>",
            html,
        )
    }

    @Test
    fun formatsStylesInDeclarationOrder() {
        val html = composeHtmlToString {
            Div({
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                    property("color", "blue")
                    variable("accent", "orange")
                }
            })
            Div({
                style { property("color", "red") }
                attr("style", "display:none")
            })
            Div({
                style { property("color", "red") }
                attr("STYLE", "display:block")
            })
        }

        assertEquals(
            "<div style=\"color: red; display: block !important; color: blue; --accent: orange\"></div>" +
                "<div style=\"display:none\"></div>" +
                "<div style=\"display:block\"></div>",
            html,
        )
    }
}
