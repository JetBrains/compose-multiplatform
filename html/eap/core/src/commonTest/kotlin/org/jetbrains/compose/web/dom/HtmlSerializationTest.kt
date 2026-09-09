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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HtmlSerializationTest {
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
