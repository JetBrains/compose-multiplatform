package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private external interface FetchResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

@JsName("fetch")
private external fun fetchResource(input: String): Promise<FetchResponse>

class JvmSsrHydrationTest {
    @Test
    fun jvmRenderedContentHydratesWithStylesEventsAndRecomposition() = MainScope().promise {
        val response = fetchResource(SSR_HYDRATION_FIXTURE_URL).await()
        assertTrue(
            response.ok,
            "Fetching $SSR_HYDRATION_FIXTURE_URL failed with HTTP ${response.status}",
        )

        val root = document.createElement("div") as HTMLElement
        root.innerHTML = response.text().await()
        document.body!!.appendChild(root)

        val leadingWhitespace = root.firstChild
        val trailingWhitespace = root.lastChild
        val serverStyle = document.getElementById(SSR_HYDRATION_STYLE_ID)
        val serverScript = document.getElementById(SSR_HYDRATION_SCRIPT_ID)
        val serverButton = document.getElementById(SSR_HYDRATION_BUTTON_ID) as? HTMLElement
        val serverCount = document.getElementById(SSR_HYDRATION_COUNT_ID)
        val serverRenderedAt = document.getElementById(SSR_HYDRATION_RENDERED_AT_ID)
        assertNotNull(serverStyle)
        assertNotNull(serverScript)
        assertNotNull(serverButton)
        assertNotNull(serverCount)
        assertNotNull(serverRenderedAt)
        assertEquals(
            "Rendered at $SSR_HYDRATION_SERVER_RENDERED_AT",
            serverRenderedAt.textContent,
        )

        var count by mutableStateOf(0)
        val composition = hydrateComposable(root) {
            SsrHydrationContent(
                count = count,
                renderedAt = SSR_HYDRATION_CLIENT_RENDERED_AT,
                increment = { count++ },
            )
        }

        try {
            assertEquals(5, root.childNodes.length)
            assertNull(leadingWhitespace?.parentNode)
            assertNull(trailingWhitespace?.parentNode)
            assertSame(serverStyle, document.getElementById(SSR_HYDRATION_STYLE_ID))
            assertSame(serverScript, document.getElementById(SSR_HYDRATION_SCRIPT_ID))
            assertEquals(SSR_HYDRATION_NORMALIZED_SCRIPT_CONTENT, serverScript.textContent)
            assertSame(serverButton, document.getElementById(SSR_HYDRATION_BUTTON_ID))
            assertSame(serverCount, document.getElementById(SSR_HYDRATION_COUNT_ID))
            assertEquals("Count: 0", serverCount.textContent)
            assertEquals("rgb(255, 0, 0)", window.getComputedStyle(serverCount).color)
            // The allowed mismatch replaces the server timestamp, keeping its element.
            assertSame(serverRenderedAt, document.getElementById(SSR_HYDRATION_RENDERED_AT_ID))
            assertEquals(
                "Rendered at $SSR_HYDRATION_CLIENT_RENDERED_AT",
                serverRenderedAt.textContent,
            )
            assertEquals(
                SSR_HYDRATION_CLIENT_RENDERED_AT,
                serverRenderedAt.getAttribute("data-rendered-at"),
            )

            serverButton.click()
            delay(100.milliseconds) // test fails without delau

            assertEquals("Count: 1", serverCount.textContent)
            assertEquals("rgb(0, 128, 0)", window.getComputedStyle(serverCount).color)
            assertSame(serverStyle, document.getElementById(SSR_HYDRATION_STYLE_ID))
            assertSame(serverScript, document.getElementById(SSR_HYDRATION_SCRIPT_ID))
            assertSame(serverButton, document.getElementById(SSR_HYDRATION_BUTTON_ID))
            assertSame(serverCount, document.getElementById(SSR_HYDRATION_COUNT_ID))
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
        }
    }
}
