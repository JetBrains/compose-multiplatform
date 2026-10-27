/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.samples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Table

@Composable
fun LoopRows(values: List<String>) {
    Div {
        values.forEach { value ->
            Div {
                Span { Text(value) }
                Span { Text(value) }
            }
        }
    }
}

@Composable
fun TopLevelLoop(values: List<String>) {
    for (value in values) {
        Span { Text(value) }
    }
}

@Composable
fun TopLevelForEach(values: List<String>) {
    values.forEachIndexed { index, value ->
        Span { Text("$index:$value") }
    }
}

@Composable
fun TopLevelLoopText(values: List<String>) {
    values.forEach { Text(it) }
}

@Composable
fun NestedLoops(values: List<String>, effect: (String) -> Unit) {
    Div {
        var index = 0
        while (index < values.size) {
            val value = values[index++]
            effect(value)
            if (value.isEmpty()) continue
            if (value == "stop") break
            repeat(2) {
                values.asSequence().forEachIndexed { position, item ->
                    if (position == 0) return@forEachIndexed
                    Span { Text(value + item) }
                }
            }
        }
        do {
            Span { Text("done:$index") }
        } while (false)
    }
}

@Composable
fun LoopCallbacks(values: List<String>, store: (@Composable () -> Unit) -> Unit) {
    Div {
        values.forEach { value ->
            Span { Text(value) }
            store { Span { Text("stored:$value") } }
            CompositionLocalProvider(LocalHtmlValidationMode provides HtmlValidationMode.Strict) {
                values.forEach { nested -> Span { Text(nested) } }
            }
        }
    }
}

@Composable
fun LoopValidationScope(values: List<String>) {
    Table {
        CompositionLocalProvider(LocalHtmlValidationMode provides HtmlValidationMode.Strict) {
            values.forEach { value -> Div { Text(value) } }
        }
    }
}

@Composable
fun ConstantRepeat() {
    val count = 1 + 2
    Div { repeat(count) { Span { Text("<&") } } }
}

@Composable
fun EmptyConstantRepeats(effect: () -> Unit) {
    Div {
        repeat(0) { effect() }
        repeat(-2) { effect() }
    }
}

@Composable
fun IndexedRepeat() {
    Div { repeat(3) { Span { Text("$it") } } }
}

@Composable
fun EffectfulRepeat(effect: () -> Unit) {
    Div { repeat(2) { effect() } }
}

@Composable
fun LargeRepeat() {
    Div { repeat(10_000) { Span { Text("<&") } } }
}

@Composable
fun LoopTextBoundaries(values: List<String>) {
    Div { values.forEach { Text(it) } }
}

private class AuthoredRepeat {
    @Composable
    fun repeat(count: Int, effect: () -> Unit) {
        effect()
        Text("authored:$count")
    }
}

@Composable
fun ShadowedRepeat(effect: () -> Unit) {
    Div { AuthoredRepeat().repeat(3, effect) }
}
