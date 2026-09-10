package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.parsing.DOMParser
import kotlin.js.JSON
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class HydrationStateJsTest {
    // Hydrates state, clicks the adopted button, and checks node identity after recomposition.
    @Test
    fun stateDrivesHydrationAndExistingNodesAreAdopted() = MainScope().promise {
        val initialState = TestState(label = "Loaded </script>", count = 41)
        val fixture = installDocument(initialState, TestState::toJson) { state ->
            CounterContent(state.label, state.count, increment = {})
        }
        val serverButton = fixture.root.firstChild
        val serverValue = fixture.root.lastChild
        val serverState = fixture.state
        var decodeCount = 0

        val composition = hydrateRoot(
            deserializeState = { json ->
                decodeCount++
                decodeTestState(json)
            },
            within = fixture.host,
            onHydrationMismatch = { throw it },
        ) { state ->
            var count by remember(state) { mutableStateOf(state.count) }
            CounterContent(state.label, count) { count++ }
        }

        try {
            assertEquals(1, decodeCount)
            assertSame(serverButton, fixture.root.firstChild)
            assertSame(serverValue, fixture.root.lastChild)
            assertSame(serverState, fixture.host.querySelector("[$HydrationStateAttribute]"))
            assertEquals("IncrementLoaded </script>: 41", fixture.root.textContent)

            (serverButton as HTMLElement).click()
            delay(100.milliseconds)

            assertEquals("IncrementLoaded </script>: 42", fixture.root.textContent)
            assertSame(serverButton, fixture.root.firstChild)
            assertSame(serverValue, fixture.root.lastChild)
        } finally {
            composition.dispose()
        }
    }

    // Reads scopeElement in a client effect to verify that the receiver is the discovered root.
    @Test
    fun clientContentReceivesTheHydrationRootElementScope() {
        val fixture = installDocument("content", { it }) { value ->
            Span { Text(value) }
        }
        var receivedRoot: HTMLDivElement? = null

        val composition = hydrateRoot(
            deserializeState = { it },
            within = fixture.host,
            onHydrationMismatch = { throw it },
        ) { value ->
            DisposableEffect(Unit) {
                receivedRoot = scopeElement
                onDispose {}
            }
            Span { Text(value) }
        }

        try {
            assertSame(fixture.root, receivedRoot)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun discoversProtocolElementsWithinTheDocumentByDefault() {
        val fixture = installDocument("document state", { it }) { value ->
            Span { Text(value) }
        }
        val host = document.createElement("div")
        host.appendChild(fixture.root)
        host.appendChild(fixture.state)
        assertNotNull(document.body).appendChild(host)

        try {
            val composition = hydrateRoot(
                deserializeState = { it },
                onHydrationMismatch = { throw it },
            ) { value ->
                Span { Text(value) }
            }

            try {
                assertEquals("document state", fixture.root.textContent)
            } finally {
                composition.dispose()
            }
        } finally {
            host.parentNode?.removeChild(host)
        }
    }

    // Forces a text mismatch and verifies that fallback replaces only the root's content.
    @Test
    fun mismatchFallbackChangesOnlyTheHydrationRoot() {
        val fixture = installDocument(Unit, { "null" }) {
            Span { Text("Server") }
        }
        val serverSpan = fixture.root.firstChild
        val serverState = fixture.state
        var reportedMismatch: HydrationMismatchException? = null

        val composition = hydrateRoot(
            deserializeState = { Unit },
            within = fixture.host,
            onHydrationMismatch = { reportedMismatch = it },
        ) {
            Span { Text("Client") }
        }

        try {
            assertNotNull(reportedMismatch)
            assertEquals("Client", fixture.root.textContent)
            assertNotSame(serverSpan, fixture.root.firstChild)
            assertSame(serverState, fixture.host.querySelector("[$HydrationStateAttribute]"))
        } finally {
            composition.dispose()
        }
    }

    // Inserts an unrelated node between protocol elements to verify that discovery still hydrates.
    @Test
    fun anUnrelatedNodeBetweenProtocolElementsIsAllowed() {
        val fixture = installDocument("state", { it }) { state ->
            Span { Text(state) }
        }
        fixture.host.insertBefore(document.createComment("injected"), fixture.state)

        val composition = hydrateRoot(
            deserializeState = { it },
            within = fixture.host,
            onHydrationMismatch = { throw it },
        ) { state ->
            Span { Text(state) }
        }

        try {
            assertEquals("state", fixture.root.textContent)
        } finally {
            composition.dispose()
        }
    }

    // Removes or clones the root and checks that validation stops before state decoding.
    @Test
    fun missingOrDuplicateRootFailsBeforeDeserialization() {
        val missing = installDocument("state", { it }) {}
        missing.root.parentNode?.removeChild(missing.root)
        var missingDecoded = false
        val failure = assertFailsWith<HydrationStateException> {
            hydrateRoot(
                deserializeState = {
                    missingDecoded = true
                    it
                },
                within = missing.host,
            ) {}
        }
        assertContains(failure.message.orEmpty(), "defer its bootstrap script")
        assertFalse(missingDecoded)

        val duplicate = installDocument("state", { it }) {}
        duplicate.host.appendChild(duplicate.root.cloneNode(deep = true))
        var duplicateDecoded = false
        assertFailsWith<HydrationStateException> {
            hydrateRoot(
                deserializeState = {
                    duplicateDecoded = true
                    it
                },
                within = duplicate.host,
            ) {}
        }
        assertFalse(duplicateDecoded)
    }

    // Removes or clones the state element and verifies that the server root remains untouched.
    @Test
    fun missingOrDuplicateStateFailsWithoutChangingTheRoot() {
        val missing = installDocument("state", { it }) { Span { Text("Server") } }
        val missingServerNode = missing.root.firstChild
        missing.state.parentNode?.removeChild(missing.state)
        assertFailsWith<HydrationStateException> {
            hydrateRoot(
                deserializeState = { it },
                within = missing.host,
            ) { Span { Text("Client") } }
        }
        assertSame(missingServerNode, missing.root.firstChild)

        val duplicate = installDocument("state", { it }) { Span { Text("Server") } }
        val duplicateServerNode = duplicate.root.firstChild
        duplicate.host.appendChild(duplicate.state.cloneNode(deep = true))
        assertFailsWith<HydrationStateException> {
            hydrateRoot(
                deserializeState = { it },
                within = duplicate.host,
            ) { Span { Text("Client") } }
        }
        assertSame(duplicateServerNode, duplicate.root.firstChild)
    }

    // Mutates state placement, type, format, and tag to verify that malformed shapes fail safely.
    @Test
    fun malformedStateElementFailsWithoutChangingTheRoot() {
        val mutations: List<(Fixture) -> Unit> = listOf(
            { fixture -> fixture.root.appendChild(fixture.state) },
            { fixture -> fixture.state.setAttribute("type", "application/json") },
            { fixture -> fixture.state.setAttribute(HydrationStateAttribute, "unknown-v2") },
            { fixture ->
                val replacement = document.createElement("div")
                replacement.setAttribute(HydrationStateAttribute, HydrationStateFormat)
                replacement.setAttribute("type", "text/plain")
                fixture.state.parentNode?.replaceChild(replacement, fixture.state)
            },
        )

        mutations.forEach { mutate ->
            val fixture = installDocument("state", { it }) { Span { Text("Server") } }
            val serverNode = fixture.root.firstChild
            mutate(fixture)
            assertFailsWith<HydrationStateException> {
                hydrateRoot(
                    deserializeState = { it },
                    within = fixture.host,
                ) { Span { Text("Client") } }
            }
            assertSame(serverNode, fixture.root.firstChild)
        }
    }

    // Throws from the decoder and verifies that neither mismatch fallback nor DOM mutation runs.
    @Test
    fun decoderFailureLeavesTheServerDomUntouched() {
        val fixture = installDocument("not valid", { it }) { Span { Text("Server") } }
        val serverNode = fixture.root.firstChild
        var mismatchCalled = false
        val decoderFailure = IllegalStateException("Cannot decode")

        val failure = assertFailsWith<HydrationStateException> {
            hydrateRoot<String>(
                deserializeState = { throw decoderFailure },
                within = fixture.host,
                onHydrationMismatch = { mismatchCalled = true },
            ) { Span { Text("Client") } }
        }

        assertSame(decoderFailure, failure.cause)
        assertFalse(mismatchCalled)
        assertSame(serverNode, fixture.root.firstChild)
        assertEquals("Server", fixture.root.textContent)
    }

    @Test
    fun arbitraryTextFormatRoundTripsThroughTheBrowserParser() {
        val serialized = "<state value=\"&lt;\"></script>\r\u0000</state>"
        val fixture = installDocument(serialized, { it }) {}
        var decoded: String? = null

        val composition = hydrateRoot(
            deserializeState = { it.also { decoded = it } },
            within = fixture.host,
            onHydrationMismatch = { throw it },
        ) {}

        try {
            assertEquals(serialized, decoded)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun textPlainMimeTypeParametersAreAccepted() {
        val fixture = installDocument("state", { it }) {}
        fixture.state.setAttribute("type", "Text/Plain; charset=utf-8")

        val composition = hydrateRoot(
            deserializeState = { it },
            within = fixture.host,
            onHydrationMismatch = { throw it },
        ) {}

        composition.dispose()
    }

    @Composable
    private fun CounterContent(label: String, count: Int, increment: () -> Unit) {
        Button(attrs = { onClick { increment() } }) { Text("Increment") }
        Span { Text("$label: $count") }
    }

    private fun <T> installDocument(
        initialState: T,
        serializeState: (T) -> String,
        content: @Composable ElementScope<HTMLDivElement>.(T) -> Unit,
    ): Fixture {
        val rendered = renderHydratedDocument {
            Html {
                Head { Title { Text("Hydration test") } }
                Body {
                    HydrationRoot(
                        initialState = initialState,
                        serializeState = serializeState,
                        content = content,
                    )
                }
            }
        }
        val parsed = DOMParser().parseFromString(rendered, "text/html")
        val host = assertNotNull(parsed.body)
        assertEquals("Hydration test", parsed.title)
        return Fixture(
            host = host,
            root = assertNotNull(
                host.querySelector("[$HydrationRootAttribute]") as? HTMLDivElement
            ),
            state = assertNotNull(host.querySelector("[$HydrationStateAttribute]")),
        )
    }

    private data class Fixture(
        val host: HTMLElement,
        val root: HTMLDivElement,
        val state: Element,
    )

    private data class TestState(
        val label: String,
        val count: Int,
    ) {
        fun toJson(): String =
            """{"label":"${label.replace("\"", "\\\"")}","count":$count}"""
    }

    private fun decodeTestState(json: String): TestState {
        val parsed = JSON.parse<dynamic>(json)
        assertTrue(parsed.label is String)
        assertTrue(parsed.count is Int)
        return TestState(
            label = parsed.label as String,
            count = parsed.count as Int,
        )
    }
}
