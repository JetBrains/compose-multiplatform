package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import kotlin.js.JSON
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private external interface HydrationDataFixtureResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

private external interface SsrHydrationDataJson {
    val label: String
    val count: Int
}

@JsName("fetch")
private external fun fetchHydrationDataFixture(input: String): Promise<HydrationDataFixtureResponse>

class JvmSsrHydrationDataTest {
    @Test
    fun jvmDataHydratesWithoutClientAccessToItsSource() = MainScope().promise {
        val response = fetchHydrationDataFixture(SSR_HYDRATION_DATA_FIXTURE_URL).await()
        assertTrue(
            response.ok,
            "Fetching $SSR_HYDRATION_DATA_FIXTURE_URL failed with HTTP ${response.status}",
        )
        val fixtureHtml = response.text().await()
        assertContains(fixtureHtml, "Loaded by JVM &lt;backend&gt;: 41")
        assertContains(fixtureHtml, "\"label\":\"Loaded by JVM &lt;backend>\"")

        val host = document.createElement("div") as HTMLElement
        host.innerHTML = fixtureHtml
        document.body!!.appendChild(host)

        val root = assertNotNull(
            document.getElementById(SSR_HYDRATION_DATA_ROOT_ID) as? HTMLElement,
        )
        val serverButton = assertNotNull(
            document.getElementById(SSR_HYDRATION_DATA_BUTTON_ID) as? HTMLElement,
        )
        val serverValue = assertNotNull(document.getElementById(SSR_HYDRATION_DATA_VALUE_ID))
        val serverPayload = assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))

        val composition = hydrateComposable(
            root = root,
            deserializeData = ::decodeSsrHydrationData,
            onHydrationMismatch = { throw it },
        ) { initialData ->
            var count by remember(initialData) { mutableStateOf(initialData.count) }
            SsrHydrationDataContent(
                label = initialData.label,
                count = count,
                increment = { count++ },
            )
        }

        try {
            assertSame(serverButton, document.getElementById(SSR_HYDRATION_DATA_BUTTON_ID))
            assertSame(serverValue, document.getElementById(SSR_HYDRATION_DATA_VALUE_ID))
            assertSame(serverPayload, document.getElementById(DEFAULT_HYDRATION_DATA_ID))
            assertEquals("Loaded by JVM <backend>: 41", serverValue.textContent)

            serverButton.click()
            delay(100.milliseconds)

            assertEquals("Loaded by JVM <backend>: 42", serverValue.textContent)
            assertSame(serverButton, document.getElementById(SSR_HYDRATION_DATA_BUTTON_ID))
            assertSame(serverValue, document.getElementById(SSR_HYDRATION_DATA_VALUE_ID))
            assertSame(serverPayload, document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            composition.dispose()
            host.parentNode?.removeChild(host)
        }
    }

    private fun decodeSsrHydrationData(json: String): SsrHydrationData {
        val decoded = JSON.parse<SsrHydrationDataJson>(json)
        return SsrHydrationData(
            label = decoded.label,
            count = decoded.count,
        )
    }
}
