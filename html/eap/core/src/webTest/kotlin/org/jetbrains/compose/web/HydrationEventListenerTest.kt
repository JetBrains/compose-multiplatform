package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.browser.dom.events.Event
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onReset
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

private suspend fun awaitAnimationFrame() {
    suspendCoroutine<Unit> { continuation ->
        window.requestAnimationFrame {
            continuation.resume(Unit)
        }
    }
}

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
    fun formEventAdaptersCreateSubmitEvents() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Form(action = "/")
        }
        val form = root.firstChild as HTMLFormElement
        val submitEvent = Event("submit")
        val resetEvent = Event("reset")
        var observedSubmitEvent: Event? = null
        var observedResetEvent: Event? = null

        val composition = hydrateComposable(root) {
            Form(
                action = "/",
                attrs = {
                    onSubmit { observedSubmitEvent = it.nativeEvent }
                    onReset { observedResetEvent = it.nativeEvent }
                },
            )
        }

        try {
            form.dispatchEvent(submitEvent)
            form.dispatchEvent(resetEvent)

            assertSame(submitEvent, observedSubmitEvent)
            assertSame(resetEvent, observedResetEvent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun controlledFormElementsRestoreAfterAllInputListeners() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) { value("controlled input") }
            TextArea(value = "controlled textarea")
        }
        val input = root.childNodes.item(0) as HTMLInputElement
        val textArea = root.childNodes.item(1) as HTMLTextAreaElement
        var observedInputValue = ""
        var observedTextAreaValue = ""

        val composition = hydrateComposable(root) {
            Input(InputType.Text) {
                value("controlled input")
                onInput { observedInputValue = it.value }
            }
            TextArea(
                value = "controlled textarea",
                attrs = { onInput { observedTextAreaValue = it.value } },
            )
        }

        try {
            input.value = "typed input"
            textArea.value = "typed textarea"
            input.dispatchEvent(Event("input"))
            textArea.dispatchEvent(Event("input"))

            assertEquals("typed input", observedInputValue)
            assertEquals("typed textarea", observedTextAreaValue)
            assertEquals("typed input", input.value)
            assertEquals("typed textarea", textArea.value)
            // The first callback can precede Compose's callback in the same frame.
            awaitAnimationFrame()
            awaitAnimationFrame()
            assertEquals("controlled input", input.value)
            assertEquals("controlled textarea", textArea.value)
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
