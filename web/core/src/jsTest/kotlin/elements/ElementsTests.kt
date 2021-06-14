/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Address
import org.jetbrains.compose.web.dom.Area
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Aside
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Caption
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Col
import org.jetbrains.compose.web.dom.Colgroup
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Em
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.HTMLMap
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Ol
import org.jetbrains.compose.web.dom.OptGroup
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.Tfoot
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.dom.Track
import org.jetbrains.compose.web.dom.Ul
import org.jetbrains.compose.web.dom.Video
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

            //Pair({ Tag.Li() }, "LI"),
            Pair({ Img(src="whatever") }, "IMG"),

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
    fun rawCreation() = runTest {
        @Composable
        fun CustomElement(
            attrs: AttrsBuilder<HTMLElement>.() -> Unit,
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
}