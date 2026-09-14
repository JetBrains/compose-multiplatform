package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationRecoveryTest {
    @Test
    fun mismatchIsReportedOnceBeforeClientFallback() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                attr("data-first", "server")
                attr("data-second", "server")
            })
        }
        val serverNode = root.firstChild
        val mismatches = mutableListOf<HydrationMismatchException>()

        val composition = hydrateComposable(
            root = root,
            onHydrationMismatch = mismatches::add,
        ) {
            Div(attrs = {
                attr("data-first", "client")
                attr("data-second", "client")
            })
        }

        try {
            assertEquals(1, mismatches.size)
            assertContains(mismatches.single().message.orEmpty(), "data-first")
            assertNotSame(serverNode, root.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun hydrationMismatchFallsBackToInteractiveClientRendering() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button { Text("Client") }
            Span()
        }
        val serverButton = root.firstChild as HTMLElement
        var label by mutableStateOf("Client")
        var mismatch: HydrationMismatchException? = null
        var refRunCount = 0
        var clickCount = 0

        val composition = hydrateComposable(
            root = root,
            onHydrationMismatch = { mismatch = it },
        ) {
            Button(attrs = {
                onClick { clickCount++ }
                ref {
                    refRunCount++
                    onDispose { }
                }
            }) {
                Text(label)
            }
        }

        val clientButton = root.firstChild as HTMLElement
        try {
            assertContains(mismatch?.message.orEmpty(), "found extra <span>")
            assertNotSame(serverButton, clientButton)
            assertEquals(1, refRunCount, "failed hydration must not run ref effects")

            serverButton.click()
            assertEquals(0, clickCount)

            clientButton.click()
            assertEquals(1, clickCount)

            label = "Updated"
            delay(100.milliseconds)
            assertEquals("Updated", clientButton.textContent)
        } finally {
            composition.dispose()
        }

        clientButton.click()
        assertEquals(1, clickCount, "disposing the composition must detach its listeners")
    }

    @Test
    fun applicationFailuresDoNotTriggerHydrationFallback() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<span>Server</span>"
        val serverHtml = root.innerHTML
        var mismatchCount = 0

        val failure = assertFailsWith<IllegalStateException> {
            hydrateComposable(
                root = root,
                onHydrationMismatch = { mismatchCount++ },
            ) {
                error("Application failure")
            }
        }

        assertEquals("Application failure", failure.message)
        assertEquals(0, mismatchCount)
        assertEquals(serverHtml, root.innerHTML)
    }
}
