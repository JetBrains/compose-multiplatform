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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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
    fun unsupportedRawTextTagHasAnExplicitDiagnostic() {
        val failure = assertFailsWith<IllegalArgumentException> {
            RawTextContent.create("div", "text")
        }
        assertEquals("Raw text content is not supported for <div>", failure.message)
    }

    @Test
    fun htmlStyleAndScriptInsideSvgSuggestSvgElements() {
        listOf("style", "script").forEach { tag ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("svg", TestSvgNamespace, null) {
                        if (tag == "style") Style {} else Script(InlineScript("text"))
                    }
                }
            }
            assertContains(failure.message.orEmpty(), "the HTML parser creates an SVG <$tag>")
            assertContains(failure.message.orEmpty(), "SvgElement<SVGElement>(\"$tag\")")
        }
    }

    @Test
    fun explicitHtmlClassAndStyleOverrideDslValuesRegardlessOfCase() {
        assertEquals("<div class=\"literal\" style=\"color: green\"></div>", composeHtmlToString {
            Div({
                classes("dsl")
                style { property("color", "red") }
                attr("CLASS", "literal")
                attr("STYLE", "color: green")
            })
        })
    }

    @Test
    fun rejectsForeignNamesWhoseCasingChangesInHtml() {
        listOf(
            "myCustomTag" to "mycustomtag",
            "foreignobject" to "foreignObject",
            "lineargradient" to "linearGradient",
        ).forEach { (name, parserName) ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("svg", TestSvgNamespace, null) {
                        TagElementNS<Element>(name, TestSvgNamespace, null, null)
                    }
                }
            }
            assertContains(failure.message.orEmpty(), "Use <$parserName> instead")
        }
        listOf(
            "dataPoints" to "datapoints",
            "viewbox" to "viewBox",
            "xlink:Href" to "xlink:href",
            "XML:LANG" to "xml:lang",
        ).forEach { (name, parserName) ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("svg", TestSvgNamespace, { attr(name, "value") }, null)
                }
            }
            assertContains(failure.message.orEmpty(), "Use \"$parserName\" instead")
        }
    }

    @Test
    fun acceptsAdjustedSvgNamesAndLowercaseCustomNames() {
        assertEquals(
            "<svg viewBox=\"0 0 10 10\"><linearGradient gradientUnits=\"userSpaceOnUse\">" +
                "</linearGradient><sparkline datapoints=\"0,1\" xlink:href=\"#line\"></sparkline></svg>",
            composeHtmlToString {
                TagElementNS<Element>("svg", TestSvgNamespace, { attr("viewBox", "0 0 10 10") }) {
                    TagElementNS<Element>("linearGradient", TestSvgNamespace,
                        { attr("gradientUnits", "userSpaceOnUse") }, null)
                    TagElementNS<Element>("sparkline", TestSvgNamespace, {
                        attr("datapoints", "0,1")
                        attr("xlink:href", "#line")
                    }, null)
                }
            },
        )
    }

    @Test
    fun validatesMathMlNameAdjustments() {
        val namespace = "http://www.w3.org/1998/Math/MathML"
        assertEquals("<math definitionURL=\"url\"></math>", composeHtmlToString {
            TagElementNS<Element>("math", namespace, { attr("definitionURL", "url") }, null)
        })
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>("math", namespace, { attr("definitionurl", "url") }, null)
            }
        }
        assertContains(failure.message.orEmpty(), "Use \"definitionURL\" instead")
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>("math", namespace, null) {
                    TagElementNS<Element>("Mi", namespace, null, null)
                }
            }
        }
    }

    @Test
    fun htmlNamespaceUsesTheSameCachedBuilder() {
        val builder = ElementBuilder.createBuilder<Element>("DIV")
        assertSame(builder, ElementBuilder.createBuilder<Element>("div", HtmlNamespace))
        val svgBuilder = ElementBuilder.createBuilder<Element>("div", TestSvgNamespace)
        assertNotSame(builder, svgBuilder)
        assertSame(svgBuilder, ElementBuilder.createBuilder<Element>("div", TestSvgNamespace))
        assertNotSame(svgBuilder, ElementBuilder.createBuilder<Element>("DIV", TestSvgNamespace))
    }

    @Test
    fun rejectsReservedSvgAttributesRegardlessOfCasing() {
        listOf("DATA-COMPOSE-HYDRATION-ROOT", "Data-Compose-Hydration-State").forEach { name ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("svg", TestSvgNamespace, { attr(name, "") }, null)
                }
            }
            assertContains(failure.message.orEmpty(), "owned by the Compose hydration protocol")
        }
    }

    @Test
    fun rawTextSerializationIsDrivenByTheHtmlParent() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes", "noscript").forEach { tag ->
            assertEquals("<$tag>A & B < C\nD</$tag>", composeHtmlToString {
                TagElement<Element>(tag.uppercase(), null) {
                    Text("A & B < C\r")
                    Text("\nD")
                }
            })
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElement<Element>(tag, null) {
                        Text("</${tag.take(2)}")
                        Text("${tag.drop(2).uppercase()}>")
                    }
                }
            }
            assertContains(failure.message.orEmpty(), "Raw text for <$tag>")
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString { TagElement<Element>(tag, null) { Div {} } }
            }
        }
    }

    @Test
    fun genericScriptUsesInlineScriptValidation() {
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElement<Element>("script", { attr("src", "/app.js") }) { Text("inline") }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElement<Element>("script", null) {
                    Text("<!-- <scr")
                    Text("ipt>")
                }
            }
        }
    }

    @Test
    fun permitsHarmlessRawTextTagPrefixesAndClosedScriptComments() {
        listOf("</scripture>", "<!-- <scripture>", "<!-- closed --> <script>", "<!--> <script>").forEach { text ->
            assertEquals("<script>$text</script>", composeHtmlToString {
                Script(InlineScript(text))
            })
        }
    }

    @Test
    fun svgScriptAndStyleStillEscapeText() {
        assertEquals(
            "<svg><script>A &amp; B &lt; C</script><style>A &amp; B &lt; C</style></svg>",
            composeHtmlToString {
                TagElementNS<Element>("svg", TestSvgNamespace, null) {
                    listOf("script", "style").forEach { tag ->
                        TagElementNS<Element>(tag, TestSvgNamespace, null) { Text("A & B < C") }
                    }
                }
            },
        )
    }

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
            TagElementNS<Element>("svg", TestSvgNamespace, null) {
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
        }

        assertEquals(
            "<svg><animate attributeName=\"viewBox\" required=\"false\"></animate></svg>",
            html,
        )
    }

    @Test
    fun usesHtmlSafeEscapingForSvgContent() {
        val html = composeHtmlToString {
            TagElementNS<Element>("svg", TestSvgNamespace, null) {
                TagElementNS<Element>(
                    tagName = "text",
                    namespace = TestSvgNamespace,
                    applyAttrs = { attr("data", "\"<&>") },
                ) {
                    Text("<&>")
                }
            }
        }

        assertEquals(
            "<svg><text data=\"&quot;&lt;&amp;&gt;\">&lt;&amp;&gt;</text></svg>",
            html,
        )
    }

    @Test
    fun rejectsSvgFragmentsWithoutAnSvgRoot() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>("animate", TestSvgNamespace, null, null)
            }
        }

        assertContains(failure.message.orEmpty(), "<animate>")
        assertContains(failure.message.orEmpty(), "string-rendering root")
        assertContains(failure.message.orEmpty(), "SVG <svg> root")
    }

    @Test
    fun rejectsSvgAttributesThatCollapseDuringHtmlParsing() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>(
                    tagName = "svg",
                    namespace = TestSvgNamespace,
                    applyAttrs = {
                        attr("dataValue", "first")
                        attr("datavalue", "second")
                    },
                    content = null,
                )
            }
        }

        assertContains(failure.message.orEmpty(), "Duplicate HTML attribute names")
        assertContains(failure.message.orEmpty(), "dataValue")
        assertContains(failure.message.orEmpty(), "datavalue")
    }

    @Test
    fun rejectsHtmlElementsDirectlyInsideSvg() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>("svg", TestSvgNamespace, null) {
                    Div { Text("HTML") }
                }
            }
        }

        assertContains(failure.message.orEmpty(), "HTML element <div>")
        assertContains(failure.message.orEmpty(), "SVG <svg>")
        assertContains(failure.message.orEmpty(), "<foreignObject>")
    }

    @Test
    fun permitsHtmlElementsInsideSvgIntegrationPoints() {
        val html = composeHtmlToString {
            TagElementNS<Element>("svg", TestSvgNamespace, null) {
                TagElementNS<Element>("foreignObject", TestSvgNamespace, null) {
                    Div { Text("foreignObject") }
                }
                TagElementNS<Element>("title", TestSvgNamespace, null) {
                    Div { Text("title") }
                }
                TagElementNS<Element>("desc", TestSvgNamespace, null) {
                    Div { Text("desc") }
                }
            }
        }

        assertEquals(
            "<svg>" +
                "<foreignObject><div>foreignObject</div></foreignObject>" +
                "<title><div>title</div></title>" +
                "<desc><div>desc</div></desc>" +
                "</svg>",
            html,
        )
    }

    @Test
    fun rejectsSvgFragmentsDirectlyInsideSvgIntegrationPoints() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                TagElementNS<Element>("svg", TestSvgNamespace, null) {
                    TagElementNS<Element>("foreignObject", TestSvgNamespace, null) {
                        TagElementNS<Element>("rect", TestSvgNamespace, null, null)
                    }
                }
            }
        }

        assertContains(failure.message.orEmpty(), "<rect>")
        assertContains(failure.message.orEmpty(), "inside <foreignObject>")
        assertContains(failure.message.orEmpty(), "SVG <svg> root")
    }

    @Test
    fun rejectsSvgElementNamesThatBreakOutOfForeignContent() {
        listOf(
            "div" to emptyMap(),
            "font" to mapOf("color" to "red"),
        ).forEach { (tagName, attributes) ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("svg", TestSvgNamespace, null) {
                        TagElementNS<Element>(
                            tagName = tagName,
                            namespace = TestSvgNamespace,
                            applyAttrs = {
                                attributes.forEach { (name, value) -> attr(name, value) }
                            },
                            content = null,
                        )
                    }
                }
            }

            assertContains(failure.message.orEmpty(), "<$tagName>")
            assertContains(failure.message.orEmpty(), "moves it out of SVG foreign content")
        }
    }

    @Test
    fun permitsNestedSvgRootsInsideSvgIntegrationPoints() {
        val html = composeHtmlToString {
            TagElementNS<Element>("svg", TestSvgNamespace, null) {
                TagElementNS<Element>("foreignObject", TestSvgNamespace, null) {
                    TagElementNS<Element>("svg", TestSvgNamespace, null) {
                        TagElementNS<Element>("rect", TestSvgNamespace, null, null)
                    }
                }
            }
        }

        assertEquals(
            "<svg><foreignObject><svg><rect></rect></svg></foreignObject></svg>",
            html,
        )
    }

    @Test
    fun mathMlIntegrationPointsPreserveNamespaceBoundaries() {
        val mathMlNamespace = "http://www.w3.org/1998/Math/MathML"
        listOf("mi", "mo", "mn", "ms", "mtext", "annotation-xml").forEach { parent ->
            assertEquals("<math><$parent><svg></svg></$parent></math>", composeHtmlToString {
                TagElementNS<Element>("math", mathMlNamespace, null) {
                    TagElementNS<Element>(parent, mathMlNamespace, null) {
                        TagElementNS<Element>("svg", TestSvgNamespace, null, null)
                    }
                }
            })
        }
        listOf("mglyph", "malignmark").forEach { child ->
            assertEquals("<math><mtext><$child></$child></mtext></math>", composeHtmlToString {
                TagElementNS<Element>("math", mathMlNamespace, null) {
                    TagElementNS<Element>("mtext", mathMlNamespace, null) {
                        TagElementNS<Element>(child, mathMlNamespace, null, null)
                    }
                }
            })
        }
        listOf("math", "mrow").forEach { parent ->
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElementNS<Element>("math", mathMlNamespace, null) {
                        TagElementNS<Element>(parent, mathMlNamespace, null) {
                            TagElementNS<Element>("svg", TestSvgNamespace, null, null)
                        }
                    }
                }
            }
        }
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
