/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import kotlinx.browser.document
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringRenderingTests {
    @Test
    fun noscriptFallbackParsesWithScriptingDisabledAndStaysContainedWhenEnabled() {
        val html = composeHtmlToString {
            TagElement<Element>("noscript", null) {
                Text("Use <b> & </noscript>")
                Span { Text("fallback") }
            }
            Span { Text("after") }
        }
        // DOMParser creates a document with scripting disabled.
        val parsed = DOMParser().parseFromString("<!doctype html><body>$html</body>", "text/html")
        val fallback = parsed.querySelector("noscript")!!
        assertEquals("Use <b> & </noscript>", fallback.firstChild!!.textContent)
        assertEquals("fallback", fallback.querySelector("span")!!.textContent)
        assertEquals("after", parsed.body!!.lastElementChild!!.textContent)

        // The active browser instead parses noscript's fallback markup as one raw text node.
        val container = document.createElement("div") as HTMLElement
        container.innerHTML = html
        val activeNoscript = container.firstElementChild!!
        assertEquals(0, activeNoscript.children.length)
        assertEquals("Use &lt;b&gt; &amp; &lt;/noscript&gt;<span>fallback</span>", activeNoscript.textContent)
        assertEquals(2, container.children.length)
        assertEquals("after", container.lastElementChild!!.textContent)
    }

    @Test
    fun genericRawTextSurvivesHtmlParsing() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes").forEach { tag ->
            val container = document.createElement("div") as HTMLElement
            container.innerHTML = composeHtmlToString {
                TagElement<Element>(tag, null) {
                    Text("A & B < C\r")
                    Text("\nD")
                }
                Span { Text("after") }
            }

            assertEquals("A & B < C\nD", container.firstElementChild!!.textContent, tag)
            assertEquals("after", container.lastElementChild!!.textContent)
            assertEquals(2, container.children.length)
        }
    }

    @Test
    fun repeatedCssAssignmentsMatchBrowserStyles() {
        val assignments = listOf(
            listOf("margin-top" to "10px", "margin" to "0px", "margin-top" to "20px"),
            listOf("margin" to "0px", "margin-top" to "10px", "margin" to "20px"),
        )
        assignments.forEach { properties ->
            val browserElement = document.createElement("div") as HTMLElement
            properties.forEach { (name, value) -> browserElement.style.setProperty(name, value) }
            val parsed = parse(composeHtmlToString {
                Div({ style { properties.forEach { (name, value) -> property(name, value) } } })
            })
            assertEquals(browserElement.style.marginTop, parsed.style.marginTop)
            assertEquals(browserElement.style.marginLeft, parsed.style.marginLeft)
        }
    }

    @Test
    fun cssFallbacksPriorityChangesAndRemovalsMatchBrowserStyles() {
        val assignments = listOf(
            listOf("100px" to false, "abcdef" to false),
            listOf("100px" to true, "abcdef" to true),
            listOf("100px" to true, "200px" to false),
            listOf("100px" to false, "" to false),
        )
        // Exercise three successive CSSOM mutations, including priority changes and removals.
        val validAssignments = listOf("100px", "200px", "").flatMap { value ->
            listOf(value to false, value to true)
        }
        val validSequences = validAssignments.flatMap { first ->
            validAssignments.flatMap { second ->
                validAssignments.map { third -> listOf(first, second, third) }
            }
        }
        val fallbackSequences = listOf("100px", "abcdef", "").flatMap { first ->
            listOf("200px", "abcdef", "").flatMap { second ->
                listOf("300px", "abcdef", "").map { third ->
                    listOf(first to false, second to false, third to false)
                }
            }
        }
        (assignments + validSequences + fallbackSequences).forEach { properties ->
            val browserElement = document.createElement("div") as HTMLElement
            properties.forEach { (value, important) ->
                browserElement.style.setProperty("width", value, if (important) "important" else "")
            }
            val parsed = parse(composeHtmlToString {
                Div({ style {
                    properties.forEach { (value, important) -> property("width", value, important) }
                } })
            })
            assertEquals(browserElement.style.cssText, parsed.style.cssText, properties.toString())
        }
    }

    @Test
    fun explicitBooleanAttributeValuesSurviveHtmlParsing() {
        listOf("div", "input", "my-widget").forEach { tag ->
            listOf("checked", "disabled", "open", "hidden").forEach { attribute ->
                listOf("false", "", "until-found").forEach { value ->
                    val parsed = parse(composeHtmlToString {
                        TagElement<Element>(tag, { attr(attribute, value) }, null)
                    })
                    assertEquals(value, parsed.getAttribute(attribute), "$tag $attribute=$value")
                }
            }
        }
    }

    @Test
    fun invalidClassTokensAreRejectedByBothRenderers() {
        listOf("", "a b", "a\tb", "a\nb", "a\rb", "a\u000Cb").forEach { token ->
            val browserElement = document.createElement("div")
            assertFailsWith<Throwable> { browserElement.classList.add(token) }
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString { Div({ classes(token) }) }
            }
        }
    }

    @Test
    fun textSurvivesHtmlParsing() {
        listOf("div", "pre", "textarea", "listing").forEach { tag ->
            listOf("", "hello", "\nhello", "\n\nhello", "\rhello", "\r\nhello").forEach { text ->
                val parsed = parse(composeHtmlToString {
                    TagElement<Element>(tag, null) {
                        Text("")
                        Text(text)
                    }
                })
                assertEquals(text, parsed.textContent, "<$tag>: $text")
            }
        }
    }

    @Test
    fun customElementAttributeValuesSurviveHtmlParsing() {
        val parsed = parse(composeHtmlToString {
            TagElement<Element>("my-widget", {
                attr("open", "details")
                attr("checked", "false")
            }, null)
        })
        assertEquals("details", parsed.getAttribute("open"))
        assertEquals("false", parsed.getAttribute("checked"))
    }

    @Test
    fun safeScriptContentSurvivesHtmlParsing() {
        listOf(
            "const value = '</scripture>';",
            "<!-- comment\n-->\nconst value = '<script>';",
            "<!-->const value = '<script>';",
            "<!-- const value = '<scripture>';",
            "<!-- first --> <!-- second --> const value = '<script>';",
            "const value = '</script';",
        ).forEach { content ->
            val container = document.createElement("div") as HTMLElement
            container.innerHTML = composeHtmlToString {
                Script(InlineScript(content)) { type(ScriptType.TextPlain) }
                Span { Text("after") }
            }
            assertEquals(content, container.firstElementChild!!.textContent)
            assertEquals("after", container.lastElementChild!!.textContent)
            assertEquals(2, container.children.length)
        }
    }

    private fun parse(html: String): HTMLElement {
        val container = document.createElement("div") as HTMLElement
        container.innerHTML = html
        return container.firstElementChild as HTMLElement
    }
}
