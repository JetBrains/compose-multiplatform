package org.jetbrains.compose.web

import kotlinx.browser.toJsString
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.parsing.DOMParser
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class JvmSsrHydrationStateTest {
    // Fetches a JVM-rendered document, hydrates it, and verifies node adoption and client updates.
    @Test
    fun jvmStateHydratesWithoutClientAccessToItsSource() = MainScope().promise {
        val fixtureHtml = fetchHydrationFixtureText(SSR_HYDRATION_STATE_FIXTURE_URL)
        assertContains(fixtureHtml, "Loaded by JVM &lt;backend&gt;: 41")
        assertContains(fixtureHtml, "\"label\":\"Loaded by JVM &lt;backend>\"")

        val parsed = DOMParser().parseFromString(fixtureHtml, "text/html".toJsString())

        val root = assertNotNull(
            parsed.querySelector("[$HydrationRootAttribute]") as? HTMLElement,
        )
        val serverButton = assertNotNull(
            parsed.getElementById(SSR_HYDRATION_STATE_BUTTON_ID) as? HTMLElement,
        )
        val serverValue = assertNotNull(parsed.getElementById(SSR_HYDRATION_STATE_VALUE_ID))
        val serverState = assertNotNull(
            parsed.querySelector("[$HydrationStateAttribute]") as? HTMLElement,
        )

        val composition = hydrateRoot(
            deserializeState = ::decodeSsrHydrationState,
            within = parsed,
            onHydrationMismatch = { throw it },
        ) { initialState ->
            SsrHydrationStateApplication(initialState)
        }

        try {
            assertSame(root, parsed.querySelector("[$HydrationRootAttribute]"))
            assertSame(serverButton, parsed.getElementById(SSR_HYDRATION_STATE_BUTTON_ID))
            assertSame(serverValue, parsed.getElementById(SSR_HYDRATION_STATE_VALUE_ID))
            assertSame(serverState, parsed.querySelector("[$HydrationStateAttribute]"))
            assertEquals("Loaded by JVM <backend>: 41", serverValue.textContent)

            serverButton.click()
            delay(100.milliseconds)

            assertEquals("Loaded by JVM <backend>: 42", serverValue.textContent)
            assertSame(serverButton, parsed.getElementById(SSR_HYDRATION_STATE_BUTTON_ID))
            assertSame(serverValue, parsed.getElementById(SSR_HYDRATION_STATE_VALUE_ID))
            assertSame(serverState, parsed.querySelector("[$HydrationStateAttribute]"))
        } finally {
            composition.dispose()
        }
    }

    private fun decodeSsrHydrationState(json: String): SsrHydrationState {
        val decoded = decodeHydrationTestState(json)
        return SsrHydrationState(
            label = decoded.label,
            count = decoded.count,
        )
    }
}
