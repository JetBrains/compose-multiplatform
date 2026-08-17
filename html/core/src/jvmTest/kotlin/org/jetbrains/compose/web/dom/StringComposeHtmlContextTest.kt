package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringComposeHtmlContextTest {
    @Test
    fun rendersCommonElementsAttributesAndText() {
        val html = composeHtmlToString {
            Div({
                id("root")
                classes("first", "second")
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                }
            }) {
                Span {
                    Text("Tom & Jerry <3")
                }
            }
        }

        assertEquals(
            "<div id=\"root\" class=\"first second\" " +
                "style=\"color: red; display: block !important\">" +
                "<span>Tom &amp; Jerry &lt;3</span></div>",
            html
        )
    }

    @Test
    fun rendersDynamicTagNames() {
        val html = composeHtmlToString {
            TagElement<Element>(
                tagName = "CUSTOM-ELEMENT",
                applyAttrs = null,
                content = { Text("content") },
            )
        }

        assertEquals("<custom-element>content</custom-element>", html)
    }

    @Test
    fun rejectsDomOnlyCustomBuilders() {
        val exception = assertFailsWith<IllegalStateException> {
            composeHtmlToString {
                TagElement(
                    elementBuilder = ElementBuilder<Element> {
                        error("The DOM builder must not be invoked")
                    },
                    applyAttrs = null,
                    content = null,
                )
            }
        }

        assertEquals(
            "String rendering requires a tag-name builder. " +
                "Use TagElement(tagName, ...) for custom elements.",
            exception.message
        )
    }
}
