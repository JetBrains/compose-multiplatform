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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private external interface NumberFixtureResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

@JsName("fetch")
private external fun fetchNumberFixture(input: String): Promise<NumberFixtureResponse>

class JvmSsrNumberHydrationTest {
    @Test
    fun jvmRenderedNumbersHydrateWithoutAMismatch() = MainScope().promise {
        val response = fetchNumberFixture(SSR_NUMBER_HYDRATION_FIXTURE_URL).await()
        assertTrue(
            response.ok,
            "Fetching $SSR_NUMBER_HYDRATION_FIXTURE_URL failed with HTTP ${response.status}",
        )
        val serverRenderedContent = response.text().await()

        // JVM contract: serialize Floats at binary32 instead of Kotlin's platform spelling.
        assertContains(serverRenderedContent, "width: 33.333332%")
        assertContains(serverRenderedContent, "flex-grow: 33.333332")
        assertContains(serverRenderedContent, "opacity: 0.33333334")
        assertContains(serverRenderedContent, "rgba(0, 0, 0, 0.33333334)")
        assertContains(serverRenderedContent, "width: 3.3333333px")

        val root = document.createElement("div") as HTMLElement
        root.innerHTML = serverRenderedContent
        document.body!!.appendChild(root)

        val serverStyle = document.getElementById(SSR_NUMBER_HYDRATION_STYLE_ID)
        val serverButton = document.getElementById(SSR_NUMBER_HYDRATION_BUTTON_ID) as? HTMLElement
        val serverRuleTarget = document.getElementById(SSR_NUMBER_HYDRATION_RULE_ID)
        val serverInlineTarget = document.getElementById(SSR_NUMBER_HYDRATION_INLINE_ID)
        assertNotNull(serverStyle)
        assertNotNull(serverButton)
        assertNotNull(serverRuleTarget)
        assertNotNull(serverInlineTarget)
        val serverInlineStyle = serverInlineTarget.getAttribute("style")

        var count by mutableStateOf(0)

        // JS contract: hydration compares raw stylesheet and style-attribute strings; different
        // number spelling throws HydrationMismatchException.
        val composition = hydrateComposable(root) {
            SsrNumberHydrationContent(
                count = count,
                increment = { count++ },
            )
        }

        try {
            assertSame(serverStyle, document.getElementById(SSR_NUMBER_HYDRATION_STYLE_ID))
            assertSame(serverButton, document.getElementById(SSR_NUMBER_HYDRATION_BUTTON_ID))
            assertSame(serverRuleTarget, document.getElementById(SSR_NUMBER_HYDRATION_RULE_ID))
            assertSame(serverInlineTarget, document.getElementById(SSR_NUMBER_HYDRATION_INLINE_ID))
            // Hydration verifies the style attribute, it never rewrites it.
            assertEquals(serverInlineStyle, serverInlineTarget.getAttribute("style"))

            serverButton.click()
            delay(100.milliseconds)

            // Recomposition uses binary32-exact 1/4, 100/4 and 10/4.
            assertEquals("Count: 1", serverRuleTarget.textContent)
            assertEquals("0.25", window.getComputedStyle(serverInlineTarget).opacity)
            assertEquals("2.5px", window.getComputedStyle(serverInlineTarget).width)
            assertEquals("0.25", window.getComputedStyle(serverRuleTarget).opacity)
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
        }
    }
}
