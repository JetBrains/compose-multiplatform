package org.jetbrains.compose.resources.vector.xmldom

/**
 * XML DOM Element.
 */
interface Element: Node {

    fun getAttributeNS(nameSpaceURI: String, localName: String): String

    fun getAttribute(name: String): String
}