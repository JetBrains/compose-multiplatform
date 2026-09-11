package org.jetbrains.compose.web

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.parsing.DOMParser
import kotlin.js.JSON
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private external interface HydrationStateFixtureResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

private external interface SsrHydrationStateJson {
    val label: String
    val count: Int
}

@JsName("fetch")
private external fun fetchHydrationStateFixture(input: String): Promise<HydrationStateFixtureResponse>

class JvmSsrHydrationStateTest {
    // Fetches a JVM-rendered document, hydrates it, and verifies node adoption and client updates.
    @Test
    fun jvmStateHydratesWithoutClientAccessToItsSource() = MainScope().promise {
        val response = fetchHydrationStateFixture(SSR_HYDRATION_STATE_FIXTURE_URL).await()
        assertTrue(
            response.ok,
            "Fetching $SSR_HYDRATION_STATE_FIXTURE_URL failed with HTTP ${response.status}",
        )
        val fixtureHtml = response.text().await()
        assertContains(fixtureHtml, "Loaded by JVM &lt;backend&gt;: 41")
        assertContains(fixtureHtml, "\"label\":\"Loaded by JVM &lt;backend>\"")

        val parsed = DOMParser().parseFromString(fixtureHtml, "text/html")

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
        val decoded = JSON.parse<SsrHydrationStateJson>(json)
        return SsrHydrationState(
            label = decoded.label,
            count = decoded.count,
        )
    }
}
