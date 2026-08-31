package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.HydrationTextBoundaryMarker
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.Comment
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.Text as DomText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class HydrationMismatchAllowanceTest {
    @Test
    fun allowedTextMismatchReusesServerNodeWithClientValue() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:00") }
        }
        val serverSpan = root.firstChild as HTMLElement
        val serverText = serverSpan.firstChild
        var mismatch: HydrationMismatchException? = null

        val composition = hydrateComposable(
            root = root,
            onHydrationMismatch = { mismatch = it },
        ) {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:05") }
        }

        try {
            assertNull(mismatch)
            assertSame(serverSpan, root.firstChild)
            assertSame(serverText, serverSpan.firstChild)
            assertEquals("12:05", serverSpan.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedAdjacentTextMismatchesReuseServerNodesAndRemoveBoundaryMarker() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) {
                Text("server first")
                Text("server second")
            }
        }
        val serverSpan = root.firstChild as HTMLElement
        val serverFirstText = serverSpan.childNodes.item(0) as DomText
        val boundaryMarker = serverSpan.childNodes.item(1) as Comment
        val serverSecondText = serverSpan.childNodes.item(2) as DomText
        assertEquals(HydrationTextBoundaryMarker, boundaryMarker.data)

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Span(attrs = { allowHydrationMismatch() }) {
                Text("client first")
                Text("client second")
            }
        }

        try {
            assertSame(serverSpan, root.firstChild)
            assertEquals(2, serverSpan.childNodes.length)
            assertSame(serverFirstText, serverSpan.childNodes.item(0))
            assertSame(serverSecondText, serverSpan.childNodes.item(1))
            assertNull(boundaryMarker.parentNode)
            assertEquals("client firstclient second", serverSpan.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedTextKeepsRecomposingAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:00") }
        }
        val serverSpan = root.firstChild as HTMLElement
        var label by mutableStateOf("12:05")
        var clickCount = 0

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Span(attrs = {
                allowHydrationMismatch()
                onClick { clickCount++ }
            }) {
                Text(label)
            }
        }

        try {
            assertEquals("12:05", serverSpan.textContent)

            serverSpan.click()
            assertEquals(1, clickCount, "allowing a mismatch must not skip deferred listeners")

            label = "12:06"
            delay(100.milliseconds)
            assertEquals("12:06", serverSpan.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun clientOnlyTextIsInsertedWhenServerRenderedNoText() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) { Text("") }
        }
        val serverSpan = root.firstChild as HTMLElement
        assertNull(serverSpan.firstChild)

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:05") }
        }

        try {
            assertSame(serverSpan, root.firstChild)
            assertEquals("12:05", serverSpan.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun serverTextIsClearedWhenClientRendersEmptyText() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:00") }
        }
        val serverSpan = root.firstChild as HTMLElement
        val serverText = serverSpan.firstChild

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Span(attrs = { allowHydrationMismatch() }) { Text("") }
        }

        try {
            assertSame(serverSpan, root.firstChild)
            assertSame(serverText, serverSpan.firstChild)
            assertEquals("", serverSpan.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedAttributeMismatchesUseClientValues() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:00")
                attr("data-server-only", "removed")
                classes("shared")
                style { width(10.px) }
            })
        }
        val serverDiv = root.firstChild as HTMLElement
        serverDiv.setAttribute("data-extension", "injected")
        serverDiv.classList.add("ext-injected")

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Div(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:05")
                classes("shared", "client-only")
                style { width(20.px) }
            })
        }

        try {
            assertSame(serverDiv, root.firstChild)
            assertEquals("12:05", serverDiv.getAttribute("data-rendered-at"))
            assertEquals("20px", serverDiv.style.getPropertyValue("width"))
            assertTrue(serverDiv.classList.contains("shared"))
            assertTrue(serverDiv.classList.contains("client-only"))
            // Attributes and classes the composition does not own stay untouched, as without
            // allowance.
            assertEquals("removed", serverDiv.getAttribute("data-server-only"))
            assertEquals("injected", serverDiv.getAttribute("data-extension"))
            assertTrue(serverDiv.classList.contains("ext-injected"))
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedRawTextMismatchUsesClientValue() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript("const renderedAt = '12:00';")) {
                allowHydrationMismatch()
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild as HTMLScriptElement

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Script(InlineScript("const renderedAt = '12:05';")) {
                allowHydrationMismatch()
                type(ScriptType.TextPlain)
            }
        }

        try {
            assertSame(serverScript, root.firstChild)
            assertEquals("const renderedAt = '12:05';", serverScript.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedRawTextIsAddedWhenServerRenderedNoText() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript("")) {
                allowHydrationMismatch()
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild as HTMLScriptElement
        assertNull(serverScript.firstChild)

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Script(InlineScript("const renderedAt = '12:05';")) {
                allowHydrationMismatch()
                type(ScriptType.TextPlain)
            }
        }

        try {
            assertSame(serverScript, root.firstChild)
            assertEquals("const renderedAt = '12:05';", serverScript.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowedStyleTextMismatchUsesClientCss() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Style(applyAttrs = { allowHydrationMismatch() }) {
                "body" style { color(Color.red) }
            }
        }
        val serverStyle = root.firstChild as HTMLStyleElement

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Style(applyAttrs = { allowHydrationMismatch() }) {
                "body" style { color(Color.blue) }
            }
        }

        try {
            assertSame(serverStyle, root.firstChild)
            // The root is detached, so the CSS stays serialized instead of moving to CSSOM.
            assertEquals("body { color: blue;}", serverStyle.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun allowanceAppliesToDirectChildrenOnly() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = { allowHydrationMismatch() }) {
                Span { Text("12:00") }
            }
        }

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div(attrs = { allowHydrationMismatch() }) {
                    Span { Text("12:05") }
                }
            }
        }

        assertContains(failure.message.orEmpty(), "expected text \"12:05\"")
    }

    @Test
    fun allowanceDoesNotTolerateStructuralMismatches() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = { allowHydrationMismatch() }) {
                Text("12:00")
                Span()
            }
        }

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div(attrs = { allowHydrationMismatch() }) {
                    Text("12:05")
                }
            }
        }

        assertContains(failure.message.orEmpty(), "found extra <span>")
    }

    @Test
    fun allowedValuesAreNotAppliedWhenHydrationFailsLater() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:00")
            }) {
                Text("12:00")
            }
            Span()
        }
        val serverHtml = root.innerHTML
        val serverSpan = root.firstChild
        val serverText = serverSpan?.firstChild

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Span(attrs = {
                    allowHydrationMismatch()
                    attr("data-rendered-at", "12:05")
                }) {
                    Text("12:05")
                }
            }
        }

        assertSame(serverSpan, root.firstChild)
        assertSame(serverText, serverSpan?.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun disallowedSiblingMismatchesAreStillReported() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Span(attrs = { allowHydrationMismatch() }) { Text("12:00") }
            Span { Text("stable") }
        }

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Span(attrs = { allowHydrationMismatch() }) { Text("12:05") }
                Span { Text("changed") }
            }
        }

        assertContains(failure.message.orEmpty(), "expected text \"changed\"")
    }

    @Test
    fun delegatingAttrsScopesForwardTheAllowance() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Select(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:00")
            })
            TextArea(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:00")
            })
            Input(InputType.Text) {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:00")
            }
        }
        val serverSelect = root.childNodes.item(0) as HTMLElement
        val serverTextArea = root.childNodes.item(1) as HTMLElement
        val serverInput = root.childNodes.item(2) as HTMLElement

        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Select(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:05")
            })
            TextArea(attrs = {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:05")
            })
            Input(InputType.Text) {
                allowHydrationMismatch()
                attr("data-rendered-at", "12:05")
            }
        }

        try {
            assertSame(serverSelect, root.childNodes.item(0))
            assertSame(serverTextArea, root.childNodes.item(1))
            assertSame(serverInput, root.childNodes.item(2))
            listOf(serverSelect, serverTextArea, serverInput).forEach { element ->
                assertEquals(
                    "12:05",
                    element.getAttribute("data-rendered-at"),
                    "<${element.tagName.lowercase()}> did not forward the allowance",
                )
            }
        } finally {
            composition.dispose()
        }
    }
}
