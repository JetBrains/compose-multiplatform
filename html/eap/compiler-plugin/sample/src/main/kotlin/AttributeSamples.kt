/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LiteralAttributesPage() {
    Span(attrs = {
        classes("label", "accent")
        id("greeting")
        attr("title", "\"<&>\r\n\t😀")
        classes("last")
        attr("data-key", "value")
        attr("aria-label", "Hello")
        attr("role", "note")
    }) {
        Text("text")
    }
}

@Composable
fun RuntimeAttributesPage(title: String) {
    Span(attrs = {
        classes("label", "accent")
        id("greeting")
        attr("title", title)
        classes("last")
        attr("data-key", "value")
        attr("aria-label", "Hello")
        attr("role", "note")
    }) {
        Text("text")
    }
}

@Composable
fun DynamicAttributesPage(nextValue: () -> String) {
    Span(attrs = { id(nextValue()) }) { Text("text") }
}

@Composable
fun DuplicateAttributesPage() {
    Span(attrs = {
        id("first")
        id("last")
        classes("label", "label")
    }) {
        Text("text")
    }
}

@Composable
fun SpreadClassesPage() {
    Span(attrs = { classes(*arrayOf("first", "last")) }) { Text("text") }
}

@Composable
fun ExplicitClassPage() {
    Span(attrs = {
        classes("ignored")
        attr("class", "explicit")
    }) {
        Text("text")
    }
}

@Composable
fun CapturedAttributeReceiverPage(target: AttrsScope<HTMLSpanElement>) {
    Span(attrs = { target.id("changed") }) { Text("text") }
}

@Composable
fun InvalidClassPage() {
    Span(attrs = { classes("two words") }) { Text("text") }
}

@Composable
fun InvalidAttributeValuePage() {
    Span(attrs = { attr("title", "bad\u0000") }) { Text("text") }
}

@Composable
fun ReservedAttributePage() {
    Span(attrs = { attr("data-compose-hydration-root", "bad") }) { Text("text") }
}
