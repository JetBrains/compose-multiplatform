/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(
    org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class,
    kotlin.js.ExperimentalWasmJsInterop::class,
)

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.dom.HTMLElement
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CompilerHydrationTest {
    @Test
    fun streamedLargeFragmentsHydrateAndKeepTheirTextNodesDuringUpdates() = MainScope().promise {
        assertEquals(jvmLargeHydrationHtml, composeHtmlToString { LargeStaticHydration(0, {}) })
        for (strict in listOf(false, true)) {
            var count by mutableStateOf(0)
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            root.innerHTML = jvmLargeHydrationHtml
            val fragment = root.firstChild!!
            // Hydration consumes boundary comments and inserts the empty Text call's node.
            val nodes = (0 until fragment.childNodes.length).map { fragment.childNodes.item(it)!! }
                .filter { it.nodeType.toInt() != 8 }
            val composition = hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) {
                LargeStaticHydration(count) { count++ }
            }
            try {
                assertSame(fragment, root.firstChild)
                nodes.forEach { node -> assertSame(fragment, node.parentNode) }
                assertEquals("x".repeat(7_994) + "😀<&\rtailchild", fragment.textContent)
                (root.querySelector("#large-increment") as HTMLElement).click()
                delay(100)
                assertEquals("Count: 1", root.querySelector("#large-count")!!.textContent)
                nodes.forEach { node -> assertSame(fragment, node.parentNode) }
            } finally {
                composition.dispose()
                root.parentNode!!.removeChild(root)
            }
        }
    }

    @Test
    fun preparedElementsHydrateAndComposeWithTheirOriginalScopes() = MainScope().promise {
        assertEquals(jvmPreparedHydrationHtml, composeHtmlToString { PreparedHydrationContent(0, {}) })
        for (hydrate in listOf(false, true)) {
            for (strict in listOf(false, true)) {
                var count by mutableStateOf(0)
                val scopes = mutableListOf<String>()
                val root = browserDocument.createElement("div") as HTMLElement
                browserDocument.body!!.appendChild(root)
                if (hydrate) root.innerHTML = jvmPreparedHydrationHtml
                val originalCard = root.querySelector(".card")
                val originalPrice = root.querySelector("#prepared-price")?.firstChild
                val originalShared = root.querySelectorAll(".shared-literal, .shared-fragment")
                    .let { nodes -> (0 until nodes.length).map { nodes.item(it) } }
                val content: @Composable () -> Unit = {
                    PreparedHydrationContent(count, { count++ }) { outer ->
                        DisposableEffect(count) {
                            val price = scopeElement
                            val card = with(outer) { scopeElement }
                            assertEquals("prepared-price", price.id)
                            assertEquals("card", card.className)
                            assertSame(card, price.parentNode)
                            scopes.add("$count:${price.tagName}:${card.tagName}")
                            onDispose {}
                        }
                    }
                }
                val composition = if (hydrate) {
                    hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) { content() }
                } else {
                    renderComposable(root = root) { content() }
                }
                try {
                    val card = root.querySelector(".card")!!
                    val price = root.querySelector("#prepared-price")!!
                    val priceText = price.firstChild
                    val details = root.querySelector(".direct-details")!!
                    assertEquals("0", details.getAttribute("data-count"))
                    val detailsText = details.querySelector(".details")!!.firstChild
                    val loopText = root.querySelectorAll(".loop-details").let { nodes ->
                        (0 until nodes.length).map { nodes.item(it)!!.firstChild }
                    }
                    val prefix = details.firstChild
                    val suffix = details.lastChild
                    val literal = root.querySelector("#prepared-literal")!!.firstChild
                    val shared = root.querySelectorAll(".shared-literal, .shared-fragment")
                        .let { nodes -> (0 until nodes.length).map { nodes.item(it)!! } }
                    assertEquals(listOf("<& literal", "<& literal", "<& fragment", "<& fragment"), shared.map { it.textContent })
                    if (hydrate) {
                        assertSame(originalCard, card)
                        assertSame(originalPrice, priceText)
                        originalShared.forEachIndexed { index, node -> assertSame(node, shared[index]) }
                    }
                    assertEquals(listOf("0:SPAN:DIV"), scopes)
                    assertEquals("Price: 0", price.textContent)
                    assertEquals("<& literal", literal!!.textContent)
                    (root.querySelector("#prepared-increment") as HTMLElement).click()
                    delay(100)
                    assertEquals("Price: 1", price.textContent)
                    assertEquals("Details 1", root.querySelector(".details")!!.textContent)
                    val updatedLoop = root.querySelectorAll(".loop-details")
                    loopText.forEachIndexed { index, text ->
                        assertSame(text, updatedLoop.item(index)!!.firstChild)
                        assertEquals("Loop $index: 1", text!!.textContent)
                    }
                    assertSame(details, root.querySelector(".direct-details"))
                    assertEquals("1", details.getAttribute("data-count"))
                    assertSame(detailsText, details.querySelector(".details")!!.firstChild)
                    assertSame(prefix, details.firstChild)
                    assertSame(suffix, details.lastChild)
                    assertEquals(listOf("0:SPAN:DIV", "1:SPAN:DIV"), scopes)
                    assertSame(card, root.querySelector(".card"))
                    assertSame(priceText, price.firstChild)
                    assertSame(literal, root.querySelector("#prepared-literal")!!.firstChild)
                    val updatedShared = root.querySelectorAll(".shared-literal, .shared-fragment")
                    shared.forEachIndexed { index, node -> assertSame(node, updatedShared.item(index)) }
                } finally {
                    composition.dispose()
                    root.parentNode!!.removeChild(root)
                }
            }
        }
    }

    @Test
    fun compilerGeneratedHtmlHydratesAndUpdates() = MainScope().promise {
        assertEquals(jvmHydrationHtml, composeHtmlToString { CompilerHydrationContent(0) {} })
        for (strict in listOf(false, true)) {
            var count by mutableStateOf(0)
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            root.innerHTML = jvmHydrationHtml
            val nodes = (0 until root.childNodes.length).map { root.childNodes.item(it) }
            val title = root.querySelector("#compiler-title")!!
            val titleText = title.firstChild
            val table = root.querySelector("#constant-table")!!
            val tableElements = table.querySelectorAll("*").let { elements ->
                (0 until elements.length).map { elements.item(it) }
            }
            val cell = table.querySelector("td")!!
            val firstText = cell.firstChild
            val lastText = cell.lastChild
            val composition = hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) {
                CompilerHydrationContent(count) { count++ }
            }
            try {
                nodes.forEachIndexed { index, node -> assertSame(node, root.childNodes.item(index)) }
                assertSame(titleText, title.firstChild)
                assertEquals(3, title.childNodes.length)
                assertSame(table, root.querySelector("#constant-table"))
                assertSame(firstText, cell.firstChild)
                assertSame(lastText, cell.lastChild)
                assertEquals(3, cell.childNodes.length)
                assertEquals("a<&b", cell.textContent)
                (nodes[1] as HTMLElement).click()
                delay(100)
                assertEquals("Count: 1", nodes[2]!!.textContent)
                assertSame(titleText, title.firstChild)
                assertSame(firstText, cell.firstChild)
                assertSame(lastText, cell.lastChild)
                val updatedElements = root.querySelectorAll("#constant-table *")
                tableElements.forEachIndexed { index, element -> assertSame(element, updatedElements.item(index)) }
            } finally {
                composition.dispose()
                root.parentNode!!.removeChild(root)
            }
        }
    }
}
