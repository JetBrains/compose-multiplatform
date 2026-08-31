package org.jetbrains.compose.web

import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.js.JSON
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HydrationDataJsTest {
    @Test
    fun arbitraryTextFormatRoundTripsWithoutJson() {
        val serializedXml = "<data value=\"&lt;\"></script>\r\u0000</data>"
        val rendered = composeHtmlToString(
            data = Unit,
            serializeData = { serializedXml },
        ) {
            Span { Text("Server") }
        }
        val fixture = installFixture(rendered)
        var deserialized = false

        val composition = hydrateComposable(
            root = fixture.root,
            deserializeData = { serializedData ->
                assertEquals(serializedXml, serializedData)
                deserialized = true
            },
        ) {
            Span { Text("Server") }
        }

        try {
            assertTrue(deserialized)
            assertEquals("Server", fixture.root.textContent)
        } finally {
            composition.dispose()
            fixture.remove()
        }
    }

    @Test
    fun deserializedDataDrivesHydrationAndRemainsAvailableForDebugging() {
        val data = TestData(label = "</script> survives", count = 42)
        val rendered = composeHtmlToString(
            data = data,
            serializeData = TestData::toJson,
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }
        val fixture = installFixture(rendered)
        val serverSpan = fixture.root.firstChild
        var decodeCount = 0

        val composition = hydrateComposable(
            root = fixture.root,
            deserializeData = { json ->
                decodeCount++
                decodeTestData(json)
            },
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }

        try {
            assertEquals(1, decodeCount)
            assertSame(serverSpan, fixture.root.firstChild)
            assertEquals("</script> survives: 42", fixture.root.textContent)
            assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            composition.dispose()
            fixture.remove()
        }
    }

    @Test
    fun mismatchFallbackReusesDecodedDataAndKeepsThePayload() {
        val data = TestData(label = "Transferred", count = 7)
        val rendered = composeHtmlToString(
            data = data,
            serializeData = TestData::toJson,
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }
        val fixture = installFixture(rendered)
        val serverSpan = fixture.root.firstChild
        fixture.root.appendChild(document.createElement("div"))
        var mismatch: HydrationMismatchException? = null

        val composition = hydrateComposable(
            root = fixture.root,
            deserializeData = ::decodeTestData,
            onHydrationMismatch = { mismatch = it },
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }

        try {
            assertNotNull(mismatch)
            assertNotSame(serverSpan, fixture.root.firstChild)
            assertEquals("Transferred: 7", fixture.root.textContent)
            assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            composition.dispose()
            fixture.remove()
        }
    }

    @Test
    fun missingPayloadThrowsBeforeCompositionWithoutUsingMismatchRecovery() {
        val fixture = installRawFixture("<span>Server</span>")
        val serverHtml = fixture.root.innerHTML
        var decodeCalled = false
        var contentCalled = false
        var mismatchCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {
                        decodeCalled = true
                    },
                    onHydrationMismatch = { mismatchCalled = true },
                ) {
                    contentCalled = true
                }
            }

            assertContains(failure.message.orEmpty(), "No hydration data element")
            assertFalse(decodeCalled)
            assertFalse(contentCalled)
            assertFalse(mismatchCalled)
            assertEquals(serverHtml, fixture.root.innerHTML)
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun decoderFailureIsWrappedBeforeCompositionAndPreservesThePayload() {
        val rendered = composeHtmlToString(
            data = Unit,
            serializeData = { "null" },
        ) {
            Span { Text("Server") }
        }
        val fixture = installFixture(rendered)
        val serverHtml = fixture.root.innerHTML
        val decoderFailure = IllegalArgumentException("invalid application schema")
        var contentCalled = false
        var mismatchCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = { throw decoderFailure },
                    onHydrationMismatch = { mismatchCalled = true },
                ) {
                    contentCalled = true
                }
            }

            assertSame(decoderFailure, failure.cause)
            assertFalse(contentCalled)
            assertFalse(mismatchCalled)
            assertEquals(serverHtml, fixture.root.innerHTML)
            assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun unsupportedPayloadFormatThrowsBeforeDecoding() {
        val fixture = installRawFixture(
            rootHtml = "<span>Server</span>",
            dataElement = "<script id=\"$DEFAULT_HYDRATION_DATA_ID\" " +
                "type=\"text/plain\" data-compose-hydration=\"escaped-text-v2\">null</script>",
        )
        var decodeCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {
                        decodeCalled = true
                    },
                ) {}
            }

            assertContains(failure.message.orEmpty(), "unsupported format")
            assertFalse(decodeCalled)
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun payloadInsideHydrationRootIsRejectedBeforeComposition() {
        val dataElement = hydrationDataElement(DEFAULT_HYDRATION_DATA_ID, "null")
        val fixture = installRawFixture("<span>Server</span>$dataElement")
        val serverHtml = fixture.root.innerHTML
        var contentCalled = false
        var mismatchCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {},
                    onHydrationMismatch = { mismatchCalled = true },
                ) {
                    contentCalled = true
                }
            }

            assertContains(failure.message.orEmpty(), "must be outside the hydration root")
            assertFalse(contentCalled)
            assertFalse(mismatchCalled)
            assertEquals(serverHtml, fixture.root.innerHTML)
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun textPlainMimeTypeParametersAreAccepted() {
        val dataElement = hydrationDataElement(DEFAULT_HYDRATION_DATA_ID, "null")
            .replace("type=\"text/plain\"", "type=\"Text/Plain; charset=utf-8\"")
        val fixture = installRawFixture(rootHtml = "", dataElement = dataElement)

        val composition = hydrateComposable(
            fixture.root,
            {},
        ) {}

        try {
            assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            composition.dispose()
            fixture.remove()
        }
    }

    @Test
    fun mimeTypesOtherThanTextPlainAreRejectedBeforeDecoding() {
        val dataElement = hydrationDataElement(DEFAULT_HYDRATION_DATA_ID, "null")
            .replace("type=\"$HydrationDataMimeType\"", "type=\"application/json\"")
        val fixture = installRawFixture(
            rootHtml = "<span>Server</span>",
            dataElement = dataElement,
        )
        var decodeCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {
                        decodeCalled = true
                    },
                ) {}
            }

            assertContains(failure.message.orEmpty(), "must have type \"$HydrationDataMimeType\"")
            assertFalse(decodeCalled)
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun nonScriptPayloadElementsAreRejectedBeforeDecoding() {
        val dataElement = hydrationDataElement(DEFAULT_HYDRATION_DATA_ID, "null")
            .replace("script", "div")
        val fixture = installRawFixture(
            rootHtml = "<span>Server</span>",
            dataElement = dataElement,
        )
        var decodeCalled = false

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {
                        decodeCalled = true
                    },
                ) {}
            }

            assertContains(failure.message.orEmpty(), "must be a <script>")
            assertFalse(decodeCalled)
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun duplicatePayloadIdsAreRejected() {
        val dataElement = hydrationDataElement(DEFAULT_HYDRATION_DATA_ID, "null")
        val fixture = installRawFixture(
            rootHtml = "<span>Server</span>",
            dataElement = dataElement + dataElement,
        )

        try {
            val failure = assertFailsWith<HydrationDataException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {},
                ) {}
            }

            assertContains(failure.message.orEmpty(), "but found 2")
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun unsupportedIdsAreRejectedBeforeTheDocumentIsQueried() {
        val rendered = composeHtmlToString(
            data = Unit,
            serializeData = { "null" },
        ) {
            Span { Text("Server") }
        }
        val fixture = installFixture(rendered)
        val serverHtml = fixture.root.innerHTML
        var decodeCalled = false
        var contentCalled = false
        var mismatchCalled = false

        try {
            val failure = assertFailsWith<IllegalArgumentException> {
                hydrateComposable(
                    root = fixture.root,
                    deserializeData = {
                        decodeCalled = true
                    },
                    hydrationDataId = "invalid id\"]",
                    onHydrationMismatch = { mismatchCalled = true },
                ) {
                    contentCalled = true
                }
            }

            assertContains(failure.message.orEmpty(), "Hydration data id must match")
            assertFalse(decodeCalled)
            assertFalse(contentCalled)
            assertFalse(mismatchCalled)
            assertEquals(serverHtml, fixture.root.innerHTML)
            assertNotNull(document.getElementById(DEFAULT_HYDRATION_DATA_ID))
        } finally {
            fixture.remove()
        }
    }

    @Test
    fun separateRootsHydrateFromTheirOwnPayloads() {
        val firstId = "first-root_data"
        val secondId = "second-root_data"
        val first = composeHtmlToString(
            data = TestData(label = "First", count = 1),
            serializeData = TestData::toJson,
            hydrationDataId = firstId,
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }
        val second = composeHtmlToString(
            data = TestData(label = "Second", count = 2),
            serializeData = TestData::toJson,
            hydrationDataId = secondId,
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }

        val host = document.createElement("div") as HTMLElement
        host.innerHTML =
            "<div id=\"first-hydration-data-root\">${first.content}</div>" +
                first.hydrationDataElement +
                "<div id=\"second-hydration-data-root\">${second.content}</div>" +
                second.hydrationDataElement
        document.body!!.appendChild(host)

        val firstRoot = assertNotNull(
            document.getElementById("first-hydration-data-root") as? HTMLElement,
        )
        val secondRoot = assertNotNull(
            document.getElementById("second-hydration-data-root") as? HTMLElement,
        )
        val firstSpan = firstRoot.firstChild
        val secondSpan = secondRoot.firstChild
        val decoded = mutableListOf<String>()
        var mismatch: HydrationMismatchException? = null

        val firstComposition = hydrateComposable(
            root = firstRoot,
            deserializeData = { json ->
                decoded += json
                decodeTestData(json)
            },
            hydrationDataId = firstId,
            onHydrationMismatch = { mismatch = it },
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }
        val secondComposition = hydrateComposable(
            root = secondRoot,
            deserializeData = { json ->
                decoded += json
                decodeTestData(json)
            },
            hydrationDataId = secondId,
            onHydrationMismatch = { mismatch = it },
        ) { initialData ->
            Span { Text("${initialData.label}: ${initialData.count}") }
        }

        try {
            assertNull(mismatch)
            assertEquals(
                listOf("""{"label":"First","count":1}""", """{"label":"Second","count":2}"""),
                decoded,
            )
            assertSame(firstSpan, firstRoot.firstChild)
            assertSame(secondSpan, secondRoot.firstChild)
            assertEquals("First: 1", firstRoot.textContent)
            assertEquals("Second: 2", secondRoot.textContent)
            assertNotNull(document.getElementById(firstId))
            assertNotNull(document.getElementById(secondId))
        } finally {
            firstComposition.dispose()
            secondComposition.dispose()
            host.parentNode?.removeChild(host)
        }
    }

    private fun installFixture(rendered: HydratableHtml): Fixture = installRawFixture(
        rootHtml = rendered.content,
        dataElement = rendered.hydrationDataElement,
    )

    private fun installRawFixture(
        rootHtml: String,
        dataElement: String = "",
    ): Fixture {
        val host = document.createElement("div") as HTMLElement
        host.innerHTML = "<div id=\"hydration-data-test-root\">$rootHtml</div>$dataElement"
        document.body!!.appendChild(host)
        val root = document.getElementById("hydration-data-test-root") as? HTMLElement
        return Fixture(host, assertNotNull(root))
    }

    private data class Fixture(
        val host: HTMLElement,
        val root: HTMLElement,
    ) {
        fun remove() {
            host.parentNode?.removeChild(host)
        }
    }

    private data class TestData(
        val label: String,
        val count: Int,
    ) {
        fun toJson(): String =
            """{"label":"${label.replace("\"", "\\\"")}","count":$count}"""
    }

    private fun decodeTestData(json: String): TestData {
        val parsed = JSON.parse<dynamic>(json)
        assertTrue(parsed.label is String)
        assertTrue(parsed.count is Int)
        return TestData(
            label = parsed.label as String,
            count = parsed.count as Int,
        )
    }
}
