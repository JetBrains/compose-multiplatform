/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

// in-memory equivalent of DOM node
internal sealed interface StringHtmlNode {
    fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean)
}

internal fun StringHtmlNode.isEmptyText(): Boolean =
    this is StringHtmlTextNode && text.isEmpty()

internal class StringHtmlElementNode private constructor(
    tagName: String?,
    namespace: String?,
    isRoot: Boolean,
) : StringHtmlNode {
    val namespace: String? = if (isRoot) null else requireNotNull(namespace)
    val tagName: String? = if (isRoot) {
        null
    } else {
        requireNotNull(tagName)
            .also(::requireValidHtmlTagName)
    }
    internal val children: MutableList<StringHtmlNode> = mutableListOf()
    private var attributes: Map<String, String> = emptyMap()

    constructor(
        tagName: String,
        namespace: String = HtmlNamespace,
    ) : this(tagName, namespace, isRoot = false)

    fun updateAttributes(attributes: Map<String, String>) = updateAttributes(
        // Direct node updates have no rendering mode; retain the full validation contract.
        StringHtmlAttributes.from(
            attributes = attributes,
            namespace = requireElementNamespace(),
            validate = true,
            hydrationProtocolAttributes = emptySet(),
        )
    )

    fun updateAttributes(attributes: StringHtmlAttributes) {
        requireElementNamespace()
        // StringHtmlAttributes owns a validated copy; it is immutable after construction.
        this.attributes = attributes.byName
    }

    fun hasAttribute(name: String): Boolean = attributes.containsKey(name)

    fun attribute(name: String): String? = attributes[name]

    fun toHtmlString(hydratable: Boolean = true): String = buildString {
        appendHtmlTo(this, hydratable)
    }

    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val tagName = tagName
        if (tagName == null) { // root
            appendChildrenHtmlTo(builder, hydratable)
            return
        }
        val namespace = requireElementNamespace()

        builder.appendStartTag(tagName, namespace, attributes)
        if (isHtmlVoidElement(tagName, namespace)) return

        val contentStart = builder.length
        // The parent determines text serialization: script/style and other raw-text elements
        // emit validated text without HTML escaping. Ordinary elements escape their text.
        if (isHtmlRawTextElement(tagName, namespace) && children.isNotEmpty()) {
            // Validate together so end tags split across children cannot bypass validation.
            val text = children.joinToString("") { child ->
                when (child) {
                    is StringHtmlTextNode -> child.text
                    is StringHtmlRawTextNode -> child.content.text
                    else -> throw IllegalArgumentException(
                        "String rendering does not support element children inside <$tagName>"
                    )
                }
            }
            val content = RawTextContent.create(tagName, text)
            content.validateAttributes(attributes)
            builder.append(content.text)
        } else {
            // RCDATA decodes escaped text, but treats boundary comments as literal content.
            appendChildrenHtmlTo(builder, hydratable && !isHtmlRcdataElement(tagName, namespace))
        }
        // The HTML parser drops the first LF in pre, textarea, and listing.
        if (isHtmlLeadingNewlineElement(tagName, namespace) &&
            builder.length > contentStart && builder[contentStart] == '\n'
        ) {
            builder.insert(contentStart, '\n')
        }
        builder.appendEndTag(tagName, namespace, contentStart)
    }

    private fun appendChildrenHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val validateTableChildren = namespace == HtmlNamespace
        var previousWasText = false
        for (index in children.indices) {
            val child = children[index]
            if (child.isEmptyText()) continue
            if (validateTableChildren) requireHtmlParserStableTableChild(tagName, child)
            val isText = child is StringHtmlTextNode
            if (hydratable && previousWasText && isText) builder.appendHydrationTextBoundaryMarker()
            child.appendHtmlTo(builder, hydratable)
            previousWasText = isText
        }
    }

    private fun requireElementNamespace(): String =
        checkNotNull(namespace) { "The string-rendering root has no element namespace" }

    companion object {
        fun root(): StringHtmlElementNode = StringHtmlElementNode(
            tagName = null,
            namespace = null,
            isRoot = true,
        )
    }
}

internal class StringHtmlTextNode(
    var text: String
) : StringHtmlNode {
    fun toHtmlString(hydratable: Boolean = true): String = buildString {
        appendHtmlTo(this, hydratable)
    }

    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        builder.appendEscapedText(text)
    }
}

internal class StringHtmlRawTextNode(
    var content: RawTextContent,
) : StringHtmlNode {
    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        builder.append(content.text)
    }
}
