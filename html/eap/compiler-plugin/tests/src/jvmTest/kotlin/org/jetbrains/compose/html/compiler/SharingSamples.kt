/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.LowerableHtmlTag
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text

internal val sharedInitialization = composeHtmlToString {
    SharedLiteral()
    EquivalentLiteral()
}

@Composable
fun SharedLiteral() {
    Span(attrs = { classes("label") }) { Text("<&\r😀") }
}

@Composable
fun EquivalentLiteral() {
    Span(attrs = { classes("la" + "bel") }) { Text("<" + "&\r😀") }
}

@Composable
fun SharedDynamic(value: () -> String) {
    Span(attrs = { classes("label") }) { Text(value()) }
}

@Composable
fun SharedContainer(value: String) {
    Span(attrs = { classes("label") }) {
        Text(value)
        Text("tail")
    }
}

@Composable
fun SharedAttributesOtherTag(value: String) {
    Div(attrs = { classes("label") }) { Text(value) }
}

@Composable
fun SameTagOtherType(value: String) {
    GenericDiv(attrs = { classes("label") }) { Text(value) }
}

@Composable
@LowerableHtmlTag("div")
private fun GenericDiv(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) {
    TagElement("div", attrs, content)
}

@Composable
fun DistinctLiteral() {
    Span(attrs = { classes("label") }) { Text("&lt;&amp;\r😀") }
}

@Composable
fun OrderedAttributes(value: String) {
    Span(attrs = {
        attr("data-a", "a")
        attr("data-b", "b")
    }) { Text(value) }
}

@Composable
fun ReorderedAttributes(value: String) {
    Span(attrs = {
        attr("data-b", "b")
        attr("data-a", "a")
    }) { Text(value) }
}

@Composable
fun FirstClassMetadata(value: String) {
    Span(attrs = {
        attr("class", "explicit")
        classes("first")
    }) { Text(value) }
}

@Composable
fun SecondClassMetadata(value: String) {
    Span(attrs = {
        attr("class", "explicit")
        classes("second")
    }) { Text(value) }
}

@Composable
fun SharedFragment() {
    Div {
        Span { Text("<&\r😀") }
        Text("tail")
        Text("end")
    }
}

@Composable
fun EquivalentFragment() {
    Div {
        Span { Text("<" + "&\r😀") }
        Text("tail")
        Text("end")
    }
}

@Composable
fun FragmentWithoutBoundary() {
    Div {
        Span { Text("<&\r😀") }
        Text("tailend")
    }
}
