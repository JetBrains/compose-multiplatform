/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun ScopedHydrationContent(
    count: Int,
    increment: () -> Unit,
    inspect: @Composable ElementScope<HTMLSpanElement>.(ElementScope<HTMLDivElement>) -> Unit = {},
) {
    val content: ContentBuilder<HTMLDivElement> = {
        Div(attrs = { classes("card") }) card@{
            Text("User $count")
            ScopedDetails(count)
            Span(attrs = {
                id("scoped-price")
                classes("price")
            }) {
                Text(scopedPrice(count, this@card, inspect))
            }
            Span(attrs = { id("scoped-literal") }) { Text("<& literal") }
            Span(attrs = { classes("shared-literal") }) { Text("<& literal") }
            Span(attrs = { classes("shared-literal") }) { Text("<& literal") }
            Div(attrs = { classes("shared-fragment") }) { Span { Text("<& fragment") } }
            Div(attrs = { classes("shared-fragment") }) { Span { Text("<& fragment") } }
        }
    }
    Div(attrs = { id("scoped-root") }, content = content)
    Button(attrs = {
        id("scoped-increment")
        onClick { increment() }
    }) { Text("Increment") }
}

@Composable
private fun ScopedDetails(count: Int) {
    Div(attrs = { classes("direct-details"); attr("data-count", count.toString()) }) {
        Span { Text("prefix") }
        Span(attrs = { classes("details") }) { Text("Details $count") }
        repeat(2) { index ->
            Span(attrs = { classes("loop-details") }) { Text("Loop $index: $count") }
        }
        Span { Text("suffix") }
    }
}

@Composable
private fun ElementScope<HTMLSpanElement>.scopedPrice(
    count: Int,
    outer: ElementScope<HTMLDivElement>,
    inspect: @Composable ElementScope<HTMLSpanElement>.(ElementScope<HTMLDivElement>) -> Unit,
): String {
    inspect(outer)
    return "Price: $count"
}
