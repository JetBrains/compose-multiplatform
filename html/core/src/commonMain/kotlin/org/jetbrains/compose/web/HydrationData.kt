package org.jetbrains.compose.web

import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script

const val DEFAULT_HYDRATION_DATA_ID: String = "__COMPOSE_HYDRATION_DATA__"

/**
 * HTML produced for a composition and the serialized data required to hydrate it.
 *
 * [content] belongs inside the hydration root. [hydrationDataElement] must be emitted outside that
 * root, normally immediately after it. The element is intentionally kept after hydration so its
 * serialized data remains available for debugging.
 */
class HydratableHtml internal constructor(
    val content: String,
    val hydrationDataElement: String,
)

/** Indicates that hydration data could not be located, validated, or deserialized. */
class HydrationDataException internal constructor(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal const val HydrationDataAttribute = "data-compose-hydration"
internal const val HydrationDataFormat = "escaped-text-v1"
internal val HydrationDataMimeType: String = ScriptType.TextPlain.typeStr

private val HydrationDataIdPattern = Regex("[A-Za-z_][A-Za-z0-9_-]{0,127}")

internal fun requireValidHydrationDataId(id: String) {
    require(HydrationDataIdPattern.matches(id)) {
        "Hydration data id must match ${HydrationDataIdPattern.pattern}: \"$id\""
    }
}

internal fun hydrationDataElement(id: String, serializedData: String): String {
    requireValidHydrationDataId(id)

    return composeHtmlToString(hydratable = false) {
        Script(
            content = InlineScript(serializedData.escapeForHydrationDataElement()),
            attrs = {
                id(id)
                type(ScriptType.TextPlain)
                attr(HydrationDataAttribute, HydrationDataFormat)
            },
        )
    }
}

// Script elements are raw-text elements, so character references are not decoded by the browser.
internal fun String.escapeForHydrationDataElement(): String = buildString {
    this@escapeForHydrationDataElement.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '\r' -> append("&#13;")
            '\u0000' -> append("&#0;")
            else -> append(character)
        }
    }
}

internal fun String.unescapeFromHydrationDataElement(): String =
    replace("&lt;", "<")
        .replace("&#13;", "\r")
        .replace("&#0;", "\u0000")
        .replace("&amp;", "&")
