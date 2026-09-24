package org.jetbrains.compose.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtmlValidationModeJsTest {
    @Test
    fun nodeSettingReadsTheEnvironmentAndBrowserDefaultsToFast() {
        val global: dynamic = js("globalThis")
        val previousProcess = global.process
        try {
            global.process = js("undefined")
            assertNull(nodeHtmlValidationSetting())

            global.process = js("({ env: { COMPOSE_HTML_VALIDATE_STRICTLY: 'true' } })")
            assertNull(nodeHtmlValidationSetting())

            global.process = js("({ versions: { node: 'test' }, env: { COMPOSE_HTML_VALIDATE_STRICTLY: ' true ' } })")
            assertEquals(" true ", nodeHtmlValidationSetting())
            assertEquals(HtmlValidationMode.Strict, parseHtmlValidationMode(nodeHtmlValidationSetting()))
        } finally {
            global.process = previousProcess
        }
    }
}
