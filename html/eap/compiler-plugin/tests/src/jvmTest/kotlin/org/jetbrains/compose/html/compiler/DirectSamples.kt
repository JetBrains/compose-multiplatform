/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.compose.web.dom.Tr

@Composable
fun DirectGreeting(value: () -> String) {
    Div(attrs = {
        classes("card")
        id("greeting")
        attr("title", "Hello & goodbye")
    }) { Text(value()) }
}

@Composable
fun DirectContainer(values: List<String>, effect: (String) -> Unit) {
    Div(attrs = { classes("container") }) {
        Span { Text("head") }
        effect("open")
        val prefix = "value: "
        for (value in values) {
            if (value.isNotEmpty()) {
                Span { Text(prefix + value) }
            } else {
                Text("empty")
            }
        }
        DirectFooter(effect)
        Span { Text("middle") }
        Span { Text("sibling") }
        effect("close")
        Span { Text("tail") }
    }
}

@Composable
private fun DirectFooter(effect: (String) -> Unit) {
    effect("footer")
    Text("footer")
}

@Composable
fun ComposableText(value: @Composable () -> String) {
    Span(attrs = { classes("user") }) { Text(value()) }
}

@Composable
fun RememberedText(value: String) {
    Span { Text(remember(value) { value }) }
}

@Composable
fun SpecialText(value: String) {
    Pre { Text(value) }
    Title { Text(value) }
    Table { Tr { Text(value) } }
}

@Composable
fun ContainerReturn(skip: Boolean, effect: () -> Unit) {
    Div {
        effect()
        if (skip) return@Div
        Text("shown")
    }
}
