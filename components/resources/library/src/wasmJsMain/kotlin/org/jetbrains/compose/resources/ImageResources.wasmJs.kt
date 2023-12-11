package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.ElementImpl
import org.jetbrains.compose.resources.vector.xmldom.MalformedXMLException
import org.w3c.dom.parsing.DOMParser

internal actual fun ByteArray.toXmlElement(): Element {
    val xmlString = decodeToString()
    val xmlDom = DOMParser().parseFromString(xmlString, "application/xml".toJsString())
    val domElement = xmlDom.documentElement ?: throw MalformedXMLException("missing documentElement")
    return ElementImpl(domElement.unsafeCast())
}