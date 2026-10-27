/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.HydrationAttributePrefix
import org.jetbrains.compose.web.HydrationProtocolAttributes
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.validateClassTokens
import org.jetbrains.compose.web.css.toStyleAttributeValue

internal class StringHtmlAttributes private constructor(
    val byName: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is StringHtmlAttributes && byName == other.byName

    override fun hashCode(): Int = byName.hashCode()

    companion object {
        private val Empty = StringHtmlAttributes(emptyMap())

        fun from(
            attributes: Map<String, String>,
            namespace: String,
            validate: Boolean,
            hydrationProtocolAttributes: Set<String>,
            classAttributeValue: String? = null,
            styleAttributeValue: (() -> String?)? = null,
        ): StringHtmlAttributes {
            if (attributes.isEmpty() && classAttributeValue == null && styleAttributeValue == null) return Empty

            validateStringHtmlAttributes(attributes, namespace, validate, hydrationProtocolAttributes, null)
            val serializedAttributes = linkedMapOf<String, String>().apply { putAll(attributes) }

            if ("class" !in serializedAttributes && classAttributeValue != null) {
                serializedAttributes["class"] = classAttributeValue
            }
            if ("style" !in serializedAttributes) {
                styleAttributeValue?.invoke()?.let { value ->
                    serializedAttributes["style"] = value
                }
            }

            return StringHtmlAttributes(serializedAttributes)
        }
    }
}

internal fun StringBuilder.appendStartTag(tagName: String, namespace: String, attributes: Map<String, String>) {
    append('<').append(tagName)
    attributes.forEach { (name, value) ->
        appendAttribute(tagName, namespace, name, value)
    }
    append('>')
}

/** Consumes a builder synchronously, preserving attribute order. */
internal fun StringBuilder.appendStartTag(
    tagName: String,
    namespace: String,
    builder: AttrsScopeBuilder<*>,
    validate: Boolean,
    formState: FormControlState?,
    nameCache: HtmlAttributeNameCache?,
): Boolean {
    // Class validation runs even when an explicit class attribute overrides the class list.
    if (validate) builder.classes.validateClassTokens()
    val attributes = builder.collect().withFormState(formState).ifEmpty { emptyMap() }
    validateStringHtmlAttributes(attributes, namespace, validate, builder.hydrationProtocolAttributes, nameCache)
    val hasScriptSource = tagName == "script" && attributes.keys.any { it.equals("src", ignoreCase = true) }

    append('<').append(tagName)
    attributes.forEach { (name, value) -> appendAttribute(tagName, namespace, name, value) }
    if ("class" !in attributes && builder.classes.isNotEmpty()) {
        append(" class=\"")
        builder.classes.forEachIndexed { index, token ->
            if (index != 0) append(' ')
            appendEscapedAttribute(token)
        }
        append('"')
    }
    if ("style" !in attributes) {
        builder.styleScopeOrNull?.toStyleAttributeValue()?.let { value ->
            appendAttribute(tagName, namespace, "style", value)
        }
    }
    append('>')
    // Only inline script validation needs attribute information after the builder is reset.
    return hasScriptSource
}

private fun validateStringHtmlAttributes(
    attributes: Map<String, String>,
    namespace: String,
    validate: Boolean,
    hydrationProtocolAttributes: Set<String>,
    nameCache: HtmlAttributeNameCache?,
) {
    for (name in attributes.keys) {
        if (validate) {
            if (nameCache == null) requireValidHtmlAttributeName(name) else nameCache.requireValid(name)
        }
        if (validate && namespace != HtmlNamespace) {
            for (previous in attributes.keys) {
                if (previous == name) break
                require(!previous.hasSameHtmlParserAttributeName(name)) {
                    "Duplicate HTML attribute names \"$previous\" and \"$name\""
                }
            }
        }
        val protocolName = if (name.startsWith(HydrationAttributePrefix, ignoreCase = true))
            HydrationProtocolAttributes.firstOrNull { it.equals(name, ignoreCase = true) } else null
        require(protocolName == null || protocolName in hydrationProtocolAttributes) {
            "Attribute \"$name\" is owned by the Compose hydration protocol"
        }
    }
}

private fun StringBuilder.appendAttribute(tagName: String, namespace: String, name: String, value: String) {
    append(' ').append(name)
    if (namespace != HtmlNamespace || value.isNotEmpty() || '-' in tagName || !name.isHtmlBooleanAttributeName()) {
        append("=\"")
        appendEscapedAttribute(value)
        append('"')
    }
}

