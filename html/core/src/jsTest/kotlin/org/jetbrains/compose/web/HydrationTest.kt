package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.HydrationDomApplier
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

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
    fun rootBoundaryWhitespaceIsIgnoredAndRemoved() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "\n    ${composeHtmlToString { Span { Text("First") } }}\n"
        val serverRenderedSpan = root.childNodes.item(1)
        val leadingWhitespace = root.firstChild
        val trailingWhitespace = root.lastChild
        var showSecond by mutableStateOf(false)

        val composition = hydrateComposable(root) {
            Span { Text("First") }
            if (showSecond) {
                Span { Text("Second") }
            }
        }

        try {
            assertEquals(1, root.childNodes.length)
            assertSame(serverRenderedSpan, root.firstChild)
            assertNull(leadingWhitespace?.parentNode)
            assertNull(trailingWhitespace?.parentNode)

            showSecond = true
            delay(100.milliseconds)

            assertEquals(2, root.childNodes.length)
            assertSame(serverRenderedSpan, root.firstChild)
            assertEquals("<span>First</span><span>Second</span>", root.innerHTML)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun rootBoundaryWhitespaceIsPreservedWhenHydrationFails() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "\n    <span>Server</span>\n"
        val serverHtml = root.innerHTML

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Span { Text("Client") }
            }
        }

        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun whitespaceInsideComposedElementsIsNotIgnored() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<div>\n    <span></span>\n</div>"

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div { Span() }
            }
        }

        assertContains(failure.message.orEmpty(), "expected <span>")
        assertContains(failure.message.orEmpty(), "found text")
    }

    @Test
    fun visibleWhitespaceAtTheRootIsNotIgnored() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "&nbsp;<span></span>"

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Span()
            }
        }

        assertContains(failure.message.orEmpty(), "expected <span>")
        assertContains(failure.message.orEmpty(), "found text")
    }

    @Test
    fun adjacentTextNodesKeepTheirLogicalBoundaries() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("Hello, ")
                Text("world")
            }
        }
        val div = root.firstChild as HTMLElement
        val firstText = div.firstChild
        val secondText = div.lastChild

        val composition = hydrateComposable(root) {
            Div {
                Text("Hello, ")
                Text("world")
            }
        }

        try {
            assertEquals(2, div.childNodes.length)
            assertSame(firstText, div.childNodes.item(0))
            assertSame(secondText, div.childNodes.item(1))
            assertEquals("Hello, world", div.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun emptyTextBeforeNonEmptyTextKeepsBothLogicalNodes() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("")
                Text("visible")
            }
        }
        val div = root.firstChild as HTMLElement
        val serverRenderedText = div.lastChild

        val composition = hydrateComposable(root) {
            Div {
                Text("")
                Text("visible")
            }
        }

        try {
            assertEquals(2, div.childNodes.length)
            assertEquals("", div.childNodes.item(0)?.textContent)
            assertSame(serverRenderedText, div.childNodes.item(1))
            assertEquals("visible", div.childNodes.item(1)?.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun nonEmptyTextBeforeEmptyTextKeepsBothLogicalNodes() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("visible")
                Text("")
            }
        }
        val div = root.firstChild as HTMLElement
        val serverRenderedText = div.firstChild

        assertEquals("<div>visible</div>", root.innerHTML)
        val composition = hydrateComposable(root) {
            Div {
                Text("visible")
                Text("")
            }
        }

        try {
            assertEquals(2, div.childNodes.length)
            assertSame(serverRenderedText, div.childNodes.item(0))
            assertEquals("", div.childNodes.item(1)?.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun textCanDisappearAndReappearAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("Before")
                Text("Middle")
                Text("After")
            }
        }
        var showMiddle by mutableStateOf(true)

        val composition = hydrateComposable(root) {
            Div {
                Text("Before")
                if (showMiddle) {
                    Text("Middle")
                }
                Text("After")
            }
        }

        try {
            val div = root.firstChild as HTMLElement
            showMiddle = false
            delay(100.milliseconds)

            assertEquals(2, div.childNodes.length)
            assertEquals("BeforeAfter", div.textContent)

            showMiddle = true
            delay(100.milliseconds)

            assertEquals(3, div.childNodes.length)
            assertEquals("BeforeMiddleAfter", div.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun textBoundariesArePreservedWhenMixedWithElements() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("before")
                Span { Text("inside") }
                Text("after")
                Text("tail")
            }
        }
        val div = root.firstChild as HTMLElement
        val before = div.firstChild
        val span = div.childNodes.item(1)
        val after = div.childNodes.item(2)
        val tail = div.lastChild

        val composition = hydrateComposable(root) {
            Div {
                Text("before")
                Span { Text("inside") }
                Text("after")
                Text("tail")
            }
        }

        try {
            assertEquals(4, div.childNodes.length)
            assertSame(before, div.childNodes.item(0))
            assertSame(span, div.childNodes.item(1))
            assertSame(after, div.childNodes.item(2))
            assertSame(tail, div.childNodes.item(3))
            assertEquals("beforeinsideaftertail", div.textContent)
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
            delay(100.milliseconds)

            assertSame(serverRenderedNode, root.firstChild)
            assertEquals("<div><span>First</span><span>Second</span></div>", root.innerHTML)
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
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
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
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div { Text("Client") }
            }
        }

        assertContains(failure.message.orEmpty(), "expected text \"Client\"")
        assertContains(failure.message.orEmpty(), "found text \"Server\"")
        assertSame(serverNode, root.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun textBoundaryMarkersRemainWhenHydrationFails() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("First")
                Text("")
                Text("Second")
                Span()
            }
        }
        val serverHtml = root.innerHTML

        assertContains(serverHtml, "<!--c-->")
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div {
                    Text("First")
                    Text("")
                    Text("Second")
                    Div()
                }
            }
        }

        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun textBoundaryMarkersUseInternalDiagnostics() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<div><!--c--></div>"

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) { Div() }
        }

        assertContains(failure.message.orEmpty(), "found extra an internal text boundary")
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
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
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
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
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
    fun domMutationsFailDuringAndAfterAbortedHydration() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<span>A</span><span>B</span><span>C</span>"
        val serverHtml = root.innerHTML
        val applier = HydrationDomApplier(DomNodeWrapper(root))

        assertFailsWith<IllegalStateException> {
            applier.move(from = 1, to = 0, count = 1)
        }
        assertFailsWith<IllegalStateException> {
            applier.remove(index = 0, count = 1)
        }
        assertFailsWith<IllegalStateException> {
            applier.clear()
        }
        assertEquals(serverHtml, root.innerHTML)

        applier.abortHydration()
        assertFailsWith<IllegalStateException> {
            applier.move(from = 1, to = 0, count = 1)
        }
        assertFailsWith<IllegalStateException> {
            applier.remove(index = 0, count = 1)
        }
        assertFailsWith<IllegalStateException> {
            applier.clear()
        }
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
