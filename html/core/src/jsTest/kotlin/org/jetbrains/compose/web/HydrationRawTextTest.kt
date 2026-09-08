package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationRawTextTest {
    @Test
    fun genericRawTextValidationDoesNotEscapeTheMismatchFallback() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<script>server</script>"
        val serverScript = root.firstChild
        var mismatch: HydrationMismatchException? = null
        val composition = hydrateComposable(root, onHydrationMismatch = { mismatch = it }) {
            TagElement<Element>("script", null) { Text("</script>") }
        }
        try {
            assertNotNull(mismatch)
            assertNotSame(serverScript, root.firstChild)
            assertEquals("</script>", root.firstChild!!.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun genericRawTextHydrationOnlyNormalizesParserInput() {
        // These values cannot be string-rendered, but can exist in a DOM built by client code.
        listOf("</script>", "<!-- <script>", "first\r\nsecond\u0000").forEach { value ->
            listOf(false, true).forEach { allowed ->
                val root = document.createElement("div") as HTMLElement
                val serverScript = document.createElement("script")
                serverScript.textContent = if (allowed) "server" else
                    value.replace("\r\n", "\n").replace('\u0000', '\uFFFD')
                root.appendChild(serverScript)
                val serverText = serverScript.firstChild
                val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
                    TagElement<Element>("script", { if (allowed) allowHydrationMismatch() }) {
                        Text(value)
                    }
                }
                try {
                    assertSame(serverScript, root.firstChild)
                    assertSame(serverText, serverScript.firstChild)
                    assertEquals(value, serverScript.textContent)
                } finally {
                    composition.dispose()
                }
            }
        }
    }

    @Test
    fun allowedRawTextMismatchStillRejectsNonTextChildren() {
        listOf(false, true).forEach { generic ->
            val root = document.createElement("div") as HTMLElement
            val serverScript = document.createElement("script")
            val child = document.createElement("span")
            serverScript.appendChild(child)
            root.appendChild(serverScript)
            assertFailsWith<HydrationMismatchException> {
                hydrateComposable(root, onHydrationMismatch = { throw it }) {
                    if (generic) {
                        TagElement<Element>("script", { allowHydrationMismatch() }) { Text("client") }
                    } else {
                        Script(InlineScript("client")) { allowHydrationMismatch() }
                    }
                }
            }
            assertSame(serverScript, root.firstChild)
            assertSame(child, serverScript.firstChild)
        }
    }

    @Test
    fun genericRawTextHydratesWithoutBoundaryComments() {
        listOf("script", "style", "iframe", "xmp", "noembed", "noframes", "noscript").forEach { tag ->
            val root = document.createElement("div") as HTMLElement
            root.innerHTML = composeHtmlToString {
                TagElement<Element>(tag, null) {
                    Text("A & B")
                    Text("")
                    Text(" < C")
                }
            }
            val parent = root.firstChild!!
            val serverText = parent.firstChild
            assertEquals(1, parent.childNodes.length, tag)
            assertEquals("A & B < C", parent.textContent, tag)
            val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
                TagElement<Element>(tag, null) {
                    Text("A & B")
                    Text("")
                    Text(" < C")
                }
            }
            try {
                assertSame(parent, root.firstChild, tag)
                assertSame(serverText, parent.firstChild, tag)
                assertEquals(3, parent.childNodes.length, tag)
                assertEquals("A & B < C", parent.textContent, tag)
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun genericRawTextSplittingWaitsUntilHydrationSucceeds() {
        listOf(false, true).forEach { mismatchInSibling ->
            val root = document.createElement("div") as HTMLElement
            root.innerHTML = "<script>first second</script><span>server</span>"
            val serverText = root.firstChild!!.firstChild
            val serverHtml = root.innerHTML
            assertFailsWith<HydrationMismatchException> {
                hydrateComposable(root, onHydrationMismatch = { throw it }) {
                    TagElement<Element>("script", null) {
                        Text("first")
                        Text(if (mismatchInSibling) " second" else " different")
                    }
                    Span { Text(if (mismatchInSibling) "client" else "server") }
                }
            }
            assertSame(serverText, root.firstChild!!.firstChild)
            assertEquals(serverHtml, root.innerHTML)
            assertEquals(1, root.firstChild!!.childNodes.length)
        }
    }

    @Test
    fun genericRawTextChildrenUpdateIndependentlyAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<script>first second</script>"
        var suffix by mutableStateOf(" second")
        val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
            TagElement<Element>("script", null) {
                Text("first")
                Text(suffix)
            }
        }
        val first = root.firstChild!!.firstChild
        val second = first!!.nextSibling
        try {
            suffix = " changed"
            delay(100.milliseconds)
            assertSame(first, root.firstChild!!.firstChild)
            assertSame(second, first.nextSibling)
            assertEquals("first changed", root.firstChild!!.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun serverRenderedRawTextIsReusedAndNewlinesAreNormalized() {
        val content = "const first = '<main>&';\r\nconst second = true;\r"
        val normalizedContent = "const first = '<main>&';\nconst second = true;\n"
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript(content)) {
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild as HTMLScriptElement
        val serverText = serverScript.firstChild

        val composition = hydrateComposable(root) {
            Script(InlineScript(content)) {
                type(ScriptType.TextPlain)
            }
        }

        try {
            assertSame(serverScript, root.firstChild)
            assertSame(serverText, serverScript.firstChild)
            assertEquals(normalizedContent, serverScript.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun rawTextMismatchLeavesServerDomUntouched() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript("server")) {
                type(ScriptType.TextPlain)
            }
        }
        val serverHtml = root.innerHTML
        val serverScript = root.firstChild
        val serverText = serverScript?.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Script(InlineScript("client")) {
                    type(ScriptType.TextPlain)
                }
            }
        }

        assertContains(failure.message.orEmpty(), "expected raw text")
        assertSame(serverScript, root.firstChild)
        assertSame(serverText, serverScript?.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun emptyRawTextIsReused() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript("")) {
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild

        val composition = hydrateComposable(root) {
            Script(InlineScript("")) {
                type(ScriptType.TextPlain)
            }
        }

        try {
            assertSame(serverScript, root.firstChild)
            assertEquals(null, serverScript?.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun extraRawTextChildIsAHydrationMismatch() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Script(InlineScript("content")) {
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild as HTMLScriptElement
        serverScript.appendChild(document.createComment("extra"))

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Script(InlineScript("content")) {
                    type(ScriptType.TextPlain)
                }
            }
        }

        assertContains(failure.message.orEmpty(), "expected end of node")
        assertSame(serverScript, root.firstChild)
    }

    @Test
    fun mismatchFallbackExecutesInlineScriptAgain() {
        val counterName = "__compose_web_hydration_fallback_script_counter__"
        val scriptContent =
            "window['$counterName'] = (window['$counterName'] || 0) + 1;"
        val root = document.createElement("div") as HTMLElement
        window.asDynamic()[counterName] = 0
        document.body!!.appendChild(root)

        try {
            val serverScript = document.createElement("script") as HTMLScriptElement
            serverScript.textContent = scriptContent
            root.appendChild(serverScript)
            root.appendChild(document.createElement("span"))
            val countAfterServerInsertion: Int = window.asDynamic()[counterName]
            assertEquals(1, countAfterServerInsertion)

            val composition = hydrateComposable(root) {
                Script(InlineScript(scriptContent))
            }
            try {
                val countAfterFallback: Int = window.asDynamic()[counterName]
                assertEquals(2, countAfterFallback)
            } finally {
                composition.dispose()
            }
        } finally {
            root.parentNode?.removeChild(root)
            window.asDynamic()[counterName] = null
        }
    }

    @Test
    fun rawTextUpdatesAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        var content by mutableStateOf("first <&>")
        root.innerHTML = composeHtmlToString {
            Script(InlineScript(content)) {
                type(ScriptType.TextPlain)
            }
        }
        val serverScript = root.firstChild as HTMLScriptElement

        val composition = hydrateComposable(root) {
            Script(InlineScript(content)) {
                type(ScriptType.TextPlain)
            }
        }

        try {
            content = "second <&>"
            delay(100.milliseconds)

            assertSame(serverScript, root.firstChild)
            assertEquals(content, serverScript.textContent)
        } finally {
            composition.dispose()
        }
    }
}
