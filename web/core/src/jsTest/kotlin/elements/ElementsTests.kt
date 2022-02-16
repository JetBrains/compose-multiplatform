/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementsTests {
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
            Pair({ Source() }, "SOURCE"),

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
        val custom = ElementBuilder.createBuilder<HTMLElement>("custom")
        val div = ElementBuilder.createBuilder<HTMLElement>("div")
        val b = ElementBuilder.createBuilder<HTMLElement>("b")
        val abc = ElementBuilder.createBuilder<HTMLElement>("abc")

        val expectedKeys = setOf("custom", "div", "b", "abc")
        assertEquals(expectedKeys, ElementBuilder.buildersCache.keys.intersect(expectedKeys))

        assertEquals("CUSTOM", custom.create().nodeName)
        assertEquals("DIV", div.create().nodeName)
        assertEquals("B", b.create().nodeName)
        assertEquals("ABC", abc.create().nodeName)
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
            TagElement({
                counter++
                document.createElement("div")
            }, null,
                if (flag) {
                    { Div() { Text("ON") } }
                } else null
            )

        }

        assertEquals(1, counter, )

        flag = true
        waitForRecompositionComplete()

        assertEquals(1, counter)
        assertEquals("<div><div>ON</div></div>", nextChild().outerHTML)
    }
}
