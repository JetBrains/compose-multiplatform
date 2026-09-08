package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLInputElement
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.attributes.required
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals

internal const val TestSvgNamespace = "http://www.w3.org/2000/svg"

@Composable
@OptIn(ComposeWebInternalApi::class)
internal fun SvgSerializationFixture() {
    TagElementNS<Element>(
        tagName = "svg",
        namespace = TestSvgNamespace,
        applyAttrs = { attr("viewBox", "0 0 10 10") },
    ) {
        TagElementNS<Element>("defs", TestSvgNamespace, null) {
            TagElementNS<Element>(
                tagName = "linearGradient",
                namespace = TestSvgNamespace,
                applyAttrs = { attr("id", "gradient") },
                content = null,
            )
        }
        TagElementNS<Element>(
            tagName = "image",
            namespace = TestSvgNamespace,
            applyAttrs = { attr("width", "10") },
            content = null,
        )
    }
}

@OptIn(ComposeWebInternalApi::class)
class HtmlSerializationTest {
    @Test
    fun serializesSvgUsingNamespaceAwareRules() {
        val html = composeHtmlToString { SvgSerializationFixture() }

        assertEquals(
            "<svg viewBox=\"0 0 10 10\">" +
                "<defs><linearGradient id=\"gradient\"></linearGradient></defs>" +
                "<image width=\"10\"></image>" +
                "</svg>",
            html,
        )
    }

    @Test
    fun preservesSvgAttributeCasingAndDoesNotMinimizeBooleanNamedAttributes() {
        val html = composeHtmlToString {
            TagElementNS<Element>(
                tagName = "animate",
                namespace = TestSvgNamespace,
                applyAttrs = {
                    attr("attributeName", "viewBox")
                    attr("required", "false")
                },
                content = null,
            )
        }

        assertEquals(
            "<animate attributeName=\"viewBox\" required=\"false\"></animate>",
            html,
        )
    }

    @Test
    fun usesHtmlSafeEscapingForSvgContent() {
        val html = composeHtmlToString {
            TagElementNS<Element>(
                tagName = "text",
                namespace = TestSvgNamespace,
                applyAttrs = { attr("data", "\"<&>") },
            ) {
                Text("<&>")
            }
        }

        assertEquals(
            "<text data=\"&quot;&lt;&amp;&gt;\">&lt;&amp;&gt;</text>",
            html,
        )
    }

    @Test
    fun continuesLowercasingElementsInTheHtmlNamespace() {
        val html = composeHtmlToString {
            TagElementNS<Element>(
                tagName = "CUSTOM-ELEMENT",
                namespace = HtmlNamespace,
                applyAttrs = { attr("DATA-VALUE", "value") },
                content = null,
            )
        }

        assertEquals(
            "<custom-element data-value=\"value\"></custom-element>",
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
    fun booleanAttributeValueDoesNotChangeItsPresenceSemantics() {
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
            "<button disabled aria-disabled=\"false\"></button>",
            html,
        )
    }

    @Test
    fun formatsClassesAsOrderedUniqueTokens() {
        val html = composeHtmlToString {
            Div({
                classes("first", "second", "first", "")
                classes("third")
            })
            Div({
                classes("ignored")
                attr("class", "manual  value")
            })
        }

        assertEquals(
            "<div class=\"first second third\"></div>" +
                "<div class=\"manual  value\"></div>",
            html,
        )
    }

    @Test
    fun formatsStylesAndLetsLaterDeclarationsReplaceEarlierOnes() {
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
        }

        assertEquals(
            "<div style=\"color: blue; display: block !important; --accent: orange\"></div>" +
                "<div style=\"display:none\"></div>",
            html,
        )
    }
}
