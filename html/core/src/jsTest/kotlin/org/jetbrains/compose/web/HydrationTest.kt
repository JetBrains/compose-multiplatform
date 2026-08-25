package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.HydrationDomApplier
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@OptIn(ComposeWebInternalApi::class)
class HydrationTest {
    @Test
    fun serverRenderedDomIsReused() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString { TestContent() }
        val serverRenderedNode = root.firstChild
        val serverRenderedText = serverRenderedNode?.firstChild

        val composition = hydrateComposable(root) {
            TestContent()
        }

        try {
            assertEquals(1, root.childElementCount)
            assertSame(serverRenderedNode, root.firstChild)
            assertSame(serverRenderedText, root.firstChild?.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun recompositionUsesNormalDomInsertionAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div { Span { Text("First") } }
        }
        val serverRenderedNode = root.firstChild
        var showSecondChild by mutableStateOf(false)

        val composition = hydrateComposable(root) {
            Div {
                Span { Text("First") }
                if (showSecondChild) {
                    Span { Text("Second") }
                }
            }
        }

        try {
            showSecondChild = true
            delay(100)

            assertSame(serverRenderedNode, root.firstChild)
            assertEquals("<div><span>First</span><span>Second</span></div>", root.innerHTML)
        } finally {
            composition.dispose()
        }
    }

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
            button.dispatchEvent(MouseEvent("click"))

            assertEquals(1, clickCount)
            assertSame(button, root.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun hydrationFailsWhenElementTagsDiffer() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span { Text("Hello") }
        }
        val serverHtml = root.innerHTML
        val serverNode = root.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Div { Text("Hello") }
            }
        }

        assertContains(failure.message.orEmpty(), "expected <div>")
        assertContains(failure.message.orEmpty(), "found <span>")
        assertSame(serverNode, root.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun hydrationFailsWhenTextDiffers() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div { Text("Server") }
        }
        val serverHtml = root.innerHTML
        val serverNode = root.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Div { Text("Client") }
            }
        }

        assertContains(failure.message.orEmpty(), "expected text \"Client\"")
        assertContains(failure.message.orEmpty(), "found text \"Server\"")
        assertSame(serverNode, root.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun hydrationFailsWhenServerHasAnExtraChild() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Span()
                Span()
            }
        }
        val serverHtml = root.innerHTML
        val serverNode = root.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Div { Span() }
            }
        }

        assertContains(failure.message.orEmpty(), "expected end of node")
        assertContains(failure.message.orEmpty(), "found extra <span>")
        assertSame(serverNode, root.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun hydrationFailsWhenServerIsMissingAChild() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div { Span() }
        }
        val serverHtml = root.innerHTML
        val serverNode = root.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Div {
                    Span()
                    Span()
                }
            }
        }

        assertContains(failure.message.orEmpty(), "root/div[0]/span[1]")
        assertContains(failure.message.orEmpty(), "found the end of the children")
        assertSame(serverNode, root.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun domMutationsAreIgnoredDuringAndAfterAbortedHydration() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<span>A</span><span>B</span><span>C</span>"
        val serverHtml = root.innerHTML
        val applier = HydrationDomApplier(DomNodeWrapper(root))

        applier.move(from = 1, to = 0, count = 1)
        applier.remove(index = 0, count = 1)
        assertEquals(serverHtml, root.innerHTML)

        applier.abortHydration()
        applier.move(from = 1, to = 0, count = 1)
        applier.remove(index = 0, count = 1)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun domMutationsAreForwardedAfterHydrationCompletes() {
        val root = document.createElement("div") as HTMLElement
        val applier = HydrationDomApplier(DomNodeWrapper(root))
        applier.finishHydration()
        root.innerHTML = "<span>A</span><span>B</span><span>C</span>"

        applier.move(from = 1, to = 0, count = 1)
        applier.remove(index = 2, count = 1)

        assertEquals("<span>B</span><span>A</span>", root.innerHTML)
    }
}

@Composable
private fun TestContent() {
    Div {
        Text("Hello from Compose HTML")
    }
}
