package org.jetbrains.compose.web.dom

import org.w3c.dom.Element

internal actual fun removeAttributesExceptStyleAndClass(node: Element) {
    node.getAttributeNames().forEach { name ->
        when (name) {
            "style", "class" -> {
                // skip style and class here, they're managed in corresponding methods
            }

            else -> node.removeAttribute(name)
        }
    }
}
