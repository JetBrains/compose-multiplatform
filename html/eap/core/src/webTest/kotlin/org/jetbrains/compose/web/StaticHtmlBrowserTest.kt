/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.*

class StaticHtmlBrowserTest {
    @Test
    fun subtreesAndDynamicTextHydrateAndUpdateWithoutReplacingNodes() = MainScope().promise {
        val attrs = preparedHtmlAttributes<HTMLElement>("span", "<span class=\"value\">") { classes("value") }
        for (strict in listOf(false, true)) {
            var value by mutableStateOf("first")
            val content: @Composable () -> Unit = {
                // Browser compilations keep the original composables for static SSR subtrees.
                Div { Span { Text("<&é😀") } }
                PreparedTextElement(attrs) { value }
                Span { Text("tail") }
            }
            val root = browserDocument.createElement("div") as HTMLElement
            val fresh = renderComposable(root) { content() }
            assertEquals("<&é😀", root.firstElementChild!!.textContent)
            assertEquals("first", root.children.item(1)!!.textContent)
            fresh.dispose()
            root.innerHTML = composeHtmlToString(validateStrictly = strict, content = content)
            val subtree = root.firstChild
            val textElement = root.childNodes.item(1)
            val textNode = textElement!!.firstChild
            val tail = root.lastChild
            val hydrated =
                hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) { content() }
            try {
                assertSame(subtree, root.firstChild)
                assertSame(textElement, root.childNodes.item(1))
                assertSame(textNode, textElement.firstChild)
                value = "updated<&"
                delay(100)
                assertEquals("updated<&", textElement.textContent)
                assertSame(textNode, textElement.firstChild)
                assertSame(subtree, root.firstChild)
                assertSame(tail, root.lastChild)
            } finally {
                hydrated.dispose()
            }
        }
    }

    @Test
    fun staticAttributesAndTextRenderAndHydrateInTheBrowser() {
        val attributes = preparedHtmlAttributes<HTMLElement>("div", "<div id=\"static\" class=\"a&amp; b\">") {
            id("static"); classes("a&", "b")
        }
        val text = "<&é😀"
        val content: @Composable () -> Unit = { PreparedTextElement(attributes) { text } }
        val root = browserDocument.createElement("div") as HTMLElement
        val fresh = renderComposable(root) { content() }
        try {
            assertEquals("a& b", root.firstElementChild!!.getAttribute("class"))
            assertEquals("<&é😀", root.firstElementChild!!.textContent)
        } finally {
            fresh.dispose()
        }
        for (strict in listOf(false, true)) {
            root.innerHTML = composeHtmlToString(validateStrictly = strict, content = content)
            val node = root.firstElementChild
            val hydrated =
                hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) { content() }
            try {
                assertSame(node, root.firstElementChild)
                assertEquals("a& b", node!!.getAttribute("class"))
                assertEquals("<&é😀", node.textContent)
            } finally {
                hydrated.dispose()
            }
        }
    }
}
