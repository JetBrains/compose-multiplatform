/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationBookkeepingTest {
    @Test
    fun independentAttributeAndContentChangesPreserveRememberRefsAndSiblings() = MainScope().promise {
        var title by mutableStateOf("before")
        var text by mutableStateOf("one")
        var visible by mutableStateOf(true)
        var remembers = 0
        var liveRefs = 0
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<div title=\"before\">one</div><span>tail</span>"
        val div = root.firstChild as HTMLElement
        val tail = root.lastChild
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Div(attrs = {
                attr("title", title)
                ref {
                    liveRefs++
                    onDispose { liveRefs-- }
                }
            }) {
                remember { remembers++ }
                if (visible) Text(text)
            }
            Span { Text("tail") }
        }
        try {
            title = "after"
            delay(100.milliseconds)
            assertEquals("after", div.getAttribute("title"))
            assertEquals("one", div.textContent)
            visible = false
            delay(100.milliseconds)
            assertEquals("", div.textContent)
            text = "two"
            visible = true
            delay(100.milliseconds)
            assertEquals("two", div.textContent)
            assertSame(div, root.firstChild)
            assertSame(tail, root.lastChild)
            assertEquals(1, remembers)
            assertEquals(1, liveRefs)
        } finally {
            composition.dispose()
        }
        assertEquals(0, liveRefs)
    }

    @Test
    fun conditionalListenersSurviveParentRecompositionAndAreRemovedOnDispose() = MainScope().promise {
        var enabled by mutableStateOf(false)
        var label by mutableStateOf("first")
        var clicks = 0
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<button>first</button>"
        val button = root.firstChild as HTMLElement
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Button(attrs = { if (enabled) onClick { clicks++ } }) { Text(label) }
        }
        try {
            button.click()
            assertEquals(0, clicks)
            enabled = true
            delay(100.milliseconds)
            button.click()
            assertEquals(1, clicks)
            label = "second"
            delay(100.milliseconds)
            button.click()
            assertEquals(2, clicks)
            assertEquals("second", button.textContent)
            enabled = false
            delay(100.milliseconds)
            button.click()
            assertEquals(2, clicks)
            enabled = true
            delay(100.milliseconds)
            assertSame(button, root.firstChild)
        } finally {
            composition.dispose()
        }
        // Keep a reference to the detached element: disposal must remove its listener too.
        button.click()
        assertEquals(2, clicks)
    }

    @Test
    fun attributesStylesAndPropertiesUpdateTogetherWithoutReplacingTheNode() = MainScope().promise {
        var version by mutableStateOf(0)
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<div class=\"old\" title=\"before\" style=\"color: red\"></div>"
        val element = root.firstChild as HTMLElement
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Div({
                // Verify invalidation after removing restart scopes from DOM forwarding functions.
                classes(if (version == 0) "old" else "new")
                if (version == 0) attr("title", "before")
                style { property("color", if (version == 0) "red" else "blue") }
                prop({ e: HTMLElement, value: Int -> e.tabIndex = value }, version)
            })
        }
        try {
            assertEquals(0, element.tabIndex)
            version = 1
            delay(100.milliseconds)
            assertSame(element, root.firstChild)
            assertEquals("new", element.className)
            assertEquals(false, element.hasAttribute("title"))
            assertEquals("blue", element.style.color)
            assertEquals(1, element.tabIndex)
        } finally {
            composition.dispose()
        }
    }
}
