package org.jetbrains.compose.web

import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class HydrationDataTest {
    @Test
    fun transportEscapingRoundTripsAdversarialText() {
        val values = listOf(
            "",
            "plain text",
            "<",
            "</script>",
            "&lt;",
            "&amp;lt;",
            "&&<<",
            "\r",
            "\u0000",
            "\r\n",
            "<value>&lt;</script>\r\u0000</value>",
        )

        values.forEach { value ->
            assertEquals(
                value,
                value.escapeForHydrationDataElement().unescapeFromHydrationDataElement(),
                value,
            )
        }
    }

    @Test
    fun rendersContentAndReadableSerializedDataWithTheDefaultId() {
        val data = TestData(label = "JVM data", count = 42)

        val rendered = composeHtmlToString(
            data = data,
            serializeData = TestData::toJson,
        ) { initialData ->
            Text("${initialData.label}: ${initialData.count}")
        }

        assertEquals("JVM data: 42", rendered.content)
        assertEquals(
            "<script id=\"__COMPOSE_HYDRATION_DATA__\" type=\"text/plain\" " +
                "data-compose-hydration=\"escaped-text-v1\">" +
                "{\"label\":\"JVM data\",\"count\":42}</script>",
            rendered.hydrationDataElement,
        )
    }

    @Test
    fun supportsACustomHydrationDataId() {
        val rendered = composeHtmlToString(
            data = Unit,
            serializeData = { "null" },
            hydrationDataId = "secondary-root_data",
        ) {}

        assertEquals(
            "<script id=\"secondary-root_data\" type=\"text/plain\" " +
                "data-compose-hydration=\"escaped-text-v1\">null</script>",
            rendered.hydrationDataElement,
        )
    }

    @Test
    fun safelyAndReversiblyEscapesArbitrarySerializedText() {
        val xml = "<value>&lt;</script>\r\u0000</value>"

        val rendered = composeHtmlToString(
            data = xml,
            serializeData = { it },
        ) {}

        assertEquals(
            "<script id=\"__COMPOSE_HYDRATION_DATA__\" type=\"text/plain\" " +
                "data-compose-hydration=\"escaped-text-v1\">" +
                "&lt;value>&amp;lt;&lt;/script>&#13;&#0;&lt;/value></script>",
            rendered.hydrationDataElement,
        )
        assertFalse("</script>\r\u0000" in rendered.hydrationDataElement)
    }

    @Test
    fun rejectsIdsOutsideTheSupportedAsciiSubsetBeforeEncodingOrComposition() {
        val invalidIds = listOf("", "starts with whitespace", "contains.dot", "quote\"", "café")

        invalidIds.forEach { id ->
            var encodeCalled = false
            var contentCalled = false

            assertFailsWith<IllegalArgumentException>(id) {
                composeHtmlToString(
                    data = Unit,
                    serializeData = {
                        encodeCalled = true
                        "null"
                    },
                    hydrationDataId = id,
                ) {
                    contentCalled = true
                }
            }

            assertFalse(encodeCalled, id)
            assertFalse(contentCalled, id)
        }
    }

    private data class TestData(
        val label: String,
        val count: Int,
    ) {
        fun toJson(): String = """{"label":"$label","count":$count}"""
    }
}