/** Closes an element whose content was appended after [contentStart]. */
internal fun StringBuilder.appendEndTag(
    tagName: String,
    namespace: String,
    contentStart: Int,
) {
    if (namespace == HtmlNamespace && tagName == "noscript") {
        // Render fallback HTML for scripting-disabled browsers, but keep it inside noscript
        // when scripting is enabled and the parser treats the entire contents as raw text.
        require(substring(contentStart).rawTextTagIndex("noscript", closing = true) < 0) {
            "String-rendered <noscript> content must not contain a </noscript end tag"
        }
    }
    append("</").append(tagName).append('>')
}

internal fun requireValidHtmlTagName(name: String) {
    require(isValidHtmlTagName(name)) {
        "Invalid HTML tag name: \"$name\""
    }
}

private const val InvalidHtmlAttributeNameCharacters = " \"'/>="

private fun requireValidHtmlAttributeName(name: String) {
    require(
        name.isNotEmpty() &&
            name.none { it.isISOControl() || it in InvalidHtmlAttributeNameCharacters }
    ) {
        "Invalid HTML attribute name: \"$name\""
    }
}

private fun String.hasSameHtmlParserAttributeName(other: String): Boolean {
    if (length != other.length) return false
    for (index in indices) {
        val first = this[index]
        val second = other[index]
        if (first == second) continue
        if (
            !(first in 'A'..'Z' && second.code == first.code + ('a'.code - 'A'.code)) &&
            !(second in 'A'..'Z' && first.code == second.code + ('a'.code - 'A'.code))
        ) {
            return false
        }
    }
    return true
}

// Benchmark-based heuristic: keep short strings on the simpler escaping path.
internal const val HtmlEscapeTableMinLength = 256

// Append ordinary runs in bulk. Besides avoiding an append per character, this lets the
// JVM copy Latin-1 and UTF-16 runs with its native StringBuilder implementation.
// Keep text and attribute scans separate so the inner loop has no mode branch.
internal fun StringBuilder.appendEscapedAttribute(value: String) {
    if (value.length >= HtmlEscapeTableMinLength && appendEscapedHtmlString(value, attribute = true)) return
    if (canAppendUnescapedHtmlString(value, attribute = true)) {
        append(value)
        return
    }
    var start = 0
    for (index in value.indices) {
        val character = value[index]
        val replacement = htmlAttributeEscapeReplacement(character)
        if (replacement != null) {
            appendHtmlRun(value, start, index)
            append(replacement)
            start = index + 1
            // Retry after ampersands only, so suffix searches cover disjoint ranges.
            if (character == '&' && canAppendUnescapedHtmlString(value, attribute = true, startIndex = start)) {
                appendHtmlRun(value, start, value.length)
                return
            }
        }
    }
    if (start == 0) append(value) else appendHtmlRun(value, start, value.length)
}

internal fun StringBuilder.appendEscapedText(value: String) {
    if (value.length >= HtmlEscapeTableMinLength && appendEscapedHtmlString(value, attribute = false)) return
    if (canAppendUnescapedHtmlString(value, attribute = false)) {
        append(value)
        return
    }
    var start = 0
    for (index in value.indices) {
        val character = value[index]
        val replacement = htmlTextEscapeReplacement(character)
        if (replacement != null) {
            appendHtmlRun(value, start, index)
            append(replacement)
            start = index + 1
            // Retry after ampersands only, so suffix searches cover disjoint ranges.
            if (character == '&' && canAppendUnescapedHtmlString(value, attribute = false, startIndex = start)) {
                appendHtmlRun(value, start, value.length)
                return
            }
        }
    }
    if (start == 0) append(value) else appendHtmlRun(value, start, value.length)
}

/** A bulk-copy fast path where the platform supports it. */
internal expect fun canAppendUnescapedHtmlString(value: String, attribute: Boolean, startIndex: Int = 0): Boolean

/** Appends the entire escaped value when a platform has a specialized serializer. */
internal expect fun StringBuilder.appendEscapedHtmlString(value: String, attribute: Boolean): Boolean

/** Per renderer, bounded, and independent of attribute values or protocol ownership. */
internal class HtmlAttributeNameCache {
    // Keep the first 128 short names without eviction bookkeeping.
    // Later names still validate on every use if dynamic names fill the cache.
    private val names = HashSet<String>()
    internal val size: Int get() = names.size
    fun requireValid(name: String) {
        if (name in names) return
        requireValidHtmlAttributeName(name)
        if (name.length <= 128 && names.size < 128) names.add(name)
    }
}

/** Copies a range using the platform's string representation. */
internal expect fun StringBuilder.appendHtmlRun(value: String, start: Int, end: Int)
