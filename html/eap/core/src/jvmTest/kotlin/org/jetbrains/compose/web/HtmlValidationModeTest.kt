package org.jetbrains.compose.web

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HtmlValidationModeTest {
    @Test
    fun acceptsCommonBooleanEnvironmentValues() {
        listOf(null, "", "  ", "false", " FALSE ", "0").forEach {
            assertEquals(HtmlValidationMode.Fast, parseHtmlValidationMode(it))
        }
        listOf("true", " TRUE ", "1").forEach {
            assertEquals(HtmlValidationMode.Strict, parseHtmlValidationMode(it))
        }
        val failure = assertFailsWith<IllegalArgumentException> {
            parseHtmlValidationMode("yes")
        }
        assertContains(failure.message.orEmpty(), "COMPOSE_HTML_VALIDATE_STRICTLY")
    }
}
