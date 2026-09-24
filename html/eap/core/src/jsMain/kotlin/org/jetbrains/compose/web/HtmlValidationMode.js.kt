package org.jetbrains.compose.web

private val configuredHtmlValidationMode: Result<HtmlValidationMode> by lazy {
    // Cache an invalid setting too, so a failing request does not reread the environment.
    runCatching { parseHtmlValidationMode(nodeHtmlValidationSetting()) }
}

internal actual fun defaultHtmlValidationMode(): HtmlValidationMode =
    configuredHtmlValidationMode.getOrThrow()

internal fun nodeHtmlValidationSetting(): String? {
    val setting: dynamic = js("typeof process === 'object' && process.versions && process.versions.node ? process.env.COMPOSE_HTML_VALIDATE_STRICTLY : undefined")
    return if (setting == js("undefined")) null else setting as String
}
