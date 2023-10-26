package org.jetbrains.compose.resources.vector.xmldom

/**
 * XML DOM Node.
 */
internal interface Node {
    val textContent: String?

    val nodeName: String
    val localName: String

    val childNodes: NodeList
    val namespaceURI: String

    fun lookupPrefix(namespaceURI: String): String
}