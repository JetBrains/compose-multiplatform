/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

private const val HundredAscii = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
private const val ThousandAscii = HundredAscii + HundredAscii + HundredAscii + HundredAscii + HundredAscii +
    HundredAscii + HundredAscii + HundredAscii + HundredAscii + HundredAscii
private const val BoundaryPadding = ThousandAscii + ThousandAscii + ThousandAscii + ThousandAscii +
    ThousandAscii + ThousandAscii + ThousandAscii + HundredAscii + HundredAscii + HundredAscii +
    HundredAscii + HundredAscii + HundredAscii + HundredAscii + HundredAscii + HundredAscii +
    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
private const val HundredWide = "界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界"
private const val ThousandWide = HundredWide + HundredWide + HundredWide + HundredWide + HundredWide +
    HundredWide + HundredWide + HundredWide + HundredWide + HundredWide
private const val WideText = ThousandWide + ThousandWide + ThousandWide

@Composable
fun LargeUnicodeFragment() {
    Div {
        Text(BoundaryPadding + "😀<&\r")
        Text("")
        Text("tail")
        Span { Text("child") }
    }
}

@Composable
fun LargeStaticPage() {
    Html { Body { Div { repeat(30) { Span { Text(WideText) } } } } }
}

@Composable
fun LargeRawFragment() {
    Iframe {
        Text(BoundaryPadding + "\r")
        Text("\n😀\u0000<&")
    }
}

@Composable
fun InvalidLargeRawFragment() {
    Iframe {
        Text(BoundaryPadding + "</IFR")
        Text("AME >")
    }
}

@Composable
fun LargeLeadingNewlineFragment() {
    Pre { Text("\n" + BoundaryPadding + "😀") }
}

@Composable
fun LargeTableFragment() {
    Table { Tbody { Tr { Td { Text(BoundaryPadding + "😀") } } } }
}

@Composable
fun LargeTableRowFragment() {
    Tr { Td { Text(BoundaryPadding + "😀") } }
}

@Composable
fun LargeStaticHydration(count: Int, increment: () -> Unit) {
    LargeUnicodeFragment()
    Button(attrs = { id("large-increment"); onClick { increment() } }) { Text("Increment") }
    Span(attrs = { id("large-count") }) { Text("Count: $count") }
}
