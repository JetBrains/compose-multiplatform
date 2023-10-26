package org.jetbrains.compose.resources.vector.xmldom

import org.w3c.dom.Element as DomElement

internal class ElementImpl(val element: DomElement): NodeImpl(element), Element {
    override fun getAttributeNS(nameSpaceURI: String, localName: String): String =
        element.getAttributeNS(nameSpaceURI, localName)

    override fun getAttribute(name: String): String  = element.getAttribute(name)
}