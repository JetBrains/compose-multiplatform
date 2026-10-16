/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Col
import org.jetbrains.compose.web.dom.Colgroup
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Link
import org.jetbrains.compose.web.dom.Meta
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.compose.web.dom.Tr

@Composable
fun MetadataPage() {
    Div {
        Meta(attrs = {
            attr("name", "description")
            attr("content", "Hello & goodbye")
        })
        Link(attrs = {
            attr("rel", "stylesheet")
            attr("href", "/style.css")
        })
    }
}

@Composable
fun LiteralTablePage() {
    Table {
        Colgroup { Col() }
        Tbody {
            Text(" ")
            Tr { Td { Text("cell") } }
        }
    }
}

@Composable
fun LiteralPrePage() {
    Pre {
        Text("")
        Text("\n<&")
    }
}

@Composable
fun LiteralTitlePage() {
    Title {
        Text("<&")
        Text("title")
    }
}

@Composable
fun LiteralIframePage() {
    Iframe {
        Text("<b>&\r")
        Text("\ncontent\u0000")
    }
}

@Composable
fun DynamicSpecialTextPage(value: String) {
    Div {
        Pre { Text(value) }
        Title { Text(value) }
        Iframe { Text(value) }
    }
}

@Composable
fun InvalidTableRowPage() {
    Table { Tr { Td { Text("cell") } } }
}

@Composable
fun InvalidTableTextPage() {
    Table { Text("invalid") }
}

@Composable
fun InvalidColgroupPage() {
    Colgroup { Div() }
}

@Composable
fun InvalidIframeTextPage() {
    Iframe {
        Text("</IFR")
        Text("AME >")
    }
}

@Composable
fun InvalidIframeChildPage() {
    Iframe { Div() }
}
