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
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationRawTextTest {
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
