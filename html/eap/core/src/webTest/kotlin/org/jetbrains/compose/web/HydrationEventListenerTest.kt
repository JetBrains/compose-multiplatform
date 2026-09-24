package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.browser.dom.events.Event
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onReset
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.internal.runtime.browserDocument
import org.jetbrains.compose.web.internal.scheduleAfterEvent
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

private suspend fun awaitBrowserCondition(condition: () -> Boolean) {
    repeat(10) {
        if (condition()) return
        awaitAnimationFrame()
    }
}

class HydrationEventListenerTest {
    @Test
    fun deferredWorkRunsAfterAllNativeEventListeners() = MainScope().promise {
        val frame = browserDocument.createElement("iframe")
        frame.setAttribute("srcdoc", "<!doctype html><title>Event dispatch</title>")
        val observed = mutableListOf<String>()
        val completed = CompletableDeferred<List<String>>()
        // A browser-generated load event allows microtask checkpoints between
        // listeners, unlike dispatchEvent() called from a JavaScript stack.
        frame.addEventListener("load", {
            observed += "first listener"
            scheduleAfterEvent {
                observed += "deferred work"
                completed.complete(observed.toList())
            }
        })
        frame.addEventListener("load", {
            observed += "second listener"
        })
        try {
            browserDocument.body!!.appendChild(frame)
            assertEquals(
                listOf("first listener", "second listener", "deferred work"),
                withTimeout(5_000) { completed.await() },
            )
        } finally {
            frame.parentNode?.removeChild(frame)
        }
    }

    @Test
    fun throwingDeferredPropertyRemovesAlreadyAttachedListeners() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<button>Click</button><span></span>"
        val button = root.firstChild as HTMLElement
        val failure = IllegalStateException("property failed")
        var clickCount = 0

        val thrown = assertFailsWith<IllegalStateException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                Button(attrs = { onClick { clickCount++ } }) { Text("Click") }
                Span(attrs = {
                    prop({ _: HTMLElement, _: Unit ->
                        button.click()
                        throw failure
                    }, Unit)
                })
            }
        }

        assertSame(failure, thrown)
        assertEquals(1, clickCount)
        button.click()
        assertEquals(1, clickCount)
        assertSame(button, root.firstChild)
        assertEquals(0, thrown.suppressedExceptions.size)
    }

    @Test
    fun hydrationAttachesEventListenersToExistingElements() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        val composition = hydrateComposable(root, validateStrictly = true) {
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
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Form(action = "/")
        }
        val form = root.firstChild as HTMLFormElement
        val submitEvent = Event("submit")
        val resetEvent = Event("reset")
        var observedSubmitEvent: Event? = null
        var observedResetEvent: Event? = null

        val composition = hydrateComposable(root, validateStrictly = true) {
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
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) { value("controlled input") }
            TextArea(value = "controlled textarea")
        }
        val input = root.childNodes.item(0) as HTMLInputElement
        val textArea = root.childNodes.item(1) as HTMLTextAreaElement
        var observedInputValue = ""
        var observedTextAreaValue = ""

        val composition = hydrateComposable(root, validateStrictly = true) {
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
            awaitBrowserCondition {
                input.value == "controlled input" &&
                    textArea.value == "controlled textarea"
            }
            assertEquals("controlled input", input.value)
            assertEquals("controlled textarea", textArea.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun recompositionReplacesEventListenersWithoutDuplicates() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var useSecondHandler by mutableStateOf(false)
        var firstHandlerCount = 0
        var secondHandlerCount = 0

        val composition = hydrateComposable(root, validateStrictly = true) {
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
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var enabled by mutableStateOf(true)
        var clickCount = 0

        val composition = hydrateComposable(root, validateStrictly = true) {
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
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        val composition = hydrateComposable(root, validateStrictly = true) {
            Button(attrs = { onClick { clickCount++ } }) { Text("Click") }
        }

        button.click()
        composition.dispose()
        button.click()

        assertEquals(1, clickCount)
    }

    @Test
    fun removingHydratedElementRemovesItsEventListeners() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
        }
        val button = root.firstChild as HTMLElement
        var showButton by mutableStateOf(true)
        var clickCount = 0

        val composition = hydrateComposable(root, validateStrictly = true) {
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
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Click") }
            Span()
        }
        val button = root.firstChild as HTMLElement
        var clickCount = 0

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
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
