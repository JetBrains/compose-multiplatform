package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.toJsString
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.TagElementNS
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.browserDocument
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.parsing.DOMParser
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ComposeWebInternalApi::class)
class HydrationValidationModeTest {
    @Test
    fun fastHydrationRetainsServerValuesUntilTheClientChangesThem() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<div id=\"server\" class=\"server\" style=\"color: red\">server</div>"
        val serverDiv = root.firstChild as HTMLElement
        var clientValue by mutableStateOf("client")
        var mismatches = 0

        val composition = hydrateComposable(root, onHydrationMismatch = { mismatches++ }) {
            Div(attrs = {
                id(clientValue)
                classes(clientValue)
                attr("style", "color: red")
            }) { Text(clientValue) }
        }
        try {
            assertEquals(0, mismatches)
            assertSame(serverDiv, root.firstChild)
            assertEquals("server", serverDiv.id)
            assertEquals("server", serverDiv.className)
            assertEquals("server", serverDiv.textContent)

            clientValue = "updated"
            delay(100.milliseconds)
            assertEquals("updated", serverDiv.id)
            assertEquals("updated", serverDiv.className)
            assertEquals("updated", serverDiv.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun fastHydrationStillFallsBackForStructuralMismatches() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<span>server</span>"
        val serverSpan = root.firstChild
        var mismatches = 0
        val composition = hydrateComposable(root, onHydrationMismatch = { mismatches++ }) {
            Div { Text("client") }
        }
        try {
            assertEquals(1, mismatches)
            assertNotSame(serverSpan, root.firstChild)
            assertEquals("client", root.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun fastHydrationStillRequiresAnElementToExist() {
        val root = browserDocument.createElement("div") as HTMLElement
        var mismatches = 0
        val composition = hydrateComposable(root, onHydrationMismatch = { mismatches++ }) { Div() }
        try {
            assertEquals(1, mismatches)
            assertEquals("div", root.firstElementChild?.localName)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun fastHydrationRetainsServerRawTextButSplitsMergedTextNodes() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<script>server</script><textarea>ab</textarea>"
        val serverScript = root.firstChild
        val serverTextArea = root.lastChild
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            Script(InlineScript("client"))
            TagElement<Element>("textarea", null) {
                Text("a")
                Text("b")
            }
        }
        try {
            assertSame(serverScript, root.firstChild)
            assertEquals("server", serverScript?.textContent)
            assertSame(serverTextArea, root.lastChild)
            assertEquals("ab", serverTextArea?.textContent)
            assertEquals(2, serverTextArea?.childNodes?.length)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun fastHydrationRetainsSingleParsedTextUntilTheClientChangesIt() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<textarea>server</textarea>"
        val serverTextArea = root.firstChild
        val serverText = serverTextArea?.firstChild
        var value by mutableStateOf("client")
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            TagElement<Element>("textarea", null) { Text(value) }
        }
        try {
            assertSame(serverTextArea, root.firstChild)
            assertSame(serverText, serverTextArea?.firstChild)
            assertEquals("server", serverTextArea?.textContent)

            value = "updated"
            delay(100.milliseconds)
            assertEquals("updated", serverTextArea?.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun fastHydrationRejectsElementsInAnotherNamespace() {
        val root = browserDocument.createElement("div") as HTMLElement
        val mathMlNamespace = "http://www.w3.org/1998/Math/MathML"
        val svgNamespace = "http://www.w3.org/2000/svg"
        val serverCircle = browserDocument.createElementNS(mathMlNamespace, "circle")
        root.appendChild(serverCircle)
        var mismatch: HydrationMismatchException? = null
        val composition = hydrateComposable(root, onHydrationMismatch = { mismatch = it }) {
            TagElementNS<Element>("circle", svgNamespace, null, null)
        }
        try {
            assertContains(assertNotNull(mismatch).message.orEmpty(), mathMlNamespace)
            assertNotSame(serverCircle, root.firstChild)
            assertEquals(svgNamespace, (root.firstChild as Element).namespaceURI)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun validationModeFromTheStateElementControlsHydration() {
        for (mode in HtmlValidationMode.entries) {
            val html = renderHydratedDocument(validateStrictly = mode == HtmlValidationMode.Strict) {
                Html { Body { HydrationRoot(Unit, { "null" }) { Span { Text("server") } } } }
            }
            val parsed = DOMParser().parseFromString(html, "text/html".toJsString())
            val root = assertNotNull(parsed.querySelector("[$HydrationRootAttribute]"))
            val state = assertNotNull(parsed.querySelector("[$HydrationStateAttribute]"))
            val serverSpan = root.firstChild
            assertEquals(mode == HtmlValidationMode.Strict, state.hasAttribute(HydrationValidationAttribute))
            var mismatches = 0

            val composition = hydrateRoot(
                deserializeState = { Unit },
                within = parsed,
                onHydrationMismatch = { mismatches++ },
            ) { Span { Text("client") } }
            try {
                if (mode == HtmlValidationMode.Strict) {
                    assertEquals(1, mismatches)
                    assertNotSame(serverSpan, root.firstChild)
                    assertEquals("client", root.textContent)
                } else {
                    assertEquals(0, mismatches)
                    assertSame(serverSpan, root.firstChild)
                    assertEquals("server", root.textContent)
                }
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun unknownValidationMarkerFailsBeforeStateDecoding() {
        val html = renderHydratedDocument(validateStrictly = false) {
            Html { Body { HydrationRoot(Unit, { "null" }) {} } }
        }
        val parsed = DOMParser().parseFromString(html, "text/html".toJsString())
        val state = assertNotNull(parsed.querySelector("[$HydrationStateAttribute]"))
        state.setAttribute(HydrationValidationAttribute, "unknown")
        var decoded = false

        assertFailsWith<HydrationStateException> {
            hydrateRoot(
                deserializeState = { decoded = true; Unit },
                within = parsed,
            ) {}
        }
        assertEquals(false, decoded)
    }
}
