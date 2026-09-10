package org.jetbrains.compose.web

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.ScriptType

internal const val HydrationRootAttribute = "data-compose-hydration-root"
internal const val HydrationStateAttribute = "data-compose-hydration-state"
internal const val HydrationStateFormat = "escaped-text-v1"
internal val HydrationStateMimeType: String = ScriptType.TextPlain.typeStr
internal val HydrationProtocolAttributes = setOf(
    HydrationRootAttribute,
    HydrationStateAttribute,
)

internal fun <TElement : Element> AttrsScope<TElement>.hydrationProtocolAttr(
    name: String,
    value: String,
) {
    val builder = this as? AttrsScopeBuilder<TElement>
        ?: error("Hydration protocol attributes require an AttrsScopeBuilder")
    builder.hydrationProtocolAttr(name, value)
}

// Script elements are raw-text elements, so the browser does not decode character references.
internal fun String.escapeForHydrationStateElement(): String = buildString {
    this@escapeForHydrationStateElement.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '\r' -> append("&#13;")
            '\u0000' -> append("&#0;")
            else -> append(character)
        }
    }
}

internal fun String.unescapeFromHydrationStateElement(): String =
    replace("&lt;", "<")
        .replace("&#13;", "\r")
        .replace("&#0;", "\u0000")
        .replace("&amp;", "&")
