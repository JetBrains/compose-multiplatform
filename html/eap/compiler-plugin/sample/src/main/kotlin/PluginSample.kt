/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span as HtmlSpan
import org.jetbrains.compose.web.dom.Text

val htmlAtInitialization = composeHtmlToString { HtmlSpan { Text("initialized") } }

@Composable
fun StaticPage() {
    Div {
        HtmlSpan { Text("Hello <world> & \"friends\"\r\né😀") }
    }
}

@Composable
fun DynamicPage(name: String) {
    Div { HtmlSpan { Text(name) } }
}

@Composable
fun AdjacentTextPage() {
    HtmlSpan {
        Text("first")
        Text("second")
    }
}

@Composable
fun AttributedPage() {
    HtmlSpan(attrs = { classes("label") }) { Text("text") }
}

@Composable
fun InvalidTextPage() {
    HtmlSpan { Text("bad\u0000") }
}

@Composable
fun ShadowedPage() {
    Span { Text("text") }
}

// This is not the HTML Span, despite sharing its short name.
@Composable
private fun Span(content: @Composable () -> Unit) {
    Text("local:")
    content()
}
