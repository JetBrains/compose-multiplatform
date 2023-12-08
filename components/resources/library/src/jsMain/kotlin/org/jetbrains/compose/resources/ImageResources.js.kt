package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.*
import org.w3c.dom.parsing.DOMParser

internal actual fun ByteArray.toXmlElement(): Element {
    val xmlString = decodeToString()
    val xmlDom = DOMParser().parseFromString(xmlString, "application/xml")
    val domElement = xmlDom.documentElement ?: throw MalformedXMLException("missing documentElement")
    return ElementImpl(domElement)
}