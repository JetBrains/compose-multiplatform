package org.jetbrains.compose.web

import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HydrationNonceTest {
    @Test
    fun nonceHydrationSupportsHiddenAttributesAndOlderBrowsers() {
        for (hasNonceProperty in listOf(true, false)) {
            val root = browserDocument.createElement("div") as HTMLElement
            root.innerHTML = "<div nonce=\"server-nonce\"></div>"
            val serverNode = root.firstChild as HTMLElement
            if (hasNonceProperty) {
                // Reproduce the DOM state produced by a header-delivered CSP.
                serverNode.setAttribute("nonce", "")
                setNoncePropertyForTest(serverNode, "server-nonce")
                assertEquals("", serverNode.getAttribute("nonce"))
            } else {
                hideNoncePropertyForTest(serverNode)
            }

            assertFailsWith<HydrationMismatchException> {
                hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                    Div({ attr("nonce", "different-nonce") })
                }
            }
            val composition = hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                Div({ attr("nonce", "server-nonce") })
            }
            try {
                assertSame(serverNode, root.firstChild)
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun missingNonceDoesNotMatchAnEmptyNonceProperty() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = "<div></div>"
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                Div({ attr("nonce", "") })
            }
        }
    }
}
