package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationEventListenerTest {
    @Test
    fun hydrationAttachesEventListenersToExistingElements() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        val composition = hydrateComposable(root) {
            Button(attrs = { onClick { clickCount++ } }) { Text("Click") }
        }

        try {
            button.click()

            assertEquals(1, clickCount)
            assertSame(button, root.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun recompositionReplacesEventListenersWithoutDuplicates() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var useSecondHandler by mutableStateOf(false)
        var firstHandlerCount = 0
        var secondHandlerCount = 0

        val composition = hydrateComposable(root) {
            Button(attrs = {
                if (useSecondHandler) {
                    onClick { secondHandlerCount++ }
                } else {
                    onClick { firstHandlerCount++ }
                }
            }) { Text("Click") }
        }

        try {
            button.click()
            useSecondHandler = true
            delay(100.milliseconds)
            button.click()

            assertEquals(1, firstHandlerCount)
            assertEquals(1, secondHandlerCount)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun conditionalEventListenersCanBeRemovedAndRestored() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var enabled by mutableStateOf(true)
        var clickCount = 0

        val composition = hydrateComposable(root) {
            Button(attrs = {
                if (enabled) {
                    onClick { clickCount++ }
                }
            }) { Text("Click") }
        }

        try {
            button.click()
            enabled = false
            delay(100.milliseconds)
            button.click()
            enabled = true
            delay(100.milliseconds)
            button.click()

            assertEquals(2, clickCount)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun disposingHydrationRemovesEventListeners() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        val composition = hydrateComposable(root) {
            Button(attrs = { onClick { clickCount++ } }) { Text("Click") }
        }

        button.click()
        composition.dispose()
        button.click()

        assertEquals(1, clickCount)
    }

    @Test
    fun removingHydratedElementRemovesItsEventListeners() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var showButton by mutableStateOf(true)
        var clickCount = 0

        val composition = hydrateComposable(root) {
            if (showButton) {
                Button(attrs = { onClick { clickCount++ } }) { Text("Click") }
            }
        }

        try {
            button.click()
            showButton = false
            delay(100.milliseconds)
            button.click()

            assertEquals(0, root.childNodes.length)
            assertEquals(1, clickCount)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun failedHydrationDoesNotRunEffectsOrLeaveEventListeners() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
            Span()
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Button(attrs = {
                    onClick { clickCount++ }
                    ref { element ->
                        element.click()
                        onDispose { }
                    }
                }) { Text("Click") }
            }
        }

        assertEquals(0, clickCount)
        button.click()
        assertEquals(0, clickCount)
    }
}
