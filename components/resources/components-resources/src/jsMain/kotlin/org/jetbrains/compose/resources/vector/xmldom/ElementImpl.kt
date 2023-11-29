package org.jetbrains.compose.resources.vector.xmldom

import org.w3c.dom.Element as DomElement

internal class ElementImpl(val element: DomElement): NodeImpl(element), Element {
    override val textContent: String?
        get() = element.textContent

    override val localName: String
        get() = element.localName

    override val namespaceURI: String
        get() = element.namespaceURI ?: ""

    override fun getAttributeNS(nameSpaceURI: String, localName: String): String =
        element.getAttributeNS(nameSpaceURI, localName) ?: ""

    override fun getAttribute(name: String): String  = element.getAttribute(name) ?: ""
}
