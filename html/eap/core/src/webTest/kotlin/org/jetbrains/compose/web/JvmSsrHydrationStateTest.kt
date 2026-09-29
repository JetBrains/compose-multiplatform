package org.jetbrains.compose.web

import kotlinx.browser.toJsString
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.testutils.fetchTestResourceText
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
        val fixtureHtml = fetchTestResourceText(SSR_HYDRATION_STATE_FIXTURE_URL)
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

    @Test
    fun jvmIslandsHydrateIndependentlyInReverseOrder() = MainScope().promise {
        val fixtureHtml = fetchTestResourceText(SSR_HYDRATION_ISLANDS_FIXTURE_URL)
        val parsed = DOMParser().parseFromString(fixtureHtml, "text/html".toJsString())

        val header = assertNotNull(parsed.getElementById(SSR_HYDRATION_ISLANDS_HEADER_ID))
        val footer = assertNotNull(parsed.getElementById(SSR_HYDRATION_ISLANDS_FOOTER_ID))
        val cartRoot = assertNotNull(parsed.querySelector("[$HydrationRootAttribute=\"$SSR_HYDRATION_CART_ID\"]"))
        val accountRoot = assertNotNull(parsed.querySelector("[$HydrationRootAttribute=\"$SSR_HYDRATION_ACCOUNT_ID\"]"))
        val cartState = assertNotNull(parsed.querySelector("[$HydrationForAttribute=\"$SSR_HYDRATION_CART_ID\"]"))
        val accountState = assertNotNull(parsed.querySelector("[$HydrationForAttribute=\"$SSR_HYDRATION_ACCOUNT_ID\"]"))
        val cartButton = assertNotNull(
            parsed.getElementById(ssrHydrationIslandButtonId(SSR_HYDRATION_CART_ID)) as? HTMLElement,
        )
        val accountButton = assertNotNull(
            parsed.getElementById(ssrHydrationIslandButtonId(SSR_HYDRATION_ACCOUNT_ID)) as? HTMLElement,
        )
        val cartValue = assertNotNull(parsed.getElementById(ssrHydrationIslandValueId(SSR_HYDRATION_CART_ID)))
        val accountValue = assertNotNull(parsed.getElementById(ssrHydrationIslandValueId(SSR_HYDRATION_ACCOUNT_ID)))

        val accountComposition = hydrateRoot(
            deserializeState = ::decodeSsrHydrationState,
            within = parsed,
            hydrationId = SSR_HYDRATION_ACCOUNT_ID,
            onHydrationMismatch = { throw it },
        ) { initialState ->
            SsrHydrationIsland(SSR_HYDRATION_ACCOUNT_ID, initialState)
        }

        try {
            val cartComposition = hydrateRoot(
                deserializeState = ::decodeSsrHydrationState,
                within = parsed,
                hydrationId = SSR_HYDRATION_CART_ID,
                onHydrationMismatch = { throw it },
            ) { initialState ->
                SsrHydrationIsland(SSR_HYDRATION_CART_ID, initialState)
            }

            try {
                assertSame(cartRoot, parsed.querySelector("[$HydrationRootAttribute=\"$SSR_HYDRATION_CART_ID\"]"))
                assertSame(accountRoot, parsed.querySelector("[$HydrationRootAttribute=\"$SSR_HYDRATION_ACCOUNT_ID\"]"))
                assertSame(cartState, parsed.querySelector("[$HydrationForAttribute=\"$SSR_HYDRATION_CART_ID\"]"))
                assertSame(accountState, parsed.querySelector("[$HydrationForAttribute=\"$SSR_HYDRATION_ACCOUNT_ID\"]"))
                assertSame(cartButton, parsed.getElementById(ssrHydrationIslandButtonId(SSR_HYDRATION_CART_ID)))
                assertSame(accountButton, parsed.getElementById(ssrHydrationIslandButtonId(SSR_HYDRATION_ACCOUNT_ID)))
                assertSame(cartValue, parsed.getElementById(ssrHydrationIslandValueId(SSR_HYDRATION_CART_ID)))
                assertSame(accountValue, parsed.getElementById(ssrHydrationIslandValueId(SSR_HYDRATION_ACCOUNT_ID)))
                assertEquals("Cart loaded by JVM: 41", cartValue.textContent)
                assertEquals("Account loaded by JVM: 7", accountValue.textContent)

                cartButton.click()
                delay(100.milliseconds)
                assertEquals("Cart loaded by JVM: 42", cartValue.textContent)
                assertEquals("Account loaded by JVM: 7", accountValue.textContent)

                accountButton.click()
                delay(100.milliseconds)
                assertEquals("Cart loaded by JVM: 42", cartValue.textContent)
                assertEquals("Account loaded by JVM: 8", accountValue.textContent)
                assertSame(header, parsed.getElementById(SSR_HYDRATION_ISLANDS_HEADER_ID))
                assertSame(footer, parsed.getElementById(SSR_HYDRATION_ISLANDS_FOOTER_ID))
            } finally {
                cartComposition.dispose()
            }
        } finally {
            accountComposition.dispose()
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
