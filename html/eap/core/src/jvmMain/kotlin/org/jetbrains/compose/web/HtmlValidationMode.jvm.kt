package org.jetbrains.compose.web

private val configuredHtmlValidationMode: Result<HtmlValidationMode> by lazy {
    // Cache an invalid setting too, so a failing request does not reread the environment.
    runCatching { parseHtmlValidationMode(System.getenv("COMPOSE_HTML_VALIDATE_STRICTLY")) }
}

internal actual fun defaultHtmlValidationMode(): HtmlValidationMode =
    configuredHtmlValidationMode.getOrThrow()
