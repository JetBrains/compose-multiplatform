package org.jetbrains.compose.web

import kotlinx.browser.document
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLScriptElement
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script
import kotlin.test.Test
import kotlin.test.assertEquals

class HydrationRawTextScriptTest {
    @Test
    fun mismatchFallbackExecutesInlineScriptAgain() {
        val counterName = "__compose_web_hydration_fallback_script_counter__"
        val scriptContent =
            "window['$counterName'] = (window['$counterName'] || 0) + 1;"
        val root = document.createElement("div") as HTMLElement
        setWindowIntProperty(counterName, 0)
        document.body!!.appendChild(root)

        try {
            val serverScript = document.createElement("script") as HTMLScriptElement
            serverScript.textContent = scriptContent
            root.appendChild(serverScript)
            root.appendChild(document.createElement("span"))
            val countAfterServerInsertion = getWindowIntProperty(counterName)
            assertEquals(1, countAfterServerInsertion)

            val composition = hydrateComposable(root) {
                Script(InlineScript(scriptContent))
            }
            try {
                val countAfterFallback = getWindowIntProperty(counterName)
                assertEquals(2, countAfterFallback)
            } finally {
                composition.dispose()
            }
        } finally {
            root.parentNode?.removeChild(root)
            setWindowIntProperty(counterName, null)
        }
    }
}
