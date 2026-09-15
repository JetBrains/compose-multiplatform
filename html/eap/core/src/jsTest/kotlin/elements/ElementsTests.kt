/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ElementsTests {
    @Test
    fun explicitHtmlClassAndStyleKeepPrecedenceDuringRecomposition() = runTest {
        var cssClass by mutableStateOf("first")
        var explicit by mutableStateOf(true)
        composition {
            Div({
                classes(cssClass)
                style { color(if (cssClass == "first") Color.red else Color.blue) }
                if (explicit) {
                    attr("CLASS", "literal")
                    attr("STYLE", "color: green")
                }
            })
        }
        val element = nextChild<HTMLElement>()
        assertEquals("literal", element.getAttribute("class"))
        assertEquals("color: green", element.getAttribute("style"))

        cssClass = "second"
        waitForRecompositionComplete()
        assertEquals("literal", element.getAttribute("class"))
        assertEquals("color: green", element.getAttribute("style"))

        explicit = false
        waitForRecompositionComplete()
        assertEquals("second", element.getAttribute("class"))
        assertEquals("blue", element.style.color)
    }

    @Test
    fun namespacedBuildersCloneIndependentElements() {
        val builder = ElementBuilder.createBuilder<Element>("linearGradient", TestSvgNamespace)
        assertSame(builder, ElementBuilder.createBuilder<Element>("linearGradient", TestSvgNamespace))
        val first = builder.create()
        first.setAttribute("id", "first")
        val second = builder.create()
        assertNotSame(first, second)
        assertEquals("linearGradient", second.localName)
        assertEquals(TestSvgNamespace, second.namespaceURI)
        assertEquals(null, second.getAttribute("id"))
    }

    @Test
    fun createsForeignAttributesInTheirParserNamespaces() = runTest {
        composition {
            TagElementNS<Element>("svg", TestSvgNamespace, {
                attr("xlink:href", "#target")
                attr("xml:lang", "en")
                attr("xmlns", TestSvgNamespace)
                attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            }, null)
        }
        val svg = nextChild<Element>()
        assertEquals("#target", svg.getAttributeNS("http://www.w3.org/1999/xlink", "href"))
        assertEquals("en", svg.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang"))
        assertEquals(TestSvgNamespace, svg.getAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns"))
        assertEquals("http://www.w3.org/1999/xlink", svg.getAttributeNS("http://www.w3.org/2000/xmlns/", "xlink"))
    }

    @Test
    fun createsNamespacedElementsWithCaseSensitiveLocalNames() = runTest {
        composition {
            TagElementNS<Element>(
                tagName = "linearGradient",
                namespace = TestSvgNamespace,
                applyAttrs = null,
                content = null,
            )
        }

        val element = root.firstElementChild
        assertEquals("linearGradient", element?.localName)
        assertEquals(TestSvgNamespace, element?.namespaceURI)
    }

    @Test
    fun parsedSvgSerializationKeepsEveryElementInTheSvgNamespace() {
        val container = document.createElement("div")
        container.innerHTML = org.jetbrains.compose.web.composeHtmlToString {
            SvgSerializationFixture()
        }

        val elements = container.querySelectorAll("*")
        assertEquals(4, elements.length)
        repeat(elements.length) { index ->
            val element = elements.item(index) as Element
            assertEquals(TestSvgNamespace, element.namespaceURI, element.localName)
        }
        assertEquals(
            listOf("svg", "defs", "linearGradient", "image"),
            List(elements.length) { (elements.item(it) as Element).localName },
        )
    }

    @Test
    fun parsedSvgIntegrationPointsSwitchTheirHtmlChildrenBackToHtml() {
        val container = document.createElement("div")
        container.innerHTML = org.jetbrains.compose.web.composeHtmlToString {
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

        val svg = container.firstElementChild as Element
        assertEquals(TestSvgNamespace, svg.namespaceURI)
        listOf("foreignObject", "title", "desc").forEach { name ->
            val integrationPoint = svg.querySelector(name) as Element
            val div = integrationPoint.firstElementChild as Element
            assertEquals(TestSvgNamespace, integrationPoint.namespaceURI, name)
            assertEquals("http://www.w3.org/1999/xhtml", div.namespaceURI, name)
            assertEquals(name, div.textContent)
        }
    }

    @Test
    fun parsedNestedSvgRootSwitchesBackToSvgInsideForeignObject() {
        val container = document.createElement("div")
        container.innerHTML = org.jetbrains.compose.web.composeHtmlToString {
            TagElementNS<Element>("svg", TestSvgNamespace, null) {
                TagElementNS<Element>("foreignObject", TestSvgNamespace, null) {
                    TagElementNS<Element>("svg", TestSvgNamespace, null) {
                        TagElementNS<Element>("rect", TestSvgNamespace, null, null)
                    }
                }
            }
        }

        val outerSvg = container.firstElementChild as Element
        val foreignObject = outerSvg.firstElementChild as Element
        val nestedSvg = foreignObject.firstElementChild as Element
        val rect = nestedSvg.firstElementChild as Element
        assertEquals(TestSvgNamespace, nestedSvg.namespaceURI)
        assertEquals(TestSvgNamespace, rect.namespaceURI)
    }

    @Test
    fun cachesNamespacedBuildersByNamespaceAndTagName() {
        val first = ElementBuilder.createBuilder<Element>("linearGradient", TestSvgNamespace)
        val same = ElementBuilder.createBuilder<Element>("linearGradient", TestSvgNamespace)
        val differentTag = ElementBuilder.createBuilder<Element>("lineargradient", TestSvgNamespace)
        val differentNamespace = ElementBuilder.createBuilder<Element>(
            "linearGradient",
            "urn:example:other",
        )

        assertSame(first, same)
        assertNotSame(first, differentTag)
        assertNotSame(first, differentNamespace)
    }

    @Test
    fun nodeNames() = runTest {
        val nodes = listOf<Pair<@Composable () -> Unit, String>>(
            Pair({ Address() }, "ADDRESS"),
            Pair({ Article() }, "ARTICLE"),
            Pair({ Aside() }, "ASIDE"),
            Pair({ Header() }, "HEADER"),

            Pair({ Area() }, "AREA"),
            Pair({ Audio() }, "AUDIO"),
            Pair({ HTMLMap() }, "MAP"),
            Pair({ Track() }, "TRACK"),
            Pair({ Video() }, "VIDEO"),

            Pair({ Datalist() }, "DATALIST"),
            Pair({ Fieldset() }, "FIELDSET"),
            Pair({ Legend() }, "LEGEND"),
            Pair({ Meter() }, "METER"),
            Pair({ Output() }, "OUTPUT"),
            Pair({ Progress() }, "PROGRESS"),

            Pair({ Embed() }, "EMBED"),
            Pair({ Iframe() }, "IFRAME"),
            Pair({ Object() }, "OBJECT"),
            Pair({ Param() }, "PARAM"),
            Pair({ Picture() }, "PICTURE"),
            Pair({ Script { src("data:text/javascript,") } }, "SCRIPT"),
            Pair({ Source() }, "SOURCE"),
            Pair({ Canvas() }, "CANVAS"),

            Pair({ Div() }, "DIV"),
            Pair({ A() }, "A"),
            Pair({ Button() }, "BUTTON"),
            Pair({ H1() }, "H1"),
            Pair({ H2() }, "H2"),
            Pair({ H3() }, "H3"),
            Pair({ H4() }, "H4"),
            Pair({ H5() }, "H5"),
            Pair({ H6() }, "H6"),

            Pair({ P() }, "P"),
            Pair({ Em() }, "EM"),
            Pair({ I() }, "I"),
            Pair({ B() }, "B"),
            Pair({ Small() }, "SMALL"),
            Pair({ Sup() }, "SUP"),
            Pair({ Sub() }, "SUB"),
            Pair({ Blockquote()}, "BLOCKQUOTE"),

            Pair({ Span() }, "SPAN"),
            Pair({ Br() }, "BR"),
            Pair({ Ul() }, "UL"),
            Pair({ Ol() }, "OL"),

            Pair({ Li() }, "LI"),
            Pair({ Img(src = "whatever") }, "IMG"),

            Pair({ Form() }, "FORM"),
            Pair({ Select() }, "SELECT"),
            Pair({ Option("whatever") }, "OPTION"),
            Pair({ OptGroup("whatever") }, "OPTGROUP"),

            Pair({ Section() }, "SECTION"),
            Pair({ TextArea(value = "whatever") }, "TEXTAREA"),
            Pair({ Nav() }, "NAV"),
            Pair({ Pre() }, "PRE"),
            Pair({ Code() }, "CODE"),
            Pair({ Main() }, "MAIN"),
            Pair({ Footer() }, "FOOTER"),

            Pair({ Hr() }, "HR"),
            Pair({ Label() }, "LABEL"),
            Pair({ Table() }, "TABLE"),
            Pair({ Caption() }, "CAPTION"),
            Pair({ Col() }, "COL"),
            Pair({ Colgroup() }, "COLGROUP"),
            Pair({ Tr() }, "TR"),
            Pair({ Thead() }, "THEAD"),
            Pair({ Th() }, "TH"),
            Pair({ Td() }, "TD"),
            Pair({ Tbody() }, "TBODY"),
            Pair({ Tfoot() }, "TFOOT"),
        )

        composition {
            nodes.forEach {
                it.first.invoke()
            }
        }

        nodes.forEachIndexed { index, it ->
            assertEquals(it.second, root.children[index]?.nodeName)
        }
    }

    @Test
    fun inlineScriptUsesRawTextAndUpdates() = runTest {
        var content by mutableStateOf("const value = '<first>&';")

        composition {
            Script(InlineScript(content)) {
                type(ScriptType.TextPlain)
            }
        }

        val script = root.firstChild as HTMLScriptElement
        assertEquals(content, script.textContent)

        content = "const value = '<second>&';"
        waitForRecompositionComplete()

        assertSame(script, root.firstChild)
        assertEquals(content, script.textContent)
    }

    @Test
    fun inlineScriptExecutesOnceAndUpdatesDoNotReexecuteIt() = runTest {
        val counterName = "__compose_web_raw_text_script_counter__"
        window.asDynamic()[counterName] = 0
        var content by mutableStateOf(
            "window['$counterName'] = window['$counterName'] + 1;"
        )

        try {
            composition {
                Script(InlineScript(content))
            }
            val countAfterInsertion: Int = window.asDynamic()[counterName]
            assertEquals(1, countAfterInsertion)

            content = "window['$counterName'] = window['$counterName'] + 10;"
            waitForRecompositionComplete()

            val countAfterUpdate: Int = window.asDynamic()[counterName]
            assertEquals(1, countAfterUpdate)
        } finally {
            window.asDynamic()[counterName] = null
        }
    }

    @Test
    @OptIn(ExperimentalComposeWebApi::class)
    fun rawCreation() = runTest {
        @Composable
        fun CustomElement(
          attrs: AttrsScope<HTMLElement>.() -> Unit,
          content: ContentBuilder<HTMLElement>? = null
        ) {
            TagElement(
                tagName = "custom",
                applyAttrs = attrs,
                content
            )
        }

        composition {
            CustomElement({
                id("container")
            }) {
                Text("CUSTOM")
            }
        }

        assertEquals("<div><custom id=\"container\">CUSTOM</custom></div>", root.outerHTML)
    }

    @Test
    fun testElementBuilderCreate() {
        val custom = ElementBuilder.createBuilder<HTMLElement>("CUSTOM")
        val div = ElementBuilder.createBuilder<HTMLElement>("DIV")
        val sameDiv = ElementBuilder.createBuilder<HTMLElement>("div")
        val b = ElementBuilder.createBuilder<HTMLElement>("b")
        val abc = ElementBuilder.createBuilder<HTMLElement>("abc")

        val expectedKeys = setOf("custom", "div", "b", "abc")
        assertEquals(expectedKeys, ElementBuilder.buildersCache.keys.intersect(expectedKeys))

        assertEquals("CUSTOM", custom.create().nodeName)
        assertEquals("DIV", div.create().nodeName)
        assertEquals("B", b.create().nodeName)
        assertEquals("ABC", abc.create().nodeName)
        assertSame(custom, ElementBuilder.createBuilder<HTMLElement>("custom"))
        assertSame(div, sameDiv)
        assertNotSame(custom, div)
    }

    @Test
    @OptIn(ExperimentalComposeWebApi::class)
    fun rawCreationAndTagChanges() = runTest {
        @Composable
        fun CustomElement(
            tagName: String,
            attrs: AttrsScope<HTMLElement>.() -> Unit,
            content: ContentBuilder<HTMLElement>? = null
        ) {
            TagElement(
                tagName = tagName,
                applyAttrs = attrs,
                content
            )
        }

        var tagName by mutableStateOf("custom")

        composition {
            CustomElement(tagName, {
                id("container")
            }) {
                Text("CUSTOM")
            }
        }

        assertEquals("<div><custom id=\"container\">CUSTOM</custom></div>", root.outerHTML)

        tagName = "anothercustom"
        waitForRecompositionComplete()

        assertEquals("<div><anothercustom id=\"container\">CUSTOM</anothercustom></div>", root.outerHTML)
    }

    @Test
    fun elementBuilderShouldBeCalledOnce() = runTest {
        var counter = 0
        var flag by mutableStateOf(false)

        composition {
            TagElement(
                {
                    counter++
                    document.createElement("div")
                },
                null,
                if (flag) {
                    { Div { Text("ON") } }
                } else null
            )
        }

        assertEquals(1, counter)

        flag = true
        waitForRecompositionComplete()

        assertEquals(1, counter)
        assertEquals("<div><div>ON</div></div>", nextChild().outerHTML)
    }

    @Test
    fun divAndSpanPassNamedBuildersToTheContext() = runTest {
        val requestedTags = mutableListOf<String>()
        val overridingContext = object : ComposeHtmlContext by DefaultComposeHtmlContext {
            @Composable
            override fun <TElement : Element> TagElement(
                elementBuilder: ElementBuilder<TElement>,
                applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
                content: (@Composable ElementScope<TElement>.() -> Unit)?,
            ) {
                val tagName = elementBuilder.tagName
                requestedTags += tagName
                val replacementTag = when (tagName) {
                    "div" -> "section"
                    "span" -> "em"
                    else -> tagName
                }
                DefaultComposeHtmlContext.TagElement(
                    ElementBuilder.createBuilder(replacementTag), applyAttrs, content
                )
            }
        }

        composition {
            CompositionLocalProvider(LocalComposeHtmlContext provides overridingContext) {
                Div {
                    Span {
                        Text("content")
                    }
                }
            }
        }

        assertEquals(listOf("div", "span"), requestedTags)
        assertEquals("<section><em>content</em></section>", nextChild().outerHTML)
    }

    @Test @NoLiveLiterals
    fun keyChangesTheOrderButKeepsSameInstances() = runTest {
        val items = mutableStateListOf(1, 2, 3)

        composition {
            items.forEach {
                key(it) {
                    Div { Text("I = $it") }
                }
            }
        }

        val refs = listOf(
            root.children[0],
            root.children[1],
            root.children[2],
        )

        root.children[0]!!.asDynamic().fakeid = "0"
        root.children[1]!!.asDynamic().fakeid = "1"
        root.children[2]!!.asDynamic().fakeid = "2"

        items[0] = 3
        items[1] = 2
        items[2] = 1

        waitForRecompositionComplete()

        assertSame(refs[0], root.children[2])
        assertSame(refs[1], root.children[1])
        assertSame(refs[2], root.children[0])

        assertEquals("0", root.children[2].asDynamic().fakeid)
        assertEquals("1", root.children[1].asDynamic().fakeid)
        assertEquals("2", root.children[0].asDynamic().fakeid)
    }
}
