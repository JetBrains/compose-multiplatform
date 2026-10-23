/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLInputElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.attributes.required
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

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
    fun strictModeRejectsTableChildrenThatRequireParserRepair() {
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
                        composeHtmlToString(hydratable, validateStrictly = true) {
                            TagElement<Element>(parent, null) {
                                TagElement<Element>(child, null, null)
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
                        composeHtmlToString(hydratable, validateStrictly = true) {
                            TagElement<Element>(parent, null) { Text(text) }
                        }
                    }
                    assertContains(failure.message.orEmpty(), "Non-whitespace text")
                    assertContains(failure.message.orEmpty(), "inside <$parent>")
                    assertEquals("<$parent>$text</$parent>", composeHtmlToString(hydratable, validateStrictly = false) {
                        TagElement<Element>(parent, null) { Text(text) }
                    })
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
            override val tagName = "my-widget"
            override fun create(): Element = error("Must not create a DOM element")
        }
        assertEquals("<my-widget>custom</my-widget><div>built-in</div>", composeHtmlToString {
            TagElement(customBuilder, null) { Text("custom") }
            TagElement(ElementBuilder.createBuilder<Element>("div"), null) { Text("built-in") }
        })
    }

    @Test
    fun preservesTagNameCase() {
        assertEquals("<MY-ÉL></MY-ÉL>", composeHtmlToString {
            TagElement<Element>("MY-ÉL", null, null)
        })
    }

    @Test
    fun rejectsInvalidClassTokensEvenWhenAnExplicitClassOverridesThem() {
        listOf("", "a b", "a\tb", "a\nb", "a\rb", "a\u000Cb").forEach { token ->
            listOf(false, true).forEach { overrideClass ->
                assertFailsWith<IllegalArgumentException>(token) {
                    composeHtmlToString(validateStrictly = true) {
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
    fun strictModeRejectsRepeatedClassTokens() {
        listOf<AttrsScope<HTMLDivElement>.() -> Unit>(
            { classes("first", "second", "first") },
            { classes("first"); classes("second", "first") },
        ).forEach { applyAttrs ->
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(validateStrictly = true) {
                    Div(applyAttrs)
                }
            }
            assertEquals("Class token must not be repeated: \"first\"", failure.message)
        }
    }

    @Test
    fun fastModeSkipsOptionalClassAndForeignAttributeChecks() {
        assertEquals(
            "<div class=\"two words\"></div>",
            composeHtmlToString(validateStrictly = false) {
                Div({ classes("two words") })
            },
        )
        assertEquals(
            "<svg dataValue=\"first\" datavalue=\"second\"></svg>",
            composeHtmlToString(validateStrictly = false) {
                TagElementNS<Element>("svg", TestSvgNamespace, {
                    attr("dataValue", "first")
                    attr("datavalue", "second")
                }, null)
            },
        )
    }

    @Test
    fun fastModeSkipsNameAndTableElementChecksWithoutAffectingStrictRenders() {
        val cases: List<Pair<String, @Composable () -> Unit>> = listOf(
            "<div invalid name=\"value\"></div>" to { Div({ attr("invalid name", "value") }) },
            "<invalid tag></invalid tag>" to { TagElement<Element>("invalid tag", null, null) },
            "<table><tr><td>cell</td></tr></table>" to { Table { Tr { Td { Text("cell") } } } },
        )
        for (key in listOf(null, "validation-mode-switch")) {
            for ((expected, content) in cases) {
                assertEquals(expected, composeHtmlToString(key = key, validateStrictly = false, content = content))
                assertFailsWith<IllegalArgumentException> {
                    composeHtmlToString(key = key, validateStrictly = true, content = content)
                }
                assertEquals(expected, composeHtmlToString(key = key, validateStrictly = false, content = content))
            }
        }
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
                TagElement<Element>(tag, null) {
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
    fun preservesClassTokenOrderInFastMode() {
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
        }

        assertEquals(
            "<div class=\"first second first third\"></div>" +
                "<div class=\"manual  value\"></div>" +
                "<div></div>",
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
        }

        assertEquals(
            "<div style=\"color: red; display: block !important; color: blue; --accent: orange\"></div>" +
                "<div style=\"display:none\"></div>",
            html,
        )
    }

    @Test
    fun unsupportedRawTextTagHasAnExplicitDiagnostic() {
        val failure = assertFailsWith<IllegalArgumentException> {
            RawTextContent.create("div", "text")
        }
        assertEquals("Raw text content is not supported for <div>", failure.message)
    }

    @Test
    fun explicitHtmlClassAndStyleOverrideDslValues() {
        assertEquals("<div class=\"literal\" style=\"color: green\"></div>", composeHtmlToString {
            Div({
                classes("dsl")
                style { property("color", "red") }
                attr("class", "literal")
                attr("style", "color: green")
            })
        })
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
    fun htmlNamespaceUsesTheSameCachedBuilder() {
        val builder = ElementBuilder.createBuilder<Element>("div")
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
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes").forEach { tag ->
            assertEquals("<$tag>A & B < C\nD</$tag>", composeHtmlToString {
                TagElement<Element>(tag, null) {
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
    fun rejectsSvgAttributesThatCollapseDuringHtmlParsing() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) {
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
    fun preservesNamesInTheHtmlNamespace() {
        val html = composeHtmlToString {
            TagElementNS<Element>(
                tagName = "CUSTOM-ELEMENT",
                namespace = HtmlNamespace,
                applyAttrs = { attr("DATA-VALUE", "value") },
                content = null,
            )
        }

        assertEquals(
            "<CUSTOM-ELEMENT DATA-VALUE=\"value\"></CUSTOM-ELEMENT>",
            html,
        )
    }

    @Test
    fun customElementBooleanNamedAttributesAreNeverMinimized() {
        val html = composeHtmlToString {
            TagElement<Element>(
                tagName = "my-widget",
                applyAttrs = {
                    attr("open", "")
                    attr("checked", "false")
                },
                content = null,
            )
        }

        assertEquals(
            "<my-widget open=\"\" checked=\"false\"></my-widget>",
            html,
        )
    }
    @Test
    fun attributeNameChecksRejectAllControlsAndProtocolSpoofing() {
        for (character in (0..31).map(Int::toChar) + (127..159).map(Int::toChar) + " \"'/>=".toList()) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(validateStrictly = true) { Div(attrs = { attr("prefix${character}suffix", "value") }) }
            }
        }
        for (name in listOf("data-compose-hydration-root", "DATA-COMPOSE-HYDRATION-STATE",
                            "Data-Compose-Hydration-For", "data-compose-hydration-validation")) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString { Div(attrs = { attr(name, "value") }) }
            }
        }
        assertEquals("<div data-compose-hydration-other=\"allowed\"></div>", composeHtmlToString {
            Div(attrs = { attr("data-compose-hydration-other", "allowed") })
        })
    }

    @Test
    fun pooledAttributeNamesNeverCacheValues() {
        repeat(6) { i ->
            assertEquals("<div data-long-name-for-validation=\"$i&amp;&quot;\"></div>", composeHtmlToString(key = "attribute-name-reuse") {
                Div({ attr("data-long-name-for-validation", "$i&\"") })
            })
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = "attribute-name-reuse", validateStrictly = true) {
                Div({ attr("invalid name", "value") })
            }
        }
        assertEquals("<div title=\"next\"></div>", composeHtmlToString(key = "attribute-name-reuse") { Div({ title("next") }) })
    }

    @Test
    fun reservedAttributeOwnershipIsCheckedAfterNameReuse() {
        val reserved = "data-compose-hydration-state"
        repeat(2) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(key = "reserved-attribute-reuse") { Div({ attr(reserved, "forged") }) }
            }
        }
    }

    @Test
    fun foreignDuplicateChecksSurviveRepeatedNames() {
        repeat(2) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString(key = "foreign-attribute-reuse", validateStrictly = true) {
                    TagElementNS<kotlinx.browser.dom.Element>("g", "http://www.w3.org/2000/svg",
                        {
                            attr("viewBox", "first")
                            attr("viewbox", "second")
                        }, null)
                }
            }
        }
    }

    @Test
    fun escapingPreservesEntitiesAndUnicodeAcrossFastPathLengths() {
        for (length in listOf(0, 1, 30, 31, 32, 33, 63, 64, 65, 1024)) {
            val prefix = "é😀a".repeat(length)
            val suffix = "<&>\r\"\n\t&amp;"
            val html = composeHtmlToString {
                Div(attrs = { attr("title", prefix + suffix) }) { Text(prefix + suffix) }
            }
            assertEquals(
                "<div title=\"${prefix}&lt;&amp;&gt;&#13;&quot;&#10;&#9;&amp;amp;\">" +
                    "${prefix}&lt;&amp;&gt;&#13;\"\n\t&amp;amp;</div>",
                html,
            )
        }
    }

    @Test
    fun longUnescapedStringsStillRejectNul() {
        for (position in listOf(0, 31, 32, 1000)) {
            val value = "x".repeat(position) + '\u0000' + "y".repeat(1000)
            assertFailsWith<IllegalArgumentException> { composeHtmlToString { Text(value) } }
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString { Div(attrs = { attr("title", value) }) }
            }
        }
    }

    @Test
    fun classTokensEscapeOnceAndExplicitClassStillOverrides() {
        for (strict in listOf(false, true)) {
            assertEquals("<div class=\"a&amp; b&quot;\"></div>", composeHtmlToString(validateStrictly = strict) {
                Div(attrs = { classes("a&", "b\"") })
            })
            assertEquals("<div class=\"explicit\"></div>", composeHtmlToString(validateStrictly = strict) {
                Div(attrs = {
                    classes("a&", "b\"")
                    attr("class", "explicit")
                })
            })
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) {
                Div(attrs = {
                    classes("invalid token")
                    attr("class", "explicit")
                })
            }
        }
    }

    @Test
    fun rendererBuilderCacheSeparatesNamespacesAndStopsAtItsBound() {
        val pool = AttrsBuilderPool()
        val html = pool.elementBuilder<Element>("script", HtmlNamespace)
        val svg = pool.elementBuilder<Element>("script", TestSvgNamespace)
        assertSame(html, pool.elementBuilder<Element>("script", HtmlNamespace))
        assertSame(svg, pool.elementBuilder<Element>("script", TestSvgNamespace))
        assertNotSame(html, svg)
        assertEquals(RawTextTagFlag or ValidTagFlag, (html as HtmlTagMetadata).serializationFlags)
        assertEquals(ValidTagFlag, (svg as HtmlTagMetadata).serializationFlags)
        repeat(126) { pool.elementBuilder<Element>("tag-$it", TestSvgNamespace) }
        assertNotSame(
            pool.elementBuilder<Element>("uncached", TestSvgNamespace),
            pool.elementBuilder<Element>("uncached", TestSvgNamespace),
        )
        assertSame(svg, pool.elementBuilder<Element>("script", TestSvgNamespace))
    }

}
