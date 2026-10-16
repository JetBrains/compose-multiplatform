/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Tr

@Composable
fun HeadingsPage() {
    Div {
        H1 { Text("one") }
        H2 { Text("two") }
        H3 { Text("three") }
        H4 { Text("four") }
        H5 { Text("five") }
        H6 { Text("six") }
        P(attrs = { id("paragraph") }) { Text("paragraph") }
    }
}

@Composable
fun RowPage() {
    Tr(attrs = { classes("row") }) {
        Th { Text("heading") }
        Td(attrs = { attr("title", "cell") }) { Text("value") }
    }
}

@Composable
fun HeaderCellPage() {
    Th { Text("heading") }
}

@Composable
fun CellPage() {
    Td(attrs = { attr("title", "cell") }) { Text("value") }
}

@Composable
fun InvalidRowTextPage() {
    Tr { Text("invalid") }
}

@Composable
fun InvalidRowChildPage() {
    Tr { Div { Text("invalid") } }
}
