package org.jetbrains.compose.web

import androidx.compose.runtime.staticCompositionLocalOf

internal enum class HtmlValidationMode {
    Fast,
    Strict,
}

internal val LocalHtmlValidationMode = staticCompositionLocalOf { HtmlValidationMode.Fast }

internal fun parseHtmlValidationMode(value: String?): HtmlValidationMode =
    when (value?.trim()?.lowercase()) {
        null, "", "false", "0" -> HtmlValidationMode.Fast
        "true", "1" -> HtmlValidationMode.Strict
        else -> throw IllegalArgumentException(
            "COMPOSE_HTML_VALIDATE_STRICTLY must be true, false, 1, or 0, but was \"$value\""
        )
    }

internal expect fun defaultHtmlValidationMode(): HtmlValidationMode
