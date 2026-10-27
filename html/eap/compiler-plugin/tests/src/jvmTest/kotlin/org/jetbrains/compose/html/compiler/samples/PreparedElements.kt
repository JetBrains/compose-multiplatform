/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.samples

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

internal val __htmlAttributes0 = "authored"

@Composable
fun PreparedAttributes(content: ContentBuilder<HTMLDivElement>?) {
    val card = CARD_CLASS
    Div(attrs = {
        val title = "A & " + "B"
        classes(card)
        attr("title", title)
    }, content = content)
}

@Composable
fun PreparedContainer(value: String, effect: (String) -> Unit) {
    Div(attrs = { classes(CARD_CLASS) }) outer@{
        effect("container")
        Text(value)
        Span(attrs = { classes("details") }) {
            Text(scopedValue(this@outer, effect))
        }
        Text("end")
    }
}

@Composable
private fun ElementScope<HTMLSpanElement>.scopedValue(
    outer: ElementScope<HTMLDivElement>,
    effect: (String) -> Unit,
): String {
    // Keep both receiver references in a nested lambda too.
    return run {
        with(outer) {
            with(this@scopedValue) {
                effect("text")
                "details"
            }
        }
    }
}

@Composable
fun PreparedDynamicText(value: () -> String) {
    Span(attrs = { classes("price") }) { Text(value()) }
}

class PreparedUser(private val nextName: () -> String) {
    val name: String get() = nextName()
}

@Composable
fun PreparedGetter(user: PreparedUser) {
    Span(attrs = { classes("user") }) { Text(user.name) }
}

@Composable
fun PreparedLiteralText() {
    Span(attrs = { classes("label") }) { Text("<&\r😀") }
}

@Composable
fun PreparedSharedAttributes(value: String) {
    Span(attrs = { classes("label") }) { Text(value) }
}

@Composable
fun OversizedLiteral() {
    val ten = "<<<<<<<<<<"
    val hundred = "$ten$ten$ten$ten$ten$ten$ten$ten$ten$ten"
    val thousand = "$hundred$hundred$hundred$hundred$hundred$hundred$hundred$hundred$hundred$hundred"
    Span { Text(thousand + thousand) }
}

@Composable
fun ContentReturn(skip: Boolean) {
    Span(attrs = { classes("return") }) {
        Text(if (skip) return@Span else "shown")
    }
}

@Composable
fun DynamicAttributes(effect: () -> String) {
    Span(attrs = { id(effect()) }) { Text("text") }
}

@Composable
fun AttributeEffects(effect: () -> Unit) {
    Span(attrs = { effect() }) { Text("text") }
}
